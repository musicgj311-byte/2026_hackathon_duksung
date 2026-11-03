
# ruff: noqa: E501
"""LLM 설명이 없을 때 사용하는 고정 문구.

한국어: 최신 기획안의 6개 수법 + 기타 기준
영어·베트남어·중국어: 위험 단계별 공통 문구
"""

from dataclasses import dataclass

from app.schemas import Level, ScamType


@dataclass(frozen=True)
class Message:
    title: str
    explanation: str
    action: str
    easy_sentence: str


# 기존 ScamType을 최신 기획안의 분류로 연결
_CATEGORY_BY_SCAM_TYPE: dict[ScamType, str] = {
    ScamType.family_impersonation: "family",
    ScamType.agency_impersonation: "government",
    ScamType.delivery: "delivery",
    ScamType.obituary_invitation: "invitation",
    ScamType.subsidy_refund: "government",
    ScamType.payment_alert: "finance",
    ScamType.job_loan: "loan",
    ScamType.immigration: "government",
    ScamType.other: "other",
    ScamType.none: "other",
}


# 최신 기획안의 한국어 경고 이유
_KO_REASON: dict[str, str] = {
    "finance": "카드사인 척 전화를 걸게 해요.",
    "loan": "싼 대출을 미끼로 돈을 빼가려 해요.",
    "government": "나라 기관은 문자 링크로 돈을 받지 않아요.",
    "delivery": "가짜 택배 문자예요.",
    "invitation": "가짜 초대장으로 위험한 앱을 깔게 해요.",
    "family": "가족인 척 돈을 보내라고 해요.",
    "other": "돈이나 개인정보를 노리는 문자일 수 있어요.",
}


# 최신 기획안의 유형별 안내 행동
_KO_ACTION: dict[str, str] = {
    "finance": "이 번호로 전화하지 마세요.",
    "loan": "은행 대표번호로 확인하세요.",
    "government": "링크를 누르지 마세요.",
    "delivery": "택배 앱이나 송장번호로 확인하세요.",
    "invitation": "아는 사람이면 원래 알던 번호로 전화해 확인하세요.",
    "family": "원래 번호로 직접 전화해 확인하세요.",
    "other": "링크를 누르거나 돈을 보내지 말고 공식 경로로 확인하세요.",
}


_KO_TITLE = {
    Level.danger: "사기 문자 같아요",
    Level.caution: "조심하세요",
}

_KO_EASY = {
    Level.danger: "누르지 마세요. 돈 보내지 마세요.",
    Level.caution: "먼저 확인하세요.",
}


# 안전 판정은 수법과 관계없이 공통 문구
_KO_SAFE = Message(
    title="특별한 위험은 보이지 않아요",
    explanation="그래도 돈을 보내거나 정보를 알려주기 전에는 꼭 직접 확인하세요.",
    action="평소처럼 확인하세요.",
    easy_sentence="확인하고 쓰세요.",
)


# 영어·베트남어·중국어 공통 문구
_OTHER: dict[str, dict[Level, Message]] = {
    "en": {
        Level.danger: Message(
            "This looks like a scam",
            "This message may be trying to take your money or information.",
            "Do not tap the link or send money. Call the sender on a number you already know.",
            "Do not tap. Do not pay.",
        ),
        Level.caution: Message(
            "Be careful",
            "This message has signs often used in scams.",
            "Before tapping or paying, call the sender directly to check.",
            "Check by phone first.",
        ),
        Level.safe: Message(
            "No clear risk found",
            "Still, check directly before sending money or personal information.",
            "Check as usual.",
            "Check before you act.",
        ),
    },
    "vi": {
        Level.danger: Message(
            "Tin nhắn này có vẻ lừa đảo",
            "Tin nhắn này có thể đang lấy tiền hoặc thông tin của bạn.",
            "Đừng bấm vào đường link và đừng chuyển tiền. Hãy gọi cho người gửi bằng số bạn đã biết.",
            "Đừng bấm. Đừng chuyển tiền.",
        ),
        Level.caution: Message(
            "Hãy cẩn thận",
            "Tin nhắn này có dấu hiệu thường gặp trong lừa đảo.",
            "Trước khi bấm hoặc chuyển tiền, hãy gọi điện kiểm tra trực tiếp.",
            "Gọi điện kiểm tra trước.",
        ),
        Level.safe: Message(
            "Chưa thấy nguy cơ rõ ràng",
            "Dù vậy, hãy kiểm tra trực tiếp trước khi chuyển tiền hoặc cung cấp thông tin.",
            "Kiểm tra như bình thường.",
            "Kiểm tra trước khi làm.",
        ),
    },
    "zh": {
        Level.danger: Message(
            "这可能是诈骗短信",
            "这条短信可能想骗取你的钱或个人信息。",
            "不要点击链接，也不要转账。请用你已知的号码直接联系对方确认。",
            "不要点。不要转账。",
        ),
        Level.caution: Message(
            "请小心",
            "这条短信有诈骗中常见的特征。",
            "点击或转账前，请先打电话直接确认。",
            "先打电话确认。",
        ),
        Level.safe: Message(
            "暂未发现明显风险",
            "不过在转账或提供个人信息前，请务必直接确认。",
            "照常确认即可。",
            "先确认再操作。",
        ),
    },
}


def base_language(language: str) -> str:
    """'vi-VN' → 'vi', 'ZH' → 'zh'"""
    return language.lower().split("-")[0].split("_")[0]


def get_fixed_message(
    level: Level,
    scam_type: ScamType,
    language: str,
) -> Message:
    """최신 기획안 기준으로 고정 안내 문구를 반환합니다."""

    lang = base_language(language)

    # 한국어가 아닌 언어는 기존 공통 문구 사용
    if lang != "ko":
        return _OTHER.get(lang, _OTHER["en"])[level]

    # 안전 판정은 공통 문구 사용
    if level is Level.safe:
        return _KO_SAFE

    # 기존 ScamType을 최신 분류로 변환
    category = _CATEGORY_BY_SCAM_TYPE.get(scam_type, "other")

    return Message(
        title=_KO_TITLE[level],
        explanation=_KO_REASON[category],
        action=_KO_ACTION[category],
        easy_sentence=_KO_EASY[level],
    )


def get_message(
    level: Level,
    scam_type: ScamType,
    language: str,
) -> Message:
    """기존 cascade.py 호출과의 호환성을 유지합니다."""
    return get_fixed_message(level, scam_type, language)
