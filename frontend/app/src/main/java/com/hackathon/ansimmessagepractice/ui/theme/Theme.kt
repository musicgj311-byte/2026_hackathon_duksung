package com.hackathon.ansimmessagepractice.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AnsimColorScheme = lightColorScheme(
    primary = AnsimGreen,
    onPrimary = Color.White,
    primaryContainer = AnsimMint,
    onPrimaryContainer = AnsimGreenDark,
    secondary = AnsimGreenDark,
    onSecondary = Color.White,
    background = AnsimCream,
    onBackground = AnsimInk,
    surface = Color.White,
    onSurface = AnsimInk,
    outline = Color(0xFFDCE1E9)
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