"""외부 서비스를 실제로 부르지 않고, 가짜 응답(MockTransport)으로 ② 단계 흐름을 확인합니다."""

import asyncio
import json
from datetime import UTC, datetime, timedelta

import httpx
import pytest

from app.config import settings
from app.judge.cache import url_cache
from app.judge.signals import collect_fast_signals

EVIL = "http://evil-test.xyz/login"
VT_PREFIX = "https://www.virustotal.com/api/v3/urls/"


@pytest.fixture(autouse=True)
def setup(monkeypatch):
    url_cache.clear()
    monkeypatch.setattr(settings, "safe_browsing_api_key", "test-key")
    monkeypatch.setattr(settings, "virustotal_api_key", "test-key")


def make_client(sb_hits: set[str], vt_malicious: int | None, age_days: int, calls: list[str]):
    def handler(req: httpx.Request) -> httpx.Response:
        url = str(req.url)
        calls.append(f"{req.method} {req.url.host}")
        if req.url.host == "bit.ly":
            return httpx.Response(301, headers={"location": EVIL})
        if req.url.host == "safebrowsing.googleapis.com":
            entries = [e["url"] for e in json.loads(req.content)["threatInfo"]["threatEntries"]]
            matches = [{"threat": {"url": u}} for u in entries if u in sb_hits]
            return httpx.Response(200, json={"matches": matches} if matches else {})
        if url.startswith(VT_PREFIX):
            if vt_malicious is None:
                return httpx.Response(404)
            stats = {"malicious": vt_malicious}
            return httpx.Response(
                200, json={"data": {"attributes": {"last_analysis_stats": stats}}}
            )
        if req.url.host == "rdap.org":
            created = (datetime.now(UTC) - timedelta(days=age_days)).strftime("%Y-%m-%dT%H:%M:%SZ")
            return httpx.Response(
                200, json={"events": [{"eventAction": "registration", "eventDate": created}]}
            )
        raise AssertionError(f"요청하면 안 되는 주소: {url}")

    return httpx.AsyncClient(transport=httpx.MockTransport(handler))


def run(text, client):
    return asyncio.run(collect_fast_signals(text, client))


def test_no_url_makes_no_requests():
    calls: list[str] = []
    s = run("엄마 나 폰 액정 깨져서 이 계좌로 보내줘", make_client(set(), None, 1000, calls))
    assert not s.has_url and not s.strong and calls == []


def test_short_url_is_expanded_and_safe_browsing_hit_is_strong():
    calls: list[str] = []
    s = run("택배 확인 bit.ly/abc", make_client({EVIL}, None, 3, calls))
    u = s.urls[0]
    assert u.final == EVIL and u.expanded and u.domain == "evil-test.xyz"
    assert s.strong
    # 최종 주소(evil-test.xyz)에는 직접 요청하지 않고, Safe Browsing에 걸렸으니 VirusTotal도 생략
    assert not any("evil-test.xyz" in c for c in calls)
    assert not any("virustotal" in c for c in calls)


def test_new_domain_is_weak_not_strong():
    s = run("확인 https://new-shop.xyz/a", make_client(set(), 0, 5, []))
    assert not s.strong
    assert s.urls[0].new_domain
    assert any("5일" in w for w in s.weak)


def test_virustotal_needs_two_engines():
    assert not run("https://a.xyz", make_client(set(), 1, 999, [])).strong
    url_cache.clear()
    assert run("https://a.xyz", make_client(set(), 2, 999, [])).strong


def test_missing_keys_means_unknown_not_safe(monkeypatch):
    monkeypatch.setattr(settings, "safe_browsing_api_key", "")
    monkeypatch.setattr(settings, "virustotal_api_key", "")
    s = run("https://a.xyz", make_client(set(), None, 999, []))
    assert s.urls[0].safe_browsing_hit is None
    assert s.urls[0].virustotal_malicious is None
    assert not s.strong


def test_cache_avoids_second_lookup():
    calls: list[str] = []
    client = make_client(set(), 0, 999, calls)
    run("https://a.xyz", client)
    first = len(calls)
    run("https://a.xyz", make_client(set(), 0, 999, calls))
    assert len(calls) == first
