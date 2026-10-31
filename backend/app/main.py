from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import settings
from app.guardian.router import router as guardian_router
from app.judge.router import router as judge_router

app = FastAPI(title="사기문자 방패 API", version="0.1.0")

# 보호자 웹 대시보드에서 호출할 수 있도록 허용 (해커톤용으로 전체 허용)
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])

app.include_router(judge_router)
app.include_router(guardian_router)


@app.get("/health")
async def health() -> dict[str, object]:
    """서버가 살아 있는지, 가짜 판정 모드인지 확인합니다. 프론트 연결 테스트용."""
    return {"status": "ok", "fake_judge": settings.use_fake_judge}
