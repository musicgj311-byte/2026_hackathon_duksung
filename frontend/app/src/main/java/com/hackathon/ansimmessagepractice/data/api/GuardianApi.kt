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
import java.time.ZoneOffset
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

/**
 * 서버 시각 → Instant. 서버는 UTC로 저장하지만 SQLite에서 다시 읽은 값은 시간대 표시 없이
 * ("2026-11-06T11:53:00.123456") 내려옵니다. 이 값을 휴대폰 현지 시각으로 읽으면 한국에서 9시간 어긋나므로 UTC로 봅니다.
 */
internal fun parseServerInstant(value: String?): Instant? {
    if (value.isNullOrBlank()) return null
    return runCatching { OffsetDateTime.parse(value).toInstant() }
        .recoverCatching { Instant.parse(value) }
        .recoverCatching { LocalDateTime.parse(value).toInstant(ZoneOffset.UTC) }
        .getOrNull()
}

/** 서버 이벤트가 아직 등록되지 않았거나 필드가 없을 때 임의의 시각을 만들지 않습니다. */
fun formatGuardianEventTime(value: String?): String {
    val instant = parseServerInstant(value) ?: return "기록 없음"
    return instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
        .format(DateTimeFormatter.ofPattern("M월 d일 HH:mm"))
}

/** GET /guardian/{id}/incidents 의 사건 한 건 (알림 기록과 짝지어 '위험 문자 수신'·'경고 화면 표시' 시각에 씀) */
internal data class GuardianIncident(
    val id: Int?, val protectedId: String, val level: String, val scamType: String,
    val receivedRaw: String, val receivedAt: Instant, val warningShownRaw: String?
)

/** 서버의 영어 수법 코드는 절대로 이용자에게 그대로 보여주지 않습니다. */
fun guardianScamTypeName(type: String): String = when (
    type.trim().lowercase(java.util.Locale.ROOT).replace('-', '_').replace(' ', '_')
) {
    "family_impersonation", "family", "가족_사칭" -> "가족 사칭"
    "delivery", "delivery_impersonation", "택배_사칭" -> "택배 사칭"
    "institution", "agency", "agency_impersonation", "institution_impersonation", "기관_사칭" -> "기관 사칭"
    "obituary_invitation", "obituary", "invitation" -> "부고·초대장 사칭"
    "subsidy_refund", "refund", "subsidy" -> "지원금·환급 사칭"
    "payment_alert", "payment", "card_payment", "payment_impersonation" -> "결제 알림 사칭"
    "job_loan", "job", "loan" -> "구인·대출 사칭"
    "immigration", "visa" -> "출입국·체류 사칭"
    "phishing", "smishing", "unknown", "none", "", "null" -> "사기 의심"
    else -> "기타 사기 의심"
}

object GuardianApi {
    suspend fun notifications(context: Context): List<GuardianNotificationRecord> {
        UserSync.ensureCurrent(context, "guardian")
        val id = Uri.encode(AnsimDeviceId.value(context, "guardian"))
        val response = BackendApi.request(context, "GET", "/guardian/$id/notifications")
        val items = response.getJSONArray("notifications")
        // 알림 기록에는 사건 시각이 없어서 사건 타임라인을 함께 받아 짝지어 줍니다. 실패해도 알림 목록은 그대로 보여 줍니다.
        val incidents = runCatching { incidents(context, id) }.getOrDefault(emptyList())
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
            ).withIncident(item.optString("protected_device_id"), incidents)
        }
    }

    private suspend fun incidents(context: Context, encodedGuardianId: String): List<GuardianIncident> {
        val list = BackendApi.request(context, "GET", "/guardian/$encodedGuardianId/incidents")
            .optJSONArray("incidents") ?: return emptyList()
        return (0 until list.length()).mapNotNull { i ->
            val item = list.optJSONObject(i) ?: return@mapNotNull null
            val raw = item.nonBlankString("received_at") ?: return@mapNotNull null
            val at = parseServerInstant(raw) ?: return@mapNotNull null
            GuardianIncident(
                id = if (item.has("incident_id") && !item.isNull("incident_id")) item.optInt("incident_id") else null,
                protectedId = item.optString("protected_device_id"),
                level = item.optString("level"),
                scamType = item.optString("scam_type"),
                receivedRaw = raw, receivedAt = at,
                warningShownRaw = findWarningShownAt(item.optJSONArray("events"))
            )
        }
    }

    /**
     * 서버는 위험 사건을 저장한 직후 보호자 알림을 보내므로, 같은 가족·위험도·수법이면서
     * 알림 직전 2분 안에 생긴 사건 중 가장 가까운 것을 그 알림의 사건으로 봅니다. 없으면 '기록 없음' 그대로 둡니다.
     */
    private fun GuardianNotificationRecord.withIncident(
        protectedId: String, incidents: List<GuardianIncident>
    ): GuardianNotificationRecord {
        if (incidentCreatedAt != null && warningShownAt != null) return this
        val sent = parseServerInstant(sentAt) ?: return this
        val match = incidents.filter {
            it.protectedId == protectedId && it.level == level && it.scamType == scamType &&
                    !it.receivedAt.isAfter(sent.plusSeconds(5)) &&
                    !it.receivedAt.isBefore(sent.minusSeconds(120))
        }.maxByOrNull { it.receivedAt } ?: return this
        return copy(
            incidentId = incidentId ?: match.id,
            incidentCreatedAt = incidentCreatedAt ?: match.receivedRaw,
            warningShownAt = warningShownAt ?: match.warningShownRaw
        )
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