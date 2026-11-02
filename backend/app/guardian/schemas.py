
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
