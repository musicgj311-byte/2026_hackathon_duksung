"""캐스케이드 ②~④ 전체를 실행해 최종 판정을 만듭니다.

② 빠른 신호 → 악성 URL 목록에 걸리면 LLM 없이 바로 '위험'
③ LLM 종합 판정 (5초 제한)
④ 신호 종합
   위험: 수법 의심 + 송금·설치·개인정보 요구·문자 속 번호로 전화 유도
         수법 의심 + 새로 만든 도메인
         수법 의심 + 모르는 번호가 보낸 링크
   저장된 연락처는 최대 '주의' (악성 URL 목록에 걸린 경우만 위험)
   주의: 수법 의심만 있음 / LLM 시간 초과·실패인데 규칙·링크에 걸린 게 있음
         LLM은 '수법 없음'이지만 모르는 번호에서 사칭 + 요구가 보임
   안전해 보임: 그 외 (문구는 항상 고정 문구, '안전하다'고 단정하지 않음)
"""

import time
from dataclasses import dataclass

from app.judge.fallback_messages import Message, get_message
from app.judge.llm import LlmVerdict, judge_with_llm
from app.judge.localize import polish
from app.judge.rules import RuleHits, check_rules
from app.judge.signals import FastSignals, collect_fast_signals
from app.schemas import JudgeRequest, JudgeResponse, Level, ScamType, UrlResult


@dataclass
class Decision:
    level: Level
    scam_type: ScamType
    stage: int
    use_llm_text: bool


def decide(
    fast: FastSignals, rules: RuleHits, verdict: LlmVerdict | None, *, from_contact: bool
) -> Decision:
    """④ 신호 종합. 외부 호출 없이 판정 규칙만 담아서 테스트하기 쉽게 분리했습니다."""
    if fast.strong:
        # 악성 URL 목록에 걸리면 저장된 연락처여도 위험 (계정 탈취 가능성)
        return Decision(Level.danger, rules.top_type, stage=2, use_llm_text=False)
    d = _decide_by_llm(fast, rules, verdict, from_contact=from_contact)
    if from_contact and d.level is Level.danger:
        # 저장된 연락처는 최대 '주의' (팀 합의: 평소 대화인지 알 수 없으니 원래 번호로 확인 안내)
        return Decision(Level.caution, d.scam_type, d.stage, d.use_llm_text)
    return d


def _decide_by_llm(
    fast: FastSignals, rules: RuleHits, verdict: LlmVerdict | None, *, from_contact: bool
) -> Decision:
    if verdict is None:
        # LLM 실패·시간 초과: 규칙이나 링크에 걸린 게 하나라도 있으면 '주의', 아무것도 없으면 통과
        if rules.scam_types or rules.pushes_action or fast.has_url:
            return Decision(Level.caution, rules.top_type, stage=3, use_llm_text=False)
        return Decision(Level.safe, ScamType.none, stage=3, use_llm_text=False)

    if verdict.is_scam_like:
        new_domain = any(u.new_domain for u in fast.urls)
        demands = verdict.pushes_action or rules.pushes_action
        unknown_link = fast.has_url and not from_contact
        if demands or new_domain or unknown_link:
            return Decision(Level.danger, verdict.scam_type, stage=3, use_llm_text=True)
        return Decision(Level.caution, verdict.scam_type, stage=3, use_llm_text=True)
    if rules.impersonation_with_demand and not from_contact:
        # LLM이 문자 속 문장에 속았을 가능성. 규칙 쪽 문구로 '주의'
        return Decision(Level.caution, rules.top_type, stage=3, use_llm_text=False)
    return Decision(Level.safe, ScamType.none, stage=3, use_llm_text=False)


def _url_result(fast: FastSignals) -> UrlResult | None:
    if not fast.urls:
        return None
    u = next((x for x in fast.urls if x.strong_hit), fast.urls[0])
    return UrlResult(
        original_url=u.original,
        final_url=u.final,
        safe_browsing_hit=u.safe_browsing_hit,
        virustotal_malicious=u.virustotal_malicious,
        domain_age_days=u.domain_age_days,
    )


@dataclass
class Detail:
    """채점·디버그용 내부 정보. API 응답에는 나가지 않습니다."""

    llm_called: bool
    llm_ok: bool
    text_source: str  # "llm" | "fixed" | "fixed_language_mismatch"
    rule_types: list[str]


async def run(req: JudgeRequest) -> JudgeResponse:
    response, _ = await run_detailed(req)
    return response


async def run_detailed(req: JudgeRequest) -> tuple[JudgeResponse, Detail]:
    started = time.perf_counter()
    rules = check_rules(req.text)
    fast = await collect_fast_signals(req.text)

    verdict = None
    if not fast.strong:
        verdict = await judge_with_llm(
            req.text,
            is_contact=req.is_contact,
            is_international=req.is_international,
            weak_signals=fast.weak,
            final_urls=[u.final for u in fast.urls],
            user_type=req.user_type,
            language=req.language,
        )

    d = decide(fast, rules, verdict, from_contact=req.is_contact)
    fixed = get_message(d.level, d.scam_type, req.language)
    if d.use_llm_text and verdict is not None:
        from_llm = Message(
            verdict.title, verdict.explanation, verdict.action, verdict.easy_sentence
        )
        m = polish(from_llm, fixed, req.language, req.user_type)
        source = "fixed_language_mismatch" if m is fixed else "llm"
        signals = verdict.signals if m is not fixed else fast.weak
    else:
        m, source = fixed, "fixed"
        signals = fast.weak if d.level is not Level.safe else []
    title, explanation, action, easy = m.title, m.explanation, m.action, m.easy_sentence

    detail = Detail(
        llm_called=not fast.strong,
        llm_ok=verdict is not None,
        text_source=source,
        rule_types=[t.value for t in rules.scam_types],
    )
    response = JudgeResponse(
        level=d.level,
        scam_type=d.scam_type,
        signals=signals,
        has_url=fast.has_url,
        url_result=_url_result(fast),
        title=title,
        explanation=explanation,
        action=action,
        easy_sentence=easy,
        language=req.language,
        stage_reached=d.stage,
        elapsed_ms=int((time.perf_counter() - started) * 1000),
    )
    return response, detail
