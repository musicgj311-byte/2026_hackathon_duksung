import hashlib
import secrets
from datetime import datetime, timedelta, timezone

from fastapi import APIRouter, Depends, HTTPException, status
from sqlmodel import Session, select

from app.database import get_session
from app.models import Device, GuardianLink, PairingCode
from app.guardian.schemas import (
    PairingCodeRequest,
    PairingCodeResponse,
    GuardianConnectRequest,
    GuardianConnectResponse,
    GuardianDisconnectRequest,
    GuardianDisconnectResponse,
)

router = APIRouter(prefix="/guardian", tags=["guardian"])


# 현재 시간 가져오기
def get_current_time() -> datetime:
    return datetime.now(timezone.utc)


# 연결 코드를 해시값으로 변환
def hash_code(code: str) -> str:
    return hashlib.sha256(code.encode()).hexdigest()


# 1. 연결 코드 발급 API
@router.post(
    "/pairing-code",
    response_model=PairingCodeResponse,
    status_code=status.HTTP_201_CREATED,
)
def create_pairing_code(
    request: PairingCodeRequest,
    session: Session = Depends(get_session),
):
    # 보호 대상자 기기 확인
    protected_user = session.get(Device, request.protected_device_id)

    if not protected_user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="등록된 사용자를 찾을 수 없습니다.",
        )

    if protected_user.role != "protected":
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="보호 대상자만 연결 코드를 발급할 수 있습니다.",
        )

    now = get_current_time()

    # 기존에 발급한 미사용 코드 무효화
    old_codes = session.exec(
        select(PairingCode).where(
            PairingCode.protected_device_id == request.protected_device_id,
            PairingCode.used_at == None,
        )
    ).all()

    for old_code in old_codes:
        old_code.used_at = now

    # 중복되지 않는 6자리 코드 생성
    while True:
        code = f"{secrets.randbelow(1000000):06d}"
        code_hash = hash_code(code)

        existing_code = session.exec(
            select(PairingCode).where(
                PairingCode.code_hash == code_hash
            )
        ).first()

        if not existing_code:
            break

    expires_at = now + timedelta(minutes=10)

    # DB에는 원본 코드가 아닌 해시값 저장
    pairing_code = PairingCode(
        protected_device_id=request.protected_device_id,
        code_hash=code_hash,
        expires_at=expires_at,
    )

    session.add(pairing_code)
    session.commit()

    return PairingCodeResponse(
        protected_device_id=request.protected_device_id,
        code=code,
        expires_at=expires_at,
        message="연결 코드가 발급되었습니다.",
    )


# 2. 보호자 연결 API
@router.post(
    "/connect",
    response_model=GuardianConnectResponse,
)
def connect_guardian(
    request: GuardianConnectRequest,
    session: Session = Depends(get_session),
):
    # 보호자 기기 확인
    guardian = session.get(Device, request.guardian_device_id)

    if not guardian:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="등록된 보호자 기기를 찾을 수 없습니다.",
        )

    if guardian.role != "guardian":
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="보호자 계정만 연결할 수 있습니다.",
        )

    # 입력한 코드의 해시값으로 DB 검색
    code_hash = hash_code(request.code)

    pairing_code = session.exec(
        select(PairingCode).where(
            PairingCode.code_hash == code_hash
        )
    ).first()

    if not pairing_code:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="유효하지 않은 연결 코드입니다.",
        )

    # 이미 사용한 코드인지 확인
    if pairing_code.used_at is not None:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="이미 사용된 연결 코드입니다.",
        )

    # 코드 만료 여부 확인
    expires_at = pairing_code.expires_at

    # SQLite에서 timezone 정보가 사라진 경우 보정
    if expires_at.tzinfo is None:
        expires_at = expires_at.replace(tzinfo=timezone.utc)

    if expires_at <= get_current_time():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="만료된 연결 코드입니다.",
        )

    protected_device_id = pairing_code.protected_device_id

    # 보호 대상자 기기 확인
    protected_user = session.get(Device, protected_device_id)

    if not protected_user or protected_user.role != "protected":
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="유효하지 않은 보호 대상자입니다.",
        )

    # 자기 자신과 연결하는 것 방지
    if protected_device_id == request.guardian_device_id:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="자기 자신과는 연결할 수 없습니다.",
        )

    # 기존 연결 관계 확인
    existing_link = session.exec(
        select(GuardianLink).where(
            GuardianLink.protected_device_id == protected_device_id,
            GuardianLink.guardian_device_id == request.guardian_device_id,
        )
    ).first()

    if existing_link and existing_link.is_active:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="이미 연결된 보호자입니다.",
        )

    # 기존에 해제된 관계라면 다시 활성화
    if existing_link:
        existing_link.is_active = True
    else:
        new_link = GuardianLink(
            protected_device_id=protected_device_id,
            guardian_device_id=request.guardian_device_id,
            is_active=True,
        )
        session.add(new_link)

    # 연결 코드 사용 처리
    pairing_code.used_at = get_current_time()

    session.commit()

    return GuardianConnectResponse(
        protected_device_id=protected_device_id,
        guardian_device_id=request.guardian_device_id,
        message="보호자 연결이 완료되었습니다.",
    )


# 3. 보호자 연결 해제 API
@router.delete(
    "/disconnect",
    response_model=GuardianDisconnectResponse,
)
def disconnect_guardian(
    request: GuardianDisconnectRequest,
    session: Session = Depends(get_session),
):
    # 보호 대상자 확인
    protected_user = session.get(Device, request.protected_device_id)

    if not protected_user or protected_user.role != "protected":
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="등록된 보호 대상자를 찾을 수 없습니다.",
        )

    # 보호자 확인
    guardian = session.get(Device, request.guardian_device_id)

    if not guardian or guardian.role != "guardian":
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="등록된 보호자를 찾을 수 없습니다.",
        )

    # 연결 관계 조회
    link = session.exec(
        select(GuardianLink).where(
            GuardianLink.protected_device_id == request.protected_device_id,
            GuardianLink.guardian_device_id == request.guardian_device_id,
            GuardianLink.is_active == True,
        )
    ).first()

    if not link:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="현재 연결된 보호자 관계가 없습니다.",
        )

    # 연결 해제: 레코드는 유지하고 비활성화
    link.is_active = False

    session.commit()

    return GuardianDisconnectResponse(
        protected_device_id=request.protected_device_id,
        guardian_device_id=request.guardian_device_id,
        message="보호자 연결이 해제되었습니다.",
    )


# 기존 ping API 유지
@router.get("/ping")
async def ping() -> dict[str, str]:
    return {"guardian": "ok"}


# 백엔드 1에서 호출할 보호자 알림 함수
async def notify_guardians(
    device_id: str,
    level: str,
    scam_type: str,
) -> None:
    """위험 판정 시 연결된 보호자에게 알림을 전송합니다."""
    pass