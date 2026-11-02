from app.judge.urls import extract_urls, is_shortener, registrable_domain


def test_extract_with_and_without_scheme():
    text = "[CJ대한통운] 주소 확인 bit.ly/abc123 또는 https://evil-test.xyz/login?id=1 확인바람"
    assert extract_urls(text) == ["http://bit.ly/abc123", "https://evil-test.xyz/login?id=1"]


def test_extract_stops_at_korean_and_trailing_punctuation():
    assert extract_urls("https://a.com/x확인하세요") == ["https://a.com/x"]
    assert extract_urls("링크(https://a.com/x).") == ["https://a.com/x"]


def test_extract_ignores_email_and_plain_text():
    assert extract_urls("문의 help@naver.com 으로 주세요") == []
    assert extract_urls("오늘 저녁 7시에 봐, 버전 2.0이야") == []


def test_extract_dedup_and_limit():
    text = " ".join(f"https://a{i}.com" for i in range(5)) + " https://a0.com"
    assert extract_urls(text) == ["https://a0.com", "https://a1.com", "https://a2.com"]


def test_registrable_domain():
    assert registrable_domain("login.kakao-pay.co.kr") == "kakao-pay.co.kr"
    assert registrable_domain("a.b.example.com") == "example.com"
    assert registrable_domain("example.com") == "example.com"


def test_is_shortener():
    assert is_shortener("http://han.gl/abc")
    assert not is_shortener("https://www.cjlogistics.com")
