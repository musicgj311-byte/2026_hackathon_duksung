
import os
from pathlib import Path

from sqlmodel import Session, SQLModel, create_engine

# 현재 프로젝트의 기본 경로
BASE_DIR = Path(__file__).resolve().parent.parent

# 환경변수에 DATABASE_PATH가 있으면 해당 경로 사용
# 없으면 기존 로컬 경로 사용
DATABASE_PATH = Path(
    os.getenv("DATABASE_PATH", str(BASE_DIR / "shield.db"))
)

# 데이터베이스 파일을 저장할 폴더가 없으면 생성
DATABASE_PATH.parent.mkdir(parents=True, exist_ok=True)

# SQLite 연결 주소
DATABASE_URL = f"sqlite:///{DATABASE_PATH.as_posix()}"

# 데이터베이스 엔진 생성
engine = create_engine(
    DATABASE_URL,
    connect_args={"check_same_thread": False},
    echo=False,
)


# 데이터베이스 테이블 생성
def create_db_and_tables():
    SQLModel.metadata.create_all(engine)


# API에서 사용할 DB 세션 제공
def get_session():
    with Session(engine) as session:
        yield session
