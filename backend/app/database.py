
from pathlib import Path

from sqlmodel import Session, SQLModel, create_engine

# SQLite 데이터베이스 파일 경로
BASE_DIR = Path(__file__).resolve().parent.parent
DATABASE_PATH = BASE_DIR / "shield.db"

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
