# ruff: noqa: E501  (문구·프롬프트·패턴은 한 줄에 둬야 읽기 쉬워서 줄 길이 제한 제외)
"""LLM 설명이 없을 때 쓰는 고정 문구.

쓰는 경우
- ② 단계에서 악성 URL로 바로 위험을 확정해 LLM을 건너뛸 때
- LLM이 5초 안에 답하지 않거나 실패했을 때
- 판정이 '안전해 보임'일 때 (LLM이 "안전합니다"처럼 단정하지 않게 항상 고정 문구 사용)

백엔드 2가 기획 문구로 get_fixed_message()를 완성하면 그쪽으로 바꿉니다.
그 전까지는 이 파일의 기본 문구를 씁니다. 한국어는 수법별, 다른 언어는 단계별 공통 문구입니다.
"""

from dataclasses import dataclass

from app.schemas import Level, ScamType


@dataclass(frozen=True)
class Message:
    title: str
    explanation: str
    action: str
    easy_sentence: str


_KO_REASON: dict[ScamType, str] = {
    ScamType.family_impersonation: "가족인 척하며 돈이나 정보를 요구하는 사기일 수 있어요.",
    ScamType.agency_impersonation: "검찰이나 금융기관인 척하는 사기일 수 있어요. 기관은 문자로 돈을 요구하지 않아요.",
    ScamType.delivery: "택배 회사를 흉내 낸 가짜 주소일 수 있어요.",
    ScamType.obituary_invitation: "부고나 청첩장을 흉내 내 가짜 주소를 누르게 하는 사기일 수 있어요.",
    ScamType.subsidy_refund: "지원금이나 환급을 핑계로 정보를 빼가는 사기일 수 있어요.",
    ScamType.payment_alert: "결제 알림을 흉내 내 전화나 주소 클릭을 유도하는 사기일 수 있어요.",
    ScamType.job_loan: "쉬운 돈벌이나 대출을 미끼로 한 사기일 수 있어요.",
    ScamType.immigration: "출입국·체류 기관을 흉내 낸 사기일 수 있어요.",
    ScamType.other: "돈이나 정보를 노리는 문자일 수 있어요.",
    ScamType.none: "돈이나 정보를 노리는 문자일 수 있어요.",
}  # fmt: skip

_KO_LEVEL = {
    Level.danger: ("사기 문자 같아요", "주소를 누르지 말고, 돈도 보내지 마세요. 보낸 사람에게 원래 번호로 직접 전화해 확인하세요.", "누르지 마세요. 돈 보내지 마세요."),
    Level.caution: ("조심하세요", "주소를 누르거나 돈을 보내기 전에, 보낸 곳에 직접 전화해 확인하세요.", "먼저 전화로 확인하세요."),
}  # fmt: skip

_KO_SAFE = Message(
    "특별한 위험은 보이지 않아요",
    "그래도 돈을 보내거나 정보를 알려주기 전에는 꼭 직접 확인하세요.",
    "평소처럼 확인하세요.",
    "확인하고 쓰세요.",
)

# 다른 언어는 단계별 공통 문구 (수법 설명 없이)
_OTHER: dict[str, dict[Level, Message]] = {
    "en": {
        Level.danger: Message("This looks like a scam", "This message may be trying to take your money or information.", "Do not tap the link or send money. Call the sender on a number you already know.", "Do not tap. Do not pay."),
        Level.caution: Message("Be careful", "This message has signs often used in scams.", "Before tapping or paying, call the sender directly to check.", "Check by phone first."),
        Level.safe: Message("No clear risk found", "Still, check directly before sending money or personal information.", "Check as usual.", "Check before you act."),
    },
    "vi": {
        Level.danger: Message("Tin nhắn này có vẻ lừa đảo", "Tin nhắn này có thể đang lấy tiền hoặc thông tin của bạn.", "Đừng bấm vào đường link và đừng chuyển tiền. Hãy gọi cho người gửi bằng số bạn đã biết.", "Đừng bấm. Đừng chuyển tiền."),
        Level.caution: Message("Hãy cẩn thận", "Tin nhắn này có dấu hiệu thường gặp trong lừa đảo.", "Trước khi bấm hoặc chuyển tiền, hãy gọi điện kiểm tra trực tiếp.", "Gọi điện kiểm tra trước."),
        Level.safe: Message("Chưa thấy nguy cơ rõ ràng", "Dù vậy, hãy kiểm tra trực tiếp trước khi chuyển tiền hoặc cung cấp thông tin.", "Kiểm tra như bình thường.", "Kiểm tra trước khi làm."),
    },
    "zh": {
        Level.danger: Message("这可能是诈骗短信", "这条短信可能想骗取你的钱或个人信息。", "不要点击链接，也不要转账。请用你已知的号码直接联系对方确认。", "不要点。不要转账。"),
        Level.caution: Message("请小心", "这条短信有诈骗中常见的特征。", "点击或转账前，请先打电话直接确认。", "先打电话确认。"),
        Level.safe: Message("暂未发现明显风险", "不过在转账或提供个人信息前，请务必直接确认。", "照常确认即可。", "先确认再操作。"),
    },
}  # fmt: skip


def base_language(language: str) -> str:
    """'vi-VN' → 'vi', 'ZH' → 'zh'"""
    return language.lower().split("-")[0].split("_")[0]


def get_message(level: Level, scam_type: ScamType, language: str) -> Message:
    """한국어는 수법별 문구, 영어·베트남어·중국어는 단계별 문구.
    고정 문구가 없는 언어는 영어로 돌려줍니다 (한국어보다 읽을 수 있을 가능성이 높아서)."""
    lang = base_language(language)
    if lang != "ko":
        return _OTHER.get(lang, _OTHER["en"])[level]
    if level is Level.safe:
        return _KO_SAFE
    title, action, easy = _KO_LEVEL[level]
    return Message(title, _KO_REASON[scam_type], action, easy)
