
import asyncio
from types import SimpleNamespace
from unittest.mock import AsyncMock

import httpx

from app.main import app
from app.judge import cascade
from app.judge import router as judge_router
from app.judge.signals import FastSignals, UrlSignal


# 1. URL 검사 결과 모의 처리
async def fake_collect_fast_signals(text: str, client=None):
    # 악성 URL 테스트 문자가 들어오면 악성 URL로 판정
    if "malicious.example" in text:
        return FastSignals(
            urls=[
                UrlSignal(
                    original="https://malicious.example/path",
                    final="https://malicious.example/path",
                    expanded=False,
                    domain="malicious.example",
                    safe_browsing_hit=True,
                )
            ]
        )

    # URL이 없는 문자는 빈 신호 반환
    return FastSignals()


# 2. LLM 실패 상황 모의 처리
async def fake_judge_with_llm(*args, **kwargs):
    return None


async def main():
    # 기존 설정과 함수 보관
    original_settings = judge_router.settings
    original_collect = cascade.collect_fast_signals
    original_llm = cascade.judge_with_llm
    original_notify = judge_router.notify_guardians

    # 외부 FCM 전송 방지
    notify_mock = AsyncMock()

    try:
        # 3. 실제 캐스케이드 실행 경로 선택
        judge_router.settings = SimpleNamespace(use_fake_judge=False)

        # 4. 외부 API 의존성 모의 처리
        cascade.collect_fast_signals = fake_collect_fast_signals
        cascade.judge_with_llm = fake_judge_with_llm
        judge_router.notify_guardians = notify_mock

        transport = httpx.ASGITransport(app=app)

        async with httpx.AsyncClient(
            transport=transport,
            base_url="http://test",
        ) as client:

            test_cases = [
                {
                    "name": "위험 문자",
                    "text": "택배 배송 주소 확인 https://malicious.example/path",
                    "expected_level": "danger",
                    "expected_type": "delivery",
                },
                {
                    "name": "주의 문자",
                    "text": "택배 배송 주소가 잘못되었습니다. 확인해 주세요.",
                    "expected_level": "caution",
                    "expected_type": "delivery",
                },
                {
                    "name": "안전 문자",
                    "text": "오늘은 날씨가 좋네요.",
                    "expected_level": "safe",
                    "expected_type": "none",
                },
            ]

            for case in test_cases:
                payload = {
                    "device_id": "integration-protected-001",
                    "source": "sms",
                    "sender": "01012345678",
                    "is_contact": False,
                    "is_international": False,
                    "text": case["text"],
                    "user_type": "default",
                    "language": "ko",
                }

                response = await client.post("/judge", json=payload)

                assert response.status_code == 200, (
                    f'{case["name"]}: HTTP {response.status_code}, '
                    f'{response.text}'
                )

                result = response.json()

                assert result["level"] == case["expected_level"]
                assert result["scam_type"] == case["expected_type"]

                for field in [
                    "title",
                    "explanation",
                    "action",
                    "easy_sentence",
                ]:
                    assert isinstance(result[field], str)
                    assert result[field].strip() != ""

                print(f'\n[{case["name"]}]')
                print("위험도:", result["level"])
                print("사기 유형:", result["scam_type"])
                print("제목:", result["title"])
                print("설명:", result["explanation"])
                print("대응:", result["action"])
                print("쉬운 문장:", result["easy_sentence"])
                print("판정 단계:", result["stage_reached"])

        # 5. 위험 문자에서만 보호자 알림 작업 실행 확인
        notify_mock.assert_awaited_once_with(
            "integration-protected-001",
            "danger",
            "delivery",
        )

        print("\n보호자 알림 작업 등록 및 실행 확인 완료")
        print("모든 API 통합 테스트 통과!")

    finally:
        # 6. 원래 함수와 설정 복구
        judge_router.settings = original_settings
        cascade.collect_fast_signals = original_collect
        cascade.judge_with_llm = original_llm
        judge_router.notify_guardians = original_notify


if __name__ == "__main__":
    asyncio.run(main())
