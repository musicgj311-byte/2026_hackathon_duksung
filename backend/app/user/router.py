from fastapi import APIRouter, Depends, HTTPException, status
from sqlmodel import Session, select

from app.database import get_session
from app.models import Device
from app.user.schemas import (
    UserRegisterRequest,
    UserRegisterResponse,
    UserUpdateRequest,
    UserUpdateResponse,
    FCMTokenRequest,
    FCMTokenResponse,
)

router = APIRouter(prefix="/users", tags=["users"])


# 1. 사용자 등록 API
@router.post(
    "/register",
    response_model=UserRegisterResponse,
    status_code=status.HTTP_201_CREATED,
)
def register_user(
    request: UserRegisterRequest,
    session: Session = Depends(get_session),
):
    # 이미 등록된 기기인지 확인
    existing_user = session.get(Device, request.device_id)

    if existing_user:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="이미 등록된 기기 ID입니다.",
        )

    # 새로운 사용자 정보 생성
    new_user = Device(
        device_id=request.device_id,
        role=request.role,
        user_type=request.user_type.value,
        language=request.language,
    )

    # 데이터베이스에 저장
    session.add(new_user)
    session.commit()
    session.refresh(new_user)

    return UserRegisterResponse(
        device_id=new_user.device_id,
        role=new_user.role,
        user_type=new_user.user_type,
        language=new_user.language,
        message="사용자 등록이 완료되었습니다.",
    )


# 2. 사용자 정보 조회 API
@router.get(
    "/{device_id}",
    response_model=UserRegisterResponse,
)
def get_user(
    device_id: str,
    session: Session = Depends(get_session),
):
    # 기기 ID를 기준으로 사용자 조회
    user = session.get(Device, device_id)

    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="등록된 사용자를 찾을 수 없습니다.",
        )

    return UserRegisterResponse(
        device_id=user.device_id,
        role=user.role,
        user_type=user.user_type,
        language=user.language,
        message="사용자 정보를 조회했습니다.",
    )


# 3. 사용자 설정 수정 API
@router.patch(
    "/{device_id}",
    response_model=UserUpdateResponse,
)
def update_user(
    device_id: str,
    request: UserUpdateRequest,
    session: Session = Depends(get_session),
):
    # 수정할 사용자 조회
    user = session.get(Device, device_id)

    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="등록된 사용자를 찾을 수 없습니다.",
        )

    # 요청에서 실제로 전달된 항목만 추출
    update_data = request.model_dump(exclude_unset=True)

    # 수정할 항목이 없는 경우
    if not update_data or all(
        value is None for value in update_data.values()
    ):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="수정할 정보를 입력해 주세요.",
        )

    # 전달된 항목만 수정
    if update_data.get("user_type") is not None:
        user.user_type = update_data["user_type"].value

    if update_data.get("language") is not None:
        user.language = update_data["language"]

    # 변경 사항 저장
    session.add(user)
    session.commit()
    session.refresh(user)

    return UserUpdateResponse(
        device_id=user.device_id,
        user_type=user.user_type,
        language=user.language,
        message="사용자 정보가 수정되었습니다.",
    )

    
# 4. FCM 토큰 저장 및 갱신 API
@router.put(
    "/{device_id}/fcm-token",
    response_model=FCMTokenResponse,
)
def update_fcm_token(
    device_id: str,
    request: FCMTokenRequest,
    session: Session = Depends(get_session),
):
    # FCM 토큰을 저장할 사용자 조회
    user = session.get(Device, device_id)

    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="등록된 사용자를 찾을 수 없습니다.",
        )

    # 기존 FCM 토큰을 새로운 토큰으로 갱신
    user.fcm_token = request.fcm_token

    # 변경 사항 저장
    session.add(user)
    session.commit()
    session.refresh(user)

    return FCMTokenResponse(
        device_id=user.device_id,
        message="FCM 토큰이 등록되었습니다.",
    )
