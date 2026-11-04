package com.hackathon.ansimmessagepractice.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackathon.ansimmessagepractice.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) { delay(850); onFinished() }
    Box(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding()) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            AnsimBrandMark(size = 68.dp)
            Spacer(Modifier.height(18.dp))
            Text("안심 문자", color = AnsimInk, fontSize = 33.sp, fontWeight = FontWeight.ExtraBold)
            Text("모두를 위한 문자피싱 예방 서비스", color = AnsimMuted, fontSize = 14.sp)
        }
        Text("설정을 확인하고 있습니다", Modifier.align(Alignment.BottomCenter).padding(bottom = 30.dp),
            color = AnsimMuted, fontSize = 12.sp)
    }
}