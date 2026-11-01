"""최종 주소의 평판을 외부 서비스로 확인합니다.

- Safe Browsing: 구글의 악성·피싱 사이트 목록 (여러 주소를 한 번에 조회)
- VirusTotal: 여러 백신 엔진의 판정 (무료 분당 4회라서 Safe Browsing에 안 걸린 것만 조회)
- 도메인 생성일: RDAP(whois의 새 표준)로 조회. .kr 등 일부 도메인은 조회되지 않을 수 있음

모든 함수는 실패해도 예외를 던지지 않고 None을 돌려줍니다.
None은 "확인 못 함"이라는 뜻이고, 판정을 멈추지 않습니다.
"""

import base64
from datetime import UTC, datetime

import httpx

from app.config import settings
from app.judge.cache import is_missing, url_cache

TIMEOUT = httpx.Timeout(3.0)
SAFE_BROWSING_URL = "https://safebrowsing.googleapis.com/v4/threatMatches:find"
VIRUSTOTAL_URL = "https://www.virustotal.com/api/v3/urls/"
RDAP_URL = "https://rdap.org/domain/"


async def check_safe_browsing(client: httpx.AsyncClient, urls: list[str]) -> dict[str, bool | None]:
    """주소별로 Safe Browsing 목록에 있으면 True, 없으면 False, 확인 못 하면 None"""
    result: dict[str, bool | None] = {}
    todo: list[str] = []
    for u in urls:
        cached = url_cache.get(f"sb:{u}")
        if is_missing(cached):
            todo.append(u)
        else:
            result[u] = cached
    if not todo:
        return result
    if not settings.safe_browsing_api_key:
        return result | dict.fromkeys(todo)

    body = {
        "client": {"clientId": "shield-hackathon", "clientVersion": "0.1"},
        "threatInfo": {
            "threatTypes": [
                "MALWARE",
                "SOCIAL_ENGINEERING",
                "UNWANTED_SOFTWARE",
                "POTENTIALLY_HARMFUL_APPLICATION",
            ],
            "platformTypes": ["ANY_PLATFORM"],
            "threatEntryTypes": ["URL"],
            "threatEntries": [{"url": u} for u in todo],
        },
    }
    try:
        r = await client.post(
            SAFE_BROWSING_URL,
            params={"key": settings.safe_browsing_api_key},
            json=body,
            timeout=TIMEOUT,
        )
        r.raise_for_status()
        hits = {m["threat"]["url"] for m in r.json().get("matches", [])}
    except (httpx.HTTPError, ValueError, KeyError):
        return result | dict.fromkeys(todo)

    for u in todo:
        result[u] = u in hits
        url_cache.set(f"sb:{u}", result[u])
    return result


async def check_virustotal(client: httpx.AsyncClient, url: str) -> int | None:
    """악성으로 판정한 백신 엔진 수. 분석 기록이 없거나 확인 못 하면 None"""
    cached = url_cache.get(f"vt:{url}")
    if not is_missing(cached):
        return cached
    if not settings.virustotal_api_key:
        return None

    url_id = base64.urlsafe_b64encode(url.encode()).decode().strip("=")
    try:
        r = await client.get(
            VIRUSTOTAL_URL + url_id,
            headers={"x-apikey": settings.virustotal_api_key},
            timeout=TIMEOUT,
        )
        if r.status_code == 404:
            url_cache.set(f"vt:{url}", None)  # 분석 기록 없음
            return None
        r.raise_for_status()
        malicious = int(r.json()["data"]["attributes"]["last_analysis_stats"]["malicious"])
    except (httpx.HTTPError, ValueError, KeyError):
        return None  # 한도 초과(429) 등은 캐시하지 않고 다음에 다시 시도

    url_cache.set(f"vt:{url}", malicious)
    return malicious


async def domain_age_days(client: httpx.AsyncClient, domain: str) -> int | None:
    """도메인이 만들어진 지 며칠 됐는지. 확인 못 하면 None"""
    cached = url_cache.get(f"age:{domain}")
    if not is_missing(cached):
        return cached
    try:
        r = await client.get(RDAP_URL + domain, follow_redirects=True, timeout=TIMEOUT)
        r.raise_for_status()
        events = r.json().get("events", [])
        created = next(e["eventDate"] for e in events if e.get("eventAction") == "registration")
        created_at = datetime.fromisoformat(created.replace("Z", "+00:00"))
    except (httpx.HTTPError, ValueError, KeyError, StopIteration):
        return None

    age = max(0, (datetime.now(UTC) - created_at).days)
    url_cache.set(f"age:{domain}", age)
    return age
