# ruff: noqa: E501
"""공식 도메인 목록.

모르는 번호가 보낸 링크라도, 단축 URL을 펼친 최종 주소가 이 목록의 도메인(또는 그 하위 주소)이면
'모르는 번호 + 링크' 이유만으로는 위험으로 올리지 않습니다. 최대 '주의'까지 내려갈 뿐이고, '안전'으로는
내리지 않습니다. 송금·설치·개인정보 요구, 문자 속 번호로 전화 유도, 악성 URL 목록에 걸린 경우는 그대로 위험입니다.

넣으면 안 되는 도메인
- 누구나 글이나 페이지를 올릴 수 있는 서비스: naver.com(블로그·폼), google.com(사이트·폼),
  kakao.com, notion.site, github.io, 모바일 청첩장·부고 서비스 등. 사기범도 같은 주소를 쓸 수 있어요.
- 단축 URL 서비스: 펼친 뒤의 최종 주소로 판단하므로 넣을 필요가 없어요.

정확히 일치하는 주소만 인정합니다. amazon.com은 인정하지만 amazon-delivery.com, amazon.com.xyz.top은 아닙니다.
"""

OFFICIAL_DOMAINS: frozenset[str] = frozenset({
    # 쇼핑
    "amazon.com", "coupang.com", "11st.co.kr", "gmarket.co.kr", "auction.co.kr", "ssg.com",
    # 택배
    "cjlogistics.com", "epost.go.kr", "hanjin.com", "lotteglogis.com", "ilogen.com",
    # 은행
    "kbstar.com", "shinhan.com", "wooribank.com", "hanabank.com", "ibk.co.kr",
    "nonghyup.com", "kakaobank.com", "tossbank.com",
    # 정부·공공
    "gov.kr", "hometax.go.kr", "nts.go.kr", "nhis.or.kr", "hira.or.kr", "hikorea.go.kr",
})  # fmt: skip


def is_official_host(host: str) -> bool:
    """host가 공식 도메인 자체이거나 그 하위 주소인지. 예) www.coupang.com → True"""
    host = host.lower().rstrip(".")
    return any(host == d or host.endswith("." + d) for d in OFFICIAL_DOMAINS)
