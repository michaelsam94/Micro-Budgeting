package com.example.data.sms

import android.content.Context
import android.net.Uri

data class SmsMessage(
    val sender: String,
    val body: String,
    val date: Long
)

class SmsReader(private val context: Context) {
    fun readInbox(limit: Int = 50): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()
        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("_id", "body", "date", "address")

        try {
            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "date DESC"
            )?.use { cursor ->
                val bodyIndex = cursor.getColumnIndex("body")
                val dateIndex = cursor.getColumnIndex("date")
                val addressIndex = cursor.getColumnIndex("address")

                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    val body = if (bodyIndex != -1) cursor.getString(bodyIndex) ?: "" else ""
                    val date = if (dateIndex != -1) cursor.getLong(dateIndex) else 0L
                    val address = if (addressIndex != -1) cursor.getString(addressIndex) ?: "" else ""
                    
                    if (body.isNotEmpty()) {
                        messages.add(SmsMessage(address, body, date))
                        count++
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return messages
    }
}
