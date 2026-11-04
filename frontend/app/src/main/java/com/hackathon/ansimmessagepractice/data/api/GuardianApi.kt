package com.hackathon.ansimmessagepractice.data.api

import android.content.Context
import android.net.Uri
import com.hackathon.ansimmessagepractice.guardian.AnsimDeviceId

/** backend/app/guardian/schemas.py: NotificationInfo. 메시지 원문은 반환되지 않습니다. */
data class GuardianNotificationRecord(
    val level: String, val scamType: String, val status: String, val sentAt: String
)

object GuardianApi {
    suspend fun notifications(context: Context): List<GuardianNotificationRecord> {
        UserSync.ensureCurrent(context, "guardian")
        val id = Uri.encode(AnsimDeviceId.value(context, "guardian"))
        val response = BackendApi.request(context, "GET", "/guardian/$id/notifications")
        val items = response.getJSONArray("notifications")
        return (0 until items.length()).map { i ->
            val item = items.getJSONObject(i)
            GuardianNotificationRecord(
                item.getString("level"), item.getString("scam_type"),
                item.getString("status"), item.getString("sent_at"))
        }
    }
}
