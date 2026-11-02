"""테스트 문자 세트로 판정 정확도를 채점합니다. 실제 키로 외부 서비스와 LLM을 부릅니다.

실행: uv run python -m scripts.evaluate
      uv run python -m scripts.evaluate --file eval/messages.csv --delay 4.5
      uv run python -m scripts.evaluate --lang vi --type developmental   (언어·모드별 문구 확인)
      uv run python -m scripts.evaluate --limit 5                        (앞 5개만 빠르게)

CSV 형식 (eval/messages.csv): id, category, text, is_contact, is_international, expected, note
  expected: danger(위험) / caution(주의) / safe(안전해 보임)

채점 기준
- 놓친 사기: 정답이 위험·주의인데 '안전해 보임'으로 판정 (가장 치명적)
- 잘못 잡은 정상: 정답이 안전인데 '위험'으로 판정
- 과한 주의: 정답이 안전인데 '주의'로 판정 (불편하지만 치명적이지 않음)
- 약하게 판정: 정답이 위험인데 '주의'로 판정
- 허용(△): 정답과 다르지만 테스트셋의 allowed_risks에 들어 있는 판정
- 결과는 eval_results/ 폴더에 CSV로 저장됩니다.

기획 테스트셋 형식(sender_saved, expected_risk, allowed_risks, label ...)도 그대로 읽습니다.

--delay: 문자 사이에 쉬는 초. Gemini 무료 등급의 분당 호출 제한에 걸리지 않게 기본 4.5초.
"""

import argparse
import asyncio
import csv
import statistics
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path

from app.judge.cascade import Detail, run_detailed
from app.schemas import JudgeRequest, JudgeResponse, Level, UserType

DEFAULT_FILE = Path("eval/messages.csv")
RESULT_DIR = Path("eval_results")
LEVEL_KO = {"danger": "위험", "caution": "주의", "safe": "안전"}


@dataclass
class Row:
    id: str
    category: str
    text: str
    is_contact: bool
    is_international: bool
    expected: Level
    note: str = ""
    allowed: frozenset[Level] = frozenset()  # 정답 외에 허용하는 판정 (비우면 정답만)
    is_scam: bool | None = None  # 사기/정상 구분 (없으면 expected로 추정)
    source: str = "sms"

    @property
    def scam(self) -> bool:
        return self.is_scam if self.is_scam is not None else self.expected is not Level.safe

    @property
    def accepted(self) -> frozenset[Level]:
        return self.allowed | {self.expected}


@dataclass
class Graded:
    row: Row
    got: JudgeResponse
    detail: Detail

    @property
    def outcome(self) -> str:
        exp, got = self.row.expected, self.got.level
        if exp == got:
            return "정답"
        if got in self.row.accepted:
            return "허용"
        if exp is not Level.safe and got is Level.safe:
            return "놓친 사기"
        if exp is Level.safe and got is Level.danger:
            return "잘못 잡은 정상"
        if exp is Level.safe and got is Level.caution:
            return "과한 주의"
        if exp is Level.danger and got is Level.caution:
            return "약하게 판정"
        return "강하게 판정"  # 정답 주의 → 위험


def _bool(v: str) -> bool:
    return v.strip().lower() in ("true", "1", "y", "yes", "o", "예")


def _levels(v: str) -> frozenset[Level]:
    return frozenset(Level(x.strip().lower()) for x in v.split("|") if x.strip())


def _row(r: dict) -> Row:
    """두 가지 CSV 형식을 모두 읽습니다.
    - 기본 형식: is_contact, is_international, expected
    - 기획 테스트셋 형식: sender_saved, sender_tag, expected_risk, allowed_risks, label, channel
    """
    g = lambda k: (r.get(k) or "").strip()  # noqa: E731
    if "expected_risk" in r:
        category = g("category")
        if g("scam_type") and g("scam_type") != "none":
            category = f"{category}/{g('scam_type')}"
        return Row(
            id=g("id"),
            category=category,
            text=g("text"),
            is_contact=_bool(g("sender_saved")),
            is_international=g("sender_tag").lower() == "international",
            expected=Level(g("expected_risk").lower()),
            note=g("note"),
            allowed=_levels(g("allowed_risks")),
            is_scam=g("label").lower() == "scam" if g("label") else None,
            source="kakao" if g("channel").lower() == "kakao" else "sms",
        )
    return Row(
        id=g("id"),
        category=g("category"),
        text=g("text"),
        is_contact=_bool(g("is_contact")),
        is_international=_bool(g("is_international")),
        expected=Level(g("expected").lower()),
        note=g("note"),
    )


def load_rows(path: Path) -> list[Row]:
    with path.open(encoding="utf-8-sig", newline="") as f:
        return [_row(r) for r in csv.DictReader(f) if (r.get("text") or "").strip()]


