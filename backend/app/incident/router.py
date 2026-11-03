from fastapi import APIRouter, HTTPException
from sqlmodel import Session, select

from app.database import engine
from app.models import ScamEvent, ScamIncident
from app.incident.schemas import (
    IncidentEventRequest,
    IncidentEventResponse,
)

router = APIRouter(prefix="/incidents", tags=["incidents"])


@router.post(
    "/{incident_id}/events",
    response_model=IncidentEventResponse,
)
async def create_incident_event(
    incident_id: int,
    req: IncidentEventRequest,
) -> IncidentEventResponse:
    """사건에 대한 이벤트를 기록합니다."""

    with Session(engine) as session:
        # 1. 사건이 실제로 존재하는지 확인
        incident = session.get(ScamIncident, incident_id)

        if incident is None:
            raise HTTPException(
                status_code=404,
                detail="존재하지 않는 사건입니다.",
            )

        # 2. 요청한 기기가 해당 사건의 보호 대상자 기기인지 확인
        if incident.protected_device_id != req.device_id:
            raise HTTPException(
                status_code=403,
                detail="해당 사건에 대한 접근 권한이 없습니다.",
            )

        # 3. 이벤트 기록 생성
        event = ScamEvent(
            incident_id=incident_id,
            event_type=req.event_type,
        )

        session.add(event)
        session.commit()
        session.refresh(event)

        # 4. 저장된 이벤트 반환
        return IncidentEventResponse(
            id=event.id,
            incident_id=event.incident_id,
            event_type=event.event_type,
            occurred_at=event.occurred_at,
        )