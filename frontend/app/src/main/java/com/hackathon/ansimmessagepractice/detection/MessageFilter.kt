package com.hackathon.ansimmessagepractice.detection

/**
 * 기기에서 수행하는 시연용 1차 필터입니다. AI 분석이나 악성 URL 평판 판정이 아닙니다.
 * 실제 서버 판정 결과는 MainActivity의 경고 전달 Intent를 통해 표시할 수 있습니다.
 */
object MessageFilter {
    data class Alert(val risk: String, val reason: String, val scamType: String = "none")
    private val link = Regex("(?i)(https?://|www\\.|[a-z0-9-]+\\.(com|net|kr|xyz|top|shop|info)/?)")
    private val family = listOf("엄마", "아빠", "아들", "딸", "휴대폰", "핸드폰", "임시폰", "폰 고장", "액정")
    private val payment = listOf("송금", "입금", "돈", "계좌", "이체", "만원", "보내줘", "보내줄")
    private val suspicious = listOf("택배", "재배송", "배송", "검찰", "경찰", "지원금", "환급", "미납", "본인인증")

    /**
     * 연락처 조회 결과: true=등록, false=미등록, null=조회할 수 없음.
     * 모르는 번호/해외 번호는 서버 분석 대상이지만 그 사실만으로 위험 판정하지 않습니다.
     * 카카오톡 발신자 이름에는 전화번호/연락처 기준을 적용하지 않습니다.
     */
    fun shouldSendToServer(message: String, source: String, isContact: Boolean?,
                           isInternational: Boolean): Boolean {
        if (classify(message) != null) return true
        if (source != "sms") return false
        return isContact != true || isInternational
    }

    fun classify(message: String): Alert? {
        val trimmed = message.trim()
        if (trimmed.length < 8) return null
        val impersonation = family.any(trimmed::contains)
        val transfer = payment.any(trimmed::contains)
        if (impersonation && transfer) return Alert("danger", "가족을 사칭해 송금이나 계좌 정보를 요구하는 내용이 의심돼요.", "family_impersonation")
        if (link.containsMatchIn(trimmed)) return Alert("caution", "문자에 인터넷 주소가 있어요. 공식 앱에서 먼저 확인해 주세요.", if (trimmed.contains("택배") || trimmed.contains("재배송")) "delivery" else "none")
        if (suspicious.any(trimmed::contains) && (transfer || trimmed.contains("확인"))) {
            return Alert("caution", "기관·배송 등을 내세워 행동을 요구하는 문자일 수 있어요.", if (trimmed.contains("택배") || trimmed.contains("배송")) "delivery" else "none")
        }
        return null
    }
}