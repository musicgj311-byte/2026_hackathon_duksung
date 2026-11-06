package com.hackathon.ansimmessagepractice.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun UserTypeScreen(selectedType: String, onTypeSelected: (String) -> Unit,
                   onNextClick: () -> Unit, onBackClick: () -> Unit) {
    val rows = listOf(
        "기본 알림" to "큰 글씨와 소리로 알려주세요. (추천)",
        "화면 안내 중심" to "소리보다는 화면과 진동으로 알려주세요.",
        "쉬운 안내" to "단순한 게 좋아요.",
        "음성 안내 중심" to "화면보다는 소리가 편해요."
    )
    OnboardingPage(bottom = { AnsimPrimaryButton("다음", onNextClick) }) {
        OnboardingStepHeader(1, onBackClick)
        Spacer(Modifier.height(22.dp))
        AnsimHeading("위험할 때\n어떻게 알려드릴까요?", "설정에서 언제든 바꿀 수 있어요.")
        Spacer(Modifier.height(31.dp))
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            rows.forEach { (value, label) -> AnsimChoiceRow(label, selectedType == value) { onTypeSelected(value) } }
        }
        Spacer(Modifier.height(16.dp))
    }
}
