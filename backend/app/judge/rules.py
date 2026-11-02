# ruff: noqa: E501  (문구·프롬프트·패턴은 한 줄에 둬야 읽기 쉬워서 줄 길이 제한 제외)
"""서버 쪽 규칙·키워드 검사.

LLM을 대신하지 않고, 두 가지 용도로만 씁니다.
1. LLM 없이 위험을 확정했을 때(악성 URL) 수법 유형을 추정해 고정 문구를 고르기
2. LLM이 실패하거나 LLM이 문자 속 속임수 문장에 넘어가 "수법 없음"이라고 답할 때의 안전장치
"""

import re
from dataclasses import dataclass, field

from app.schemas import ScamType

_SCAM_PATTERNS: dict[ScamType, str] = {
    ScamType.family_impersonation: r"엄마|아빠|아들|딸|액정|임시\s*폰|폰\s*(이\s*)?고장|번호\s*바뀌|폰\s*바꿨",
    ScamType.agency_impersonation: r"검찰|검사|수사관|경찰청|금감원|금융감독원|법원|국세청|건강보험공단|출석\s*요구",
    ScamType.delivery: r"택배|배송|운송장|주소\s*(불일치|불명|확인)|보관\s*중|반송",
    ScamType.obituary_invitation: r"부고|별세|청첩장|모바일\s*초대장|결혼식",
    ScamType.subsidy_refund: r"지원금|환급|민생|소비\s*쿠폰|재난\s*지원|정부\s*지원",
    ScamType.payment_alert: r"해외\s*(결제|승인)|결제.{0,15}(승인|완료)|승인.{0,15}결제|카드\s*발급|이용\s*내역|본인\s*(결제|사용|이용)?\s*(가|이)?\s*아니",
    ScamType.job_loan: r"고수익|알바|부업|재택\s*근무|대출|저금리|한도\s*조회",
    ScamType.immigration: r"출입국|체류|비자|외국인\s*등록|immigration|visa",
}  # fmt: skip

_MONEY = r"송금|이체|계좌|입금|보내\s*줘|보내\s*주|돈\s*좀|상품권|기프트\s*카드"
_INSTALL = r"앱\s*설치|설치\s*해|\.apk|원격|팀뷰어|애니데스크|보안\s*앱"
_PERSONAL = r"인증\s*번호|비밀\s*번호|주민\s*(등록)?\s*번호|신분증|카드\s*번호|OTP"


@dataclass
class RuleHits:
    scam_types: list[ScamType] = field(default_factory=list)  # 많이 걸린 순
    requests_money: bool = False
    requests_install: bool = False
    requests_personal: bool = False

    @property
    def top_type(self) -> ScamType:
        return self.scam_types[0] if self.scam_types else ScamType.other

    @property
    def pushes_action(self) -> bool:
        """송금·앱 설치·개인정보 중 하나라도 요구하는지"""
        return self.requests_money or self.requests_install or self.requests_personal

    @property
    def impersonation_with_demand(self) -> bool:
        """가족·기관 사칭 + 행동 요구. LLM이 '안전'이라 해도 최소 '주의'로 올리는 기준"""
        impersonation = {ScamType.family_impersonation, ScamType.agency_impersonation}
        return bool(impersonation & set(self.scam_types)) and self.pushes_action


def check_rules(text: str) -> RuleHits:
    counts = {t: len(re.findall(p, text, re.IGNORECASE)) for t, p in _SCAM_PATTERNS.items()}
    types = [t for t, n in sorted(counts.items(), key=lambda kv: -kv[1]) if n > 0]
    return RuleHits(
        scam_types=types,
        requests_money=bool(re.search(_MONEY, text)),
        requests_install=bool(re.search(_INSTALL, text, re.IGNORECASE)),
        requests_personal=bool(re.search(_PERSONAL, text, re.IGNORECASE)),
    )
