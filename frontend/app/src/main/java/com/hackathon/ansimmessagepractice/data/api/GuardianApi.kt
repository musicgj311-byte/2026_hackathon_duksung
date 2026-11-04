package com.hackathon.ansimmessagepractice.data.api

import android.content.Context
import android.net.Uri
import com.hackathon.ansimmessagepractice.guardian.AnsimDeviceId
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * GET /guardian/{id}/notifications 의 알림 전송 이력.
 * sentAt은 보호자 알림 *전송* 시각이며, 사건 생성 시각이 아닙니다.
 * incidentCreatedAt / warningShownAt은 서버가 명시적으로 제공할 때만 사용합니다.
 * 기존 4개 필드만 있는 응답과도 호환됩니다.
 */
data class GuardianNotificationRecord(
    val level: String,
    val scamType: String,
    val status: String,
    val sentAt: String,
    val incidentId: Int? = null,
    val incidentCreatedAt: String? = null,
    val warningShownAt: String? = null,
    val protectedName: String? = null,
    val explanation: String? = null
)

private fun JSONObject.nonBlankString(key: String): String? =
    if (has(key) && !isNull(key)) optString(key).trim().takeIf { it.isNotEmpty() && it != "null" } else null

/** 서버 이벤트가 아직 등록되지 않았거나 필드가 없을 때 임의의 시각을 만들지 않습니다. */
fun formatGuardianEventTime(value: String?): String {
    if (value.isNullOrBlank()) return "기록 없음"
    val fmt = DateTimeFormatter.ofPattern("M월 d일 HH:mm")
    val local = runCatching { OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime() }
        .recoverCatching { Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDateTime() }
        .recoverCatching { LocalDateTime.parse(value) }
        .getOrNull()
    return local?.format(fmt) ?: "기록 없음"
}

fun guardianScamTypeName(type: String): String = when (type.lowercase()) {
    "family_impersonation", "family", "가족 사칭" -> "가족 사칭"
    "delivery", "delivery_impersonation", "택배 사칭" -> "택배 사칭"
    "institution", "agency_impersonation", "기관 사칭" -> "기관 사칭"
    "none", "unknown", "" -> "사기 의심"
    else -> type.replace('_', ' ').take(30)
}

object GuardianApi {
    suspend fun notifications(context: Context): List<GuardianNotificationRecord> {
        UserSync.ensureCurrent(context, "guardian")
        val id = Uri.encode(AnsimDeviceId.value(context, "guardian"))
        val response = BackendApi.request(context, "GET", "/guardian/$id/notifications")
        val items = response.getJSONArray("notifications")
        return (0 until items.length()).map { i ->
            val item = items.getJSONObject(i)
            val incident = item.optJSONObject("incident")
            GuardianNotificationRecord(
                level = item.optString("level", "caution"),
                scamType = item.optString("scam_type", "unknown"),
                status = item.optString("status", "unknown"),
                sentAt = item.optString("sent_at", ""),
                incidentId = if (item.has("incident_id") && !item.isNull("incident_id"))
                    item.optInt("incident_id").takeIf { it > 0 } else null,
                // 오직 명시적인 사건 생성 필드만 사용합니다. sent_at / 알림 created_at은 사용하지 않습니다.
                incidentCreatedAt = item.nonBlankString("incident_created_at")
                    ?: incident?.nonBlankString("created_at"),
                warningShownAt = item.nonBlankString("warning_shown_at")
                    ?: findWarningShownAt(item.optJSONArray("events"))
                    ?: findWarningShownAt(incident?.optJSONArray("events")),
                protectedName = item.nonBlankString("protected_name")
                    ?: item.optJSONObject("protected_user")?.nonBlankString("name"),
                explanation = item.nonBlankString("explanation")
            )
        }
    }

    /** 사건별 이벤트 배열이 함께 올 때 warning_shown만 선택. 다른 이벤트/전송 시각으로 대체 금지. */
    private fun findWarningShownAt(events: JSONArray?): String? {
        if (events == null) return null
        for (i in 0 until events.length()) {
            val event = events.optJSONObject(i) ?: continue
            if (event.optString("event_type") != "warning_shown") continue
            return event.nonBlankString("occurred_at")
                ?: event.nonBlankString("created_at")
                ?: event.nonBlankString("timestamp")
        }
        return null
    }
}
