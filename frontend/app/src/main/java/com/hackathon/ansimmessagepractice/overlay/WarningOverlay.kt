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
import com.hackathon.ansimmessagepractice.ui.onboarding.largeTextScale
import com.hackathon.ansimmessagepractice.ui.theme.DESIGN_FONT_SCALE
import com.hackathon.ansimmessagepractice.ui.warning.vibrateSafely
import com.hackathon.ansimmessagepractice.ui.warning.OverlaySpeech
import com.hackathon.ansimmessagepractice.ui.warning.spokenAccessibilityEnabled

/** 다른 앱 위에 뜨는 상단 경고 배너 + 자세히 보기/닫기. */
class WarningOverlay(private val context: Context) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val density = context.resources.displayMetrics.density
    private var shown: FrameLayout? = null
    private var audioPlayer: OverlaySpeech? = null
    // 오버레이는 Compose 바깥의 TextView이므로 글자 확대 설정을 별도로 적용합니다.
    private fun uiFontScale(): Float {
        val prefs = context.getSharedPreferences("ansim_settings", Context.MODE_PRIVATE)
        val mode = prefs.getString("notification_type", "기본 알림").orEmpty()
        val role = prefs.getString("role", "protected")
        return largeTextScale(mode, role)
    }
    private val checkingOverlay = CheckingOverlay(context)
    private fun dp(value: Int) = (value * density + 0.5f).toInt()
    private fun shape(color: Int, radius: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }
    private fun text(label: String, size: Float, color: Int, bold: Boolean = false): TextView =
        TextView(context).apply {
            this.text = label; textSize = size * uiFontScale(); setTextColor(color)
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
        // 확인 지연(timeout)도 사용 중인 화면 위에 노란 카드로 알려 줍니다.
        if (risk !in setOf("danger", "caution", "timeout") || !hasPermission()) return false
        dismiss()
        val prefs = context.getSharedPreferences("ansim_settings", Context.MODE_PRIVATE)
        val mode = prefs.getString("notification_type", "기본 알림").orEmpty()
        val language = prefs.getString("language", "한국어").orEmpty()
        val copy = overlayCopy(risk, mode, language, scamType)
        // 외국어 설정이면 머리줄·버튼도 '선택 언어 + 한국어'로 표시합니다.
        val foreign = if (copy.bilingual) com.hackathon.ansimmessagepractice.ui.warning.foreignCopyForDisplayLanguage(language) else null
        val danger = risk == "danger"
        val red = Color.rgb(192, 38, 46)
        val yellow = Color.rgb(255, 200, 61)
        val ink = Color.rgb(25, 27, 33)
        val accent = if (danger) red else yellow
        fun size(designPx: Float) = designPx * DESIGN_FONT_SCALE

        // 최종 시안 17p: 화면을 어둡게 가리지 않고 위쪽 카드만 표시합니다(좌우 13dp, 위 17dp).
        // 카드 바깥 터치는 아래 앱으로 그대로 전달됩니다.
        val root = FrameLayout(context).apply { setPadding(dp(13), dp(17), dp(13), dp(18)) }
        val banner = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(18))
            background = shape(Color.WHITE, 16)
            clipToOutline = true
            elevation = dp(9).toFloat()
            isClickable = true
        }
        root.addView(banner, FrameLayout.LayoutParams(-1, -2, Gravity.TOP))
        val kind = if (sourceApp == "kakao") "카카오톡" else "문자"
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), 0, dp(20), 0)
            setBackgroundColor(accent)
        }
        header.addView(android.widget.ImageView(context).apply {
            setImageResource(com.hackathon.ansimmessagepractice.R.drawable.ic_overlay_alert)
            if (!danger) setColorFilter(ink)
        }, LinearLayout.LayoutParams(dp(18), dp(18)).apply { rightMargin = dp(8) })
        header.addView(text(
            when {
                risk == "timeout" && foreign != null -> "${foreign.timeoutEyebrow} · 확인 지연"
                risk == "timeout" -> "확인 지연 · 안전 여부 확인 전"
                foreign != null -> "${foreign.level(danger)} · ${if (danger) "위험" else "주의"} · 사기 의심 $kind"
                danger -> "위험 · 사기 의심 $kind"
                else -> "주의 · 사기 의심 $kind"
            },
            size(15f), if (danger) Color.WHITE else ink, true
        ))
        banner.addView(header, LinearLayout.LayoutParams(-1, dp(42)))
        banner.addView(text(copy.headline,
            size(if (copy.hearing || copy.easy) 20f else 18f), ink, true).apply {
            setPadding(dp(20), dp(18), dp(20), dp(if (copy.guide.isBlank()) 15 else 4))
            if (Build.VERSION.SDK_INT >= 28) lineHeight = (dp(27) * uiFontScale()).toInt()
        }, LinearLayout.LayoutParams(-1, -2))
        if (copy.guide.isNotBlank()) {
            banner.addView(text(copy.guide, size(14f), Color.rgb(76, 82, 92)).apply {
                setPadding(dp(20), 0, dp(20), dp(14))
            }, LinearLayout.LayoutParams(-1, -2))
        }
        val buttons = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), 0, dp(20), 0)
        }
        fun add(label: String, primary: Boolean, weight: Float, clicked: () -> Unit) {
            val button = text(label, size(if (label.contains('\n')) 14f else 17f),
                if (primary) (if (danger) Color.WHITE else ink) else ink, true).apply {
                gravity = Gravity.CENTER
                background = if (primary) shape(accent, 24) else GradientDrawable().apply {
                    setColor(Color.WHITE)
                    cornerRadius = dp(24).toFloat()
                    setStroke(dp(1), Color.rgb(220, 221, 225))
                }
                isClickable = true; isFocusable = true; contentDescription = label
                setOnClickListener { clicked() }
            }
            val tall = label.contains('\n') || uiFontScale() > 1f
            buttons.addView(button, LinearLayout.LayoutParams(0, dp(if (tall) 56 else 48), weight).apply {
                rightMargin = if (primary) dp(6) else 0
                leftMargin = if (primary) 0 else dp(6)
            })
        }
        add(foreign?.let { "${it.details}\n자세히 보기" } ?: "자세히 보기", true, 1.76f) {
            val opened = if (onDetails != null) runCatching { onDetails() }.isSuccess
            else openWarningDetails(context, risk, receivedMessage, senderLabel, sourceApp,
                analysisReason, scamType, serverTitle, serverExplanation, serverAction,
                serverEasySentence, serverLanguage)
            if (opened) dismiss() else Toast.makeText(context, "경고 상세를 열 수 없어요.", Toast.LENGTH_SHORT).show()
        }
        add(foreign?.let { "${it.close}\n닫기" } ?: "닫기", false, 1f) { dismiss() }
        banner.addView(buttons)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP }
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