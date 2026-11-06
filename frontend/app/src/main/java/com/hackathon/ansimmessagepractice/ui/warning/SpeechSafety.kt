package com.hackathon.ansimmessagepractice.ui.warning

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.speech.tts.TextToSpeech
import android.view.accessibility.AccessibilityManager
import java.util.Locale

/** TalkBack 등 음성 읽기 접근성 서비스가 켜져 있으면 해당 서비스에서 읽기를 담당합니다. */
internal fun spokenAccessibilityEnabled(context: Context): Boolean {
    val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
    return manager.isEnabled && manager.getEnabledAccessibilityServiceList(
        AccessibilityServiceInfo.FEEDBACK_SPOKEN).isNotEmpty()
}

/** 안내 문구만 읽고, 수신 문자 원문·링크·전화번호 숫자는 음성으로 읽지 않도록 정리합니다. */
internal fun sanitizedSpeech(text: String): String = text
    .replace(Regex("(?i)(https?://|www\\.)\\S+"), " ")
    .replace(Regex("(?<![\\d])\\+?[\\d][\\d\\s().-]{7,}[\\d](?![\\d])"), " ")
    .replace(Regex("[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}"), " ")
    .replace(Regex("\\s+"), " ")
    .trim()

internal fun speechLocale(code: String): Locale = when (code.lowercase().substringBefore('-')) {
    "en" -> Locale.ENGLISH
    "vi" -> Locale("vi", "VN")
    "zh" -> Locale.SIMPLIFIED_CHINESE
    else -> Locale.KOREAN
}

/** WindowManager 오버레이에서 사용하는 음성 안내 도우미입니다. Compose 화면의 TTS는 별도로 시작·종료를 관리합니다. */
internal class OverlaySpeech(private val context: Context) {
    private var engine: TextToSpeech? = null
    private var initialized = false
    private var queued: Pair<String, String>? = null

    init {
        engine = TextToSpeech(context.applicationContext) { status ->
            initialized = status == TextToSpeech.SUCCESS
            if (initialized) queued?.let { (text, lang) -> speak(text, lang) }
        }
    }

    fun speak(text: String, language: String) {
        if (spokenAccessibilityEnabled(context)) return
        val safe = sanitizedSpeech(text)
        if (safe.isBlank()) return
        if (!initialized) { queued = safe to language; return }
        queued = null
        engine?.let { tts ->
            if (tts.setLanguage(speechLocale(language)) >= TextToSpeech.LANG_AVAILABLE)
                tts.speak(safe, TextToSpeech.QUEUE_FLUSH, null, "ansim_overlay_warning")
        }
    }
    fun release() { queued = null; engine?.stop(); engine?.shutdown(); engine = null }
}
