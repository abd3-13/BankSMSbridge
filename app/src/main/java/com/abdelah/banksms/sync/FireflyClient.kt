package com.abdelah.banksms.sync

import com.abdelah.banksms.db.TransactionEntity
import com.abdelah.banksms.logging.AppLogger
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class FireflyClient(
    private val baseUrl: String,
    private val apiToken: String
) {

    fun canSync(): Boolean {
        return baseUrl.isNotBlank() && apiToken.isNotBlank()
    }

    fun postTransaction(transaction: TransactionEntity): Boolean {
        if (!canSync()) return false

        val normalizedBaseUrl = baseUrl.trim().trimEnd('/')
        val endpoint = "$normalizedBaseUrl/api/v1/transactions"
        val payload = buildPayload(transaction)

        return try {
            val connection = URL(endpoint).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.doOutput = true
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $apiToken")

            connection.outputStream.use { output ->
                output.write(payload.toByteArray(Charsets.UTF_8))
            }

            val code = connection.responseCode
            code in 200..299
        } catch (e: Exception) {
            AppLogger.e("BankSMSSync", "Failed posting transaction ${transaction.id}", e)
            false
        }
    }

    private fun buildPayload(transaction: TransactionEntity): String {
        // Firefly transaction type src https://api-docs.firefly-iii.org/#/transactions/storeTransaction
        val type = if (transaction.type.equals("credit", true)) "deposit" else "withdrawal"
        

        val attributes = JSONObject().apply {
            put("type", type)
            put("date", toIsoTimestamp(transaction.dateTime))
            put("amount", String.format(java.util.Locale.US, "%.2f", transaction.amount))
            put("description", transaction.description ?: "${transaction.bank} ${transaction.type}")
            put("source_name", sourceNameFor(type, transaction.bank))
            put("destination_name", destinationNameFor(type, transaction.bank))
            put("currency_code", transaction.currency)
            put("external_id", transaction.reference ?: "tx-${transaction.id}")
            put("notes", transaction.rawMessage)
        }

        val root = JSONObject().apply {
            put("error_if_duplicate_hash", true)
            put("apply_rules", false)
            put("transactions", JSONArray().put(attributes))
        }

        return root.toString()
    }

    private fun toIsoTimestamp(dbDate: String): String {
        val normalized = dbDate.replace(' ', 'T')
        return if (normalized.contains("+")) normalized else "${normalized}+00:00"
    }

    private fun sourceNameFor(type: String, bank: String): String {
        return if (type == "deposit") bank else "Cash"
    }

    private fun destinationNameFor(type: String, bank: String): String {
        return if (type == "deposit") "Cash" else bank
    }
}
