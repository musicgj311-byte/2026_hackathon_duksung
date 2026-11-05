package com.hackathon.ansimmessagepractice.ui.warning

/**
 * 경고 화면·오버레이·알림에서 함께 쓰는 고정 문구 모음.
 *
 * - 큰 제목은 서버 title 대신 여기의 짧은 행동 문장을 씁니다(시안 18·19·23·31·32p).
 *   서버 title은 길이가 일정하지 않아 54px 큰 글씨에서 여러 줄로 깨지기 때문입니다.
 *   서버의 explanation / action 은 그대로 본문에 표시합니다.
 * - 사기 유형에 따라 '돈을 보내지 마세요'(송금 유도형)와 '링크를 누르지 마세요'(링크 유도형)를 나눕니다.
 * - 외국어 화면은 시안 32p처럼 '선택 언어 + 한국어'를 함께 보여줍니다.
 */

/** 링크를 누르게 하는 유형인지 (택배·부고·지원금·결제 알림). 나머지는 송금·정보 요구형으로 봅니다. */
internal fun isLinkScam(scamType: String, danger: Boolean): Boolean = when (scamType) {
    "delivery", "obituary_invitation", "subsidy_refund", "payment_alert" -> true
    "family_impersonation", "agency_impersonation", "job_loan", "immigration" -> false
    else -> !danger // 유형을 모르면: 위험=송금 금지, 주의=링크 금지
}

/** 사기 유형별 한국어 한 줄 설명 (외국어 화면의 한국어 보조 문장) */
internal fun koreanReason(scamType: String, danger: Boolean): String = when (scamType) {
    "family_impersonation" -> "가족인 척 돈을 보내라고 해요."
    "agency_impersonation" -> "기관인 척 개인정보나 돈을 요구해요."
    "delivery" -> "택배를 사칭해 링크를 누르게 해요."
    "obituary_invitation" -> "부고·청첩장인 척 링크를 누르게 해요."
    "subsidy_refund" -> "지원금·환급을 핑계로 링크를 누르게 해요."
    "payment_alert" -> "결제 알림인 척 링크나 전화를 유도해요."
    "job_loan" -> "일자리·대출을 미끼로 돈이나 정보를 요구해요."
    "immigration" -> "출입국·체류 안내인 척 개인정보를 요구해요."
    else -> if (danger) "사기로 의심되는 문자예요." else "사기에 자주 쓰이는 표현이 있어요."
}

/** 한국어 한 줄 요약: "사기 문자예요. 돈을 보내지 마세요." */
internal fun koreanSummary(danger: Boolean, link: Boolean): String = when {
    danger && link -> "사기 문자예요. 링크를 누르지 마세요."
    danger -> "사기 문자예요. 돈을 보내지 마세요."
    link -> "사기일 수 있어요. 링크를 누르지 마세요."
    else -> "사기일 수 있어요. 돈을 보내기 전에 확인하세요."
}

/** 한국어 두 줄 제목 (카카오톡 23p · 음성 31p 머리 영역) */
internal fun koreanTwoLine(danger: Boolean, link: Boolean): String = koreanSummary(danger, link).replace(". ", ".\n")

/** 한국어 큰 제목 (기본 모드 18·19p, 54px) */
internal fun koreanBigHeadline(danger: Boolean, link: Boolean): String = when {
    link -> "링크를\n누르지\n마세요."
    danger -> "돈을\n보내지\n마세요."
    else -> "돈 보내기 전\n확인하세요."
}

/** 쉬운 안내(30p) 큰 문장 */
internal fun koreanEasyHeadline(danger: Boolean, link: Boolean): String = when {
    danger && link -> "사기 문자예요.\n링크 누르지\n마세요."
    danger -> "사기 문자예요.\n돈 보내지\n마세요."
    link -> "링크를 누르지\n마세요."
    else -> "돈 보내기 전\n확인하세요."
}

/** 화면 안내(29p) 두 번째 큰 줄 */
internal fun koreanHearingAction(link: Boolean): String = if (link) "링크를 누르지\n마세요." else "돈을 보내지\n마세요."

/** 외국어 화면 문구 묶음 */
internal data class ForeignCopy(
    val languageName: String,
    val danger: String,
    val caution: String,
    val dangerMoney: String,
    val dangerLink: String,
    val cautionMoney: String,
    val cautionLink: String,
    /** 쉬운 안내·화면 안내용 짧은 문장 */
    val shortMoney: String,
    val shortLink: String,
    val shortScam: String,
    val shortCareful: String,
    val received: String,
    val receivedKakao: String,
    val call: String,
    val ask: String,
    val sms: String,
    val videoCall: String,
    val close: String,
    val details: String,
    val timeoutEyebrow: String,
    val timeoutTitle: String,
    val timeoutBody: String,
    val timeoutWarn: String,
    val retry: String,
    val callToCheck: String,
    val checkingTitle: String,
    val checkingBody: String,
    val notifyDanger: String,
    val notifyCaution: String
) {
    fun headline(danger: Boolean, link: Boolean) = when {
        danger && link -> dangerLink
        danger -> dangerMoney
        link -> cautionLink
        else -> cautionMoney
    }
    fun level(danger: Boolean) = if (danger) this.danger else caution
}

