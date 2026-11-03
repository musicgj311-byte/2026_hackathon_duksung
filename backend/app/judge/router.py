"""판정 API (백엔드 1 담당).

USE_FAKE_JUDGE=true  : 키워드만 보고 고정된 가짜 응답 (프론트 연결 확인용)
USE_FAKE_JUDGE=false : 캐스케이드 ②~④ 실제 판정 (cascade.run)
어느 쪽이든 '위험'이면 보호자 알림 함수를 호출합니다.
"""

import re
import time

from fastapi import APIRouter, BackgroundTasks
from pydantic import BaseModel, Field
from sqlmodel import Session

from app.config import settings
from app.database import engine
from app.guardian.router import notify_guardians
from app.judge import cascade
from app.judge.signals import collect_fast_signals
from app.models import ScamIncident
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


class SignalsRequest(BaseModel):
    text: str = Field(..., min_length=1, max_length=2000)


@router.post("/judge/debug/signals")
async def debug_signals(req: SignalsRequest) -> dict:
    """② 빠른 신호만 실행해서 결과를 그대로 보여줍니다. 개발 확인용 (Day 2)."""
    return (await collect_fast_signals(req.text)).to_dict()


@router.post("/judge", response_model=JudgeResponse)
async def judge(req: JudgeRequest, background: BackgroundTasks) -> JudgeResponse:
    # 1. 문자 판정 실행
    result = fake_judge(req) if settings.use_fake_judge else await cascade.run(req)

    # 2. 위험 또는 주의 판정이면 사건 기록 생성
    if result.level in (Level.danger, Level.caution):
        incident = ScamIncident(
            protected_device_id=req.device_id,
            level=result.level.value,
            scam_type=result.scam_type.value,
        )

        # DB에 사건 저장
        with Session(engine) as session:
            session.add(incident)
            session.commit()
            session.refresh(incident)

        # 생성된 사건 ID를 판정 응답에 추가
        result.incident_id = incident.id

    # 3. 위험 판정일 때만 보호자 알림 전송
    if result.level is Level.danger:
        background.add_task(
            notify_guardians,
            req.device_id,
            result.level.value,
            result.scam_type.value,
        )

    return result