def summarize(results: list[Graded]) -> dict:
    """채점 결과를 숫자로 요약합니다. 테스트하기 쉽게 출력과 분리했습니다."""
    n = len(results)
    count = lambda name: sum(1 for g in results if g.outcome == name)  # noqa: E731
    scams = [g for g in results if g.row.scam]
    normals = [g for g in results if not g.row.scam]
    times = [g.got.elapsed_ms for g in results]
    return {
        "total": n,
        "exact": count("정답"),
        "accepted": count("정답") + count("허용"),
        "missed": count("놓친 사기"),
        "false_alarm": count("잘못 잡은 정상"),
        "over_caution": count("과한 주의"),
        "weaker": count("약하게 판정"),
        "stronger": count("강하게 판정"),
        "scam_total": len(scams),
        "scam_warned": sum(1 for g in scams if g.got.level is not Level.safe),
        "normal_total": len(normals),
        "normal_quiet": sum(1 for g in normals if g.got.level is Level.safe),
        "normal_danger": sum(1 for g in normals if g.got.level is Level.danger),
        "avg_ms": int(statistics.mean(times)) if times else 0,
        "max_ms": max(times) if times else 0,
        "llm_called": sum(1 for g in results if g.detail.llm_called),
        "llm_failed": sum(1 for g in results if g.detail.llm_called and not g.detail.llm_ok),
        "lang_mismatch": sum(
            1 for g in results if g.detail.text_source == "fixed_language_mismatch"
        ),
    }


def print_report(results: list[Graded], s: dict) -> None:
    print("\n" + "=" * 72)
    for g in results:
        mark = {"정답": "O", "허용": "△"}.get(g.outcome, "X")
        exp, got = LEVEL_KO[g.row.expected.value], LEVEL_KO[g.got.level.value]
        print(
            f"{mark} #{g.row.id:>3} [{g.row.category}] 정답 {exp} / 판정 {got}"
            f" ({g.outcome}) {g.got.elapsed_ms}ms"
        )
        if g.outcome not in ("정답", "허용"):
            print(f"      문자: {g.row.text[:60]}")
            print(f"      설명: {g.got.explanation[:60]}")
    pct = lambda a, b: f"{a}/{b} ({a / b:.0%})" if b else "0/0"  # noqa: E731
    print("=" * 72)
    print(f"허용 범위 안 (O + △)      {pct(s['accepted'], s['total'])}")
    print(f"단계까지 정확히 맞힘 (O)  {pct(s['exact'], s['total'])}")
    print(f"사기 문자에 경고함        {pct(s['scam_warned'], s['scam_total'])}")
    print(f"정상 문자를 조용히 통과   {pct(s['normal_quiet'], s['normal_total'])}")
    print(f"정상 문자를 '위험'으로    {s['normal_danger']}")
    print(f"놓친 사기 (치명적)        {s['missed']}")
    print(f"잘못 잡은 정상 (위험)     {s['false_alarm']}")
    print(f"과한 주의 (정상 → 주의)   {s['over_caution']}")
    print(f"약하게 판정 (위험 → 주의) {s['weaker']}")
    print(f"강하게 판정 (주의 → 위험) {s['stronger']}")
    print(f"평균 응답 시간            {s['avg_ms']}ms (최대 {s['max_ms']}ms)")
    print(f"LLM 호출 / 실패           {s['llm_called']} / {s['llm_failed']}")
    if s["lang_mismatch"]:
        print(f"언어 불일치로 고정 문구   {s['lang_mismatch']}")
    if s["llm_failed"]:
        print("\n※ LLM 실패가 있으면 그 문자는 고정 문구의 '주의'로 판정됩니다.")
        print("  키·모델 이름을 확인하거나, 한도 초과(429)라면 --delay를 늘려 다시 돌려보세요.")


def save_csv(results: list[Graded], lang: str, user_type: str) -> Path:
    RESULT_DIR.mkdir(exist_ok=True)
    path = RESULT_DIR / f"eval_{datetime.now():%Y%m%d_%H%M%S}_{lang}_{user_type}.csv"
    with path.open("w", encoding="utf-8-sig", newline="") as f:
        w = csv.writer(f)
        w.writerow(
            ["id", "category", "expected", "got", "outcome", "scam_type", "stage", "elapsed_ms",
             "llm_ok", "text_source", "title", "explanation", "action", "easy_sentence", "text"]
        )  # fmt: skip
        for g in results:
            r = g.got
            w.writerow(
                [g.row.id, g.row.category, g.row.expected.value, r.level.value, g.outcome,
                 r.scam_type.value, r.stage_reached, r.elapsed_ms, g.detail.llm_ok,
                 g.detail.text_source, r.title, r.explanation, r.action, r.easy_sentence,
                 g.row.text]
            )  # fmt: skip
    return path


async def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--file", type=Path, default=DEFAULT_FILE)
    p.add_argument("--lang", default="ko")
    p.add_argument("--type", default="default", choices=[t.value for t in UserType])
    p.add_argument("--delay", type=float, default=4.5)
    p.add_argument("--limit", type=int, default=0)
    a = p.parse_args()

    rows = load_rows(a.file)
    if a.limit:
        rows = rows[: a.limit]
    print(f"{a.file} 에서 {len(rows)}개 문자 채점 (언어 {a.lang}, 모드 {a.type}, 간격 {a.delay}초)")

    results: list[Graded] = []
    for i, row in enumerate(rows, 1):
        req = JudgeRequest(
            device_id="eval",
            source=row.source,
            sender="010-0000-0000",
            is_contact=row.is_contact,
            is_international=row.is_international,
            text=row.text,
            user_type=UserType(a.type),
            language=a.lang,
        )
        got, detail = await run_detailed(req)
        results.append(Graded(row, got, detail))
        print(f"  {i}/{len(rows)} #{row.id} → {LEVEL_KO[got.level.value]}", flush=True)
        if i < len(rows) and detail.llm_called:
            await asyncio.sleep(a.delay)

    s = summarize(results)
    print_report(results, s)
    print(f"\n자세한 결과: {save_csv(results, a.lang, a.type)}")


if __name__ == "__main__":
    asyncio.run(main())
