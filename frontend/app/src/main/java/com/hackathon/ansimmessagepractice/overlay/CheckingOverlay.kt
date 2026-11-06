package com.hackathon.ansimmessagepractice.overlay

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.hackathon.ansimmessagepractice.ui.onboarding.largeTextScale
import com.hackathon.ansimmessagepractice.ui.theme.DESIGN_FONT_SCALE

/**
 * 의심 메시지를 분석하는 동안 화면 위쪽에 잠깐 보여주는 카드.
 */
internal class CheckingOverlay(private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val density = context.resources.displayMetrics.density
    private var statusView: FrameLayout? = null

    private fun dp(value: Int) = (value * density + 0.5f).toInt()

    fun show() {
        if (statusView != null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) return

        val prefs = context.getSharedPreferences("ansim_settings", Context.MODE_PRIVATE)
        val mode = prefs.getString("notification_type", "기본 알림").orEmpty()
        val uiFontScale = largeTextScale(mode, prefs.getString("role", "protected"))
        val language = prefs.getString("language", "한국어").orEmpty()
        val foreign = com.hackathon.ansimmessagepractice.ui.warning.foreignCopyForDisplayLanguage(language)
        val koreanTitle = if (mode == "쉬운 안내") "잠깐 기다려 주세요" else "문자를 확인하고 있어요"
        val koreanSubtitle = when (mode) {
            "쉬운 안내" -> "링크와 송금은 기다려 주세요."
            "화면 안내 중심" -> "확인이 끝날 때까지 기다려 주세요."
            else -> "확인이 끝날 때까지 링크를 누르거나 송금하지 마세요."
        }
        // 외국어 설정: 선택 언어 문장 + 아래에 한국어
        val title = if (foreign != null) "${foreign.checkingTitle} · 확인 중" else koreanTitle
        val subtitle = if (foreign != null) "${foreign.checkingBody}\n$koreanSubtitle" else koreanSubtitle
        fun size(designPx: Float) = designPx * DESIGN_FONT_SCALE * uiFontScale
        val blue = Color.rgb(27, 78, 161)
        val ink = Color.rgb(25, 27, 33)
        val muted = Color.rgb(76, 82, 92)

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(17), dp(20), dp(20))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), Color.rgb(220, 221, 226))
            }
            elevation = dp(8).toFloat()
            // TalkBack으로 읽을 수 있지만 화면 포커스를 빼앗거나 아래 앱의 터치를 막지는 않습니다.
            contentDescription = "$title. $subtitle"
        }
        val brandRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        brandRow.addView(ProgressBar(context, null, android.R.attr.progressBarStyleSmall).apply {
            isIndeterminate = true
            indeterminateTintList = ColorStateList.valueOf(blue)
        }, LinearLayout.LayoutParams(dp(17), dp(17)).apply { rightMargin = dp(10) })
        brandRow.addView(TextView(context).apply {
            text = "안심문자"
            textSize = size(15f)
            setTextColor(muted)
            setTypeface(null, Typeface.BOLD)
        })
        card.addView(brandRow)
        card.addView(TextView(context).apply {
            text = title
            textSize = size(if (mode == "화면 안내 중심") 21f else 19f)
            setTypeface(null, Typeface.BOLD)
            setTextColor(ink)
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(7) })
        card.addView(TextView(context).apply {
            text = subtitle
            textSize = size(if (mode == "화면 안내 중심") 16f else 15f)
            setTextColor(muted)
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(9) })

        // 그림자가 잘리지 않도록 바깥 여백을 둔 투명 틀 안에 카드를 둡니다.
        val root = FrameLayout(context).apply { setPadding(dp(13), dp(17), dp(13), dp(16)) }
        root.addView(card, FrameLayout.LayoutParams(-1, -2))

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        }
        runCatching { windowManager.addView(root, params) }
            .onSuccess { statusView = root }
    }

    fun dismiss() {
        statusView?.let { view -> runCatching { windowManager.removeView(view) } }
        statusView = null
    }
}
