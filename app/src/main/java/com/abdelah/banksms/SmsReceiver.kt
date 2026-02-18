package com.abdelah.banksms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.abdelah.banksms.db.AppDatabase
import com.abdelah.banksms.db.TransactionEntity
import com.abdelah.banksms.parser.SmsParser
import com.abdelah.banksms.sync.SyncScheduler
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {

            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            for (sms in messages) {

                val sender = sms.originatingAddress ?: "Unknown"
                val messageBody = sms.messageBody

                Log.d("BankSMS", "Sender: $sender")
                val parsed = SmsParser.parse(context, sender, messageBody, sms.timestampMillis)

                parsed?.let { tx ->
                    val entity = TransactionEntity(
                        bank = tx.bank,
                        type = tx.type,
                        amount = tx.amount,
                        currency = tx.currency,
                        dateTime = tx.dateTime ?: SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                            .format(Date(sms.timestampMillis)),
                        description = tx.description,
                        reference = tx.reference,
                        rawMessage = tx.rawMessage
                    )
                
                    // Insert into DB
                    val db = AppDatabase.getDatabase(context)
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        val insertedId = db.transactionDao().insert(entity)
                        Log.d("BankSMS", "Saved to DB: $entity (id=$insertedId)")
                        if (insertedId > 0) {
                            SyncScheduler.enqueueImmediate(context)
                        }
                    }
                }

                if (parsed != null) {
                    Log.d("BankSMS", "Parsed: $parsed")
                } else {
                    Log.d("BankSMS", "Not recognized format")
                }

            }
        }
    }
}
