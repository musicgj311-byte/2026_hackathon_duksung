# ruff: noqa: E501
"""Day 3: 규칙, LLM 응답 처리, ④ 신호 종합, /judge 실제 모드를 확인합니다.
외부 서비스는 부르지 않고 가짜 응답을 씁니다.
"""

import asyncio
import json

import httpx
import pytest
from fastapi.testclient import TestClient

from app.config import settings
from app.judge import cascade, llm
from app.judge.cascade import decide
from app.judge.llm import LlmVerdict, judge_with_llm, parse_verdict
from app.judge.rules import check_rules
from app.judge.signals import FastSignals, UrlSignal
from app.main import app
from app.schemas import Level, ScamType, UserType

FAMILY = "엄마 나 폰 액정 깨져서 임시폰이야 이 계좌로 50만원만 보내줘"


def verdict(**kw) -> LlmVerdict:
    base = dict(
        scam_type="family_impersonation",
        suspicion=90,
        requests_money=True,
        signals=["가족 사칭", "송금 요구"],
        title="사기 문자 같아요",
        explanation="가족인 척 돈을 요구하는 문자예요.",
        action="가족에게 원래 번호로 전화해 확인하세요.",
        easy_sentence="돈 보내지 마세요.",
    )
    return LlmVerdict.model_validate(base | kw)


def url(**kw) -> UrlSignal:
    return UrlSignal(
        **(
            {
                "original": "http://a.xyz",
                "final": "http://a.xyz",
                "expanded": False,
                "domain": "a.xyz",
            }
            | kw
        )
    )


# ---------- rules ----------


def test_rules_family_with_money():
    r = check_rules(FAMILY)
    assert r.top_type is ScamType.family_impersonation
    assert r.requests_money and r.impersonation_with_demand


def test_rules_plain_message():
    r = check_rules("오늘 저녁 7시에 집 앞에서 봐")
    assert r.scam_types == [] and not r.pushes_action


# ---------- LLM 응답 처리 ----------


def test_parse_handles_code_fence():
    raw = "```json\n" + json.dumps(verdict().model_dump(mode="json"), ensure_ascii=False) + "\n```"
    assert parse_verdict(raw).scam_type is ScamType.family_impersonation


def test_parse_rejects_bad_type():
    assert parse_verdict('{"scam_type": "nonsense", "suspicion": 10}') is None
    assert parse_verdict("모르겠어요") is None


def gemini_client(handler):
    return httpx.AsyncClient(transport=httpx.MockTransport(handler))


def call_llm(client, text=FAMILY, **kw):
    args = dict(
        is_contact=False,
        is_international=False,
        weak_signals=[],
        final_urls=[],
        user_type=UserType.default,
        language="ko",
    )
    return asyncio.run(judge_with_llm(text, client=client, **(args | kw)))


def test_llm_request_and_response(monkeypatch):
    monkeypatch.setattr(settings, "llm_api_key", "AQ.test")
    monkeypatch.setattr(settings, "llm_model", "gemini-3.5-flash-lite")
    seen = {}

    def handler(req: httpx.Request):
        seen["url"] = str(req.url)
        seen["key"] = req.headers["x-goog-api-key"]
        seen["body"] = json.loads(req.content)
        text = json.dumps(verdict().model_dump(mode="json"), ensure_ascii=False)
        return httpx.Response(200, json={"candidates": [{"content": {"parts": [{"text": text}]}}]})

    v = call_llm(gemini_client(handler), language="vi", user_type=UserType.developmental)
    assert v.scam_type is ScamType.family_impersonation
    assert "gemini-3.5-flash-lite:generateContent" in seen["url"]
    assert seen["key"] == "AQ.test"
    assert seen["body"]["generationConfig"]["temperature"] == 0
    system = seen["body"]["systemInstruction"]["parts"][0]["text"]
    assert "Tiếng Việt" in system and "초등학교 저학년" in system
    assert "<message>" in seen["body"]["contents"][0]["parts"][0]["text"]


def test_llm_timeout_returns_none(monkeypatch):
    monkeypatch.setattr(settings, "llm_api_key", "AQ.test")
    monkeypatch.setattr(llm, "LLM_TIMEOUT_SECONDS", 0.05)

    async def slow(req):
        await asyncio.sleep(1)
        return httpx.Response(200)

    assert call_llm(gemini_client(slow)) is None


