
from pathlib import Path

import firebase_admin
from firebase_admin import credentials

from app.config import settings


# backend 폴더의 절대 경로
BASE_DIR = Path(__file__).resolve().parent.parent


def initialize_firebase():
    """Firebase Admin SDK 초기화"""

    # .env에 설정한 인증 파일 경로 가져오기
    credential_path = Path(settings.firebase_credentials_path)

    # 상대 경로라면 backend 폴더를 기준으로 변환
    if not credential_path.is_absolute():
        credential_path = BASE_DIR / credential_path

    # 인증 파일 존재 여부 확인
    if not credential_path.exists():
        raise FileNotFoundError(
            f"Firebase 인증 파일을 찾을 수 없습니다: {credential_path}"
        )

    # 이미 Firebase가 초기화되어 있다면 기존 앱 반환
    try:
        return firebase_admin.get_app()
    except ValueError:
        pass

    # 서비스 계정 JSON으로 인증
    firebase_credential = credentials.Certificate(
        str(credential_path)
    )

    # Firebase 초기화
    firebase_app = firebase_admin.initialize_app(
        firebase_credential
    )

    print("Firebase Admin SDK 초기화 완료")

    return firebase_app
