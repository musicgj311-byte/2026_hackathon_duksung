from datetime import datetime
from typing import Literal

from pydantic import BaseModel, Field


class IncidentEventRequest(BaseModel):
    """사건 이벤트 기록 요청 모델."""

    device_id: str = Field(..., description="사건이 발생한 보호 대상자 기기 ID")
    event_type: Literal["warning_shown"] = Field(
        ...,
        description="이벤트 종류 (현재 warning_shown만 지원)",
    )


class IncidentEventResponse(BaseModel):
    """사건 이벤트 기록 응답 모델."""

    id: int
    incident_id: int
    event_type: str
    occurred_at: datetime