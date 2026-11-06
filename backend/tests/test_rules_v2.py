# ruff: noqa: E501
"""채점에서 드러난 판정 규칙 보완"""

from app.judge.cascade import decide
from app.judge.fallback_messages import get_message
from app.judge.llm import LlmVerdict, parse_verdict
from app.judge.rules import check_rules
from app.judge.signals import FastSignals, UrlSignal
from app.schemas import Level, ScamType


def verdict(**kw) -> LlmVerdict:
    base = dict(
        scam_type="other", suspicion=90, title="t", explanation="e", action="a", easy_sentence="s"
    )
    return LlmVerdict.model_validate(base | kw)


def link(**kw) -> UrlSignal:
    return UrlSignal(
        **(
            {
                "original": "http://x.example/a",
                "final": "http://x.example/a",
                "expanded": False,
                "domain": "x.example",
            }
            | kw
        )
    )


def test_call_inducement_is_danger():
    # S01: 카드 개통 + 문자 속 번호로 신고 유도
    v = verdict(scam_type="payment_alert", requests_call=True)
    d = decide(
        FastSignals(),
        check_rules("카드 개통완료 개통내용이 아니시면 신고 접수바랍니다 070-1234-5678"),
        v,
        from_contact=False,
    )
    assert d.level is Level.danger


def test_requests_call_parsed_and_defaults_false():
    assert parse_verdict(
        '{"scam_type":"other","suspicion":80,"requests_call":true,"title":"t","explanation":"e","action":"a","easy_sentence":"s"}'
    ).requests_call
    assert not verdict().requests_call


def test_unknown_sender_scam_with_link_is_danger():
    # S07·S13·S14: 사칭 + 링크 (목록에 없고 생성일도 모름)
    v = verdict(scam_type="obituary_invitation", requests_link_click=True)
    assert (
        decide(
            FastSignals(urls=[link()]), check_rules("[부고 알림] 별세"), v, from_contact=False
        ).level
        is Level.danger
    )


def test_saved_contact_capped_at_caution():
    # S15·S18·N59·N79: 저장된 연락처는 최대 주의
    v = verdict(scam_type="family_impersonation", requests_money=True)
    assert (
        decide(FastSignals(), check_rules("엄마 계좌로 300만원 보내줘"), v, from_contact=True).level
        is Level.caution
    )
    v2 = verdict(scam_type="obituary_invitation", requests_link_click=True)
    assert (
        decide(FastSignals(urls=[link()]), check_rules("청첩장"), v2, from_contact=True).level
        is Level.caution
    )


def test_saved_contact_with_malicious_link_still_danger():
    fast = FastSignals(urls=[link(safe_browsing_hit=True)])
    assert decide(fast, check_rules("청첩장"), None, from_contact=True).level is Level.danger


def test_probe_message_without_demand_stays_caution():
    # S16: 떠보는 첫 문자 (요구 없음, 링크 없음)
    v = verdict(scam_type="family_impersonation")
    assert (
        decide(
            FastSignals(),
            check_rules("엄마 지금 뭐해 ? 폰 폰파손금 접수 넣오본적 있어 ?"),
            v,
            from_contact=False,
        ).level
        is Level.caution
    )


def test_fixed_messages_never_say_call_the_sender():
    for level in (Level.danger, Level.caution):
        for t in ScamType:
            m = get_message(level, t, "ko")
            assert "보낸 곳에 직접 전화" not in m.action and "보낸 사람에게" not in m.action
    assert "번호로 전화하지" in get_message(Level.danger, ScamType.payment_alert, "ko").action
