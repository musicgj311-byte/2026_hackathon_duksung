"""LLM이 만든 문구가 사용자에게 그대로 보여줘도 되는지 확인하고 다듬습니다.

확인하는 것
1. 언어: 베트남어 사용자에게 한국어 문구가 가는 등 언어가 어긋나면 고정 문구로 바꿈
2. 빈 칸: 비어 있는 항목은 고정 문구의 같은 항목으로 채움
3. 시각장애인 모드: 화면 낭독기가 이상하게 읽는 이모지·기호 제거
4. 한 문장(easy_sentence): 너무 길면 고정 문구의 한 문장으로 바꿈
"""

import re

from app.judge.fallback_messages import Message, base_language
from app.schemas import UserType

EASY_MAX_CHARS = {"ko": 20, "zh": 15}  # 그 외 언어는 EASY_MAX_DEFAULT
EASY_MAX_DEFAULT = 45
TEXT_MAX_CHARS = 200

_HANGUL = re.compile(r"[가-힣]")
_HAN = re.compile(f"[{chr(0x4E00)}-{chr(0x9FFF)}]")  # 한자 (CJK 통합 한자)
_LETTER = re.compile(r"[^\W\d_]")
# 이모지, 화살표·도형·기호, 이모지 변형 선택자
_SYMBOL_RANGES = [
    (0x1F000, 0x1FAFF),  # 이모지
    (0x2190, 0x21FF),  # 화살표
    (0x2300, 0x23FF),  # 기술 기호
    (0x2460, 0x27BF),  # 동그라미 숫자, 도형, 딩뱃
    (0x2900, 0x2BFF),  # 추가 화살표·기호
    (0xFE0F, 0xFE0F),  # 이모지 변형 선택자
    (0x200D, 0x200D),  # 이모지 결합 문자
    (0x203C, 0x203C),  # 느낌표 두 개 이모지
    (0x2049, 0x2049),  # 느낌표·물음표 이모지
]
_SYMBOLS = re.compile("[" + "".join(f"{chr(a)}-{chr(b)}" for a, b in _SYMBOL_RANGES) + "]")
_BRACKETS = re.compile(r"[\[\]{}<>*#_~`|]")


def language_matches(text: str, language: str) -> bool:
    """문구가 대체로 요청한 언어로 쓰였는지 글자 종류로 어림합니다."""
    letters = _LETTER.findall(text)
    if not letters:
        return True
    hangul = len(_HANGUL.findall(text)) / len(letters)
    lang = base_language(language)
    if lang == "ko":
        return hangul >= 0.3
    if lang == "zh":
        return len(_HAN.findall(text)) / len(letters) >= 0.3 and hangul < 0.1
    return hangul < 0.1


def _clean(text: str, user_type: UserType) -> str:
    text = " ".join(text.split())
    if user_type is UserType.visual:
        text = _BRACKETS.sub("", _SYMBOLS.sub("", text))
        text = " ".join(text.split())
    return text[:TEXT_MAX_CHARS]


def polish(llm: Message, fallback: Message, language: str, user_type: UserType) -> Message:
    """LLM 문구를 검사해 쓸 수 있으면 다듬어서, 아니면 고정 문구를 돌려줍니다."""
    whole = " ".join([llm.title, llm.explanation, llm.action, llm.easy_sentence])
    if not language_matches(whole, language):
        return fallback

    title = _clean(llm.title, user_type) or fallback.title
    explanation = _clean(llm.explanation, user_type) or fallback.explanation
    action = _clean(llm.action, user_type) or fallback.action

    easy = _clean(llm.easy_sentence, user_type)
    limit = EASY_MAX_CHARS.get(base_language(language), EASY_MAX_DEFAULT)
    if not easy or len(easy) > limit:
        easy = fallback.easy_sentence

    return Message(title, explanation, action, easy)
