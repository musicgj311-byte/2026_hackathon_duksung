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
                level = "caution", scamType = "delivery", title = "링크를\n누르지\n마세요.",
                explanation = "처음 보는 인터넷 주소가 있어요.",
                action = "택배는 쓰시는 택배 앱이나 송장번호로 직접 확인하세요.",
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
                level = "danger", scamType = "family_impersonation", title = "돈을\n보내지\n마세요.",
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
                "danger" -> listOf("This is a scam message.\nDon't send money.", "They pretend to be family and ask you to send money.", "Call your family directly on the number you already know.", "Don't send money.")
                "caution" -> listOf("This may be a scam.\nDon't tap the link.", "It has a web address you haven't seen before.", "Check your delivery only in your delivery app or with the tracking number.", "Don't tap the link.")
                "timeout" -> listOf("We couldn't finish checking", "The network is unstable. We don't know yet if this message is safe.", "Until it's checked, don't tap links or send money.", "Check first.")
                else -> listOf("Checking the message", "Don't tap links or send money until it's done.", "Wait for the result.", "Please wait.")
            }
            "vi" -> when (base.level) {
                "danger" -> listOf("Đây là tin nhắn lừa đảo.\nĐừng chuyển tiền.", "Họ giả làm người thân để bảo bạn chuyển tiền.", "Hãy gọi thẳng cho gia đình bằng số bạn đã biết.", "Đừng chuyển tiền.")
                "caution" -> listOf("Có thể là lừa đảo.\nĐừng nhấn vào liên kết.", "Tin nhắn có địa chỉ web lạ.", "Hãy kiểm tra trong ứng dụng giao hàng hoặc bằng mã vận đơn.", "Đừng nhấn liên kết.")
                "timeout" -> listOf("Chưa thể kiểm tra xong", "Kết nối mạng không ổn định. Chưa biết tin nhắn này có an toàn không.", "Trước khi kiểm tra xong, đừng nhấn liên kết hoặc chuyển tiền.", "Hãy kiểm tra trước.")
                else -> listOf("Đang kiểm tra tin nhắn", "Đừng nhấn liên kết hoặc chuyển tiền cho đến khi xong.", "Hãy chờ kết quả.", "Hãy chờ.")
            }
            "zh" -> when (base.level) {
                "danger" -> listOf("这是诈骗短信。\n不要转账。", "对方假装家人，要求你转账。", "请用你已知道的号码直接打给家人确认。", "不要转账。")
                "caution" -> listOf("可能是诈骗。\n不要点击链接。", "短信里有陌生的网址。", "请在快递App里或用运单号直接查询。", "不要点击链接。")
                "timeout" -> listOf("未能完成检查", "网络不稳定，暂时无法确定这条短信是否安全。", "确认之前，请不要点击链接或转账。", "请先确认。")
                else -> listOf("正在检查短信", "检查完成前，请不要点击链接或转账。", "请等待结果。", "请稍等。")
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