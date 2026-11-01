"""Day 1: .env의 API 키가 실제로 동작하는지 확인합니다.

실행: uv run python -m scripts.check_keys
"""

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



def check_llm() -> str:
    if not settings.llm_api_key:
        return "키 없음"

    # Google Gemini API 호출
    model_name = settings.llm_model.removeprefix("models/")

    r = httpx.post(
        f"https://generativelanguage.googleapis.com/v1beta/models/{model_name}:generateContent",
        headers={
            "x-goog-api-key": settings.llm_api_key,
            "content-type": "application/json",
        },
        json={
            "contents": [
                {
                    "parts": [
                        {"text": "한 단어로 대답해: 연결 확인"}
                    ]
                }
            ],
            "generationConfig": {
                "maxOutputTokens": 20,
            },
        },
        timeout=TIMEOUT,
    )

    if r.status_code != 200:
        return f"실패 {r.status_code}: {r.text[:200]}"

    data = r.json()

    # Gemini 응답에서 텍스트 추출
    candidates = data.get("candidates", [])
    if not candidates:
        return f"응답 없음: {str(data)[:200]}"

    parts = candidates[0].get("content", {}).get("parts", [])
    answer = "".join(
        part.get("text", "") for part in parts
    ).strip()

    return f"정상 (응답: {answer})"


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
