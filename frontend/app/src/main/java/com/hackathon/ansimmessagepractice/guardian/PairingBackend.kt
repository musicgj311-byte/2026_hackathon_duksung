package com.hackathon.ansimmessagepractice.guardian

import android.content.Context
import java.util.UUID

/** 보호자 연결 API 계약. */
data class PairingTicket(val code: String, val expiresAtEpochMillis: Long)
data class PairingLink(val protectedDeviceId: String, val guardianDeviceId: String)

interface PairingBackend {
    /** 보호 대상자만 발급 */
    suspend fun issueCode(protectedDeviceId: String): PairingTicket
    /** 보호자만 입력 */
    suspend fun redeemCode(guardianDeviceId: String, sixDigitCode: String): PairingLink
    /** 두 ID 모두 전달 */
    suspend fun unlinkGuardian(protectedDeviceId: String, guardianDeviceId: String)
}

/** 서버에 보호 대상자 및 보호자 등록을 보장한 뒤 연결 API를 호출합니다. */
class HttpPairingBackend(private val context: Context) : PairingBackend {
    override suspend fun issueCode(protectedDeviceId: String): PairingTicket {
        com.hackathon.ansimmessagepractice.data.api.UserSync.ensureCurrent(context, "protected")
        val result = com.hackathon.ansimmessagepractice.data.api.BackendApi.request(
            context, "POST", "/guardian/pairing-code",
            org.json.JSONObject().put("protected_device_id", protectedDeviceId))
        val date = java.time.OffsetDateTime.parse(result.getString("expires_at"))
        return PairingTicket(result.getString("code"), date.toInstant().toEpochMilli())
    }
    override suspend fun redeemCode(guardianDeviceId: String, sixDigitCode: String): PairingLink {
        com.hackathon.ansimmessagepractice.data.api.UserSync.ensureCurrent(context, "guardian")
        val result = com.hackathon.ansimmessagepractice.data.api.BackendApi.request(
            context, "POST", "/guardian/connect",
            org.json.JSONObject().put("guardian_device_id", guardianDeviceId).put("code", sixDigitCode))
        return PairingLink(result.getString("protected_device_id"), result.getString("guardian_device_id"))
    }
    override suspend fun unlinkGuardian(protectedDeviceId: String, guardianDeviceId: String) {
        com.hackathon.ansimmessagepractice.data.api.BackendApi.request(
            context, "DELETE", "/guardian/disconnect",
            org.json.JSONObject().put("protected_device_id", protectedDeviceId)
                .put("guardian_device_id", guardianDeviceId))
    }
}

private object UnconnectedBackend : PairingBackend {
    private fun unavailable(): Nothing = error("아직 보호자 연결 서버가 설정되지 않았어요.")
    override suspend fun issueCode(protectedDeviceId: String): PairingTicket = unavailable()
    override suspend fun redeemCode(guardianDeviceId: String, sixDigitCode: String): PairingLink = unavailable()
    override suspend fun unlinkGuardian(protectedDeviceId: String, guardianDeviceId: String): Unit = unavailable()
}

object PairingIntegration {
    var backend: PairingBackend = UnconnectedBackend
    val isConfigured: Boolean get() = backend !== UnconnectedBackend
    fun configure(context: Context) {
        backend = if (com.hackathon.ansimmessagepractice.data.api.BackendConfig.isConfigured(context))
            HttpPairingBackend(context.applicationContext) else UnconnectedBackend
    }
}

/** 프론트 1 /judge 요청과 사용자 등록 요청에 공통으로 쓸 앱 설치 UUID. */
object AnsimDeviceId {
    fun value(context: Context, requestedRole: String? = null): String {
        val prefs = context.getSharedPreferences("ansim_settings", Context.MODE_PRIVATE)
        val role = requestedRole ?: prefs.getString("role", "protected") ?: "protected"
        val key = if (role == "guardian") "local_device_id_guardian" else "local_device_id_protected"
        val existing = prefs.getString(key, null)
        if (!existing.isNullOrBlank()) return existing
        // 이전 버전의 ID는 보호 대상자 ID로만 이전합니다.
        val legacy = if (role == "protected") prefs.getString("local_device_id", null) else null
        val id = legacy?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        prefs.edit().putString(key, id).apply()
        return id
    }
}