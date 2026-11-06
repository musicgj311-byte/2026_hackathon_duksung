
import asyncio

from sqlmodel import Session, select

from app.database import create_db_and_tables, engine
from app.models import Device, GuardianLink, NotificationLog
from app.guardian import router as guardian_module


# 테스트 전용 기기 ID
PROTECTED_ID = "mock-fcm-protected-001"
GUARDIAN_ID = "mock-fcm-guardian-001"


# 1. 테스트용 DB 데이터 준비
def prepare_test_data():
    create_db_and_tables()

    with Session(engine) as session:

        # 기존 테스트 기록 삭제
        old_logs = session.exec(
            select(NotificationLog).where(
                NotificationLog.protected_device_id == PROTECTED_ID
            )
        ).all()

        for log in old_logs:
            session.delete(log)

        # 보호 대상자 등록 또는 갱신
        protected = session.get(Device, PROTECTED_ID)

        if not protected:
            protected = Device(
                device_id=PROTECTED_ID,
                role="protected",
            )
            session.add(protected)

        # 보호자 등록 또는 갱신
        guardian = session.get(Device, GUARDIAN_ID)

        if not guardian:
            guardian = Device(
                device_id=GUARDIAN_ID,
                role="guardian",
                fcm_token="mock-fcm-token-for-test-only",
            )
            session.add(guardian)
        else:
            guardian.fcm_token = "mock-fcm-token-for-test-only"

        # 보호자 연결 관계 등록 또는 활성화
        link = session.exec(
            select(GuardianLink).where(
                GuardianLink.protected_device_id == PROTECTED_ID,
                GuardianLink.guardian_device_id == GUARDIAN_ID,
            )
        ).first()

        if not link:
            link = GuardianLink(
                protected_device_id=PROTECTED_ID,
                guardian_device_id=GUARDIAN_ID,
                is_active=True,
            )
            session.add(link)
        else:
            link.is_active = True

        session.commit()

    print("테스트 데이터 준비 완료")


# 2. FCM 성공을 가정하는 모의 함수
def fake_send_success(message, app=None):
    print("\n[모의 FCM 전송 성공]")
    print("알림 제목:", message.notification.title)
    print("알림 내용:", message.notification.body)
    print("위험도:", message.data["level"])

    return "mock-message-id"


# 3. FCM 실패를 가정하는 모의 함수
def fake_send_failure(message, app=None):
    print("\n[모의 FCM 전송 실패]")
    raise RuntimeError("테스트용 전송 실패")


# 4. 실제 FCM 대신 모의 함수로 테스트
async def run_tests():

    # 원래 함수 보관
    original_send = guardian_module.messaging.send
    original_get_app = guardian_module.firebase_admin.get_app

    try:
        # Firebase 초기화 없이 테스트할 수 있도록 임시 대체
        guardian_module.firebase_admin.get_app = lambda: object()

        # 테스트 ① danger + 전송 성공
        print("\n===== 테스트 ① 전송 성공 =====")

        guardian_module.messaging.send = fake_send_success

        await guardian_module.notify_guardians(
            device_id=PROTECTED_ID,
            level="danger",
            scam_type="smishing",
        )

        # 테스트 ② caution + 전송하지 않음
        print("\n===== 테스트 ② 알림 미전송 =====")

        await guardian_module.notify_guardians(
            device_id=PROTECTED_ID,
            level="caution",
            scam_type="smishing",
        )

        print("caution 알림 전송 함수 종료")

        # 테스트 ③ danger + 전송 실패
        print("\n===== 테스트 ③ 전송 실패 =====")

        guardian_module.messaging.send = fake_send_failure

        await guardian_module.notify_guardians(
            device_id=PROTECTED_ID,
            level="danger",
            scam_type="smishing",
        )

    finally:
        # 테스트 종료 후 원래 함수 복구
        guardian_module.messaging.send = original_send
        guardian_module.firebase_admin.get_app = original_get_app


# 5. DB에 저장된 결과 확인
def check_results():

    with Session(engine) as session:

        logs = session.exec(
            select(NotificationLog).where(
                NotificationLog.protected_device_id == PROTECTED_ID
            )
        ).all()

        print("\n===== DB 알림 기록 확인 =====")

        for log in logs:
            print(
                f"보호자: {log.guardian_device_id} | "
                f"위험도: {log.level} | "
                f"전송 상태: {log.status}"
            )

        statuses = sorted(log.status for log in logs)

        assert len(logs) == 2, (
            f"알림 기록 개수가 예상과 다릅니다: {len(logs)}"
        )

        assert statuses == ["failed", "success"], (
            f"전송 상태가 예상과 다릅니다: {statuses}"
        )

        print("\n모든 모의 테스트 통과!")


if __name__ == "__main__":
    prepare_test_data()
    asyncio.run(run_tests())
    check_results()