def test_llm_error_and_no_key(monkeypatch):
    monkeypatch.setattr(settings, "llm_api_key", "AQ.test")
    assert call_llm(gemini_client(lambda r: httpx.Response(402, json={}))) is None
    monkeypatch.setattr(settings, "llm_api_key", "")
    assert call_llm(gemini_client(lambda r: pytest.fail("키 없으면 호출 안 함"))) is None


# ---------- ④ 신호 종합 ----------


def test_strong_url_is_danger_without_llm():
    fast = FastSignals(urls=[url(safe_browsing_hit=True)])
    d = decide(fast, check_rules("택배 주소 확인"), None, from_contact=False)
    assert (d.level, d.scam_type, d.stage, d.use_llm_text) == (
        Level.danger,
        ScamType.delivery,
        2,
        False,
    )


def test_llm_failure_is_caution():
    d = decide(FastSignals(), check_rules(FAMILY), None, from_contact=False)
    assert (d.level, d.scam_type) == (Level.caution, ScamType.family_impersonation)
    d = decide(FastSignals(urls=[url()]), check_rules("확인해 주세요"), None, from_contact=False)
    assert d.level is Level.caution


def test_llm_failure_with_nothing_suspicious_is_safe():
    d = decide(FastSignals(), check_rules("오늘 저녁 7시에 봐"), None, from_contact=True)
    assert d.level is Level.safe


def test_scam_plus_money_is_danger():
    d = decide(FastSignals(), check_rules(FAMILY), verdict(), from_contact=False)
    assert d.level is Level.danger and d.use_llm_text


def test_scam_plus_new_domain_is_danger():
    v = verdict(scam_type="delivery", requests_money=False, requests_link_click=True)
    fast = FastSignals(urls=[url(domain_age_days=3)])
    assert decide(fast, check_rules("택배 확인"), v, from_contact=False).level is Level.danger


def test_scam_only_is_caution():
    v = verdict(scam_type="delivery", requests_money=False, requests_link_click=True)
    fast = FastSignals(urls=[url(domain_age_days=900)])
    assert decide(fast, check_rules("택배 확인"), v, from_contact=False).level is Level.caution


def test_guard_when_llm_says_none_for_unknown_sender():
    v = verdict(scam_type="none", suspicion=5, requests_money=False)
    assert decide(FastSignals(), check_rules(FAMILY), v, from_contact=False).level is Level.caution
    assert decide(FastSignals(), check_rules(FAMILY), v, from_contact=True).level is Level.safe


# ---------- /judge 실제 모드 ----------


@pytest.fixture
def real_mode(monkeypatch):
    monkeypatch.setattr(settings, "use_fake_judge", False)
    calls = []

    async def fake_notify(device_id, level, scam_type):
        calls.append((device_id, level, scam_type))

    monkeypatch.setattr("app.judge.router.notify_guardians", fake_notify)

    async def no_signals(text, client=None):
        return FastSignals()

    monkeypatch.setattr(cascade, "collect_fast_signals", no_signals)
    return calls


def post(text, **kw):
    body = {
        "device_id": "dev-1",
        "source": "sms",
        "sender": "010",
        "is_contact": False,
        "text": text,
    } | kw
    with TestClient(app) as c:
        return c.post("/judge", json=body)


def test_judge_danger_uses_llm_text_and_notifies(monkeypatch, real_mode):
    async def fake_llm(*a, **k):
        return verdict()

    monkeypatch.setattr(cascade, "judge_with_llm", fake_llm)
    r = post(FAMILY)
    assert r.status_code == 200
    data = r.json()
    assert (
        data["level"] == "danger"
        and data["title"] == "사기 문자 같아요"
        and data["stage_reached"] == 3
    )
    assert real_mode == [("dev-1", "danger", "family_impersonation")]


def test_judge_safe_uses_fixed_text_and_no_notify(monkeypatch, real_mode):
    async def fake_llm(*a, **k):
        return verdict(scam_type="none", suspicion=3, requests_money=False, title="안전합니다")

    monkeypatch.setattr(cascade, "judge_with_llm", fake_llm)
    data = post("오늘 저녁 7시에 봐", language="en").json()
    assert data["level"] == "safe"
    assert data["title"] == "No clear risk found"  # LLM의 '안전합니다' 대신 고정 문구
    assert real_mode == []


def test_judge_llm_down_is_caution(monkeypatch, real_mode):
    async def fake_llm(*a, **k):
        return None

    monkeypatch.setattr(cascade, "judge_with_llm", fake_llm)
    data = post(FAMILY).json()
    assert data["level"] == "caution" and data["scam_type"] == "family_impersonation"
    assert real_mode == []
