package com.hackathon.ansimmessagepractice.ui.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hackathon.ansimmessagepractice.ui.theme.*

@Composable
fun LanguageScreen(selectedLanguage: String, onLanguageSelected: (String) -> Unit,
                   onNextClick: () -> Unit, onBackClick: () -> Unit) {
    OnboardingPage(bottom = {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AnsimOutlineButton("이전", onBackClick, Modifier.weight(1f))
            AnsimPrimaryButton("다음", onNextClick, modifier = Modifier.weight(1.74f))
        }
    }) {
        OnboardingStepHeader(2, onBackClick)
        Spacer(Modifier.height(22.dp))
        AnsimHeading("안내 언어를\n선택해 주세요", "경고 화면과 음성 안내에 적용돼요.")
        Spacer(Modifier.height(27.dp))
        HorizontalDivider(color = AnsimDivider, thickness = 1.dp)
        listOf("한국어", "English", "Tiếng Việt", "中文").forEach { language ->
            val selected = selectedLanguage == language
            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp)
                .clickable(role = Role.RadioButton) { onLanguageSelected(language) },
                verticalAlignment = Alignment.CenterVertically) {
                Text(language, Modifier.weight(1f), color = AnsimInk,
                    style = ansimStyle(18, 25, if (selected) FontWeight.Bold else FontWeight.Normal))
                AnsimRadio(selected)
            }
            HorizontalDivider(color = AnsimDivider, thickness = 1.dp)
        }
    }
}
