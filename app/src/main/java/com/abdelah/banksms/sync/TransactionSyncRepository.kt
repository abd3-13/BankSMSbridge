package com.abdelah.banksms.sync

import com.abdelah.banksms.db.TransactionDao

class TransactionSyncRepository(
    private val transactionDao: TransactionDao,
    private val fireflyClient: FireflyClient
) {

    suspend fun syncPendingAndFailed(): SyncOutcome {
        if (!fireflyClient.canSync()) {
            return SyncOutcome(successful = 0, failed = 0, skipped = true)
        }

        val pending = transactionDao.getByStatuses(listOf("PENDING", "FAILED"))
        if (pending.isEmpty()) {
            return SyncOutcome(successful = 0, failed = 0, skipped = false)
        }

        var successfulCount = 0
        var failedCount = 0

        pending.forEach { tx ->
            val sent = fireflyClient.postTransaction(tx)
            if (sent) {
                transactionDao.updateStatus(tx.id, "SENT")
                successfulCount++
            } else {
                transactionDao.updateStatus(tx.id, "FAILED")
                failedCount++
            }
        }

        return SyncOutcome(successful = successfulCount, failed = failedCount, skipped = false)
    }
}

data class SyncOutcome(
    val successful: Int,
    val failed: Int,
    val skipped: Boolean
)
