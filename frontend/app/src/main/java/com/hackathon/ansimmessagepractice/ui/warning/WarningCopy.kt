package com.hackathon.ansimmessagepractice.ui.warning

/** 여러 경고 화면에서 공통으로 사용하는 위험 상태와 쉬운 안내 문구입니다. 모델의 세부 추론 내용은 표시하지 않습니다. */
internal enum class Status { DANGER, CAUTION, CHECKING }

internal fun statusOf(value: String): Status = when (value.lowercase()) {
    "danger" -> Status.DANGER
    "checking" -> Status.CHECKING
    else -> Status.CAUTION
}

internal data class WarningWords(
    val title: String,
    val guide: String,
    val call: String,
    val close: String,
    val received: String,
    val replay: String,
    val notified: String
)

internal fun wordsFor(state: Status, language: String): WarningWords = when (language) {
    "English" -> WarningWords(
        when (state) {
            Status.DANGER -> "Scam message.\nStop!"
            Status.CAUTION -> "Be careful.\nThis may be a scam."
            Status.CHECKING -> "Checking your message."
        }, "Do not send money or open links. Call your family first.", "Call family", "Close",
        "Received message", "Listen again", "Your family has been notified"
    )
    "Tiếng Việt" -> WarningWords(
        when (state) {
            Status.DANGER -> "Tin nhắn lừa đảo.\nHãy dừng lại!"
            Status.CAUTION -> "Hãy cẩn thận.\nCó thể là lừa đảo."
            Status.CHECKING -> "Đang kiểm tra tin nhắn."
        }, "Đừng chuyển tiền hoặc nhấn liên kết. Hãy gọi người thân để xác nhận.",
        "Gọi cho gia đình", "Đóng", "Tin nhắn nhận được", "Nghe lại", "Đã báo cho gia đình bạn"
    )
    "中文" -> WarningWords(
        when (state) {
            Status.DANGER -> "可疑诈骗短信。\n请立即停止！"
            Status.CAUTION -> "请小心。\n这可能是诈骗。"
            Status.CHECKING -> "正在检查短信。"
        }, "不要转账或点击链接。请先致电家人核实。", "给家人打电话", "关闭",
        "收到的短信", "再次播放", "已通知家人"
    )
    else -> WarningWords(
        when (state) {
            Status.DANGER -> "사기 문자예요.\n멈추세요!"
            Status.CAUTION -> "조심하세요.\n사기일 수 있어요."
            Status.CHECKING -> "문자를 확인하고 있어요."
        }, "돈을 보내거나 링크를 누르지 마세요.\n저장된 가족 번호로 확인하세요.",
        "가족에게 전화하기", "닫기", "받은 문자", "다시 듣기", "가족에게 알렸어요"
    )
}