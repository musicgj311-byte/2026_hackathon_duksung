package com.hackathon.ansimmessagepractice.ui.onboarding

/** 일반 알림(큰 글씨+소리)을 선택한 보호 대상자에게 적용하는 SP 글꼴 확대 배율. */
const val BIG_TEXT_FONT_MULTIPLIER: Float = 1.25f

/**
 * 추가 요구 사항: '큰 글씨와 소리' 모드를 고른 보호 대상자는 앱 전체 글씨(오버레이 포함)를 키웁니다.
 * false로 바꾸면 시안처럼 일반 화면을 기본 크기로 표시합니다.
 */
const val APPLY_LARGE_TEXT_TO_APP_SCREENS: Boolean = true

fun usesLargeText(notificationType: String, role: String? = "protected"): Boolean =
    role != "guardian" && notificationType == "기본 알림"

/** 화면·오버레이에 실제로 적용할 글꼴 배율 */
fun largeTextScale(notificationType: String, role: String? = "protected"): Float =
    if (APPLY_LARGE_TEXT_TO_APP_SCREENS && usesLargeText(notificationType, role)) BIG_TEXT_FONT_MULTIPLIER else 1f
