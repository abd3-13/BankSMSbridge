package com.abdelah.banksms.parser

import android.content.Context
import com.abdelah.banksms.model.ParsedTransaction
import com.abdelah.banksms.sync.SyncConfig

object SmsParser {

    fun parse(context: Context, sender: String?, message: String, receivedTimeMillis: Long): ParsedTransaction? {
        val plugins = configuredPlugins(context)
        for (plugin in plugins) {
            if (!plugin.matches(sender, message)) {
                continue
            }
            val parsed = plugin.parse(message, receivedTimeMillis)
            if (parsed != null) {
                return parsed
            }
        }
        return null
    }

    fun configuredPlugins(context: Context): List<SmsParsePlugin> {
        val configured = SmsParsePlugin.listFromJson(SyncConfig.getParserPluginsJson(context))
            ?.takeIf { it.isNotEmpty() }
        return configured ?: defaultPlugins()
    }

    fun defaultPlugins(): List<SmsParsePlugin> = listOf(
        SmsParsePlugin(
            id = "cbe",
            bankName = "CBE",
            senderContains = "CBE",
            messageHints = listOf("Commercial Bank of Ethiopia", "Thank you for Banking with CBE"),
            debitRegex = "transfered ETB ([\\d,]+\\.\\d{2})",
            creditRegex = "credited.*ETB ([\\d,]+\\.\\d{2})",
            referenceRegex = "(https?://\\S+)",
            dateRegex = "on (\\d{2}/\\d{2}/\\d{4} at \\d{2}:\\d{2}:\\d{2})",
            debitDescription = "Transfer",
            creditDescription = "Credit"
        ),
        SmsParsePlugin(
            id = "boa",
            bankName = "BOA",
            senderContains = "BOA",
            messageHints = listOf("Bank of Abyssinia"),
            debitRegex = "debited with ETB ([\\d,]+\\.\\d{2})",
            creditRegex = "credited with ETB ([\\d,]+\\.\\d{2})",
            referenceRegex = "(https?://\\S+)",
            debitDescription = "Debit",
            creditDescription = "Credit"
        ),
        SmsParsePlugin(
            id = "telebirr",
            bankName = "Telebirr",
            senderContains = "telebirr",
            messageHints = listOf("telebirr", "Ethio telecom"),
            debitRegex = "transferred ETB ([\\d,]+\\.\\d{2})",
            creditRegex = "received\\s+ETB ([\\d,]+\\.\\d{2})",
            referenceRegex = "transaction number is ([A-Z0-9]+)",
            debitDescription = "Wallet Transfer",
            creditDescription = "Wallet Credit"
        ),
        SmsParsePlugin(
            id = "enat",
            bankName = "Enat",
            senderContains = "ENAT",
            messageHints = listOf("ENAT", "Enat"),
            debitRegex = "transferred ([\\d,]+\\.\\d{2}) ETB",
            creditRegex = "credited with ([\\d,]+\\.\\d{2}) ETB",
            referenceRegex = "Ref\\. no is: ([A-Z0-9]+)",
            debitDescription = "Transfer",
            creditDescription = "Credit"
        )
    )
}
