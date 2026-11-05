import hashlib
import secrets
from datetime import UTC, datetime, timedelta

import firebase_admin
from fastapi import APIRouter, Depends, HTTPException, status
from firebase_admin import messaging
from sqlmodel import Session, select

from app.database import engine, get_session
from app.guardian.schemas import (
    GuardianConnectRequest,
    GuardianConnectResponse,
    GuardianDisconnectRequest,
    GuardianDisconnectResponse,
    IncidentEventInfo,
    IncidentInfo,
    IncidentListResponse,
    NotificationInfo,
    NotificationListResponse,
    PairingCodeRequest,
    PairingCodeResponse,
    ProtectedUserInfo,
    ProtectedUserListResponse,
)
from app.models import (
    Device,
    GuardianLink,
    NotificationLog,
    PairingCode,
    ScamEvent,
    ScamIncident,
)

router = APIRouter(prefix="/guardian", tags=["guardian"])


# 현재 시간 가져오기
def get_current_time() -> datetime:
    return datetime.now(UTC)


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
        expires_at = expires_at.replace(tzinfo=UTC)

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
    """위험 판정 시 연결된 보호자에게 FCM 알림을 전송합니다."""

    # 위험도가 danger가 아니면 알림을 보내지 않음
    if level != "danger":
        return

    # 보호자 연결 및 FCM 토큰 조회를 위한 DB 세션 생성
    with Session(engine) as session:

        # 해당 보호 대상자와 연결된 활성 보호자 조회
        guardian_links = session.exec(
            select(GuardianLink).where(
                GuardianLink.protected_device_id == device_id,
                GuardianLink.is_active == True,
            )
        ).all()

        # 연결된 보호자가 없으면 종료
        if not guardian_links:
            return

        # Firebase가 초기화되어 있는지 확인
        firebase_app = firebase_admin.get_app()

        # 연결된 보호자 각각에게 알림 전송
        for link in guardian_links:

            guardian = session.get(
                Device,
                link.guardian_device_id,
            )

            if not guardian:
                continue

            # 푸시 토큰이 없으면 전송은 건너뛰고 기록만 남김 (보호자 앱 목록에 표시용)
            if not guardian.fcm_token:
                session.add(NotificationLog(
                    protected_device_id=device_id,
                    guardian_device_id=guardian.device_id,
                    level=level,
                    scam_type=scam_type,
                    status="no_token",
                ))
                session.commit()
                continue

            # 푸시 알림 내용 구성
            message = messaging.Message(
                notification=messaging.Notification(
                    title="사기 의심 문자 감지",
                    body=(
                        "보호 대상자 기기에서 위험도가 높은 문자가 "
                        "감지되었습니다. 앱에서 확인해 주세요."
                    ),
                ),
                data={
                    "protected_device_id": device_id,
                    "level": level,
                    "scam_type": scam_type,
                },
                token=guardian.fcm_token,
            )

            # 알림 전송 결과 기록
            log = NotificationLog(
                protected_device_id=device_id,
                guardian_device_id=guardian.device_id,
                level=level,
                scam_type=scam_type,
                status="pending",
            )

            session.add(log)
            session.commit()
            session.refresh(log)

            try:
                # Firebase Cloud Messaging으로 푸시 알림 전송
                messaging.send(
                    message,
                    app=firebase_app,
                )

                # 전송 성공
                log.status = "success"

            except Exception as e:
                # 전송 실패
                log.status = "failed"

                print(
                    f"보호자 알림 전송 실패 "
                    f"({guardian.device_id}): {e}"
                )

            # 전송 결과 DB 반영
            session.add(log)
            session.commit()


