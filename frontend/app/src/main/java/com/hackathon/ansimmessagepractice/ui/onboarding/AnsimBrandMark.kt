package com.hackathon.ansimmessagepractice.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hackathon.ansimmessagepractice.ui.theme.AnsimGreen

/** 시작·스플래시·홈 전체에서 동일하게 쓰는 파란 사각형 + 방패/체크 로고. */
@Composable
fun AnsimBrandMark(size: Dp = 46.dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(size).background(AnsimGreen, RoundedCornerShape(size * .20f)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(size * .72f)) {
            val w = this.size.width
            val h = this.size.height
            val path = Path().apply {
                moveTo(w * .50f, h * .09f)
                lineTo(w * .85f, h * .23f)
                lineTo(w * .82f, h * .59f)
                quadraticTo(w * .78f, h * .77f, w * .50f, h * .91f)
                quadraticTo(w * .22f, h * .77f, w * .18f, h * .59f)
                lineTo(w * .15f, h * .23f)
                close()
            }
            drawPath(path, Color.White, style = Stroke(width = w * .078f))
            drawLine(Color.White, Offset(w * .32f, h * .48f), Offset(w * .46f, h * .62f),
                strokeWidth = w * .09f, cap = StrokeCap.Round)
            drawLine(Color.White, Offset(w * .46f, h * .62f), Offset(w * .72f, h * .36f),
                strokeWidth = w * .09f, cap = StrokeCap.Round)
        }
    }
}
