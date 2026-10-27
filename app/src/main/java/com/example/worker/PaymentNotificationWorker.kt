package com.example.worker

import android.content.Context
import androidx.work.*
import com.example.util.MensagensQueueManager
import com.example.util.NotificationSchedulerHelper
import java.util.Calendar
import java.util.concurrent.TimeUnit

class PaymentNotificationWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // 1. Trigger the single source of truth notification dispatch
            NotificationSchedulerHelper.executeNotificationDispatch(appContext)

            // 2. Automatically process offline message queue if network is available
            MensagensQueueManager.processarFilaQuandoConectado(appContext)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        const val WORK_TAG_8AM = "work_payment_notification_8am"
        const val WORK_TAG_16PM = "work_payment_notification_16pm"

        fun scheduleDailyWorker(context: Context) {
            NotificationSchedulerHelper.createNotificationChannel(context)
            val workManager = WorkManager.getInstance(context)

            // Cancel any old 17PM worker tag if exists
            workManager.cancelAllWorkByTag("work_payment_notification_17pm")

            // Schedule for 08:00 AM
            scheduleSpecificTimeWorker(workManager, WORK_TAG_8AM, 8, 0)

            // Schedule for 16:00 PM (4:00 PM)
            scheduleSpecificTimeWorker(workManager, WORK_TAG_16PM, 16, 0)
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
