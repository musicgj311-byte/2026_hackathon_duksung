# ruff: noqa: E501
"""글자 깨뜨리기와 http 없는 링크 대응 (개발용 추가 세트 D19·D20·D22에서 발견)."""

from app.judge.cascade import decide
from app.judge.llm import LlmVerdict
from app.judge.rules import check_rules, normalize_text
from app.judge.signals import FastSignals
from app.judge.urls import extract_urls
from app.schemas import Level, ScamType


def test_links_without_scheme_and_test_domains():
    assert extract_urls("확인: efine-notice.example") == ["http://efine-notice.example"]
    assert extract_urls("재배송 신청 cj-redelivery.example") == ["http://cj-redelivery.example"]
    assert extract_urls("접속 safe-check.test/a") == ["http://safe-check.test/a"]


def test_spaced_or_disguised_links():
    assert extract_urls("확인 h t t p s : / / loan-gov.example") == ["https://loan-gov.example"]
    assert extract_urls("hxxp://bad.example/x") == ["http://bad.example/x"]
    assert extract_urls("bad-site[.]com 접속") == ["http://bad-site.com"]


def test_plain_text_is_not_a_link():
    assert extract_urls("오늘 저녁 7시에 봐. 반찬 뭐 필요해?") == []


def test_normalize_removes_fillers_inside_words():
    assert normalize_text("자,,금") == "자금"
    assert normalize_text("대.출 승·인") == "대출 승인"
    assert normalize_text("ｄ대출") == "d대출"  # 전각 → 일반
    assert normalize_text("대​출") == "대출"  # 폭 0 문자


def test_obfuscated_keyword_is_caught_and_flagged():
    r = check_rules("최고의 조건으로 이용가능한 대.출 자,,금을 전해드립니다")
    assert ScamType.job_loan in r.scam_types
    assert r.obfuscated


def test_normal_text_is_not_flagged():
    for text in (
        "엄마 오늘 저녁에 갈게. 반찬 뭐 필요해?",
        "회의는 3시, 장소는 2층입니다.",
        "사과,배,귤 사 왔어",
    ):
        assert not check_rules(text).obfuscated


def test_obfuscated_unknown_sender_is_at_least_caution():
    rules = check_rules("정부 지,원 대.출 승.인 대상자 확인")
    none = LlmVerdict.model_validate(
        dict(
            scam_type="none",
            suspicion=10,
            signals=[],
            title="t",
            explanation="e",
            action="a",
            easy_sentence="s",
        )
    )
    assert decide(FastSignals(), rules, none, from_contact=False).level is Level.caution
    assert decide(FastSignals(), rules, None, from_contact=False).level is Level.caution
