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
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import kotlin.math.min

/** 의심 메시지를 분석하는 동안 잠깐 보여주는 작은 진행 상태 표시입니다. 사용자가 직접 누르는 UI는 아닙니다. */
internal class CheckingOverlay(private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val density = context.resources.displayMetrics.density
    private var statusView: LinearLayout? = null

    private fun dp(value: Int) = (value * density + 0.5f).toInt()

    fun show() {
        if (statusView != null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) return

        val prefs = context.getSharedPreferences("ansim_settings", Context.MODE_PRIVATE)
        val mode = prefs.getString("notification_type", "기본 알림").orEmpty()
        val language = prefs.getString("language", "한국어").orEmpty()
        val title = when {
            mode == "쉬운 안내" -> "잠깐 기다려 주세요"
            language == "English" -> "Checking message · 확인 중"
            language == "Tiếng Việt" -> "Đang kiểm tra · 확인 중"
            language == "中文" -> "正在检查 · 확인 중"
            else -> "문자를 확인하고 있어요"
        }
        val subtitle = when (mode) {
            "쉬운 안내" -> "링크와 송금은 기다려 주세요"
            "화면 안내 중심" -> "확인이 끝날 때까지 기다려 주세요"
            else -> "확인이 끝날 때까지 링크를 누르거나 송금하지 마세요"
        }
        val green = Color.rgb(32, 83, 160)
        val background = GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = dp(18).toFloat()
            setStroke(dp(1), Color.rgb(219, 225, 238))
        }
        val chip = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(15), dp(13), dp(15), dp(13))
            this.background = background
            elevation = dp(8).toFloat()
            // TalkBack으로 읽을 수 있지만 화면 포커스를 빼앗거나 아래 앱의 터치를 막지는 않습니다.
            contentDescription = "$title. $subtitle"
        }
        val loading = ProgressBar(context, null, android.R.attr.progressBarStyleSmall).apply {
            isIndeterminate = true
            indeterminateTintList = ColorStateList.valueOf(green)
        }
        chip.addView(loading, LinearLayout.LayoutParams(dp(25), dp(25)).apply {
            rightMargin = dp(11)
        })
        val labels = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        labels.addView(TextView(context).apply {
            text = title
            textSize = if (mode == "화면 안내 중심") 20f else 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(green)
        })
        labels.addView(TextView(context).apply {
            text = subtitle
            textSize = if (mode == "화면 안내 중심") 16f else 14f
            setTextColor(Color.rgb(85, 91, 88))
        })
        chip.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))

        val width = min(dp(335), context.resources.displayMetrics.widthPixels - dp(32))
        val params = WindowManager.LayoutParams(
            width, WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(16)
            y = dp(28)
        }
        runCatching { windowManager.addView(chip, params) }
            .onSuccess { statusView = chip }
    }

    fun dismiss() {
        statusView?.let { view -> runCatching { windowManager.removeView(view) } }
        statusView = null
    }
}