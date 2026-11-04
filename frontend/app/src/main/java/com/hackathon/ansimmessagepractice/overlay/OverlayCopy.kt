package com.hackathon.ansimmessagepractice.overlay

import android.graphics.Color

/** 알림 유형별로 표시할 오버레이 문구입니다. 개인 메시지 원문이나 AI 분석의 상세 근거는 여기서 표시하지 않습니다. */
internal data class OverlayCopy(
    val accent: Int,
    val headline: String,
    val guide: String,
    val titleKo: String,
    val bilingual: Boolean,
    val hearing: Boolean,
    val voice: Boolean,
    val easy: Boolean,
    val callLabel: String,
    val closeLabel: String
)

internal fun overlayCopy(risk: String, mode: String, language: String): OverlayCopy {
    val easy = mode == "쉬운 안내"
    val hearing = mode == "화면 안내 중심"
    val voice = mode == "음성 안내 중심"
    val bilingual = language != "한국어" && !easy && !hearing

    val accent = when (risk) {
        "danger" -> Color.rgb(165, 30, 35)
        "checking" -> Color.rgb(14, 90, 65)
        else -> Color.rgb(149, 93, 11)
    }
    // 화면 사용 중 상단 오버레이는 길고 반복적인 안내 대신 한 문장만 보여줍니다.
    val titleKo = when (risk) {
        "danger" -> "방금 받은 문자의 링크를 누르거나 송금하지 마세요."
        "checking" -> "문자를 확인하고 있어요."
        else -> "사기일 수 있어요. 링크를 누르지 마세요."
    }
    val titleForeign = when (language) {
        "Tiếng Việt" -> when (risk) {
            "danger" -> "Đừng nhấn liên kết hoặc chuyển tiền."
            "checking" -> "Đang kiểm tra tin nhắn."
            else -> "Có thể là lừa đảo. Đừng nhấn liên kết."
        }
        "English" -> when (risk) {
            "danger" -> "Do not tap links or send money."
            "checking" -> "Checking your message."
            else -> "This may be a scam. Do not tap the link."
        }
        "中文" -> when (risk) {
            "danger" -> "不要点击链接或转账。"
            "checking" -> "正在检查短信。"
            else -> "可能是诈骗，请勿点击链接。"
        }
        else -> titleKo
    }
    val shortTitle = when (risk) {
        "danger" -> "돈 보내지 마세요."
        "checking" -> "잠깐 멈춰 주세요."
        else -> "링크 누르지 마세요."
    }
    val headline = when {
        easy -> shortTitle
        bilingual -> titleForeign
        hearing -> if (risk == "danger") "사기 문자예요. 돈을 보내지 마세요." else titleKo
        else -> titleKo
    }
    val guide = if (bilingual) titleKo else ""

    val callLabel = if (bilingual) when (language) {
        "English" -> "Call family · 가족에게 전화하기"
        "Tiếng Việt" -> "Gọi gia đình · 가족에게 전화하기"
        "中文" -> "给家人打电话 · 가족에게 전화하기"
        else -> "가족에게 전화하기"
    } else "☎  가족에게 전화하기"
    return OverlayCopy(accent, headline, guide, titleKo, bilingual, hearing, voice,
        easy, callLabel, if (bilingual) "닫기 · Close" else "닫기")
}