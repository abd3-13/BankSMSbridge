package com.abdelah.banksms.parser

import com.abdelah.banksms.model.ParsedTransaction
import java.text.SimpleDateFormat
import java.util.Date

object SmsParser {

fun parse(sender: String?, message: String, receivedTimeMillis: Long): ParsedTransaction? {
    val baseTransaction = when {
        message.contains("Commercial Bank of Ethiopia", true)
                || message.contains("Thank you for Banking with CBE", true)
                || sender?.contains("CBE", true) == true ->
            parseCBE(message, receivedTimeMillis)

        message.contains("Bank of Abyssinia", true) ->
            parseBoA(message, receivedTimeMillis)

        message.contains("telebirr", true)
                || message.contains("Ethio telecom", true) ->
            parseTelebirr(message, receivedTimeMillis)

        message.contains("ENAT", true)
                || message.contains("Enat", true) ->
            parseEnat(message, receivedTimeMillis)

        else -> null
    }
    return baseTransaction
}
private fun parseCBE(message: String, receivedTimeMillis: Long): ParsedTransaction? {
    val debitRegex = Regex("""transfered ETB ([\d,]+\.\d{2})""")
    val creditRegex = Regex("""credited.*ETB ([\d,]+\.\d{2})""")
    val dateRegex = Regex("""on (\d{2}/\d{2}/\d{4} at \d{2}:\d{2}:\d{2})""")

    val dateTime = dateRegex.find(message)?.groupValues?.get(1)
        ?: java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(receivedTimeMillis))

    return when {
        debitRegex.containsMatchIn(message) -> {
            val amount = debitRegex.find(message)?.groupValues?.get(1)
                ?.replace(",", "")?.toDoubleOrNull()

            ParsedTransaction(
                bank = "CBE",
                type = "debit",
                amount = amount ?: return null,
                dateTime = dateTime,
                description = "Transfer",
                reference = extractUrlRef(message),
                rawMessage = message
            )
        }

        creditRegex.containsMatchIn(message) -> {
            val amount = creditRegex.find(message)?.groupValues?.get(1)
                ?.replace(",", "")?.toDoubleOrNull()

            ParsedTransaction(
                bank = "CBE",
                type = "credit",
                amount = amount ?: return null,
                dateTime = dateTime,
                description = "Credit",
                reference = extractUrlRef(message),
                rawMessage = message
            )
        }

        else -> null
    }
}

    private fun parseBoA(message: String, receivedTimeMillis: Long): ParsedTransaction? {

        val debitRegex = Regex("""debited with ETB ([\d,]+\.\d{2})""")
        val creditRegex = Regex("""credited with ETB ([\d,]+\.\d{2})""")
        
        val dateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(receivedTimeMillis))
        return when {
            debitRegex.containsMatchIn(message) -> {
                val amount = debitRegex.find(message)?.groupValues?.get(1)
                    ?.replace(",", "")?.toDoubleOrNull()

                ParsedTransaction(
                    bank = "BOA",
                    type = "debit",
                    amount = amount ?: return null,
                    dateTime = dateTime,
                    description = "Debit",
                    reference = extractUrlRef(message),
                    rawMessage = message
                )
            }

            creditRegex.containsMatchIn(message) -> {
                val amount = creditRegex.find(message)?.groupValues?.get(1)
                    ?.replace(",", "")?.toDoubleOrNull()

                ParsedTransaction(
                    bank = "BOA",
                    type = "credit",
                    amount = amount ?: return null,
                    dateTime = dateTime,
                    description = "Credit",
                    reference = extractUrlRef(message),
                    rawMessage = message
                )
            }

            else -> null
        }
    }
    private fun parseTelebirr(message: String, receivedTimeMillis: Long): ParsedTransaction? {

        val debitRegex = Regex("""transferred ETB ([\d,]+\.\d{2})""")
        val creditRegex = Regex("""received\s+ETB ([\d,]+\.\d{2})""")
        val dateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(receivedTimeMillis))
        return when {
            debitRegex.containsMatchIn(message) -> {
                val amount = debitRegex.find(message)?.groupValues?.get(1)
                    ?.replace(",", "")?.toDoubleOrNull()

                ParsedTransaction(
                    bank = "Telebirr",
                    type = "debit",
                    amount = amount ?: return null,
                    dateTime = dateTime,
                    description = "Wallet Transfer",
                    reference = extractTransactionNumber(message),
                    rawMessage = message
                )
            }

            creditRegex.containsMatchIn(message) -> {
                val amount = creditRegex.find(message)?.groupValues?.get(1)
                    ?.replace(",", "")?.toDoubleOrNull()

                ParsedTransaction(
                    bank = "Telebirr",
                    type = "credit",
                    amount = amount ?: return null,
                    dateTime = dateTime,
                    description = "Wallet Credit",
                    reference = extractTransactionNumber(message),
                    rawMessage = message
                )
            }

            else -> null
        }
    }
    private fun parseEnat(message: String, receivedTimeMillis: Long): ParsedTransaction? {

        val debitRegex = Regex("""transferred ([\d,]+\.\d{2}) ETB""")
        val creditRegex = Regex("""credited with ([\d,]+\.\d{2}) ETB""")
        val dateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(receivedTimeMillis))
        return when {
            debitRegex.containsMatchIn(message) -> {
                val amount = debitRegex.find(message)?.groupValues?.get(1)
                    ?.replace(",", "")?.toDoubleOrNull()

                ParsedTransaction(
                    bank = "Enat",
                    type = "debit",
                    amount = amount ?: return null,
                    dateTime = dateTime,
                    description = "Transfer",
                    reference = extractRefNumber(message),
                    rawMessage = message
                )
            }

            creditRegex.containsMatchIn(message) -> {
                val amount = creditRegex.find(message)?.groupValues?.get(1)
                    ?.replace(",", "")?.toDoubleOrNull()

                ParsedTransaction(
                    bank = "Enat",
                    type = "credit",
                    amount = amount ?: return null,
                    dateTime = dateTime,
                    description = "Credit",
                    reference = extractRefNumber(message),
                    rawMessage = message
                )
            }

            else -> null
        }
    }

    private fun extractUrlRef(message: String): String? {
        val urlRegex = Regex("""https?://\S+""")
        return urlRegex.find(message)?.value
    }

    private fun extractTransactionNumber(message: String): String? {
        val refRegex = Regex("""transaction number is ([A-Z0-9]+)""")
        return refRegex.find(message)?.groupValues?.get(1)
    }

    private fun extractRefNumber(message: String): String? {
        val refRegex = Regex("""Ref\. no is: ([A-Z0-9]+)""")
        return refRegex.find(message)?.groupValues?.get(1)
    }
}
