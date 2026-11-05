package com.hackathon.ansimmessagepractice.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hackathon.ansimmessagepractice.ui.theme.AnsimBlue
import com.hackathon.ansimmessagepractice.ui.theme.AnsimType

/**
 * 시안 3~10p 상단: "n / 4" (15px 굵게, 파랑) + 7dp 아래 3dp 진행 표시줄.
 * 화면 위쪽 여백(22dp)은 [OnboardingPage]가 담당합니다.
 * 뒤로 가기는 시스템 뒤로 버튼(BackHandler)과 하단 '이전' 버튼으로 처리합니다.
 */
@Composable
internal fun OnboardingStepHeader(step: Int, @Suppress("UNUSED_PARAMETER") onBackClick: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text("$step / 4", style = AnsimType.Step, color = AnsimBlue)
        Spacer(Modifier.height(7.5.dp))
        Box(Modifier.fillMaxWidth().height(3.dp).background(Color(0xFFECEEF0), RoundedCornerShape(2.dp))) {
            Box(Modifier.fillMaxWidth(step / 4f).fillMaxHeight().background(AnsimBlue, RoundedCornerShape(2.dp)))
        }
    }
}

/** 단계 라벨 대신 쓰는 작은 파란 머리말 (8p "보호자 · 가족 연결") */
@Composable
internal fun OnboardingEyebrow(text: String) {
    Text(text, style = AnsimType.Step, color = AnsimBlue)
}

/**
 * 온보딩 공통 틀: 흰 배경, 좌우 24dp, 위 22dp, 아래 32dp(버튼 아래 여백).
 * [content]는 위에서부터, [bottom]은 화면 아래에 붙습니다.
 */
@Composable
internal fun OnboardingPage(
    bottomPadding: androidx.compose.ui.unit.Dp = 32.dp,
    bottom: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = bottomPadding)
    ) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), content = content)
        bottom()
    }
}
