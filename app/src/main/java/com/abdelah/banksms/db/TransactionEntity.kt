package com.abdelah.banksms.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bank: String,
    val type: String,          // debit / credit
    val amount: Double,
    val currency: String,
    val dateTime: String,
    val description: String?,
    val reference: String?,
    val rawMessage: String,
    val status: String = "PENDING" // PENDING, SENT, FAILED
)
