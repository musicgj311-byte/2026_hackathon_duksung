package com.hackathon.ansimmessagepractice.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackathon.ansimmessagepractice.ui.theme.AnsimGreen

/** 페이지 상단에 단계 및 얇은 진행 표시줄. */
@Composable
internal fun OnboardingStepHeader(step: Int, onBackClick: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text("$step / 4", color = AnsimGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(9.dp))
        Row(Modifier.fillMaxWidth().height(2.dp).background(Color(0xFFE1E5EB), RoundedCornerShape(2.dp))) {
            Box(Modifier.fillMaxWidth(step / 4f).fillMaxHeight().background(AnsimGreen, RoundedCornerShape(2.dp)))
        }
    }
}
