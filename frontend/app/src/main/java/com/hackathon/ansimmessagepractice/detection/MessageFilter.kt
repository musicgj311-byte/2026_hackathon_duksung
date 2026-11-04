package com.hackathon.ansimmessagepractice.detection

/**
 * 기기 내 1차 선별용 간단한 규칙입니다. AI 판정이나 악성 URL 검사 결과가 아닙니다.
 *
 * - danger/caution: 서버가 없을 때도 사용자에게 안내할 만한 조합 패턴
 * - null: 로컬 경고 없음. 서버를 사용 중이면 URL만 있는 문자도 별도로 검사할 수 있습니다.
 *
 * '안전' 판정기가 아니므로 null이라고 사기가 아님을 보장하지 않습니다.
 */
object MessageFilter {
    data class Alert(val risk: String, val reason: String, val scamType: String = "none")

    // https:// 없이 적힌 gift-card-event.example 같은 테스트 주소도 인식합니다.
    // 도메인의 위험성 자체를 단정하지 않으며, .example은 테스트용 예약 도메인입니다.
    private val link = Regex(
        """(?i)(?:https?://|www\.)[^\s<>]+|(?<![\w@])(?:[a-z0-9-]+\.)+(?:com|net|kr|org|xyz|top|shop|info|io|me|cc|ly|site|app|link|example)(?=[^\w-]|$)"""
    )
    private val family = Regex("""엄마|아빠|아들|딸|손자|손녀|임시\s*폰|폰\s*(?:이\s*)?고장|액정\s*(?:파손|깨짐)|번호\s*바뀌""")
    private val familyChange = Regex("""임시\s*폰|폰\s*(?:이\s*)?고장|액정\s*(?:파손|깨짐)|번호\s*바뀌""")
    private val requestTransfer = Regex(
        """돈\s*(?:좀\s*)?보내\s*줘|(?:입금|송금|이체)\s*(?:해\s*줘|해\s*주세요|해\s*주세|부탁|하세요|하셔야|해야)|계좌\s*(?:로|에)\s*(?:보내|입금|송금|이체)"""
    )
    private val money = Regex("""송금|입금|이체|계좌|돈\s*보내|만\s*원|상환금""")

    private val loan = Regex("""대출|저금리|대환|저축은행|상환금|대출\s*전환""")
    private val upfront = Regex(
        """선\s*(?:입금|송금|이체|납부)|(?:먼저|사전에|우선)\s*(?:\S+\s*){0,4}(?:입금|송금|이체|납부)|(?:입금|송금|이체)\s*(?:하셔야|해야)\s*(?:승인|처리)"""
    )

    private val delivery = Regex("""택배|배송|운송장|통운|주소지|수취\s*불가|반송|재배송""")
    private val health = Regex("""국민\s*건강|건강\s*검진|검진\s*통지서|건강보험|국민건강보험""")
    private val gift = Regex("""상품권|기프트\s*카드|모바일\s*쿠폰|명절\s*선물|선물\s*증정|당첨|gift[ -]?card""", RegexOption.IGNORE_CASE)
    private val agency = Regex("""검찰|경찰|법원|금감원|금융감독원|국세청|정부\s*기관|출입국""")
    private val support = Regex("""지원금|환급|미납|체납|보조금|정부\s*지원""")
    private val action = Regex("""확인|신청|등록|수정|변경|접속|클릭|누르|인증|승인|설치|납부|입금|송금|이체""")

    fun hasUrl(message: String): Boolean = link.containsMatchIn(message)

    fun classify(message: String): Alert? {
        val text = message.trim()
        if (text.length < 8) return null

        // URL이 없어도 가족 사칭 + 직접적인 송금 요구는 위험 신호입니다.
        if (family.containsMatchIn(text) && requestTransfer.containsMatchIn(text)) {
            return Alert("danger", "가족을 사칭하며 돈을 보내라고 요구하는 내용이 의심돼요.", "family_impersonation")
        }
        // 대출 전환·승인을 조건으로 먼저 돈을 요구하는 경우도 링크 없이 탐지합니다.
        if (loan.containsMatchIn(text) && upfront.containsMatchIn(text)) {
            return Alert("danger", "대출을 처리해 준다며 돈을 먼저 입금하라고 요구해요. 송금 전 확인하세요.", "job_loan")
        }

        val hasLink = hasUrl(text)
        if (hasLink && delivery.containsMatchIn(text)) {
            return Alert("caution", "배송 문제를 이유로 링크 확인을 유도해요. 택배사 공식 앱에서 확인하세요.", "delivery")
        }
        if (hasLink && health.containsMatchIn(text)) {
            return Alert("caution", "건강검진 안내를 내세워 링크 확인을 유도해요. 공식 기관에서 확인하세요.", "agency_impersonation")
        }
        if (hasLink && gift.containsMatchIn(text)) {
            return Alert("caution", "상품권이나 선물을 내세운 링크예요. 누르기 전에 발신자를 확인하세요.", "other")
        }
        if (hasLink && agency.containsMatchIn(text)) {
            return Alert("caution", "기관 안내를 내세운 링크예요. 공식 채널에서 다시 확인하세요.", "agency_impersonation")
        }
        if (hasLink && support.containsMatchIn(text)) {
            return Alert("caution", "지원금·환급 안내 링크예요. 공식 채널에서 확인하세요.", "subsidy_refund")
        }

        if (loan.containsMatchIn(text) && action.containsMatchIn(text)) {
            return Alert("caution", "대출 관련 행동을 요청하는 내용이에요. 금융기관에 직접 확인하세요.", "job_loan")
        }
        if (agency.containsMatchIn(text) && (action.containsMatchIn(text) || money.containsMatchIn(text))) {
            return Alert("caution", "기관을 내세우며 행동을 요청하는 내용이에요.", "agency_impersonation")
        }
        if (delivery.containsMatchIn(text) && action.containsMatchIn(text)) {
            return Alert("caution", "배송 문제를 내세워 주소 확인 등을 요구하는 내용이에요.", "delivery")
        }
        if (gift.containsMatchIn(text) && action.containsMatchIn(text)) {
            return Alert("caution", "상품권이나 선물을 받으라고 유도하는 내용이에요.", "other")
        }
        if (support.containsMatchIn(text) && action.containsMatchIn(text)) {
            return Alert("caution", "지원금·환급 등을 내세워 확인을 요구하는 내용이에요.", "subsidy_refund")
        }
        if (familyChange.containsMatchIn(text) && money.containsMatchIn(text)) {
            return Alert("caution", "새 전화번호를 내세워 돈을 요구하는 내용인지 직접 확인해 주세요.", "family_impersonation")
        }
        // 일반 주소만 있다는 이유로 로컬 경고를 확정하지 않습니다.
        return null
    }
}
