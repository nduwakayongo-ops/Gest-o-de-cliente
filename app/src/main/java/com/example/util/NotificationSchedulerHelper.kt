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
import com.example.ui.components.KwanzaFormatters
import com.example.data.model.ClienteEntity
import com.example.data.model.CreditoEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class PaymentNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Trigger notification check
        NotificationSchedulerHelper.checkAndTriggerNotifications(context)
    }
}

object NotificationSchedulerHelper {

    private const val CHANNEL_ID = "lembretes_pagamentos_v2"
    private const val CHANNEL_NAME = "Alertas de Pagamento (08:00 & 17:00)"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Lembretes diários às 08:00 e 17:00 para pagamentos em atraso ou prestação a vencer em 5 dias."
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleDailyAlarms(context: Context) {
        createNotificationChannel(context)

        // WorkManager daily worker scheduling
        com.example.worker.PaymentNotificationWorker.scheduleDailyWorker(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Schedule for 8:00 AM
        scheduleAlarmAtTime(context, alarmManager, 8, 0, 1001)

        // Schedule for 17:00 PM (5:00 PM)
        scheduleAlarmAtTime(context, alarmManager, 17, 0, 1002)
    }

    private fun scheduleAlarmAtTime(context: Context, alarmManager: AlarmManager, hour: Int, minute: Int, requestCode: Int) {
        val intent = Intent(context, PaymentNotificationReceiver::class.java)
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
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun checkAndTriggerNotifications(context: Context) {
        createNotificationChannel(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val clientes: List<ClienteEntity> = db.clienteDao().getAllClientes()
                val creditos: List<CreditoEntity> = db.creditoDao().getAllCreditos()

                val now = System.currentTimeMillis()
                val limit5Days = Calendar.getInstance().apply {
                    timeInMillis = now
                    add(Calendar.DAY_OF_YEAR, 5)
                }.timeInMillis

                val upcomingOrOverdueList: List<Pair<ClienteEntity, CreditoEntity>> = creditos.mapNotNull { credito ->
                    val cliente = clientes.find { it.id == credito.clienteId }
                    if (cliente != null && credito.estado != "Liquidado") {
                        val isOverdue = credito.estado == "Em atraso" || credito.diasEmAtraso > 0
                        val isUpcoming5Days = credito.proximoVencimento in now..limit5Days
                        if (isOverdue || isUpcoming5Days) {
                            Pair(cliente, credito)
                        } else null
                    } else null
                }

                if (upcomingOrOverdueList.isNotEmpty()) {
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        Intent(context, MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    upcomingOrOverdueList.take(5).forEachIndexed { idx, pair ->
                        val cliente = pair.first
                        val credito = pair.second
                        val isOverdue = credito.estado == "Em atraso" || credito.diasEmAtraso > 0
                        val days = KwanzaFormatters.calculateDaysUntilOrOverdue(credito.proximoVencimento)

                        val title = if (isOverdue) {
                            "⚠️ ALERTA DE ATRASO: ${cliente.nome}"
                        } else {
                            "🔔 VENCIMENTO EM 5 DIAS: ${cliente.nome}"
                        }

                        val content = if (isOverdue) {
                            "Pagamento em atraso há $days dias. Prestação: ${KwanzaFormatters.formatAOA(credito.prestacaoMensal)}"
                        } else {
                            "Prestação vence dia ${KwanzaFormatters.formatDate(credito.proximoVencimento)}. Valor: ${KwanzaFormatters.formatAOA(credito.prestacaoMensal)}"
                        }

                        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_launcher_foreground)
                            .setContentTitle(title)
                            .setContentText(content)
                            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setDefaults(NotificationCompat.DEFAULT_ALL)
                            .setAutoCancel(true)
                            .setContentIntent(pendingIntent)

                        notificationManager.notify(2000 + idx, builder.build())
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
