#설계 결정: 보호자와 보호 대상자 모두 앱 등록. 보호자 연결 수: 여러 명. 인증 코드 유효기간: 10분
from datetime import datetime, timezone

from sqlalchemy import UniqueConstraint
from sqlmodel import Field, SQLModel


def get_current_time() -> datetime:
    return datetime.now(timezone.utc)


# 1. 사용자 및 보호자 기기 정보
class Device(SQLModel, table=True):
    __tablename__ = "devices"

    device_id: str = Field(primary_key=True)
    role: str = Field(index=True)  # protected / guardian
    user_type: str = Field(default="default")
    language: str = Field(default="ko")
    fcm_token: str | None = Field(default=None)
    created_at: datetime = Field(default_factory=get_current_time)
    updated_at: datetime = Field(default_factory=get_current_time)


# 2. 보호자 연결 인증 코드(발급시간, 만료 시간, 사용 여부 기록)
class PairingCode(SQLModel, table=True):
    __tablename__ = "pairing_codes"

    id: int | None = Field(default=None, primary_key=True)
    protected_device_id: str = Field(foreign_key="devices.device_id", index=True)
    code_hash: str = Field(unique=True, index=True) #해시값으로 저장
    expires_at: datetime
    used_at: datetime | None = Field(default=None)
    created_at: datetime = Field(default_factory=get_current_time)


# 3. 보호 대상자와 보호자의 연결 관계(보호자 여러 명 가능)
class GuardianLink(SQLModel, table=True):
    __tablename__ = "guardian_links"

    __table_args__ = (
        UniqueConstraint(
            "protected_device_id",
            "guardian_device_id",
            name="uq_guardian_link_pair",
        ),
    )

    id: int | None = Field(default=None, primary_key=True)
    protected_device_id: str = Field(foreign_key="devices.device_id", index=True)
    guardian_device_id: str = Field(foreign_key="devices.device_id", index=True)
    is_active: bool = Field(default=True)
    created_at: datetime = Field(default_factory=get_current_time)


# 4. 보호자 알림 발송 기록, 위험 문자가 발견됐을 때 보호자에게 알림이 제대로 전달됐는지 기록. 문자 원문은 저장하지 않음.
class NotificationLog(SQLModel, table=True):
    __tablename__ = "notification_logs"

    id: int | None = Field(default=None, primary_key=True)
    protected_device_id: str = Field(foreign_key="devices.device_id", index=True)
    guardian_device_id: str = Field(foreign_key="devices.device_id", index=True)
    level: str
    scam_type: str
    status: str = Field(default="pending")  # pending / success / failed
    sent_at: datetime = Field(default_factory=get_current_time)

# 5. 사기 의심 사건 기록
class ScamIncident(SQLModel, table=True):
    __tablename__ = "scam_incidents"

    id: int | None = Field(default=None, primary_key=True)

    # 사건이 발생한 보호 대상자 기기
    protected_device_id: str = Field(
        foreign_key="devices.device_id",
        index=True,
    )

    # 판정 결과
    level: str  # danger / caution
    scam_type: str

    # 백엔드가 판정 요청을 받은 시각
    received_at: datetime = Field(default_factory=get_current_time)


# 6. 사건별 진행 이벤트 기록
class ScamEvent(SQLModel, table=True):
    __tablename__ = "scam_events"

    id: int | None = Field(default=None, primary_key=True)

    # 어떤 사건에서 발생한 이벤트인지 연결
    incident_id: int = Field(
        foreign_key="scam_incidents.id",
        index=True,
    )

    # 이벤트 종류 (현재는 warning_shown만 사용)
    event_type: str

    # 백엔드가 이벤트 요청을 받은 시각
    occurred_at: datetime = Field(default_factory=get_current_time)