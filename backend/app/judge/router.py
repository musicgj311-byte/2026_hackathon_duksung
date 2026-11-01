"""판정 API (백엔드 1 담당).

Day 1~2: USE_FAKE_JUDGE=true 이면 키워드만 보고 고정된 가짜 응답을 돌려줍니다.
프론트 1은 이 응답으로 먼저 연결 작업을 하고, Day 3에 실제 판정으로 교체합니다.
"""

import re
import time

from fastapi import APIRouter

from app.config import settings
from app.schemas import JudgeRequest, JudgeResponse, Level, ScamType

router = APIRouter(tags=["judge"])

URL_PATTERN = re.compile(
    r"https?://\S+|\b[\w-]+\.(?:kr|com|net|me|ly|io|cc|xyz)/\S*", re.IGNORECASE
)


def fake_judge(req: JudgeRequest) -> JudgeResponse:
    started = time.perf_counter()
    has_url = bool(URL_PATTERN.search(req.text))
    money = any(k in req.text for k in ("송금", "계좌", "보내줘", "이체"))
    family = any(k in req.text for k in ("엄마", "아빠", "액정", "임시폰", "폰 고장"))

    if family and money:
        level, scam_type = Level.danger, ScamType.family_impersonation
        title = "사기 문자 같아요"
        explanation = "가족인 척하며 돈을 보내라고 하는 사기일 가능성이 높아요."
        action = "돈을 보내지 말고, 가족에게 원래 번호로 직접 전화해 확인하세요."
        easy = "돈 보내지 마세요."
        signals = ["가족 사칭", "송금 요구"]
    elif has_url and not req.is_contact:
        level, scam_type = Level.caution, ScamType.other
        title = "조심하세요"
        explanation = "모르는 번호가 보낸 주소예요. 가짜 사이트일 수 있어요."
        action = "주소를 누르지 말고, 보낸 곳에 직접 전화해 확인하세요."
        easy = "누르지 마세요."
        signals = ["모르는 번호", "주소 포함"]
    else:
        level, scam_type = Level.safe, ScamType.none
        title = "특별한 위험은 보이지 않아요"
        explanation = "그래도 돈을 보내기 전에는 꼭 직접 확인하세요."
        action = "평소처럼 확인하세요."
        easy = "확인하고 쓰세요."
        signals = []

    return JudgeResponse(
        level=level,
        scam_type=scam_type,
        signals=signals,
        has_url=has_url,
        url_result=None,
        title=title,
        explanation=explanation,
        action=action,
        easy_sentence=easy,
        language=req.language,
        stage_reached=2,
        elapsed_ms=int((time.perf_counter() - started) * 1000),
    )


@router.post("/judge", response_model=JudgeResponse)
async def judge(req: JudgeRequest) -> JudgeResponse:
    if settings.use_fake_judge:
        return fake_judge(req)
    # TODO(백엔드 1, Day 3): cascade.run(req) 로 교체
    return fake_judge(req)
