package com.hackathon.ansimmessagepractice.data.api

import android.content.Context
import android.net.Uri
import com.hackathon.ansimmessagepractice.guardian.AnsimDeviceId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 서버 주소는 앱 코드에서 한 곳만 수정합니다.
 * SERVER_URL이 비어 있으면 기존 로컬 경고 시연을 유지합니다.
 * 예: "https://your-api.example.com" (마지막 '/' 없이)
 */
object BackendConfig {
    private const val SERVER_URL = "" // TODO: 백엔드 팀의 HTTPS 서버 주소 입력

    // 기존 호출부와 호환하도록 Context 인자를 유지합니다.
    fun get(context: Context): String = SERVER_URL.trim().trimEnd('/')

    fun isConfigured(context: Context): Boolean = get(context).isNotBlank()
}

class BackendHttpException(val statusCode: Int, message: String) : Exception(message)

/** 외부 Retrofit 라이브러리 없이 Android 기본 HTTP + org.json 사용. */
object BackendApi {
    suspend fun request(context: Context, method: String, path: String, data: JSONObject? = null): JSONObject =
        withContext(Dispatchers.IO) {
            val base = BackendConfig.get(context)
            require(base.isNotBlank()) { "서버 주소가 아직 설정되지 않았어요." }
            require(path.startsWith("/") && !path.startsWith("//")) { "올바르지 않은 API 경로" }
            val conn = (URL(base + path).openConnection() as HttpURLConnection)
            try {
                conn.requestMethod = method
                conn.connectTimeout = 12_000
                conn.readTimeout = 25_000
                conn.setRequestProperty("Accept", "application/json")
                conn.useCaches = false
                if (data != null) {
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    conn.outputStream.use { it.write(data.toString().toByteArray(Charsets.UTF_8)) }
                }
                val status = conn.responseCode
                val stream = if (status in 200..299) conn.inputStream else conn.errorStream
                val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                if (status !in 200..299) {
                    val detail = runCatching {
                        val value = JSONObject(raw).opt("detail")
                        if (value is String) value else value?.toString().orEmpty()
                    }.getOrDefault("")
                    throw BackendHttpException(status, "서버 오류 $status${if (detail.isNotBlank()) ": $detail" else ""}")
                }
                if (raw.isBlank()) JSONObject() else JSONObject(raw)
            } finally {
                conn.disconnect()
            }
        }

    suspend fun health(context: Context): JSONObject = request(context, "GET", "/health")
}

/** backend/app/user/router.py 와 동기화. 서버에 role 변경 API가 없어 역할별 UUID를 분리합니다. */
object UserSync {
    suspend fun ensureRegistered(context: Context, role: String, userType: String, language: String) {
        require(role == "protected" || role == "guardian")
        val id = AnsimDeviceId.value(context, role)
        val body = JSONObject().put("device_id", id).put("role", role)
            .put("user_type", userType).put("language", language)
        try {
            BackendApi.request(context, "POST", "/users/register", body)
        } catch (e: BackendHttpException) {
            if (e.statusCode != 409) throw e
            val existing = BackendApi.request(context, "GET", "/users/${Uri.encode(id)}")
            check(existing.optString("role") == role) { "서버 역할 정보가 일치하지 않아요." }
            if (existing.optString("user_type") != userType || existing.optString("language") != language) {
                BackendApi.request(context, "PATCH", "/users/${Uri.encode(id)}",
                    JSONObject().put("user_type", userType).put("language", language))
            }
        }
        context.getSharedPreferences("ansim_settings", Context.MODE_PRIVATE).edit()
            .putBoolean("needs_server_sync", false).apply()
    }

    suspend fun ensureCurrent(context: Context, role: String? = null) {
        val prefs = context.getSharedPreferences("ansim_settings", Context.MODE_PRIVATE)
        val actualRole = role ?: prefs.getString("role", "protected").orEmpty()
        val type = if (actualRole == "guardian") "default"
        else prefs.getString("user_type_code", "default").orEmpty()
        val language = prefs.getString("language_code", "ko").orEmpty()
        ensureRegistered(context, actualRole, type.ifBlank { "default" }, language.ifBlank { "ko" })
    }
}