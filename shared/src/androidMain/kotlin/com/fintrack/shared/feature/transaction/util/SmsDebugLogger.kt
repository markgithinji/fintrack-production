package com.fintrack.shared.feature.transaction.util

import android.content.Context
import android.provider.Telephony
import android.util.Log
import java.io.File

object SmsDebugLogger {
    private const val TAG = "SMS_DEBUG_LOG"

    fun logInboxSms(context: Context, limit: Int = 500) {
        dumpProviderSms(context, "mpesa", listOf("%MPESA%", "%M-PESA%"), limit)
        dumpProviderSms(context, "equity", listOf("%EQUITY%", "%EQUIT%", "%EQTY%"), limit)
    }

    fun dumpProviderSms(
        context: Context,
        providerName: String,
        addressPatterns: List<String>,
        limit: Int = 500
    ) {
        val whereClause = addressPatterns.joinToString(" OR ") { "${Telephony.Sms.Inbox.ADDRESS} LIKE ?" }
        val cursor = try {
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.Inbox.BODY, Telephony.Sms.Inbox.ADDRESS, Telephony.Sms.Inbox.DATE),
                whereClause,
                addressPatterns.toTypedArray(),
                "${Telephony.Sms.Inbox.DATE} DESC"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query SMS for $providerName dump", e)
            null
        }

        val dumpFile = File(context.filesDir, "${providerName}_sms_dump.txt")
        val externalDumpFile = context.getExternalFilesDir(null)?.let { File(it, "${providerName}_sms_dump.txt") }

        val stringBuilder = StringBuilder()
        var count = 0

        cursor?.use { smsCursor ->
            val bodyIndex = smsCursor.getColumnIndex(Telephony.Sms.Inbox.BODY)
            val addressIndex = smsCursor.getColumnIndex(Telephony.Sms.Inbox.ADDRESS)
            val dateIndex = smsCursor.getColumnIndex(Telephony.Sms.Inbox.DATE)

            stringBuilder.append("--- START $providerName SMS DUMP (Top $limit) ---\n")

            while (smsCursor.moveToNext() && count < limit) {
                val body = smsCursor.getString(bodyIndex) ?: ""
                val address = smsCursor.getString(addressIndex) ?: "Unknown"
                val date = smsCursor.getLong(dateIndex)

                val line = "Row: $count | Sender: $address | Date: $date | Body: ${body.replace("\n", " ")}\n"
                stringBuilder.append(line)
                Log.d(TAG, "[$providerName #$count] $line".trimEnd())
                count++
            }

            stringBuilder.append("--- END $providerName SMS DUMP ($count messages) ---\n")
        }

        try {
            val content = stringBuilder.toString()
            dumpFile.writeText(content)
            externalDumpFile?.writeText(content)
            Log.d(TAG, "Successfully dumped $count $providerName SMS messages to file: ${dumpFile.absolutePath}")
            if (externalDumpFile != null) {
                Log.d(TAG, "Also dumped to external file: ${externalDumpFile.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write dump file for $providerName", e)
        }
    }
}
