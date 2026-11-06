# ruff: noqa: E501
"""LLM 생각 단계 설정과 예비 모델 전환을 확인합니다."""

import asyncio
import json

import httpx
import pytest

from app.config import settings
from app.judge import llm
from app.schemas import ScamType, UserType

VERDICT = {
    "scam_type": "family_impersonation", "suspicion": 95, "requests_money": True,
    "signals": ["가족 사칭"], "title": "가족 사칭 주의", "explanation": "가족인 척 돈을 요구해요.",
    "action": "원래 번호로 전화하세요.", "easy_sentence": "돈 보내지 마세요.",
}  # fmt: skip


def ok(text=None):
    body = text if text is not None else json.dumps(VERDICT, ensure_ascii=False)
    return httpx.Response(200, json={"candidates": [{"content": {"parts": [{"text": body}]}}]})


@pytest.fixture(autouse=True)
def keys(monkeypatch):
    monkeypatch.setattr(settings, "llm_api_key", "AQ.test")
    monkeypatch.setattr(settings, "llm_model", "primary-model")
    monkeypatch.setattr(settings, "llm_fallback_model", "backup-model")
    monkeypatch.setattr(settings, "llm_thinking_level", "minimal")


def call(handler):
    client = httpx.AsyncClient(transport=httpx.MockTransport(handler))
    return asyncio.run(
        llm.judge_with_llm(
            "엄마 나 폰 고장 이 계좌로 보내줘", is_contact=False, is_international=False,
            weak_signals=[], final_urls=[], user_type=UserType.default, language="ko", client=client,
        )
    )  # fmt: skip


def test_thinking_level_sent(monkeypatch):
    seen = []

    def handler(req):
        seen.append(json.loads(req.content)["generationConfig"])
        return ok()

    assert call(handler).scam_type is ScamType.family_impersonation
    assert seen[0]["thinkingConfig"] == {"thinkingLevel": "minimal"}

    monkeypatch.setattr(settings, "llm_thinking_level", "")
    seen.clear()
    call(handler)
    assert "thinkingConfig" not in seen[0]


def test_busy_primary_falls_back():
    tried = []

    def handler(req):
        model = req.url.path.split("/")[-1].split(":")[0]
        tried.append(model)
        return httpx.Response(503, json={}) if model == "primary-model" else ok()

    assert call(handler) is not None
    assert tried == ["primary-model", "backup-model"]


def test_bad_answer_falls_back():
    tried = []

    def handler(req):
        model = req.url.path.split("/")[-1].split(":")[0]
        tried.append(model)
        return ok("모르겠어요") if model == "primary-model" else ok()

    assert call(handler) is not None
    assert tried == ["primary-model", "backup-model"]


def test_slow_primary_falls_back(monkeypatch):
    monkeypatch.setattr(llm, "PRIMARY_TIMEOUT_SECONDS", 0.05)
    monkeypatch.setattr(llm, "LLM_TIMEOUT_SECONDS", 2.0)

    async def handler(req):
        if "primary-model" in req.url.path:
            await asyncio.sleep(1)
        return ok()

    assert call(handler) is not None


def test_no_fallback_when_same_or_empty(monkeypatch):
    tried = []

    def handler(req):
        tried.append(req.url.path)
        return httpx.Response(503, json={})

    monkeypatch.setattr(settings, "llm_fallback_model", "")
    assert call(handler) is None and len(tried) == 1
    tried.clear()
    monkeypatch.setattr(settings, "llm_fallback_model", "primary-model")
    assert call(handler) is None and len(tried) == 1
