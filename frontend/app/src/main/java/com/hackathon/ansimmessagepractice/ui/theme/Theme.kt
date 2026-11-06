package com.hackathon.ansimmessagepractice.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AnsimColorScheme = lightColorScheme(
    primary = AnsimBlue,
    onPrimary = Color.White,
    primaryContainer = AnsimBlueTint,
    onPrimaryContainer = AnsimBlueText,
    secondary = AnsimBlueText,
    onSecondary = Color.White,
    background = Color.White,
    onBackground = AnsimInk,
    surface = Color.White,
    onSurface = AnsimInk,
    onSurfaceVariant = AnsimMuted,
    outline = AnsimBorder,
    outlineVariant = AnsimDivider,
    error = AnsimDanger
)

@Composable
fun AnsimMessagePracticeTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // 해커톤 시연에서는 기기 설정과 상관없이 같은 브랜드 색을 유지합니다.
    MaterialTheme(
        colorScheme = AnsimColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun StatusBarIcons(light: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view, light) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = !light
        onDispose { if (previous != null) controller?.isAppearanceLightStatusBars = previous }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
