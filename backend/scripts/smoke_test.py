"""서버가 켜진 상태에서 /health와 /judge를 호출해 봅니다.

실행: uv run python -m scripts.smoke_test            (로컬)
      uv run python -m scripts.smoke_test https://xxxx.ngrok-free.app   (통합 서버)
"""

import json
import sys

import httpx

BASE = sys.argv[1].rstrip("/") if len(sys.argv) > 1 else "http://127.0.0.1:8000"

SAMPLES = [
    ("가족 사칭", "엄마 나 폰 액정 깨져서 임시폰이야 이 계좌로 50만원만 보내줘", False),
    ("택배 링크", "[CJ대한통운] 주소 불일치로 보관중 확인 https://bit.ly/abc123", False),
    ("정상 문자", "오늘 저녁 7시에 집 앞에서 봐", True),
]

if __name__ == "__main__":
    print("health:", httpx.get(f"{BASE}/health", timeout=10).json())
    for name, text, is_contact in SAMPLES:
        body = {
            "device_id": "test-device-001",
            "source": "sms",
            "sender": "010-0000-0000",
            "is_contact": is_contact,
            "text": text,
        }
        r = httpx.post(f"{BASE}/judge", json=body, timeout=15)
        data = r.json()
        print(f"\n[{name}] {r.status_code} → {data.get('level')} / {data.get('title')}")
        if r.status_code != 200:
            print(json.dumps(data, ensure_ascii=False, indent=2))
