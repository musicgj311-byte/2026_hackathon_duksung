package com.hackathon.ansimmessagepractice.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * 시안은 Pretendard(웹 px) 기준이고, 안드로이드 기본 한글 글꼴(Noto Sans KR)은 같은 숫자에서
 * 글자가 약 7% 더 크게 보입니다. 시안 PDF에서 글자 높이·폭을 직접 재서 맞춘 비율입니다.
 * 예) 시안 26px 제목 → 24.3sp, 시안 17px 본문 → 15.9sp
 *
 * 화면 코드에서는 시안 숫자를 그대로 쓰도록 `26.dsp`처럼 작성합니다.
 * 줄 간격(lineHeight)은 레이아웃 높이이므로 시안 px 값을 그대로 `.sp`로 씁니다.
 */
const val DESIGN_FONT_SCALE = 0.935f

/** 시안 글자 크기(px) → 기기 글꼴 기준 sp */
val Int.dsp: TextUnit get() = (this * DESIGN_FONT_SCALE).sp
val Double.dsp: TextUnit get() = (this * DESIGN_FONT_SCALE).sp

// CSS처럼 줄 높이 안에서 글자를 가운데 정렬하고, 첫 줄/마지막 줄 여백을 잘라내지 않습니다.
// 이렇게 해야 시안의 줄 간격·버튼 높이와 같은 세로 리듬이 나옵니다.
private val CssLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

private fun base(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = ansimStyle(size, line.toDouble(), weight)

/** 시안 px 크기·줄 높이로 만든 글자 스타일 */
fun ansimStyle(size: Number, line: Number, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = (size.toFloat() * DESIGN_FONT_SCALE).sp,
    lineHeight = line.toFloat().sp,
    letterSpacing = 0.sp,
    lineHeightStyle = CssLineHeight
)

/** 시안에서 반복되는 글자 스타일 (크기는 시안 px 기준) */
object AnsimType {
    /** 화면 제목 26px / 줄 36.4 */
    val Title = ansimStyle(26, 36.4, FontWeight.Bold)
    /** 제목 아래 설명 17px */
    val Lead = ansimStyle(17, 24)
    /** 두 줄 이상 설명 17px / 줄 27 */
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

// Material 3 기본 letterSpacing(0.5sp 등)은 한글을 시안보다 넓게 벌려 놓으므로 모두 0으로 둡니다.
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
