package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.model.ClienteEntity
import com.example.data.model.CreditoEntity
import com.example.data.model.PagamentoEntity
import com.example.ui.components.KwanzaFormatters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class PaymentNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Re-arm alarms for both times (08:00 and 16:00) so they run permanently even when closed
                NotificationSchedulerHelper.scheduleDailyAlarms(context)
                // Schedule all individual credit alarms (5-day warning & due date)
                NotificationSchedulerHelper.rescheduleAllAlarmsAndWorkers(context)
                // Trigger notification check with exact dates for all active clients
                NotificationSchedulerHelper.executeNotificationDispatch(context)
                // Also process any queued SMS/WhatsApp messages if network is available
                MensagensQueueManager.processarFilaQuandoConectado(context)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}

object NotificationSchedulerHelper {

    const val CHANNEL_ID = "lembretes_pagamentos_v4"
    private const val CHANNEL_NAME = "Alertas de Reembolso e Vencimento (Mbuli)"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Lembretes 5 dias antes do vencimento, no dia e cobrança diária de atraso com juros de mora (4%/dia sobre juros contratuais) até liquidação total."
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Reschedules daily recurring checks and individual alarms on reboot or app start.
     */
    fun rescheduleAllAlarmsAndWorkers(context: Context) {
        scheduleDailyAlarms(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val creditos = db.creditoDao().getAllCreditos()
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return@launch

                for (credito in creditos) {
                    if (credito.estado == "Liquidado" || credito.prestacoesPagas >= credito.quantidadePrestacoes) {
                        cancelCreditAlarms(context, credito.id)
                    } else {
                        scheduleIndividualCreditAlarms(context, alarmManager, credito)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Schedules individual AlarmManager alerts for:
     * 1. Exactly 5 days before due date (at 09:00 AM)
     * 2. Exactly on the due date (at 09:00 AM)
     */
    fun scheduleIndividualCreditAlarms(
        context: Context,
        alarmManager: AlarmManager,
        credito: CreditoEntity
    ) {
        if (credito.proximoVencimento <= 0L || credito.estado == "Liquidado" || credito.prestacoesPagas >= credito.quantidadePrestacoes) {
            return
        }

        // 1. Alarm 5 days before due date at 09:00
        val cal5DaysBefore = Calendar.getInstance().apply {
            timeInMillis = credito.proximoVencimento
            add(Calendar.DAY_OF_YEAR, -5)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (cal5DaysBefore.timeInMillis > System.currentTimeMillis()) {
            val reqCode5Days = (50000 + credito.id * 10 + 5).toInt()
            setExactOrInexactAlarm(context, alarmManager, cal5DaysBefore.timeInMillis, reqCode5Days)
        }

        // 2. Alarm on due date at 09:00
        val calDueDate = Calendar.getInstance().apply {
            timeInMillis = credito.proximoVencimento
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (calDueDate.timeInMillis > System.currentTimeMillis()) {
            val reqCodeDueDate = (50000 + credito.id * 10 + 1).toInt()
            setExactOrInexactAlarm(context, alarmManager, calDueDate.timeInMillis, reqCodeDueDate)
        }
    }

    private fun setExactOrInexactAlarm(
        context: Context,
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        requestCode: Int
    ) {
        val intent = Intent(context, PaymentNotificationReceiver::class.java).apply {
            action = "com.example.ACTION_PAYMENT_ALARM_TICK"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Cancels individual alarms for a credit when paid or liquidated.
     */
    fun cancelCreditAlarms(context: Context, creditoId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val reqCode5Days = (50000 + creditoId * 10 + 5).toInt()
        val reqCodeDueDate = (50000 + creditoId * 10 + 1).toInt()

        val intent = Intent(context, PaymentNotificationReceiver::class.java)

        val pi5 = PendingIntent.getBroadcast(
            context,
            reqCode5Days,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pi5?.let {
            alarmManager.cancel(it)
            it.cancel()
        }

        val piDue = PendingIntent.getBroadcast(
            context,
            reqCodeDueDate,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        piDue?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    /**
     * Interruption of reminders upon payment confirmation (Section 5):
     * Cancels notification and alarms for liquidated obligation.
     * If more installments remain, schedules for next installment.
     */
    fun onPagamentoConfirmado(context: Context, creditoId: Long, isLiquidadoTotal: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Cancel notification for this client
        notificationManager.cancel(2000 + creditoId.toInt())
        notificationManager.cancel(3000 + creditoId.toInt())

        // Cancel alarms for current due date
        cancelCreditAlarms(context, creditoId)

        if (!isLiquidadoTotal) {
            // Re-arm alarms for next installment
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val credito = db.creditoDao().getCreditoById(creditoId)
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                    if (credito != null && alarmManager != null && credito.estado != "Liquidado") {
                        scheduleIndividualCreditAlarms(context, alarmManager, credito)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Schedules daily alarm reminders at 08:00 and 16:00 (twice daily, even with app closed)
     */
    fun scheduleDailyAlarms(context: Context) {
        createNotificationChannel(context)

        // WorkManager periodic worker scheduling
        com.example.worker.PaymentNotificationWorker.scheduleDailyWorker(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Schedule for 08:00 AM
        scheduleAlarmAtTime(context, alarmManager, 8, 0, 1001)

        // Schedule for 16:00 PM (4:00 PM)
        scheduleAlarmAtTime(context, alarmManager, 16, 0, 1002)
    }

    private fun scheduleAlarmAtTime(context: Context, alarmManager: AlarmManager, hour: Int, minute: Int, requestCode: Int) {
        val intent = Intent(context, PaymentNotificationReceiver::class.java).apply {
            action = "com.example.ACTION_DAILY_CHECK"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        cal.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        cal.timeInMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    cal.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    cal.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            try {
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    cal.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            } catch (err: Exception) {
                err.printStackTrace()
            }
        }
    }

    fun checkAndTriggerNotifications(context: Context) {
        createNotificationChannel(context)
        CoroutineScope(Dispatchers.IO).launch {
            executeNotificationDispatch(context)
        }
    }

    /**
     * Checks database and sends notifications for all clients with exact repayment dates,
     * tracking every client month by month until total credit liquidation.
     * Complies strictly with Sections 2.1, 2.2, 2.3, 5, and 6.
     */
    suspend fun executeNotificationDispatch(context: Context) {
        createNotificationChannel(context)
        try {
            val db = AppDatabase.getDatabase(context)
            val config = db.configuracaoDao().getConfiguracao()

            // Check if internal notification channel is authorized
            if (config?.canalNotificacaoInterna == false) {
                return
            }

            val clientes: List<ClienteEntity> = db.clienteDao().getAllClientes()
            val creditos: List<CreditoEntity> = db.creditoDao().getAllCreditos()
            val allPagamentos: List<PagamentoEntity> = db.pagamentoDao().getAllPagamentos()

            val activeList: List<Pair<ClienteEntity, CreditoEntity>> = creditos.mapNotNull { credito ->
                val cliente = clientes.find { it.id == credito.clienteId }
                if (cliente != null && credito.estado != "Liquidado" && credito.prestacoesPagas < credito.quantidadePrestacoes) {
                    Pair(cliente, credito)
                } else null
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Dismiss notifications for any liquidated credits
            creditos.filter { it.estado == "Liquidado" || it.prestacoesPagas >= it.quantidadePrestacoes }.forEach {
                notificationManager.cancel(2000 + it.id.toInt())
                notificationManager.cancel(3000 + it.id.toInt())
            }

            if (activeList.isEmpty()) {
                val emptyBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("✅ MBULI-MICROCRÉDITO: CARTEIRA EM DIA")
                    .setContentText("Nenhum reembolso pendente. Todos os créditos estão devidamente liquidados.")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)

                notificationManager.notify(999, emptyBuilder.build())
                return
            }

            // 1. Master Portfolio Summary Notification
            val totalAtivos = activeList.size
            val emAtrasoCount = activeList.count { it.second.estado == "Em atraso" || it.second.diasEmAtraso > 0 }
            val masterBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("📋 ACOMPANHAMENTO DIÁRIO • NDUWA KAYONGO")
                .setContentText("$totalAtivos créditos ativos em acompanhamento ($emAtrasoCount com mora ativa).")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("Mbuli-Microcrédito: Acompanhamento contínuo de $totalAtivos clientes com datas exatas de reembolso. Lembretes automáticos 5 dias antes, no vencimento e diários até à confirmação do pagamento.")
                        .setSummaryText("Gestor: Nduwa Kayongo")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            notificationManager.notify(1000, masterBuilder.build())

            // 2. Individual Notifications for Each Client with Exact Dates, Capital in debt, Contractual Interest, and Formula
            activeList.forEach { (cliente, credito) ->
                val shortName = KwanzaFormatters.formatPrimeiroEUltimoNome(cliente.nome)
                val dataFormatada = KwanzaFormatters.formatDate(credito.proximoVencimento)
                val prestacaoFormatada = KwanzaFormatters.formatAOA(credito.prestacaoMensal)
                val prestacaoAtual = credito.prestacoesPagas + 1
                val totalPrestacoes = credito.quantidadePrestacoes
                val contratoId = cliente.numeroInterno.ifBlank { "CTR-${credito.id}" }

                // Calculate calendar-day difference normalized to midnight
                val daysUntil = calculateCalendarDaysUntil(credito.proximoVencimento)

                val pagamentosDoCredito = allPagamentos.filter { it.creditoId == credito.id }

                val isOverdue = daysUntil < 0 || credito.estado == "Em atraso" || credito.diasEmAtraso > 0
                val isWithinReminderWindow = daysUntil <= 5 || isOverdue

                // Section 2.1: First reminder is scheduled for 5 days before due date,
                // and daily notifications are sent starting from those 5 days until payment is confirmed.
                if (!isWithinReminderWindow) {
                    return@forEach
                }

                // Capital em dívida (Secção 2.2)
                val capitalMensal = if (totalPrestacoes > 0) credito.valorDesembolsado / totalPrestacoes else 0.0
                val capitalAmortizado = capitalMensal * credito.prestacoesPagas
                val capitalEmDivida = (credito.valorDesembolsado - capitalAmortizado).coerceAtLeast(0.0)
                val capitalEmDividaStr = KwanzaFormatters.formatAOA(capitalEmDivida)

                // Juros contratuais aplicáveis (Secção 2.2)
                val jurosMensais = credito.valorDesembolsado * (credito.taxaPercentual / 100.0)
                val jurosTotais = jurosMensais * totalPrestacoes
                val jurosContratuaisStr = "${KwanzaFormatters.formatAOA(jurosTotais)} (${credito.taxaPercentual}%/mês)"

                val title: String
                val shortSummary: String
                val officialMessage: String

                if (isOverdue) {
                    val diasAtraso = if (credito.diasEmAtraso > 0) credito.diasEmAtraso else -daysUntil
                    val mora = MoraCalculatorHelper.calcular(
                        credito = credito,
                        pagamentos = pagamentosDoCredito,
                        diasAtrasoManual = diasAtraso
                    )

                    val moraDiariaStr = KwanzaFormatters.formatAOA(mora.jurosMoraDiarios)
                    val moraAcumuladaStr = KwanzaFormatters.formatAOA(mora.jurosMoraAcumulados)
                    val totalDevidoStr = KwanzaFormatters.formatAOA(mora.totalExigivel)

                    title = "PRESTAÇÃO EM ATRASO: $shortName"
                    shortSummary = "Vencida: $dataFormatada | Atraso: $diasAtraso d | Mora: $moraAcumuladaStr | Total: $totalDevidoStr"
                    officialMessage = "PRESTAÇÃO EM ATRASO: O cliente $shortName possui uma prestação vencida de $prestacaoFormatada. " +
                            "Dias de atraso: $diasAtraso. Mora acumulada: $moraAcumuladaStr.\n" +
                            "• Contrato: $contratoId (Prestação $prestacaoAtual de $totalPrestacoes)\n" +
                            "• Vencimento: $dataFormatada (Atraso: $diasAtraso dias)\n" +
                            "• Capital em dívida: $capitalEmDividaStr\n" +
                            "• Juros contratuais: $jurosContratuaisStr\n" +
                            "• Mora diária (4% juros contratuais): $moraDiariaStr/dia\n" +
                            "• Valor total actualmente exigível: $totalDevidoStr\n" +
                            "• Estado do pagamento: Em atraso. Toque para abrir a ficha do cliente e acompanhar a cobrança."
                } else if (daysUntil == 0) {
                    val mora = MoraCalculatorHelper.calcular(credito = credito, pagamentos = pagamentosDoCredito, diasAtrasoManual = 0)
                    val totalDevidoStr = KwanzaFormatters.formatAOA(mora.totalExigivel)

                    title = "LEMBRETE DE REEMBOLSO: $shortName (Vence Hoje)"
                    shortSummary = "Vence HOJE ($dataFormatada) | Valor: $prestacaoFormatada (Prestação $prestacaoAtual/$totalPrestacoes)"
                    officialMessage = "LEMBRETE DE REEMBOLSO: O cliente $shortName tem uma prestação de $prestacaoFormatada com vencimento HOJE ($dataFormatada). " +
                            "Consulte a carteira e acompanhe a confirmação do pagamento.\n" +
                            "• Contrato: $contratoId (Prestação $prestacaoAtual de $totalPrestacoes)\n" +
                            "• Capital em dívida: $capitalEmDividaStr\n" +
                            "• Juros contratuais: $jurosContratuaisStr\n" +
                            "• Data de vencimento: $dataFormatada (HOJE)\n" +
                            "• Valor total actualmente exigível: $totalDevidoStr\n" +
                            "• Estado do pagamento: Pendente de confirmação. Toque para abrir a ficha do cliente."
                } else {
                    val mora = MoraCalculatorHelper.calcular(credito = credito, pagamentos = pagamentosDoCredito, diasAtrasoManual = 0)
                    val totalDevidoStr = KwanzaFormatters.formatAOA(mora.totalExigivel)

                    title = "LEMBRETE DE REEMBOLSO: $shortName (Faltam $daysUntil dias)"
                    shortSummary = "Vence em $daysUntil dias ($dataFormatada) | Prestação: $prestacaoFormatada"
                    officialMessage = "LEMBRETE DE REEMBOLSO: O cliente $shortName tem uma prestação de $prestacaoFormatada com vencimento em $dataFormatada. " +
                            "Faltam $daysUntil dias. Consulte a carteira e acompanhe a confirmação do pagamento.\n" +
                            "• Contrato: $contratoId (Prestação $prestacaoAtual de $totalPrestacoes)\n" +
                            "• Capital em dívida: $capitalEmDividaStr\n" +
                            "• Juros contratuais: $jurosContratuaisStr\n" +
                            "• Data de vencimento: $dataFormatada (Faltam $daysUntil dias)\n" +
                            "• Valor total actualmente exigível: $totalDevidoStr\n" +
                            "• Estado do pagamento: Pendente. Toque para abrir a ficha do cliente."
                }

                val clientIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("CLIENTE_ID", cliente.id)
                }
                val clientPendingIntent = PendingIntent.getActivity(
                    context,
                    credito.id.toInt(),
                    clientIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(title)
                    .setContentText(shortSummary)
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText(officialMessage)
                            .setSummaryText("Contrato $contratoId • Prestação $prestacaoAtual/$totalPrestacoes")
                    )
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setContentIntent(clientPendingIntent)
                    .addAction(
                        R.drawable.ic_launcher_foreground,
                        "Ver Detalhes do Cliente",
                        clientPendingIntent
                    )

                notificationManager.notify(2000 + credito.id.toInt(), builder.build())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Calculates days until due date normalized to calendar midnights (00:00:00).
     * Example: Due on 20 Nov, today 15 Nov -> returns exactly 5.
     */
    fun calculateCalendarDaysUntil(dueDateMillis: Long): Int {
        if (dueDateMillis <= 0L) return 0

        val calToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val calDue = Calendar.getInstance().apply {
            timeInMillis = dueDateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val diffMillis = calDue.timeInMillis - calToday.timeInMillis
        return (diffMillis / (1000 * 60 * 60 * 24)).toInt()
    }
}
