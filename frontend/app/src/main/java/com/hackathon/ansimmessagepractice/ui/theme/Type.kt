package com.hackathon.ansimmessagepractice.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

const val DESIGN_FONT_SCALE = 0.935f

val Int.dsp: TextUnit get() = (this * DESIGN_FONT_SCALE).sp
val Double.dsp: TextUnit get() = (this * DESIGN_FONT_SCALE).sp

private val CssLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

private fun base(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = ansimStyle(size, line.toDouble(), weight)

fun ansimStyle(size: Number, line: Number, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = (size.toFloat() * DESIGN_FONT_SCALE).sp,
    lineHeight = line.toFloat().sp,
    letterSpacing = 0.sp,
    lineHeightStyle = CssLineHeight
)

object AnsimType {
    val Title = ansimStyle(26, 36.4, FontWeight.Bold)
    val Lead = ansimStyle(17, 24)
    val LeadLong = ansimStyle(17, 27)
    val Body = ansimStyle(15, 22)
    val BodyLong = ansimStyle(15, 24)
    val Step = ansimStyle(15, 22, FontWeight.Bold)
    val Button = ansimStyle(18, 24, FontWeight.Bold)
    val ButtonSmall = ansimStyle(17, 24, FontWeight.Bold)
    val Link = ansimStyle(15, 22, FontWeight.Medium)
    val TopBar = ansimStyle(18, 26, FontWeight.Bold)
    val Section = ansimStyle(15, 22, FontWeight.SemiBold)
    val Row = ansimStyle(17, 24)
}

val Typography = Typography(
    displayLarge = base(52, 58, FontWeight.ExtraBold),
    displayMedium = base(45, 52, FontWeight.ExtraBold),
    displaySmall = base(36, 44, FontWeight.ExtraBold),
    headlineLarge = base(30, 40, FontWeight.Bold),
    headlineMedium = base(28, 38, FontWeight.Bold),
    headlineSmall = base(26, 36, FontWeight.Bold),
    titleLarge = base(22, 30, FontWeight.Bold),
    titleMedium = base(18, 26, FontWeight.Bold),
    titleSmall = base(15, 22, FontWeight.Bold),
    bodyLarge = base(17, 25),
    bodyMedium = base(15, 22),
    bodySmall = base(13, 19),
    labelLarge = base(17, 24, FontWeight.Bold),
    labelMedium = base(15, 22, FontWeight.Medium),
    labelSmall = base(13, 18, FontWeight.Medium)
)
