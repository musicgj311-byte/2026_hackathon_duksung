"""보호자 연결·알림 API (백엔드 2 담당).

Day 1에는 자리만 잡아둡니다. Day 2에 6자리 코드 발급·연결·해제를 구현하세요.
"""

from fastapi import APIRouter

router = APIRouter(prefix="/guardian", tags=["guardian"])


@router.get("/ping")
async def ping() -> dict[str, str]:
    return {"guardian": "ok"}


async def notify_guardians(device_id: str, level: str, scam_type: str) -> None:
    """위험 판정 시 백엔드 1이 호출하는 함수. 문자 원문은 넘기지 않습니다.

    TODO(백엔드 2, Day 3): 연결된 보호자 조회 → FCM 발송 → 알림 이력 저장
    """
    pass
