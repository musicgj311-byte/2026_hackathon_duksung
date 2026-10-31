"""판정 API 요청·응답 모델.

프론트 1, 백엔드 1, 백엔드 2가 함께 쓰는 약속입니다.
필드를 바꿀 때는 반드시 세 사람이 합의하고, 바뀐 내용을 팀 채널에 공유하세요.
"""

from enum import StrEnum

from pydantic import BaseModel, Field


class Source(StrEnum):
    sms = "sms"
    kakao = "kakao"


class UserType(StrEnum):
    default = "default"  # 기본 (고령층 등)
    visual = "visual"  # 시각장애인: TalkBack 호환, 기호·이모지 없는 문장
    hearing = "hearing"  # 청각장애인: 짧고 굵은 문장
    developmental = "developmental"  # 발달·지적장애인: easy_sentence + 그림 아이콘


class Level(StrEnum):
    danger = "danger"  # 위험: 전체 화면 경고 + 보호자 알림
    caution = "caution"  # 주의: 사용자 화면에만 표시
    safe = "safe"  # 안전해 보임: 조용히 통과


class ScamType(StrEnum):
    family_impersonation = "family_impersonation"  # 가족·지인 사칭
    agency_impersonation = "agency_impersonation"  # 검찰·금감원 등 기관 사칭
    delivery = "delivery"  # 택배·배송
    obituary_invitation = "obituary_invitation"  # 부고·청첩장
    subsidy_refund = "subsidy_refund"  # 지원금·환급금
    payment_alert = "payment_alert"  # 결제 승인 알림
    job_loan = "job_loan"  # 고수익 알바·대출
    immigration = "immigration"  # 출입국·체류 사칭
    other = "other"
    none = "none"


class JudgeRequest(BaseModel):
    device_id: str = Field(..., description="앱 설치 시 생성한 기기 UUID")
    source: Source
    sender: str = Field(..., description="발신 번호 또는 카톡 발신자 이름")
    is_contact: bool = Field(..., description="연락처에 저장된 발신자인지")
    is_international: bool = False
    text: str = Field(..., min_length=1, max_length=2000)
    user_type: UserType = UserType.default
    language: str = Field("ko", description="설명 문구 언어 (ko, en, vi, zh 등)")


class UrlResult(BaseModel):
    original_url: str
    final_url: str
    safe_browsing_hit: bool | None = None  # None = 확인 못 함
    virustotal_malicious: int | None = None  # 악성으로 판정한 엔진 수
    domain_age_days: int | None = None


class JudgeResponse(BaseModel):
    level: Level
    scam_type: ScamType
    signals: list[str] = Field(default_factory=list, description="판정 근거 (사용자 언어)")
    has_url: bool
    url_result: UrlResult | None = None
    # 아래 네 필드는 사용자 언어로 번역된 상태로 내려갑니다. 프론트는 그대로 표시만 합니다.
    title: str
    explanation: str
    action: str
    easy_sentence: str
    language: str
    stage_reached: int = Field(..., ge=2, le=3, description="판정이 끝난 캐스케이드 단계")
    elapsed_ms: int
