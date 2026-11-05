
import base64
import json
from pathlib import Path

import firebase_admin
from firebase_admin import credentials

from app.config import settings

# backend 폴더 경로
BASE_DIR = Path(__file__).resolve().parent.parent


def initialize_firebase():
    """Firebase Admin SDK 초기화"""

    # 이미 Firebase가 초기화되어 있다면 기존 앱 반환
    try:
        return firebase_admin.get_app()
    except ValueError:
        pass

    # Railway 환경변수에 Base64 인증 정보가 있는 경우
    if settings.firebase_credentials_base64:
        try:
            credential_json = base64.b64decode(
                settings.firebase_credentials_base64
            ).decode("utf-8")

            credential_info = json.loads(credential_json)

        except Exception as e:
            raise ValueError(
                "Firebase Base64 인증 정보를 읽을 수 없습니다."
            ) from e

        firebase_credential = credentials.Certificate(
            credential_info
        )

    # Base64 인증 정보가 없으면 로컬 JSON 파일 사용
    else:
        credential_path = Path(settings.firebase_credentials_path)

        if not credential_path.is_absolute():
            credential_path = BASE_DIR / credential_path

        if not credential_path.exists():
            raise FileNotFoundError(
                f"Firebase 인증 파일을 찾을 수 없습니다: {credential_path}"
            )

        firebase_credential = credentials.Certificate(
            str(credential_path)
        )

    # Firebase 초기화
    firebase_app = firebase_admin.initialize_app(
        firebase_credential
    )

    print("Firebase Admin SDK 초기화 완료")

    return firebase_app
