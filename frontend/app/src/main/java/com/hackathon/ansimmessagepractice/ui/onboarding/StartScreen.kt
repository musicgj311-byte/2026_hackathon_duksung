package com.hackathon.ansimmessagepractice.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackathon.ansimmessagepractice.ui.theme.*

/** 상단 소개·세 가지 핵심 설명·하단 시작 버튼. */
@Composable
fun StartScreen(name: String, modifier: Modifier = Modifier, onStartClick: () -> Unit) {
    Column(modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding()
        .padding(horizontal = 25.dp, vertical = 22.dp)) {
        AnsimBrandMark(size = 48.dp)
        Spacer(Modifier.height(18.dp))
        Text("문자 사기,\n먼저 확인해 드려요", fontSize = 29.sp, lineHeight = 37.sp,
            color = AnsimInk, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(7.dp))
        Text("직접 의심하거나 확인하지 않아도 됩니다.", color = AnsimMuted, fontSize = 15.sp)
        Spacer(Modifier.height(25.dp))
        listOf(
            "▢" to "문자가 오면 자동으로 확인해요",
            "⚠" to "위험하면 쉬운 말로 알려드려요",
            "♧" to "가족에게도 함께 알려요"
        ).forEach { (icon, label) ->
            Row(Modifier.fillMaxWidth().height(55.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(icon, color = AnsimGreen, fontSize = 21.sp, modifier = Modifier.width(42.dp))
                Text(label, color = AnsimInk, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
            androidx.compose.material3.HorizontalDivider(color = Color(0xFFF0F1F3))
        }
        Spacer(Modifier.weight(1f))
        AnsimPrimaryButton("시작하기", onStartClick)
    }
}