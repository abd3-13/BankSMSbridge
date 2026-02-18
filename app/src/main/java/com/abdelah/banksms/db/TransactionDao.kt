package com.abdelah.banksms.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Query("SELECT * FROM transactions WHERE status = :status ORDER BY dateTime ASC")
    suspend fun getByStatus(status: String = "PENDING"): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE status IN (:statuses) ORDER BY dateTime ASC")
    suspend fun getByStatuses(statuses: List<String>): List<TransactionEntity>

    @Query("UPDATE transactions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE status = :status")
    suspend fun countByStatus(status: String): Int

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
