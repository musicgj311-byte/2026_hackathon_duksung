
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

# 영어·베트남어·중국어의 유형별 안내 문구
# 각 유형의 튜플 순서: (위험 이유, 행동 지침)
_CATEGORY_TEXT: dict[str, dict[str, tuple[str, str]]] = {
    "en": {
        "finance": (
            "This message pretends to be from a card company and tries to get you to call.",
            "Do not call the number in this message.",
        ),
        "loan": (
            "It uses a cheap loan offer to trick you into giving away money.",
            "Check with your bank using its official phone number.",
        ),
        "government": (
            "Government agencies do not collect money through links in text messages.",
            "Do not tap the link.",
        ),
        "delivery": (
            "This may be a fake delivery message.",
            "Check in the delivery app or with your tracking number.",
        ),
        "invitation": (
            "It uses a fake invitation to get you to install a dangerous app.",
            "Call the sender using a number you already know.",
        ),
        "family": (
            "It pretends to be a family member and asks you to send money.",
            "Call them using their usual phone number to check.",
        ),
        "other": (
            "This message may be trying to steal your money or personal information.",
            "Do not tap links or send money. Check through an official channel.",
        ),
    },
    "vi": {
        "finance": (
            "Tin nhắn giả danh công ty thẻ để dụ bạn gọi điện.",
            "Đừng gọi số điện thoại trong tin nhắn này.",
        ),
        "loan": (
            "Tin nhắn dùng lời mời vay tiền lãi suất thấp để lừa lấy tiền của bạn.",
            "Hãy kiểm tra bằng số điện thoại chính thức của ngân hàng.",
        ),
        "government": (
            "Cơ quan nhà nước không thu tiền qua đường link trong tin nhắn.",
            "Đừng bấm vào đường link.",
        ),
        "delivery": (
            "Đây có thể là tin nhắn giao hàng giả mạo.",
            "Hãy kiểm tra bằng ứng dụng giao hàng hoặc mã vận đơn.",
        ),
        "invitation": (
            "Tin nhắn dùng thiệp mời giả để dụ bạn cài ứng dụng nguy hiểm.",
            "Hãy gọi cho người gửi bằng số điện thoại bạn đã biết.",
        ),
        "family": (
            "Tin nhắn giả danh người thân và yêu cầu bạn chuyển tiền.",
            "Hãy gọi lại bằng số điện thoại quen thuộc của người đó.",
        ),
        "other": (
            "Tin nhắn này có thể đang tìm cách lấy tiền hoặc thông tin cá nhân của bạn.",
            "Đừng bấm vào đường link hoặc chuyển tiền. Hãy kiểm tra qua kênh chính thức.",
        ),
    },
    "zh": {
        "finance": (
            "这条短信冒充银行卡公司，诱导你打电话。",
            "不要拨打这条短信中的电话号码。",
        ),
        "loan": (
            "这条短信用低息贷款作诱饵，试图骗走你的钱。",
            "请使用银行的官方电话号码进行确认。",
        ),
        "government": (
            "政府机构不会通过短信链接收取费用。",
            "不要点击链接。",
        ),
        "delivery": (
            "这可能是一条虚假的快递短信。",
            "请通过快递应用或运单号进行确认。",
        ),
        "invitation": (
            "这条短信利用假邀请函诱导你安装危险的应用。",
            "请使用你已知的电话号码联系发件人。",
        ),
        "family": (
            "这条短信冒充家人，要求你转账。",
            "请使用家人原来的电话号码直接联系确认。",
        ),
        "other": (
            "这条短信可能试图骗取你的钱或个人信息。",
            "不要点击链接或转账，请通过官方渠道确认。",
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
    """최신 기획안 기준으로 다국어 고정 안내 문구를 반환합니다."""

    lang = base_language(language)

    # 기존 ScamType을 최신 분류로 변환
    category = _CATEGORY_BY_SCAM_TYPE.get(scam_type, "other")

    # 안전 판정은 언어별 공통 문구 사용
    if level is Level.safe:
        return _KO_SAFE if lang == "ko" else _OTHER.get(lang, _OTHER["en"])[level]

    # 한국어 문구
    if lang == "ko":
        return Message(
            title=_KO_TITLE[level],
            explanation=_KO_REASON[category],
            action=_KO_ACTION[category],
            easy_sentence=_KO_EASY[level],
        )

    # 지원 언어의 유형별 문구
    language_messages = _OTHER.get(lang, _OTHER["en"])
    category_messages = _CATEGORY_TEXT.get(lang, _CATEGORY_TEXT["en"])

    explanation, action = category_messages[category]
    common_message = language_messages[level]

    return Message(
        title=common_message.title,
        explanation=explanation,
        action=action,
        easy_sentence=common_message.easy_sentence,
    )


def get_message(
    level: Level,
    scam_type: ScamType,
    language: str,
) -> Message:
    """기존 cascade.py 호출과의 호환성을 유지합니다."""
    return get_fixed_message(level, scam_type, language)
