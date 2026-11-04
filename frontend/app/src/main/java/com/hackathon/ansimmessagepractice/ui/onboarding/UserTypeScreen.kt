package com.hackathon.ansimmessagepractice.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackathon.ansimmessagepractice.ui.theme.*

@Composable
fun UserTypeScreen(selectedType: String, onTypeSelected: (String) -> Unit,
                   onNextClick: () -> Unit, onBackClick: () -> Unit) {
    val rows = listOf(
        "기본 알림" to "큰 글씨와 소리로 알려주세요. (추천)",
        "화면 안내 중심" to "소리보다는 화면과 진동으로 알려주세요.",
        "쉬운 안내" to "단순한 게 좋아요.",
        "음성 안내 중심" to "화면보다는 소리가 편해요."
    )
    Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding()
        .padding(horizontal = 25.dp, vertical = 17.dp)) {
        OnboardingStepHeader(2, onBackClick)
        Spacer(Modifier.height(20.dp))
        Text("위험할 때\n어떻게 알려드릴까요?", color = AnsimInk, fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold, lineHeight = 35.sp)
        Spacer(Modifier.height(6.dp))
        Text("설정에서 언제든 바꿀 수 있어요.", color = AnsimMuted, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(9.dp)) {
            rows.forEach { (value, label) -> AnsimChoiceRow(label, selectedType == value) { onTypeSelected(value) } }
        }
        Spacer(Modifier.height(14.dp))
        AnsimPrimaryButton("다음", onNextClick)
    }
}