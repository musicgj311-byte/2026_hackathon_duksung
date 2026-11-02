"""캐스케이드 ② 단계: 빠른 신호를 동시에 모읍니다.

순서
1. 문자에서 링크 찾기 (최대 3개)
2. 단축 URL 먼저 풀기 (여러 링크를 동시에)
3. 최종 주소로 Safe Browsing 조회 + 도메인 생성일 확인 (동시에)
4. Safe Browsing에 안 걸린 주소만 VirusTotal 조회

강한 신호(바로 위험 확정)는 악성 URL 목록에 걸린 경우뿐입니다.
새 도메인 같은 약한 신호는 ③ LLM 단계에 넘깁니다.
"""

import asyncio
import time
from dataclasses import asdict, dataclass, field

import httpx

from app.judge.url_check import check_safe_browsing, check_virustotal, domain_age_days
from app.judge.urls import expand, extract_urls, host_of, registrable_domain

NEW_DOMAIN_DAYS = 30  # 이 일수 이내에 만든 도메인은 약한 신호
VT_MALICIOUS_MIN = 2  # 백신 엔진 1개만 악성이라 하는 오탐이 흔해서 2개 이상부터 인정


@dataclass
class UrlSignal:
    original: str
    final: str
    expanded: bool
    domain: str
    safe_browsing_hit: bool | None = None
    virustotal_malicious: int | None = None
    domain_age_days: int | None = None
    note: str = ""

    @property
    def strong_hit(self) -> bool:
        return self.safe_browsing_hit is True or (
            self.virustotal_malicious is not None and self.virustotal_malicious >= VT_MALICIOUS_MIN
        )

    @property
    def new_domain(self) -> bool:
        return self.domain_age_days is not None and self.domain_age_days <= NEW_DOMAIN_DAYS


@dataclass
class FastSignals:
    urls: list[UrlSignal] = field(default_factory=list)
    elapsed_ms: int = 0

    @property
    def has_url(self) -> bool:
        return bool(self.urls)

    @property
    def strong(self) -> bool:
        """True면 LLM을 건너뛰고 바로 위험으로 확정"""
        return any(u.strong_hit for u in self.urls)

    @property
    def weak(self) -> list[str]:
        """③ LLM에 넘길 약한 신호 설명"""
        reasons: list[str] = []
        for u in self.urls:
            if u.expanded:
                reasons.append(f"단축 URL이 {u.domain}(으)로 연결됨")
            if u.new_domain:
                reasons.append(f"{u.domain}은(는) 만든 지 {u.domain_age_days}일 된 도메인")
            if u.virustotal_malicious == 1:
                reasons.append(f"{u.domain}을(를) 백신 엔진 1곳이 악성으로 판정")
        return reasons

    def to_dict(self) -> dict:
        return {
            "has_url": self.has_url,
            "strong": self.strong,
            "weak": self.weak,
            "elapsed_ms": self.elapsed_ms,
            "urls": [asdict(u) | {"strong_hit": u.strong_hit} for u in self.urls],
        }


async def collect_fast_signals(text: str, client: httpx.AsyncClient | None = None) -> FastSignals:
    started = time.perf_counter()
    urls = extract_urls(text)
    if not urls:
        return FastSignals(elapsed_ms=int((time.perf_counter() - started) * 1000))

    own_client = client is None
    client = client or httpx.AsyncClient()
    try:
        expanded = await asyncio.gather(*(expand(client, u) for u in urls))
        signals = [
            UrlSignal(
                original=e.original,
                final=e.final,
                expanded=e.expanded,
                domain=registrable_domain(host_of(e.final)),
                note=e.note,
            )
            for e in expanded
        ]
        finals = [s.final for s in signals]
        domains = list(dict.fromkeys(s.domain for s in signals))

        sb, *ages = await asyncio.gather(
            check_safe_browsing(client, finals),
            *(domain_age_days(client, d) for d in domains),
        )
        age_by_domain = dict(zip(domains, ages, strict=True))
        for s in signals:
            s.safe_browsing_hit = sb.get(s.final)
            s.domain_age_days = age_by_domain.get(s.domain)

        # VirusTotal은 한도가 빡빡해서 Safe Browsing에 안 걸린 주소만, 하나씩 조회
        for s in signals:
            if s.safe_browsing_hit is not True:
                s.virustotal_malicious = await check_virustotal(client, s.final)
    finally:
        if own_client:
            await client.aclose()

    return FastSignals(urls=signals, elapsed_ms=int((time.perf_counter() - started) * 1000))
