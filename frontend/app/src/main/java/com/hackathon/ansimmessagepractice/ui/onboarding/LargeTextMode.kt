package com.hackathon.ansimmessagepractice.ui.onboarding

/** 한 번 선택한 알림 방식만으로 확대 여부를 결정합니다. 이전 버전의 별도 스위치는 무시합니다. */
internal const val BIG_TEXT_FONT_MULTIPLIER = 1.22f

internal fun usesLargeText(type: String?, role: String? = "protected"): Boolean =
    role == "protected" && type == "기본 알림"