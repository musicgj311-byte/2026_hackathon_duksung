package com.hackathon.ansimmessagepractice.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hackathon.ansimmessagepractice.ui.theme.AnsimBlue
import com.hackathon.ansimmessagepractice.ui.theme.AnsimIcon
import com.hackathon.ansimmessagepractice.ui.theme.AnsimIcons

/**
 * 시안 1p 로고: 둥근 사각형 안에 '말풍선 꼬리가 달린 방패' + 체크.
 * 좌표는 시안 로고(56dp)를 픽셀 단위로 측정한 값입니다.
 * inverse=true: 흰 사각형 + 파란 방패 (시작 화면), false: 파란 사각형 + 흰 방패.
 */
@Composable
fun AnsimBrandMark(size: Dp = 46.dp, modifier: Modifier = Modifier, inverse: Boolean = false,
                   showCheck: Boolean = true) {
    val boxColor = if (inverse) Color.White else AnsimBlue
    val markColor = if (inverse) AnsimBlue else Color.White
    Box(modifier.size(size).background(boxColor, RoundedCornerShape(size * .25f))) {
        Canvas(Modifier.fillMaxSize()) {
            drawShieldBubble(markColor, if (showCheck) boxColor else null)
        }
    }
}

/** 보호자 홈(34p)·잠금화면 알림(22p)의 앱 아이콘: 파란 사각형 + 흰 말풍선 방패 */
@Composable
fun AnsimAppIcon(size: Dp = 24.dp, modifier: Modifier = Modifier) =
    AnsimBrandMark(size = size, modifier = modifier, inverse = false, showCheck = false)

/** 보호자 알림 카드(33p) 아이콘: 파란 사각형 + 흰 선 방패 체크 */
@Composable
fun AnsimShieldBadge(size: Dp = 38.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).background(AnsimBlue, RoundedCornerShape(size * .24f)),
        contentAlignment = Alignment.Center) {
        AnsimIcon(AnsimIcons.ShieldCheck, Color.White, size * .58f)
    }
}

/** 56 격자 기준 방패+말풍선 꼬리 */
private fun DrawScope.drawShieldBubble(color: Color, checkColor: Color?) {
    val k = size.width / 56f
    val shield = Path().apply {
        moveTo(28f * k, 11.4f * k)
        lineTo(41.2f * k, 16f * k)
        lineTo(41.2f * k, 29f * k)
        cubicTo(41.2f * k, 36.5f * k, 36.2f * k, 41.6f * k, 28.6f * k, 44.6f * k)
        lineTo(14.2f * k, 45.8f * k)
        lineTo(18.6f * k, 38.6f * k)
        cubicTo(16.1f * k, 35.6f * k, 14.6f * k, 32.6f * k, 14.6f * k, 29f * k)
        lineTo(14.6f * k, 16f * k)
        close()
    }
    drawPath(shield, color)
    drawPath(shield, color, style = Stroke(width = 1.2f * k, join = StrokeJoin.Round))
    if (checkColor != null) {
        val check = Path().apply {
            moveTo(21.2f * k, 27.4f * k)
            lineTo(26.2f * k, 32.3f * k)
            lineTo(34.8f * k, 23.6f * k)
        }
        drawPath(check, checkColor, style = Stroke(width = 3.6f * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
