# ruff: noqa: E501
"""채점 스크립트가 기획 테스트셋 형식(allowed_risks 등)을 읽고 '허용'을 구분하는지 확인합니다."""

from pathlib import Path

from app.judge.cascade import Detail
from app.schemas import JudgeResponse, Level, ScamType
from scripts.evaluate import Graded, load_rows, summarize

HEADER = "id,label,category,scam_type,channel,sender,sender_saved,sender_tag,text,url,url_status,expected_risk,allowed_risks,signals,source,note\n"


def write(tmp_path: Path, lines: list[str]) -> Path:
    p = tmp_path / "set.csv"
    p.write_text(HEADER + "\n".join(lines) + "\n", encoding="utf-8")
    return p


def resp(level: Level) -> JudgeResponse:
    return JudgeResponse(
        level=level, scam_type=ScamType.other, has_url=False, title="t", explanation="e",
        action="a", easy_sentence="s", language="ko", stage_reached=3, elapsed_ms=100,
    )  # fmt: skip


DETAIL = Detail(llm_called=True, llm_ok=True, text_source="llm", rule_types=[])


def test_team_format_fields(tmp_path):
    rows = load_rows(
        write(
            tmp_path,
            [
                "S01,scam,scam,finance,sms,국제발신 번호,FALSE,international,[국제발신] 카드 개통완료 신고 070-1234-5678,,none,danger,danger,asks_call,사례,",
                "N59,normal,normal_lookalike,none,kakao,딸 (저장됨),TRUE,none,엄마 학원비 30만원만,,none,caution,safe|caution,,직접 작성,",
            ],
        )
    )
    s01, n59 = rows
    assert (s01.scam, s01.is_international, s01.is_contact, s01.source) == (
        True,
        True,
        False,
        "sms",
    )
    assert s01.category == "scam/finance" and s01.accepted == {Level.danger}
    assert (n59.scam, n59.is_contact, n59.source) == (False, True, "kakao")
    assert n59.accepted == {Level.safe, Level.caution}


def test_allowed_counts_as_pass(tmp_path):
    (n59,) = load_rows(
        write(
            tmp_path,
            [
                "N59,normal,normal_lookalike,none,kakao,딸,TRUE,none,엄마 학원비,,none,caution,safe|caution,,,"
            ],
        )
    )
    g_safe = Graded(n59, resp(Level.safe), DETAIL)
    g_danger = Graded(n59, resp(Level.danger), DETAIL)
    assert g_safe.outcome == "허용"
    assert g_danger.outcome == "강하게 판정"
    s = summarize([g_safe])
    assert s["accepted"] == 1 and s["exact"] == 0
    assert s["normal_total"] == 1 and s["normal_quiet"] == 1  # label=normal 기준으로 집계


def test_bundled_testset_loads():
    rows = load_rows(Path(__file__).parent.parent / "eval" / "smishing_testset.csv")
    assert len(rows) >= 90
    assert sum(r.scam for r in rows) >= 15
