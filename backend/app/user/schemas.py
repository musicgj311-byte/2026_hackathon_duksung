
from typing import Literal

from pydantic import BaseModel, Field

from app.schemas import UserType


# 사용자 최초 등록 시 전달받는 데이터
class UserRegisterRequest(BaseModel):
    device_id: str = Field(min_length=1) #앱이 사용자 기기에 부여한 고유 ID
    role: Literal["protected", "guardian"] #보호 대상자(protected)인지 보호자(guardian)인지 구분
    user_type: UserType = UserType.default #기본 사용자, 시각·청각·발달 지원 유형(등록 후에도 변경 가능)
    language: str = "ko" #앱에서 사용할 언어(등록 후에도 변경 가능)


# 사용자 등록 후 반환할 데이터
class UserRegisterResponse(BaseModel):
    device_id: str
    role: str
    user_type: UserType
    language: str
    message: str


# 사용자 설정 수정 시 전달받는 데이터
class UserUpdateRequest(BaseModel):
    user_type: UserType | None = None
    language: str | None = None


# 사용자 설정 수정 후 반환할 데이터
class UserUpdateResponse(BaseModel):
    device_id: str
    user_type: UserType
    language: str
    message: str
