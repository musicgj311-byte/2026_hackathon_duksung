package com.hackathon.ansimmessagepractice.data.api

import android.content.Context
import com.hackathon.ansimmessagepractice.guardian.AnsimDeviceId
import org.json.JSONObject

/** backend/app/schemas.py: JudgeRequest -> JudgeResponse (실제 응답 필드 그대로). */
data class JudgeVerdict(
    val level: String,
    val scamType: String,
    val title: String,
    val explanation: String,
    val action: String,
    val easySentence: String,
    val language: String,
    val incidentId: Int?
) {
    companion object {
        fun fromJson(obj: JSONObject): JudgeVerdict {
            val level = obj.getString("level")
            require(level in setOf("danger", "caution", "safe")) { "알 수 없는 판정: $level" }
            return JudgeVerdict(
                level = level,
                scamType = obj.getString("scam_type"),
                title = obj.getString("title"),
                explanation = obj.getString("explanation"),
                action = obj.getString("action"),
                easySentence = obj.getString("easy_sentence"),
                language = obj.getString("language"),
                incidentId = if (obj.isNull("incident_id") || !obj.has("incident_id")) null
                else obj.getInt("incident_id")
            )
        }
    }
}

object JudgeApi {
    suspend fun judge(context: Context, source: String, sender: String, text: String,
                      isContact: Boolean, isInternational: Boolean): JudgeVerdict {
        UserSync.ensureCurrent(context, "protected")
        val prefs = context.getSharedPreferences("ansim_settings", Context.MODE_PRIVATE)
        val body = JSONObject()
            .put("device_id", AnsimDeviceId.value(context, "protected"))
            .put("source", source)
            .put("sender", sender)
            .put("is_contact", isContact)
            .put("is_international", isInternational)
            .put("text", text.take(2000))
            .put("user_type", prefs.getString("user_type_code", "default"))
            .put("language", prefs.getString("language_code", "ko"))
        return JudgeVerdict.fromJson(BackendApi.request(context, "POST", "/judge", body))
    }

    /** 화면 경고를 실제 오버레이에 표시했을 때만 기록. */
    suspend fun recordWarningShown(context: Context, incidentId: Int) {
        val body = JSONObject().put("device_id", AnsimDeviceId.value(context, "protected"))
            .put("event_type", "warning_shown")
        BackendApi.request(context, "POST", "/incidents/$incidentId/events", body)
    }
}
