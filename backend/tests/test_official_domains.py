# ruff: noqa: E501
"""공식 도메인 예외: 모르는 번호 + 공식 링크는 최대 '주의', 요구·악성 URL은 그대로 위험."""

from app.judge.cascade import decide
from app.judge.llm import LlmVerdict
from app.judge.official_domains import is_official_host
from app.judge.rules import check_rules
from app.judge.signals import FastSignals, UrlSignal
from app.schemas import Level


def link(final: str, **kw) -> UrlSignal:
    return UrlSignal(original=final, final=final, expanded=False, domain="", **kw)


def scam_like(**kw) -> LlmVerdict:
    base = dict(
        scam_type="delivery", suspicion=70, signals=["배송 안내"], title="t",
        explanation="e", action="a", easy_sentence="s",
    )  # fmt: skip
    return LlmVerdict.model_validate(base | kw)


def level(fast, text="주문하신 상품이 발송되었습니다", verdict=None, contact=False):
    return decide(fast, check_rules(text), verdict or scam_like(), from_contact=contact).level


def test_exact_match_only():
    assert is_official_host("www.coupang.com")
    assert is_official_host("WWW.Amazon.com.")
    assert is_official_host("www.gov.kr")
    assert not is_official_host("amazon-delivery.com")
    assert not is_official_host("amazon.com.xyz.top")
    assert not is_official_host("coupang.com-track.info")
    assert not is_official_host("blog.naver.com")


def test_official_link_from_unknown_is_caution():
    fast = FastSignals(urls=[link("https://www.amazon.com/track/114")])
    assert level(fast) is Level.caution


def test_fake_lookalike_link_stays_danger():
    fast = FastSignals(urls=[link("https://amazon-delivery.xyz/track")])
    assert level(fast) is Level.danger


def test_one_unofficial_link_among_official_is_danger():
    fast = FastSignals(urls=[link("https://www.coupang.com"), link("http://cpang-help.top")])
    assert level(fast) is Level.danger


def test_official_link_with_money_demand_stays_danger():
    fast = FastSignals(urls=[link("https://www.coupang.com")])
    assert level(fast, verdict=scam_like(requests_money=True)) is Level.danger


def test_official_link_on_blocklist_stays_danger():
    fast = FastSignals(urls=[link("https://www.coupang.com/x", safe_browsing_hit=True)])
    assert level(fast) is Level.danger


def test_official_link_never_becomes_safe_when_scam_like():
    fast = FastSignals(urls=[link("https://www.kbstar.com")])
    assert level(fast) is not Level.safe


def test_saved_contact_link_is_caution():
    # 청첩장처럼 저장된 연락처가 보낸 링크는 기존 규칙대로 최대 '주의'
    fast = FastSignals(urls=[link("https://m-card.example/abc")])
    text = "저 결혼합니다. 모바일 청첩장 보내드려요"
    assert (
        level(fast, text, scam_like(scam_type="obituary_invitation"), contact=True) is Level.caution
    )
