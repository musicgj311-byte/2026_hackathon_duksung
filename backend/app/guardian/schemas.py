
from datetime import datetime

from pydantic import BaseModel, Field


# 1. 연결 코드를 발급받을 보호 대상자의 ID 전달
class PairingCodeRequest(BaseModel):
    protected_device_id: str = Field(min_length=1)


# 2. 발급된 6자리 코드와 만료 시간 반환
class PairingCodeResponse(BaseModel):
    protected_device_id: str
    code: str
    expires_at: datetime
    message: str


# 3. 보호자 ID와 연결 코드 전달
class GuardianConnectRequest(BaseModel):
    guardian_device_id: str = Field(min_length=1)
    code: str = Field(pattern=r"^\d{6}$") #입력값이 숫자 6자리인지 검사


# 4. 보호자 연결 결과(연결 성공 결과 반환)
class GuardianConnectResponse(BaseModel):
    protected_device_id: str
    guardian_device_id: str
    message: str


# 5. 보호자 연결 해제 요청(연결을 해제할 두 기기의 ID 전달)
class GuardianDisconnectRequest(BaseModel):
    protected_device_id: str = Field(min_length=1)
    guardian_device_id: str = Field(min_length=1)


# 6. 보호자 연결 해제 결과(연결 해제 결과 반환)
class GuardianDisconnectResponse(BaseModel):
    protected_device_id: str
    guardian_device_id: str
    message: str


# 7. 연결된 보호 대상자 정보
class ProtectedUserInfo(BaseModel):
    protected_device_id: str
    user_type: str
    language: str


# 8. 연결된 보호 대상자 목록 조회 결과
class ProtectedUserListResponse(BaseModel):
    guardian_device_id: str
    protected_users: list[ProtectedUserInfo]


# 9. 보호자 알림 이력 정보
class NotificationInfo(BaseModel):
    id: int
    protected_device_id: str
    level: str
    scam_type: str
    status: str
    sent_at: datetime


# 10. 보호자 알림 이력 조회 결과
class NotificationListResponse(BaseModel):
    guardian_device_id: str
    notifications: list[NotificationInfo]

# 11. 사건별 이벤트 정보
class IncidentEventInfo(BaseModel):
    event_type: str
    occurred_at: datetime


# 12. 사건 정보 및 이벤트 목록
class IncidentInfo(BaseModel):
    incident_id: int
    protected_device_id: str
    level: str
    scam_type: str
    received_at: datetime
    events: list[IncidentEventInfo]


# 13. 보호자 사건 타임라인 조회 결과
class IncidentListResponse(BaseModel):
    guardian_device_id: str
    incidents: list[IncidentInfo]


# 14. 보호 대상자에게 연결된 보호자 정보
class GuardianInfo(BaseModel):
    guardian_device_id: str
    connected_at: datetime


# 15. 보호 대상자의 보호자 목록 조회 결과 (피보호자 앱이 연결 완료를 확인할 때 사용)
class GuardianListResponse(BaseModel):
    protected_device_id: str
    guardians: list[GuardianInfo]
