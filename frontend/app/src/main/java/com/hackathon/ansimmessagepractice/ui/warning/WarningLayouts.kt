package com.hackathon.ansimmessagepractice.ui.warning

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackathon.ansimmessagepractice.ui.theme.*

/** 가족 연락 버튼을 강조한 간단한 큰 글씨 경고 화면입니다. */
@Composable
internal fun StandardWarning(
    status: Status, words: WarningWords, color: Color,
    message: String?, sender: String?, notified: Boolean, canReplay: Boolean,
    replay: () -> Unit, close: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(color).statusBarsPadding().navigationBarsPadding()
        .padding(horizontal = 24.dp, vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Spacer(Modifier.height(12.dp))
            WarningIcon(status, color)
            Text(words.title, fontSize = 33.sp, lineHeight = 42.sp, fontWeight = FontWeight.ExtraBold,
                color = Color.White, textAlign = TextAlign.Center)
            Text(words.guide, fontSize = 19.sp, lineHeight = 27.sp,
                color = Color.White, textAlign = TextAlign.Center)
            ReceivedCard(words.received, message, sender, color)
            if (canReplay) ReplayButton(words.replay, replay)
            if (notified) Text("✓ ${words.notified}", color = Color.White, fontSize = 16.sp)
        }
        Spacer(Modifier.height(14.dp))
        CallButton(words.call, color)
        Spacer(Modifier.height(9.dp))
        CloseButton(words.close, close)
    }
}

/** 쉬운 안내가 필요한 사용자를 위해 그림과 짧은 문장 하나로 경고합니다. */
@Composable
internal fun EasyWarning(status: Status, color: Color, close: () -> Unit) {
    val action = when (status) {
        Status.DANGER -> "돈 보내지\n마세요."
        Status.CAUTION -> "링크 누르지\n마세요."
        Status.CHECKING -> "잠깐 멈춰\n주세요."
    }
    Column(Modifier.fillMaxSize().background(Color(0xFFFFF8F6))
        .statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(188.dp).semantics { contentDescription = "하지 마세요" },
                contentAlignment = Alignment.Center) {
                Text(if (status == Status.CAUTION) "🔗" else "₩", fontSize = 98.sp, color = AnsimGreen,
                    fontWeight = FontWeight.Bold)
                Canvas(Modifier.fillMaxSize()) {
                    val strokeWidth = 11.dp.toPx()
                    drawCircle(Color(0xFFA51E23), radius = size.minDimension / 2 - strokeWidth,
                        style = Stroke(width = strokeWidth))
                    drawLine(Color(0xFFA51E23), Offset(size.width * .16f, size.height * .16f),
                        Offset(size.width * .84f, size.height * .84f), strokeWidth = strokeWidth)
                }
            }
            Spacer(Modifier.height(42.dp))
            Text(action, color = Color(0xFFA51E23), fontSize = 46.sp, lineHeight = 59.sp,
                fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        }
        CallButton("가족에게 전화하기", AnsimGreenDark, inverted = true)
        Spacer(Modifier.height(10.dp))
        TextButton(onClick = close) { Text("닫기", color = AnsimMuted, fontSize = 17.sp) }
    }
}

/** 화면 안내 중심 경고입니다. 큰 글씨와 강한 진동을 사용하며, 빠른 점멸이나 추가 연락 버튼은 두지 않습니다. */
@Composable
internal fun HearingWarning(status: Status, color: Color, close: () -> Unit) {
    val headline = when (status) {
        Status.DANGER -> "사기 문자예요.\n돈을 보내지 마세요."
        Status.CAUTION -> "조심하세요.\n링크를 누르지 마세요."
        Status.CHECKING -> "확인 중이에요.\n잠깐 멈춰 주세요."
    }
    Column(Modifier.fillMaxSize().background(color).statusBarsPadding().navigationBarsPadding()
        .padding(horizontal = 23.dp, vertical = 17.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Spacer(Modifier.height(13.dp))
            Text("!", modifier = Modifier.background(Color.White, CircleShape).size(78.dp).wrapContentSize(),
                color = color, fontSize = 57.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center)
            Text(headline, color = Color.White, fontSize = 40.sp, lineHeight = 51.sp,
                fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Surface(shape = RoundedCornerShape(18.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                Text("문자 속 링크나 번호를 사용하지 말고, 저장된 가족에게 확인하세요.",
                    Modifier.padding(20.dp), color = AnsimInk, fontSize = 20.sp, lineHeight = 30.sp,
                    fontWeight = FontWeight.SemiBold)
            }
        }
        CallButton("가족에게 전화하기", color)
        Spacer(Modifier.height(9.dp))
        CloseButton("닫기", close)
    }
}

/** 선택한 언어와 한국어를 한 화면에 표시하며 삭제 동작은 제공하지 않습니다. */
@Composable
internal fun BilingualWarning(
    status: Status, words: WarningWords, color: Color, message: String?, notified: Boolean,
    canReplay: Boolean, replay: () -> Unit, close: () -> Unit
) {
    val korean = wordsFor(status, "한국어")
    Column(Modifier.fillMaxSize().background(color).statusBarsPadding().navigationBarsPadding()
        .padding(horizontal = 24.dp, vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(15.dp)) {
            Text(words.title, color = Color.White, fontSize = 34.sp, lineHeight = 43.sp,
                fontWeight = FontWeight.ExtraBold)
            Text(korean.title.replace("\n", " "), color = Color.White, fontSize = 18.sp)
            Spacer(Modifier.height(5.dp))
            Text(words.guide, color = Color.White, fontSize = 22.sp, lineHeight = 31.sp)
            Text(korean.guide, color = Color.White, fontSize = 16.sp, lineHeight = 23.sp)
            ReceivedCard(words.received, message, null, color)
            if (canReplay) ReplayButton(words.replay, replay)
            if (notified) Text("✓ ${words.notified}", color = Color.White, fontSize = 16.sp)
        }
        Spacer(Modifier.height(12.dp))
        CallButton("${words.call}\n${korean.call}", color)
        Spacer(Modifier.height(9.dp))
        CloseButton("${words.close} · 닫기", close)
    }
}