# 보호자와 연결된 보호 대상자 목록 조회
@router.get(
    "/{guardian_device_id}/protected-users",
    response_model=ProtectedUserListResponse,
)
def get_protected_users(
    guardian_device_id: str,
    session: Session = Depends(get_session),
):
    # 1. 요청한 기기가 실제 보호자인지 확인
    guardian = session.get(Device, guardian_device_id)

    if not guardian or guardian.role != "guardian":
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="보호자를 찾을 수 없습니다.",
        )

    # 2. 해당 보호자와 현재 연결된 보호 대상자 조회
    protected_users = session.exec(
        select(Device)
        .join(
            GuardianLink,
            Device.device_id == GuardianLink.protected_device_id,
        )
        .where(
            GuardianLink.guardian_device_id == guardian_device_id,
            GuardianLink.is_active == True,
            Device.role == "protected",
        )
    ).all()

    # 3. 응답 형식에 맞게 데이터 구성
    return ProtectedUserListResponse(
        guardian_device_id=guardian_device_id,
        protected_users=[
            ProtectedUserInfo(
                protected_device_id=user.device_id,
                user_type=user.user_type,
                language=user.language,
            )
            for user in protected_users
        ],
    )


# 보호자에게 전송된 알림 이력 조회
@router.get(
    "/{guardian_device_id}/notifications",
    response_model=NotificationListResponse,
)
def get_notifications(
    guardian_device_id: str,
    session: Session = Depends(get_session),
):
    # 1. 요청한 기기가 실제 보호자인지 확인
    guardian = session.get(Device, guardian_device_id)

    if not guardian or guardian.role != "guardian":
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="보호자를 찾을 수 없습니다.",
        )

    # 2. 해당 보호자에게 기록된 알림 이력 조회
    notifications = session.exec(
        select(NotificationLog)
        .where(
            NotificationLog.guardian_device_id == guardian_device_id
        )
        .order_by(NotificationLog.sent_at.desc())
    ).all()

    # 3. 응답 형식에 맞게 데이터 구성
    return NotificationListResponse(
        guardian_device_id=guardian_device_id,
        notifications=[
            NotificationInfo(
                id=notification.id,
                protected_device_id=notification.protected_device_id,
                level=notification.level,
                scam_type=notification.scam_type,
                status=notification.status,
                sent_at=notification.sent_at,
            )
            for notification in notifications
        ],
    )

# 보호자에게 연결된 보호 대상자의 사건 타임라인 조회
@router.get(
    "/{guardian_device_id}/incidents",
    response_model=IncidentListResponse,
)
def get_incidents(
    guardian_device_id: str,
    session: Session = Depends(get_session),
):
    # 1. 요청한 기기가 실제 보호자인지 확인
    guardian = session.get(Device, guardian_device_id)

    if not guardian or guardian.role != "guardian":
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="보호자를 찾을 수 없습니다.",
        )

    # 2. 해당 보호자와 현재 연결된 보호 대상자 ID 조회
    protected_device_ids = session.exec(
        select(GuardianLink.protected_device_id).where(
            GuardianLink.guardian_device_id == guardian_device_id,
            GuardianLink.is_active == True,
        )
    ).all()

    # 연결된 보호 대상자가 없으면 빈 목록 반환
    if not protected_device_ids:
        return IncidentListResponse(
            guardian_device_id=guardian_device_id,
            incidents=[],
        )

    # 3. 연결된 보호 대상자의 사건 조회 (최신순)
    incidents = session.exec(
        select(ScamIncident)
        .where(
            ScamIncident.protected_device_id.in_(protected_device_ids)
        )
        .order_by(ScamIncident.received_at.desc())
    ).all()

    # 4. 각 사건에 연결된 이벤트 조회 및 응답 구성
    incident_infos = []

    for incident in incidents:
        events = session.exec(
            select(ScamEvent)
            .where(
                ScamEvent.incident_id == incident.id
            )
            .order_by(ScamEvent.occurred_at.asc())
        ).all()

        incident_infos.append(
            IncidentInfo(
                incident_id=incident.id,
                protected_device_id=incident.protected_device_id,
                level=incident.level,
                scam_type=incident.scam_type,
                received_at=incident.received_at,
                events=[
                    IncidentEventInfo(
                        event_type=event.event_type,
                        occurred_at=event.occurred_at,
                    )
                    for event in events
                ],
            )
        )

    # 5. 보호자에게 사건 목록 반환
    return IncidentListResponse(
        guardian_device_id=guardian_device_id,
        incidents=incident_infos,
    )