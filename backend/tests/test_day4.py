# ruff: noqa: E501
"""Day 4: 사용자 언어 문구 검사, easy_sentence, 채점 스크립트의 집계를 확인합니다."""

from pathlib import Path

from app.judge.cascade import Detail
from app.judge.fallback_messages import Message, get_message
from app.judge.localize import language_matches, polish
from app.schemas import JudgeResponse, Level, ScamType, UserType
from scripts.evaluate import Graded, Row, load_rows, summarize

FALLBACK_KO = get_message(Level.danger, ScamType.family_impersonation, "ko")
FALLBACK_VI = get_message(Level.danger, ScamType.family_impersonation, "vi")


def msg(
    title="사기 문자 같아요",
    explanation="가족인 척 돈을 요구해요.",
    action="원래 번호로 전화하세요.",
    easy="돈 보내지 마세요.",
):
    return Message(title, explanation, action, easy)


# ---------- 언어 확인 ----------


def test_language_matches():
    assert language_matches("가족인 척 돈을 요구해요", "ko")
    assert not language_matches("This is a scam", "ko")
    assert language_matches("Tin nhắn này có vẻ lừa đảo", "vi")
    assert not language_matches("가족인 척 돈을 요구해요", "vi")
    assert language_matches("这可能是诈骗短信", "zh")
    assert not language_matches("가족인 척 돈을 요구해요", "zh-CN")


def test_wrong_language_falls_back_to_fixed():
    out = polish(msg(), FALLBACK_VI, "vi", UserType.default)
    assert out is FALLBACK_VI


def test_unknown_language_fixed_message_is_english():
    m = get_message(Level.caution, ScamType.delivery, "th")
    assert m.title == "Be careful"


# ---------- 모드별 다듬기 ----------


def test_visual_mode_strips_emoji_and_symbols():
    out = polish(
        msg(title="⚠️ 사기 문자 같아요 ‼", action="➡ [원래 번호]로 전화하세요"),
        FALLBACK_KO,
        "ko",
        UserType.visual,
    )
    assert out.title == "사기 문자 같아요"
    assert out.action == "원래 번호로 전화하세요"


def test_default_mode_keeps_text():
    out = polish(msg(title="⚠️ 사기 문자 같아요"), FALLBACK_KO, "ko", UserType.default)
    assert out.title == "⚠️ 사기 문자 같아요"


def test_long_easy_sentence_replaced():
    long_easy = (
        "이 문자는 가족을 흉내 낸 사기일 수 있으니 절대로 돈을 보내지 마시고 먼저 전화하세요"
    )
    out = polish(msg(easy=long_easy), FALLBACK_KO, "ko", UserType.developmental)
    assert out.easy_sentence == FALLBACK_KO.easy_sentence
    assert out.title == "사기 문자 같아요"  # 나머지는 LLM 문구 유지


def test_empty_fields_filled_from_fixed():
    out = polish(msg(action="  ", easy=""), FALLBACK_KO, "ko", UserType.default)
    assert out.action == FALLBACK_KO.action and out.easy_sentence == FALLBACK_KO.easy_sentence


# ---------- 채점 ----------


def graded(expected: Level, got: Level, ms=1000, llm_ok=True) -> Graded:
    row = Row("1", "테스트", "문자", False, False, expected)
    resp = JudgeResponse(
        level=got, scam_type=ScamType.other, has_url=False, title="t", explanation="e",
        action="a", easy_sentence="s", language="ko", stage_reached=3, elapsed_ms=ms,
    )  # fmt: skip
    return Graded(
        row, resp, Detail(llm_called=True, llm_ok=llm_ok, text_source="llm", rule_types=[])
    )


def test_outcomes_and_summary():
    results = [
        graded(Level.danger, Level.danger, 1000),
        graded(Level.danger, Level.safe, 2000),  # 놓친 사기
        graded(Level.danger, Level.caution, 3000),  # 약하게
        graded(Level.safe, Level.danger, 1000),  # 잘못 잡은 정상
        graded(Level.safe, Level.caution, 1000, llm_ok=False),  # 과한 주의
        graded(Level.safe, Level.safe, 1000),
    ]
    assert [g.outcome for g in results] == [
        "정답",
        "놓친 사기",
        "약하게 판정",
        "잘못 잡은 정상",
        "과한 주의",
        "정답",
    ]
    s = summarize(results)
    assert (s["exact"], s["missed"], s["false_alarm"], s["over_caution"], s["weaker"]) == (
        2,
        1,
        1,
        1,
        1,
    )
    assert (s["scam_warned"], s["scam_total"]) == (2, 3)
    assert (s["normal_quiet"], s["normal_total"]) == (1, 3)
    assert s["avg_ms"] == 1500 and s["max_ms"] == 3000 and s["llm_failed"] == 1


def test_sample_csv_loads():
    rows = load_rows(Path(__file__).parent.parent / "eval" / "messages.csv")
    assert len(rows) >= 20
    assert {r.expected for r in rows} == {Level.danger, Level.caution, Level.safe}
    assert any(r.is_contact for r in rows)


def test_payment_rule_with_amount_between():
    from app.judge.rules import check_rules

    for text in [
        "[Web발신] 쿠팡 결제 879000원 승인. 본인 결제가 아니면 즉시 신고 1600-0000",
        "[해외승인] 980000원 결제 완료. 본인 아닐 시 고객센터로 연락",
    ]:
        assert check_rules(text).top_type is ScamType.payment_alert
