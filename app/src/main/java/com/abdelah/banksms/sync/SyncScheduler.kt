package com.abdelah.banksms.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SyncScheduler {
    private const val UNIQUE_SYNC_WORK = "transaction_sync_once"
    private const val UNIQUE_PERIODIC_SYNC_WORK = "transaction_sync_periodic"

    private val networkConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun enqueueImmediate(context: Context) {
        val work = OneTimeWorkRequestBuilder<TransactionSyncWorker>()
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_SYNC_WORK,
            ExistingWorkPolicy.REPLACE,
            work
        )
    }

    fun ensurePeriodic(context: Context) {
        val periodic = PeriodicWorkRequestBuilder<TransactionSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_SYNC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            periodic
        )
    }
}
