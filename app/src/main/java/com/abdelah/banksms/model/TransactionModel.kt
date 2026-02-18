package com.abdelah.banksms.model

data class ParsedTransaction(
    val bank: String,
    val type: String,          // debit | credit
    val amount: Double,
    val currency: String = "ETB",
    val dateTime: String?,
    val description: String?,
    val reference: String?,
    val rawMessage: String
)
