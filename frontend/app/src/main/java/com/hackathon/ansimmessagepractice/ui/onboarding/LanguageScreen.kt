package com.hackathon.ansimmessagepractice.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackathon.ansimmessagepractice.ui.theme.*

@Composable
fun LanguageScreen(selectedLanguage: String, onLanguageSelected: (String) -> Unit,
                   onNextClick: () -> Unit, onBackClick: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding()
        .padding(horizontal = 25.dp, vertical = 17.dp)) {
        OnboardingStepHeader(3, onBackClick)
        Spacer(Modifier.height(20.dp))
        Text("안내 언어를\n선택해 주세요", fontSize = 28.sp, lineHeight = 35.sp,
            fontWeight = FontWeight.ExtraBold, color = AnsimInk)
        Spacer(Modifier.height(6.dp))
        Text("경고 화면과 음성 안내에 적용돼요.", fontSize = 14.sp, color = AnsimMuted)
        Spacer(Modifier.height(25.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            listOf("한국어", "English", "Tiếng Việt", "中文").forEach { language ->
                Row(Modifier.fillMaxWidth().height(61.dp).clickable { onLanguageSelected(language) },
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(language, Modifier.weight(1f), color = AnsimInk, fontSize = 17.sp,
                        fontWeight = FontWeight.Medium)
                    RadioButton(selected = selectedLanguage == language, onClick = null,
                        colors = RadioButtonDefaults.colors(selectedColor = AnsimGreen))
                }
                HorizontalDivider(color = Color(0xFFE9EBEF))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            OutlinedButton(onClick = onBackClick, modifier = Modifier.weight(1f).height(54.dp)) { Text("이전", color = AnsimInk) }
            Box(Modifier.weight(2f)) { AnsimPrimaryButton("다음", onNextClick) }
        }
    }
}
