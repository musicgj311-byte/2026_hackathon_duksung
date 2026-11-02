# ruff: noqa: E501
"""Gemini 모델과 '생각 단계' 설정별로 응답 속도와 판정 결과를 비교합니다.

실행: uv run python -m scripts.llm_speed
      uv run python -m scripts.llm_speed gemini-3.5-flash-lite gemini-3.1-flash-lite   (모델 직접 지정)

결과 표에서 '판정 OK'이면서 가장 빠른 조합을 .env의 LLM_MODEL, LLM_THINKING에 넣으면 됩니다.
"""

import asyncio
import sys
import time

import httpx

from app.config import settings
from app.judge import llm
from app.schemas import UserType

DEFAULT_MODELS = [
    "gemini-3.5-flash-lite",
    "gemini-3.1-flash-lite",
    "gemini-flash-lite-latest",
    "gemini-3.5-flash",
]
# (표에 보일 이름, generationConfig에 넣을 thinkingConfig)
THINKING = [
    ("기본", None),
    ("minimal", {"thinkingLevel": "minimal"}),
    ("low", {"thinkingLevel": "low"}),
    ("budget0", {"thinkingBudget": 0}),
]
TIMEOUT = 20.0
TEXT = "엄마 나 폰 고장나서 이 계좌로 50만원만 보내줘"


async def try_one(
    client: httpx.AsyncClient, model: str, thinking: dict | None
) -> tuple[str, float, str]:
    system, user = llm.build_prompt(
        TEXT, is_contact=False, is_international=False, weak_signals=[], final_urls=[],
        user_type=UserType.default, language="ko",
    )  # fmt: skip
    config = {"temperature": 0, "responseMimeType": "application/json"}
    if thinking:
        config["thinkingConfig"] = thinking
    started = time.perf_counter()
    try:
        r = await client.post(
            llm.GEMINI_URL.format(model=model),
            headers={"x-goog-api-key": settings.llm_api_key.strip()},
            json={
                "systemInstruction": {"parts": [{"text": system}]},
                "contents": [{"role": "user", "parts": [{"text": user}]}],
                "generationConfig": config,
            },
            timeout=TIMEOUT,
        )
    except httpx.TimeoutException:
        return "시간 초과", time.perf_counter() - started, ""
    except httpx.HTTPError as e:
        return "연결 실패", time.perf_counter() - started, type(e).__name__
    sec = time.perf_counter() - started
    if r.status_code != 200:
        msg = r.json().get("error", {}).get("message", "")[:60] if r.content else ""
        return f"오류 {r.status_code}", sec, msg
    try:
        raw = r.json()["candidates"][0]["content"]["parts"][0]["text"]
    except (KeyError, IndexError):
        return "빈 응답", sec, ""
    v = llm.parse_verdict(raw)
    if v is None:
        return "형식 오류", sec, raw[:60]
    return "판정 OK", sec, f"{v.scam_type.value} / 의심 {v.suspicion} / {v.title}"


async def main() -> None:
    models = sys.argv[1:] or DEFAULT_MODELS
    print(f"{'모델':28} {'생각 단계':9} {'결과':10} {'시간':>6}  내용")
    async with httpx.AsyncClient() as client:
        for model in models:
            for name, thinking in THINKING:
                status, sec, info = await try_one(client, model, thinking)
                print(f"{model:28} {name:9} {status:10} {sec:5.1f}초  {info}", flush=True)
                if status == "오류 404":
                    break  # 이 모델은 쓸 수 없음, 다음 모델로
                await asyncio.sleep(4.5)  # 무료 등급 분당 호출 제한 대비


if __name__ == "__main__":
    asyncio.run(main())
