import httpx

from app.config import settings

TIMEOUT = 10.0


def check_safe_browsing() -> str:
    if not settings.safe_browsing_api_key:
        return "키 없음"
    url = f"https://safebrowsing.googleapis.com/v4/threatMatches:find?key={settings.safe_browsing_api_key}"
    body = {
        "client": {"clientId": "shield-hackathon", "clientVersion": "0.1"},
        "threatInfo": {
            "threatTypes": ["MALWARE", "SOCIAL_ENGINEERING"],
            "platformTypes": ["ANY_PLATFORM"],
            "threatEntryTypes": ["URL"],
            # 구글이 제공하는 테스트용 피싱 주소. 정상이면 일치 항목이 나옵니다.
            "threatEntries": [{"url": "http://testsafebrowsing.appspot.com/s/phishing.html"}],
        },
    }
    r = httpx.post(url, json=body, timeout=TIMEOUT)
    if r.status_code != 200:
        return f"실패 {r.status_code}: {r.text[:200]}"
    return (
        "정상 (테스트 피싱 주소 탐지됨)" if r.json().get("matches") else "응답은 왔지만 탐지 없음"
    )


def check_virustotal() -> str:
    if not settings.virustotal_api_key:
        return "키 없음"
    r = httpx.get(
        "https://www.virustotal.com/api/v3/domains/google.com",
        headers={"x-apikey": settings.virustotal_api_key},
        timeout=TIMEOUT,
    )
    return "정상" if r.status_code == 200 else f"실패 {r.status_code}: {r.text[:200]}"


PROMPT = "한 단어로 대답해: 연결 확인"
GEMINI_BASE = "https://generativelanguage.googleapis.com/v1beta/models"


def _check_gemini(key: str, model: str) -> str:
    r = httpx.post(
        f"{GEMINI_BASE}/{model}:generateContent",
        headers={"x-goog-api-key": key},
        json={"contents": [{"parts": [{"text": PROMPT}]}]},
        timeout=TIMEOUT,
    )
    if r.status_code != 200:
        return f"실패 {r.status_code}: {r.text[:300]}"
    text = r.json()["candidates"][0]["content"]["parts"][0]["text"]
    return f"정상 (Gemini {model}, 응답: {text.strip()})"


def _check_claude(key: str, model: str) -> str:
    r = httpx.post(
        "https://api.anthropic.com/v1/messages",
        headers={"x-api-key": key, "anthropic-version": "2023-06-01"},
        json={
            "model": model,
            "max_tokens": 20,
            "messages": [{"role": "user", "content": PROMPT}],
        },
        timeout=TIMEOUT,
    )
    if r.status_code != 200:
        return f"실패 {r.status_code}: {r.text[:300]}"
    return f"정상 (Claude {model}, 응답: {r.json()['content'][0]['text'].strip()})"


def check_llm() -> str:
    """키 앞부분으로 업체를 구분합니다. Claude는 sk-ant-로 시작하고, 나머지는 Gemini로 봅니다.

    Gemini 키는 예전 형식(AIza...)과 새 형식(AQ....)이 있습니다.
    """
    key, model = settings.llm_api_key.strip(), settings.llm_model.strip()
    if not key:
        return "키 없음"
    if key.startswith("sk-ant-"):
        return _check_claude(key, model)
    return _check_gemini(key, model)


def list_gemini_flash_models() -> list[str]:
    """이 키로 쓸 수 있는 Gemini flash 모델 이름 목록 (LLM_MODEL에 넣을 값)"""
    r = httpx.get(
        GEMINI_BASE,
        headers={"x-goog-api-key": settings.llm_api_key.strip()},
        params={"pageSize": 200},
        timeout=TIMEOUT,
    )
    if r.status_code != 200:
        return [f"목록 조회 실패 {r.status_code}: {r.text[:200]}"]
    names = [m["name"].removeprefix("models/") for m in r.json().get("models", [])]
    return [n for n in names if "flash" in n]


if __name__ == "__main__":
    for name, fn in [
        ("Safe Browsing", check_safe_browsing),
        ("VirusTotal", check_virustotal),
        ("LLM", check_llm),
    ]:
        try:
            result = fn()
        except httpx.HTTPError as e:
            result = f"네트워크 오류: {e}"
        print(f"{name:14} {result}")

    if settings.llm_api_key.strip() and not settings.llm_api_key.startswith("sk-ant-"):
        try:
            models = list_gemini_flash_models()
        except httpx.HTTPError as e:
            models = [f"네트워크 오류: {e}"]
        print(f"\n현재 LLM_MODEL: {settings.llm_model}")
        print("쓸 수 있는 Gemini flash 모델:")
        for m in models:
            print(f"  {m}")