package com.hackathon.ansimmessagepractice.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.hackathon.ansimmessagepractice.ui.theme.*

/**
 * 최종 시안 1p. 배경의 두 링은 중심이 서로 다릅니다(시안 측정값, 390dp 폭 기준).
 *  - 바깥 링: 중심 (244, 386), 반지름 238, 두께 56, 흰색 7%
 *  - 안쪽 링: 중심 (260, 370), 반지름 150, 두께 41, 흰색 9%
 */
@Composable
fun StartScreen(name: String, modifier: Modifier = Modifier, onStartClick: () -> Unit) {
    StatusBarIcons(light = true)
    Box(modifier.fillMaxSize().background(AnsimBlue).statusBarsPadding().navigationBarsPadding()) {
        Canvas(Modifier.fillMaxSize()) {
            val k = size.width / 390.dp.toPx() * 1.dp.toPx()
            drawCircle(Color.White.copy(alpha = .07f), radius = 238f * k, center = Offset(244f * k, 386f * k),
                style = Stroke(width = 56f * k))
            drawCircle(Color.White.copy(alpha = .09f), radius = 150f * k, center = Offset(260f * k, 370f * k),
                style = Stroke(width = 41f * k))
        }
        Column(Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 64.dp, bottom = 32.dp)) {
            AnsimBrandMark(size = 56.dp, inverse = true)
            Spacer(Modifier.weight(1f))
            Text("의심하지\n않아도\n괜찮아요.", color = Color.White,
                style = ansimStyle(52, 58.2, FontWeight.ExtraBold).copy(letterSpacing = (-0.02).em))
            Spacer(Modifier.height(20.dp))
            Text("사기 문자는 안심문자가 먼저 보고,\n위험하면 가족에게도 알려요.",
                color = Color.White.copy(alpha = .92f), style = ansimStyle(19, 29.5))
            Spacer(Modifier.height(30.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("자동 확인", "쉬운 경고", "가족 알림").forEach { label ->
                    Box(Modifier.heightIn(min = 32.dp).background(Color.White.copy(alpha = .12f), CircleShape)
                        .padding(horizontal = 14.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text(label, color = Color.White, style = ansimStyle(15, 20, FontWeight.Bold))
                    }
                }
            }
            Spacer(Modifier.height(34.dp))
            AnsimPrimaryButton("시작하기", onStartClick, container = Color.White, contentColor = AnsimBlue,
                height = 64.dp, textStyle = ansimStyle(19, 26, FontWeight.Bold))
        }
    }
}
