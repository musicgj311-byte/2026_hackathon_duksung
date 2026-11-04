package com.hackathon.ansimmessagepractice.ui.warning

/**
 * POST /judge -> JudgeResponse (backend/app/schemas.py) 화면 계약.
 * Mock 데이터는 '화면 미리보기'에서만 사용하며 실제 AI 판정으로 취급하지 않습니다.
 */
data class WarningPayload(
    val level: String,
    val scamType: String,
    val title: String,
    val explanation: String,
    val action: String,
    val easySentence: String,
    val language: String = "ko",
    val sender: String? = null,
    val sourceApp: String = "sms",
    val receivedMessage: String? = null,
    /** null이면 FCM 발송 상태를 모름. /judge 응답만으로 성공을 알 수 없음. */
    val guardianNotificationStatus: String? = null
)

object PreviewJudgeResults {
    fun forLevel(level: String, language: String = "ko"): WarningPayload {
        val base = when (level) {
            "caution" -> WarningPayload(
                level = "caution", scamType = "delivery", title = "사기일 수 있어요.\n링크를 누르지 마세요.",
                explanation = "처음 보는 인터넷 주소가 포함되어 있어요.",
                action = "택배는 사용하는 택배 앱이나 송장번호로 직접 확인하세요.",
                easySentence = "링크 누르지 마세요.", language = language,
                sender = "1588-****", receivedMessage = "고객님 택배가 미수령 상태입니다. 재배송 신청 http://post-redeliver.example/k2"
            )
            "checking" -> WarningPayload(
                level = "checking", scamType = "none", title = "문자를 확인하고 있어요",
                explanation = "확인이 끝날 때까지 링크를 누르거나 송금하지 마세요.",
                action = "잠시 후 결과를 확인하세요.", easySentence = "잠깐 멈춰 주세요.", language = language
            )
            "timeout" -> WarningPayload(
                level = "timeout", scamType = "none", title = "확인을 완료하지 못했어요",
                explanation = "연결 상태가 불안정해 안전 여부를 아직 알 수 없어요.",
                action = "확인되기 전까지 링크를 누르거나 송금하지 마세요.",
                easySentence = "먼저 확인하세요.", language = language
            )
            else -> WarningPayload(
                level = "danger", scamType = "family_impersonation", title = "사기 문자예요.\n돈을 보내지 마세요.",
                explanation = "가족인 척 송금을 요구하고 있어요.",
                action = "가족에게 원래 번호로 직접 전화해 확인하세요.",
                easySentence = "돈 보내지 마세요.", language = language,
                sender = "010-****-2741", receivedMessage = "엄마 나 폰 액정 깨져서 임시폰으로 연락해. 50만원만 이 계좌로 보내줄 수 있어?"
            )
        }
        if (language == "ko") return base
        // 다국어 시연에서는 fallback_messages.py의 단계별 문구를 기반으로 표시합니다.
        // 실제 동적 번역은 /judge 응답의 문구를 그대로 사용합니다.
        val translated = when (language) {
            "en" -> when (base.level) {
                "danger" -> listOf("This looks like a scam", "This message may be trying to take your money or information.", "Do not tap the link or send money. Call someone you trust.", "Do not tap. Do not pay.")
                "caution" -> listOf("Be careful", "This message has signs often used in scams.", "Before tapping or paying, call the sender directly to check.", "Check by phone first.")
                else -> listOf("Checking your message", "Do not tap links or send money yet.", "Wait for the result.", "Wait and check.")
            }
            "vi" -> when (base.level) {
                "danger" -> listOf("Đây là tin nhắn lừa đảo.\nĐừng chuyển tiền.", "Họ giả làm người thân để bảo bạn chuyển tiền.", "Gọi trực tiếp cho gia đình bằng số đã lưu để kiểm tra.", "Đừng chuyển tiền.")
                "caution" -> listOf("Hãy cẩn thận", "Tin nhắn này có dấu hiệu thường gặp trong lừa đảo.", "Trước khi bấm hoặc chuyển tiền, hãy gọi điện kiểm tra trực tiếp.", "Gọi điện kiểm tra trước.")
                else -> listOf("Đang kiểm tra", "Đừng nhấn liên kết hoặc chuyển tiền.", "Hãy chờ kết quả.", "Hãy chờ.")
            }
            "zh" -> when (base.level) {
                "danger" -> listOf("这可能是诈骗短信", "这条短信可能想骗取你的钱或个人信息。", "不要点击链接，也不要转账。请直接致电家人确认。", "不要转账。")
                "caution" -> listOf("请小心", "这条短信有诈骗中常见的特征。", "点击或转账前，请先打电话直接确认。", "先打电话确认。")
                else -> listOf("正在检查短信", "请先不要点击链接或转账。", "等待检查结果。", "请稍等。")
            }
            else -> return base
        }
        return base.copy(title = translated[0], explanation = translated[1], action = translated[2], easySentence = translated[3])
    }
}

fun localizedCode(displayLanguage: String): String = when (displayLanguage) {
    "English" -> "en"
    "Tiếng Việt" -> "vi"
    "中文" -> "zh"
    else -> "ko"
}

fun userTypeCode(displayType: String): String = when (displayType) {
    "음성 안내 중심" -> "visual"
    "화면 안내 중심" -> "hearing"
    "쉬운 안내" -> "developmental"
    else -> "default"
}

fun scamLabel(code: String): String = when (code) {
    "family_impersonation" -> "가족 사칭"
    "agency_impersonation" -> "기관 사칭"
    "delivery" -> "택배 사칭"
    "obituary_invitation" -> "부고·초대장 사칭"
    "subsidy_refund" -> "지원금·환급 사칭"
    "payment_alert" -> "결제 알림 사칭"
    "job_loan" -> "구인·대출 사칭"
    "immigration" -> "출입국·체류 사칭"
    "none" -> "사기 의심"
    else -> "사기 의심"
}