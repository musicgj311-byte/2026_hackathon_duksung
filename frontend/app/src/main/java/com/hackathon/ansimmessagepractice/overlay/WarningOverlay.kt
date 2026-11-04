package com.hackathon.ansimmessagepractice.overlay

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.hackathon.ansimmessagepractice.navigation.openWarningDetails
import com.hackathon.ansimmessagepractice.ui.warning.vibrateSafely
import com.hackathon.ansimmessagepractice.ui.warning.OverlaySpeech
import com.hackathon.ansimmessagepractice.ui.warning.spokenAccessibilityEnabled

/** 다른 앱 위에 뜨는 상단 경고 배너 + 자세히 보기/닫기. */
class WarningOverlay(private val context: Context) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val density = context.resources.displayMetrics.density
    private var shown: FrameLayout? = null
    private var audioPlayer: OverlaySpeech? = null
    private val checkingOverlay = CheckingOverlay(context)
    private fun dp(value: Int) = (value * density + 0.5f).toInt()
    private fun shape(color: Int, radius: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }
    private fun text(label: String, size: Float, color: Int, bold: Boolean = false): TextView =
        TextView(context).apply {
            this.text = label; textSize = size; setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
        }

    fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

    fun openPermissionSettings() {
        context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    fun showChecking() {
        if (!hasPermission()) return
        dismiss()
        checkingOverlay.show()
    }
    fun dismissChecking() = checkingOverlay.dismiss()

    @Suppress("UNUSED_PARAMETER")
    fun show(riskLevel: String, onDetails: (() -> Unit)? = null,
             receivedMessage: String? = null, senderLabel: String? = null,
             analysisReason: String? = null, sourceApp: String = "sms", scamType: String = "none",
             serverTitle: String? = null, serverExplanation: String? = null,
             serverAction: String? = null, serverEasySentence: String? = null,
             serverLanguage: String? = null): Boolean {
        val risk = riskLevel.lowercase()
        if (risk == "checking") { showChecking(); return false }
        dismissChecking()
        if (risk !in setOf("danger", "caution") || !hasPermission()) return false
        dismiss()
        val prefs = context.getSharedPreferences("ansim_settings", Context.MODE_PRIVATE)
        val mode = prefs.getString("notification_type", "기본 알림").orEmpty()
        val language = prefs.getString("language", "한국어").orEmpty()
        val copy = overlayCopy(risk, mode, language)
        val red = Color.rgb(195, 35, 53)
        val accent = if (risk == "danger") red else Color.rgb(170, 120, 29)

        // 원본 앱을 가리는 스크림: 사기 링크를 실수로 누르는 것 방지.
        val root = FrameLayout(context).apply {
            setBackgroundColor(Color.argb(92, 22, 25, 30))
            isClickable = true
            setOnClickListener { }
        }
        val banner = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(13))
            background = shape(Color.WHITE, 12)
            clipToOutline = true
            elevation = dp(9).toFloat()
            isClickable = true
        }
        root.addView(banner, FrameLayout.LayoutParams(-1, -2, Gravity.TOP).apply {
            leftMargin = dp(16); rightMargin = dp(16); topMargin = dp(30)
        })
        val kind = if (sourceApp == "kakao") "카카오톡" else "문자"
        banner.addView(text(
            if (risk == "danger") "⚠  위험 · 사기 의심 $kind" else "⚠  주의 · 사기 의심 $kind",
            14f, Color.WHITE, true
        ).apply {
            setPadding(dp(16), dp(9), dp(16), dp(9))
            setBackgroundColor(accent)
        }, LinearLayout.LayoutParams(-1, -2))
        banner.addView(text(copy.headline,
            if (copy.hearing || copy.easy) 20f else 18f,
            Color.rgb(27, 29, 35), true).apply {
            setPadding(dp(17), dp(15), dp(17), dp(if (copy.guide.isBlank()) 14 else 5))
        }, LinearLayout.LayoutParams(-1, -2))
        if (copy.guide.isNotBlank()) {
            banner.addView(text(copy.guide, 13f, Color.rgb(92, 99, 111)).apply {
                setPadding(dp(17), 0, dp(17), dp(13))
            }, LinearLayout.LayoutParams(-1, -2))
        }
        val buttons = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(15), 0, dp(15), 0)
        }
        fun add(label: String, primary: Boolean, clicked: () -> Unit) {
            val button = text(label, 15f, if (primary) Color.WHITE else Color.rgb(28, 30, 37), true).apply {
                gravity = Gravity.CENTER
                background = if (primary) shape(accent, 6) else GradientDrawable().apply {
                    setColor(Color.WHITE)
                    cornerRadius = dp(6).toFloat()
                    setStroke(dp(1), Color.rgb(222, 225, 231))
                }
                isClickable = true; isFocusable = true; contentDescription = label
                setOnClickListener { clicked() }
            }
            buttons.addView(button, LinearLayout.LayoutParams(0, dp(43), 1f).apply {
                rightMargin = if (primary) dp(6) else 0
                leftMargin = if (primary) 0 else dp(6)
            })
        }
        add("자세히 보기", true) {
            val opened = if (onDetails != null) runCatching { onDetails() }.isSuccess
            else openWarningDetails(context, risk, receivedMessage, senderLabel, sourceApp,
                analysisReason, scamType, serverTitle, serverExplanation, serverAction,
                serverEasySentence, serverLanguage)
            if (opened) dismiss() else Toast.makeText(context, "경고 상세를 열 수 없어요.", Toast.LENGTH_SHORT).show()
        }
        add("닫기", false) { dismiss() }
        banner.addView(buttons)

        val params = WindowManager.LayoutParams(
            -1, -1,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }
        // addView 성공한 경우에만 표시 성공으로 판단합니다.
        // TTS·진동 실패는 실제 경고창 표시 성공 여부에 영향을 주지 않습니다.
        try {
            wm.addView(root, params)
            shown = root
        } catch (e: Exception) {
            shown = null
            Log.w("WarningOverlay", "경고 오버레이 표시 실패", e)
            return false
        }

        runCatching {
            if (copy.voice) {
                if (spokenAccessibilityEnabled(context)) {
                    // TalkBack owns spoken output; never start competing app TTS.
                    banner.post { banner.announceForAccessibility("${copy.headline}. ${copy.guide}") }
                } else {
                    audioPlayer = OverlaySpeech(context).also {
                        it.speak("${copy.headline}. ${copy.guide}", when (language) {
                            "English" -> "en"
                            "Tiếng Việt" -> "vi"
                            "中文" -> "zh"
                            else -> "ko"
                        })
                    }
                }
            }
            if (copy.voice || copy.hearing) vibrateSafely(context)
        }.onFailure { Log.w("WarningOverlay", "경고 음성/진동 안내 실패", it) }
        return true
    }

    fun dismiss() {
        checkingOverlay.dismiss()
        shown?.let { runCatching { wm.removeView(it) } }
        shown = null
        audioPlayer?.release(); audioPlayer = null
    }
}