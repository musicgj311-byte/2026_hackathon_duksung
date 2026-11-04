package com.hackathon.ansimmessagepractice.detection

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** 앱 화면에 표시할 최근 경고 내역만 간단히 저장합니다. 메시지 원문·발신자·URL은 저장하지 않습니다. */
data class AlertRecord(val level: String, val title: String, val source: String, val at: Long)

/** 화면 잠금 중 발생한 최신 위험 이벤트. 원문·발신번호는 저장하지 않음. */
data class PendingDanger(val source: String, val scamType: String, val reason: String)

object AlertStore {
    private const val FILE = "ansim_local_alerts"
    private const val HISTORY = "recent_v1"
    private const val PENDING = "pending_locked_danger_source"
    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun list(context: Context): List<AlertRecord> = runCatching {
        val raw = prefs(context).getString(HISTORY, "[]") ?: "[]"
        val json = JSONArray(raw)
        (0 until json.length()).mapNotNull { i ->
            val item = json.optJSONObject(i) ?: return@mapNotNull null
            val level = item.optString("level")
            if (level !in setOf("danger", "caution")) null else AlertRecord(
                level, item.optString("title"), item.optString("source"), item.optLong("at"))
        }
    }.getOrDefault(emptyList())

    fun add(context: Context, level: String, title: String, source: String) {
        if (level !in setOf("danger", "caution")) return // safe/checking never appear
        val json = JSONArray()
        val current = list(context)
        val at = System.currentTimeMillis()
        val record = JSONObject().put("level", level).put("title", title.take(60))
            .put("source", if (source == "kakao") "kakao" else "sms").put("at", at)
        json.put(record)
        current.take(29).forEach {
            json.put(JSONObject().put("level", it.level).put("title", it.title)
                .put("source", it.source).put("at", it.at))
        }
        prefs(context).edit().putString(HISTORY, json.toString()).apply()
    }

    fun setPendingDanger(context: Context, source: String, scamType: String, reason: String) {
        val safe = JSONObject()
            .put("source", if (source == "kakao") "kakao" else "sms")
            .put("scamType", scamType.take(50))
            .put("reason", reason.take(300))
        prefs(context).edit().putString(PENDING, safe.toString()).apply()
    }

    fun takePendingDanger(context: Context): PendingDanger? {
        val stored = prefs(context).getString(PENDING, null) ?: return null
        clearPendingDanger(context)
        // 이전 버전의 단순 문자열도 안전하게 처리
        return runCatching {
            val item = JSONObject(stored)
            PendingDanger(
                source = item.optString("source", "sms"),
                scamType = item.optString("scamType", "none"),
                reason = item.optString("reason", "의심스러운 메시지가 도착했어요.")
            )
        }.getOrElse { PendingDanger(if (stored == "kakao") "kakao" else "sms", "none", "의심스러운 메시지가 도착했어요.") }
    }
    fun clearPendingDanger(context: Context) { prefs(context).edit().remove(PENDING).apply() }
}