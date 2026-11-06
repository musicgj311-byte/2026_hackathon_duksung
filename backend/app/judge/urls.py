"""문자에서 링크를 찾고, 단축 URL을 풀어 최종 주소를 얻습니다.

안전 원칙
- 서버가 직접 요청을 보내는 곳은 아래 SHORTENERS 목록의 단축 URL 서비스뿐입니다.
- 단축 URL이 다른 주소로 넘겨주면 그 주소는 요청하지 않고 주소만 기록합니다.
  사기 링크에는 받은 사람마다 다른 값이 붙어 있는 경우가 많아서, 서버가 그 주소를 열면
  "이 번호는 살아 있다"는 신호를 사기범에게 주게 됩니다.
- 페이지 본문은 받지 않고 리다이렉트 응답의 Location 헤더만 봅니다.
"""

import re
from dataclasses import dataclass
from urllib.parse import urljoin, urlsplit

import httpx

from app.judge.cache import is_missing, url_cache

MAX_URLS = 3  # 한 문자에서 검사할 최대 링크 수
MAX_HOPS = 5
TIMEOUT = httpx.Timeout(3.0)
USER_AGENT = (
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"
)

SHORTENERS = {
    "bit.ly", "han.gl", "me2.do", "url.kr", "vo.la", "t.ly", "tinyurl.com", "goo.gl",
    "is.gd", "buly.kr", "c11.kr", "naver.me", "t.co", "ow.ly", "rebrand.ly", "cutt.ly",
    "shorturl.at", "s.id", "rb.gy", "tiny.cc", "lrl.kr", "zrr.kr", "abit.ly", "bitly.ws",
}  # fmt: skip

# 주소 앞에 http가 없는 문자도 많아서, 흔한 도메인 끝말이면 링크로 봅니다.
_TLDS = (
    "com|net|org|kr|me|ly|io|co|cc|xyz|top|info|shop|site|link|app|gl|do|so|be|to|ai"
    "|click|online|store|vip|club|live|icu|cn|ru|tk|ml|ga|cf|gq|ws|la|id|gy|at"
    "|biz|pro|life|fun|work|buzz|sbs|cfd|cyou|rest|monster|asia|world|today|news|page"
    "|example|test"  # 시연·테스트용 예약 도메인 (.example, .test)
)
_URL_RE = re.compile(
    r"https?://[^\s<>\"'가-힣]+"
    rf"|(?<![@\w.])(?:www\.)?[a-z0-9-]+(?:\.[a-z0-9-]+)*\.(?:{_TLDS})\b(?:/[^\s<>\"'가-힣]*)?",
    re.IGNORECASE,
)
_TRAILING = ".,;:!?)]}>'\"…"

# .co.kr처럼 두 단계로 된 국가 도메인
_SECOND_LEVEL = {
    "co.kr", "or.kr", "go.kr", "ne.kr", "re.kr", "pe.kr", "ac.kr", "ms.kr", "hs.kr",
    "es.kr", "sc.kr", "kg.kr", "mil.kr", "co.jp", "co.uk", "com.cn", "com.vn",
}  # fmt: skip


# 링크 검사를 피하려고 'h t t p s : / /', 'hxxp', 'abc[.]com'처럼 쓴 주소를
# 원래 모양으로 되돌립니다.
_ZERO_WIDTH = re.compile("[\u200b-\u200f\u2060\ufeff]")
_SPACED_SCHEME = re.compile(r"h\s*[tx]\s*[tx]\s*p\s*(s?)\s*:\s*/\s*/\s*", re.IGNORECASE)
_DOT_TRICK = re.compile(r"\s*(?:\[\.\]|\(\.\)|\[dot\]|\(dot\))\s*", re.IGNORECASE)


def deobfuscate_links(text: str) -> str:
    text = _ZERO_WIDTH.sub("", text)
    text = _SPACED_SCHEME.sub(lambda m: f"http{m.group(1).lower()}://", text)
    return _DOT_TRICK.sub(".", text)


def extract_urls(text: str) -> list[str]:
    """문자에서 링크를 찾아 http(s):// 형태로 돌려줍니다. 중복은 제거합니다."""
    found: list[str] = []
    for m in _URL_RE.finditer(deobfuscate_links(text)):
        url = m.group(0).rstrip(_TRAILING)
        if not url.lower().startswith(("http://", "https://")):
            url = "http://" + url
        if url not in found:
            found.append(url)
        if len(found) >= MAX_URLS:
            break
    return found


def host_of(url: str) -> str:
    return (urlsplit(url).hostname or "").lower().rstrip(".")


def registrable_domain(host: str) -> str:
    """sub.example.co.kr → example.co.kr, a.b.example.com → example.com"""
    parts = host.split(".")
    if len(parts) >= 3 and ".".join(parts[-2:]) in _SECOND_LEVEL:
        return ".".join(parts[-3:])
    return ".".join(parts[-2:])


def is_shortener(url: str) -> bool:
    return host_of(url) in SHORTENERS


@dataclass
class Expanded:
    original: str
    final: str
    expanded: bool  # 단축 URL을 풀어서 다른 주소를 얻었는지
    note: str = ""  # 풀지 못한 이유 등


async def _next_location(client: httpx.AsyncClient, url: str) -> str | None:
    """리다이렉트 주소만 받아옵니다. 본문은 읽지 않습니다."""
    headers = {"User-Agent": USER_AGENT}
    resp = await client.head(url, headers=headers, follow_redirects=False, timeout=TIMEOUT)
    if resp.status_code in (400, 403, 405, 501):
        # HEAD를 막는 서비스는 GET으로 다시 시도하되 본문은 받지 않음
        async with client.stream(
            "GET", url, headers=headers, follow_redirects=False, timeout=TIMEOUT
        ) as r:
            resp = r
    if resp.is_redirect and "location" in resp.headers:
        return urljoin(url, resp.headers["location"])
    return None


async def expand(client: httpx.AsyncClient, url: str) -> Expanded:
    """단축 URL이면 최종 주소까지 따라가고, 아니면 그대로 돌려줍니다."""
    if not is_shortener(url):
        return Expanded(url, url, expanded=False)

    cached = url_cache.get(f"expand:{url}")
    if not is_missing(cached):
        return cached

    current = url
    note = ""
    try:
        for _ in range(MAX_HOPS):
            nxt = await _next_location(client, current)
            if nxt is None:
                note = "리다이렉트 없음"
                break
            current = nxt
            if not is_shortener(current):
                break  # 단축 서비스를 벗어나면 그 주소는 요청하지 않음
        else:
            note = "리다이렉트가 너무 많음"
    except httpx.HTTPError as e:
        note = f"요청 실패: {type(e).__name__}"

    result = Expanded(url, current, expanded=current != url, note=note)
    if result.expanded:
        url_cache.set(f"expand:{url}", result)
    return result
