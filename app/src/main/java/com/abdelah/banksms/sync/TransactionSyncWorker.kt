package com.abdelah.banksms.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.abdelah.banksms.db.AppDatabase

class TransactionSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!isNetworkAvailable()) {
            Log.w("BankSMSSync", "No network, retrying sync later")
            return Result.retry()
        }

        val db = AppDatabase.getDatabase(applicationContext)
        val repository = TransactionSyncRepository(
            transactionDao = db.transactionDao(),
            fireflyClient = FireflyClient(
                baseUrl = SyncConfig.getBaseUrl(applicationContext),
                apiToken = SyncConfig.getToken(applicationContext)
            )
        )

        return try {
            val outcome = repository.syncPendingAndFailed()
            if (outcome.skipped) {
                Log.w("BankSMSSync", "Sync skipped: Firefly config missing")
                Result.success()
            } else if (outcome.failed > 0) {
                Log.w("BankSMSSync", "Partial sync: sent=${outcome.successful}, failed=${outcome.failed}")
                Result.retry()
            } else {
                Log.i("BankSMSSync", "Sync successful: sent=${outcome.successful}")
                Result.success()
            }
        } catch (e: Exception) {
            Log.e("BankSMSSync", "Sync worker crashed", e)
            Result.retry()
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
