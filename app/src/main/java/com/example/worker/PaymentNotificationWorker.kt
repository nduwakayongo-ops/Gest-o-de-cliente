package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.model.ClienteEntity
import com.example.data.model.CreditoEntity
import com.example.ui.components.KwanzaFormatters
import java.util.Calendar
import java.util.concurrent.TimeUnit

class PaymentNotificationWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            createNotificationChannel(appContext)
            val db = AppDatabase.getDatabase(appContext)
            val clientes: List<ClienteEntity> = db.clienteDao().getAllClientes()
            val creditos: List<CreditoEntity> = db.creditoDao().getAllCreditos()

            val now = System.currentTimeMillis()
            val limit5Days = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, 5)
            }.timeInMillis

            val alertList = creditos.mapNotNull { credito ->
                val cliente = clientes.find { it.id == credito.clienteId }
                if (cliente != null && credito.estado != "Liquidado") {
                    val isOverdue = credito.estado == "Em atraso" || credito.diasEmAtraso > 0
                    val isUpcoming = credito.proximoVencimento in now..limit5Days
                    if (isOverdue || isUpcoming) Pair(cliente, credito) else null
                } else null
            }

            if (alertList.isNotEmpty()) {
                val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val intent = Intent(appContext, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingIntent = PendingIntent.getActivity(
                    appContext,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                alertList.take(5).forEachIndexed { idx, pair ->
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
                        "Pagamento em atraso há $days dias no banco ${cliente.banco}. Prestação: ${KwanzaFormatters.formatAOA(credito.prestacaoMensal)}"
                    } else {
                        "Prestação vence a ${KwanzaFormatters.formatDate(credito.proximoVencimento)} no banco ${cliente.banco}. Valor: ${KwanzaFormatters.formatAOA(credito.prestacaoMensal)}"
                    }

                    val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle(title)
                        .setContentText(content)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)

                    notificationManager.notify(3000 + idx, builder.build())
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        const val CHANNEL_ID = "lembretes_pagamentos_worker"
        const val CHANNEL_NAME = "Agendamento Diário WorkManager"
        const val WORK_TAG_8AM = "work_payment_notification_8am"
        const val WORK_TAG_17PM = "work_payment_notification_17pm"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Verificação automática dos prazos de vencimento às 08:00 e 17:00."
                    enableVibration(true)
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }

        fun scheduleDailyWorker(context: Context) {
            createNotificationChannel(context)
            val workManager = WorkManager.getInstance(context)

            // Schedule for 08:00 AM
            scheduleSpecificTimeWorker(workManager, WORK_TAG_8AM, 8, 0)

            // Schedule for 17:00 PM (5:00 PM)
            scheduleSpecificTimeWorker(workManager, WORK_TAG_17PM, 17, 0)
        }

        private fun scheduleSpecificTimeWorker(
            workManager: WorkManager,
            workTag: String,
            targetHour: Int,
            targetMinute: Int
        ) {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, targetHour)
                set(Calendar.MINUTE, targetMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now)) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val initialDelayMillis = target.timeInMillis - now.timeInMillis

            val dailyWorkRequest = PeriodicWorkRequestBuilder<PaymentNotificationWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
                .addTag(workTag)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .build()

            workManager.enqueueUniquePeriodicWork(
                workTag,
                ExistingPeriodicWorkPolicy.UPDATE,
                dailyWorkRequest
            )
        }
    }
}
