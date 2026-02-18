package com.abdelah.banksms.parser

import com.abdelah.banksms.model.ParsedTransaction
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SmsParsePlugin(
    val id: String,
    val bankName: String,
    val senderContains: String,
    val messageHints: List<String>,
    val debitRegex: String,
    val creditRegex: String,
    val referenceRegex: String? = null,
    val dateRegex: String? = null,
    val debitDescription: String = "Debit",
    val creditDescription: String = "Credit",
    val currency: String = "ETB"
) {
    fun matches(sender: String?, message: String): Boolean {
        val senderMatches = sender?.contains(senderContains, ignoreCase = true) == true
        val hintMatches = messageHints.any { hint -> message.contains(hint, ignoreCase = true) }
        return senderMatches || hintMatches
    }

    fun parse(message: String, receivedTimeMillis: Long): ParsedTransaction? {
        val debitMatch = runCatching { Regex(debitRegex, RegexOption.IGNORE_CASE).find(message) }.getOrNull()
        val creditMatch = runCatching { Regex(creditRegex, RegexOption.IGNORE_CASE).find(message) }.getOrNull()
        val txType = when {
            debitMatch != null -> "debit"
            creditMatch != null -> "credit"
            else -> return null
        }

        val amount = (debitMatch ?: creditMatch)
            ?.groupValues
            ?.getOrNull(1)
            ?.replace(",", "")
            ?.toDoubleOrNull()
            ?: return null

        val normalizedDateTime = dateRegex
            ?.let { runCatching { Regex(it, RegexOption.IGNORE_CASE).find(message) }.getOrNull() }
            ?.groupValues
            ?.getOrNull(1)
            ?: SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(receivedTimeMillis))

        val reference = referenceRegex
            ?.let { regex -> runCatching { Regex(regex, RegexOption.IGNORE_CASE).find(message) }.getOrNull() }
            ?.groupValues
            ?.getOrNull(1)

        return ParsedTransaction(
            bank = bankName,
            type = txType,
            amount = amount,
            currency = currency,
            dateTime = normalizedDateTime,
            description = if (txType == "debit") debitDescription else creditDescription,
            reference = reference,
            rawMessage = message
        )
    }

    fun toJsonObject(): JSONObject = JSONObject()
        .put("id", id)
        .put("bankName", bankName)
        .put("senderContains", senderContains)
        .put("messageHints", JSONArray(messageHints))
        .put("debitRegex", debitRegex)
        .put("creditRegex", creditRegex)
        .put("referenceRegex", referenceRegex)
        .put("dateRegex", dateRegex)
        .put("debitDescription", debitDescription)
        .put("creditDescription", creditDescription)
        .put("currency", currency)

    companion object {
        fun fromJsonObject(obj: JSONObject): SmsParsePlugin? {
            val id = obj.optString("id").trim()
            val bankName = obj.optString("bankName").trim()
            val senderContains = obj.optString("senderContains").trim()
            val debitRegex = obj.optString("debitRegex").trim()
            val creditRegex = obj.optString("creditRegex").trim()

            if (id.isBlank() || bankName.isBlank() || senderContains.isBlank() || debitRegex.isBlank() || creditRegex.isBlank()) {
                return null
            }

            val hintsArray = obj.optJSONArray("messageHints") ?: JSONArray()
            val hints = buildList {
                for (index in 0 until hintsArray.length()) {
                    val value = hintsArray.optString(index).trim()
                    if (value.isNotBlank()) {
                        add(value)
                    }
                }
            }

            return SmsParsePlugin(
                id = id,
                bankName = bankName,
                senderContains = senderContains,
                messageHints = hints,
                debitRegex = debitRegex,
                creditRegex = creditRegex,
                referenceRegex = obj.optString("referenceRegex").takeIf { it.isNotBlank() },
                dateRegex = obj.optString("dateRegex").takeIf { it.isNotBlank() },
                debitDescription = obj.optString("debitDescription").ifBlank { "Debit" },
                creditDescription = obj.optString("creditDescription").ifBlank { "Credit" },
                currency = obj.optString("currency").ifBlank { "ETB" }
            )
        }

        fun listToJson(plugins: List<SmsParsePlugin>): String {
            val array = JSONArray()
            plugins.forEach { array.put(it.toJsonObject()) }
            return array.toString(2)
        }

        fun listFromJson(raw: String): List<SmsParsePlugin>? {
            if (raw.isBlank()) return emptyList()
            val array = runCatching { JSONArray(raw) }.getOrNull() ?: return null
            val plugins = mutableListOf<SmsParsePlugin>()
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: return null
                val plugin = fromJsonObject(item) ?: return null
                plugins.add(plugin)
            }
            return plugins
        }
    }
}