internal fun foreignCopy(languageCode: String): ForeignCopy? = when (languageCode.lowercase().substringBefore('-')) {
    "en" -> ForeignCopy(
        languageName = "English", danger = "Danger", caution = "Caution",
        dangerMoney = "This is a scam message.\nDon't send money.",
        dangerLink = "This is a scam message.\nDon't tap the link.",
        cautionMoney = "This may be a scam.\nCheck before sending money.",
        cautionLink = "This may be a scam.\nDon't tap the link.",
        shortMoney = "Don't send\nmoney.", shortLink = "Don't tap\nthe link.",
        shortScam = "Scam message.", shortCareful = "Be careful.",
        received = "Received message", receivedKakao = "Received KakaoTalk",
        call = "Call your family", ask = "Ask your family", sms = "Text your family",
        videoCall = "Check by video call", close = "Close", details = "Details",
        timeoutEyebrow = "Check delayed", timeoutTitle = "We couldn't finish\nchecking",
        timeoutBody = "The network is unstable. We don't know yet if this message is safe.",
        timeoutWarn = "Until it's checked, don't tap links or send money.",
        retry = "Check again", callToCheck = "Call your family to check",
        checkingTitle = "Checking the message",
        checkingBody = "Don't tap links or send money until it's done.",
        notifyDanger = "Dangerous message received", notifyCaution = "Be careful with this message"
    )
    "vi" -> ForeignCopy(
        languageName = "Tiếng Việt", danger = "Nguy hiểm", caution = "Chú ý",
        dangerMoney = "Đây là tin nhắn lừa đảo.\nĐừng chuyển tiền.",
        dangerLink = "Đây là tin nhắn lừa đảo.\nĐừng nhấn vào liên kết.",
        cautionMoney = "Có thể là lừa đảo.\nHãy kiểm tra trước khi chuyển tiền.",
        cautionLink = "Có thể là lừa đảo.\nĐừng nhấn vào liên kết.",
        shortMoney = "Đừng\nchuyển tiền.", shortLink = "Đừng nhấn\nliên kết.",
        shortScam = "Lừa đảo.", shortCareful = "Hãy cẩn thận.",
        received = "Tin nhắn nhận được", receivedKakao = "Tin nhắn KakaoTalk nhận được",
        call = "Gọi cho gia đình", ask = "Hỏi gia đình", sms = "Nhắn tin cho gia đình",
        videoCall = "Kiểm tra bằng gọi video", close = "Đóng", details = "Xem chi tiết",
        timeoutEyebrow = "Chậm kiểm tra", timeoutTitle = "Chưa thể kiểm tra\nxong",
        timeoutBody = "Kết nối mạng không ổn định. Chưa biết tin nhắn này có an toàn không.",
        timeoutWarn = "Trước khi kiểm tra xong, đừng nhấn liên kết hoặc chuyển tiền.",
        retry = "Kiểm tra lại", callToCheck = "Gọi cho gia đình để hỏi",
        checkingTitle = "Đang kiểm tra tin nhắn",
        checkingBody = "Đừng nhấn liên kết hoặc chuyển tiền cho đến khi xong.",
        notifyDanger = "Có tin nhắn nguy hiểm", notifyCaution = "Hãy cẩn thận với tin nhắn này"
    )
    "zh" -> ForeignCopy(
        languageName = "中文", danger = "危险", caution = "注意",
        dangerMoney = "这是诈骗短信。\n不要转账。",
        dangerLink = "这是诈骗短信。\n不要点击链接。",
        cautionMoney = "可能是诈骗。\n转账前请先确认。",
        cautionLink = "可能是诈骗。\n不要点击链接。",
        shortMoney = "不要\n转账。", shortLink = "不要\n点击链接。",
        shortScam = "诈骗短信。", shortCareful = "请小心。",
        received = "收到的短信", receivedKakao = "收到的KakaoTalk消息",
        call = "给家人打电话", ask = "问问家人", sms = "给家人发短信",
        videoCall = "用视频通话确认", close = "关闭", details = "查看详情",
        timeoutEyebrow = "检查延迟", timeoutTitle = "未能完成\n检查",
        timeoutBody = "网络不稳定，暂时无法确定这条短信是否安全。",
        timeoutWarn = "确认之前，请不要点击链接或转账。",
        retry = "重新检查", callToCheck = "打电话问家人",
        checkingTitle = "正在检查短信",
        checkingBody = "检查完成前，请不要点击链接或转账。",
        notifyDanger = "收到危险短信", notifyCaution = "请注意这条短信"
    )
    else -> null
}

/** 설정 화면의 표시 이름(한국어 / English / Tiếng Việt / 中文) → 외국어 문구 */
internal fun foreignCopyForDisplayLanguage(displayLanguage: String): ForeignCopy? =
    foreignCopy(localizedCode(displayLanguage))
