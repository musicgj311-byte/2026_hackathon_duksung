# 모두안심 백엔드

사기 의심 문자를 판정하는 API와, 보호자 연결·위험 알림 API를 담은 FastAPI 서버입니다.
앱(`frontend/`)이 의심 문자를 보내면 단계별로 판정해 사용자 언어로 된 경고 문구를 돌려주고, '위험'이면 연결된 보호자에게 FCM 푸시를 보냅니다.


## 1. 설치와 클론

**맥 (백엔드 1)**
```bash
brew install uv git
git clone https://github.com/musicgj311-byte/2026_hackathon_duksung.git
cd 2026_hackathon_duksung/backend
uv sync
```

**윈도우 (백엔드 2, PowerShell)**
```powershell
winget install --id=astral-sh.uv -e
winget install --id=Git.Git -e
git config --global core.autocrlf false
[Environment]::SetEnvironmentVariable("PYTHONUTF8", "1", "User")
```
PowerShell을 닫았다가 다시 연 뒤:
```powershell
git clone https://github.com/musicgj311-byte/2026_hackathon_duksung.git
cd 2026_hackathon_duksung/backend
uv sync
```

`uv sync`가 Python 3.12 설치와 가상환경 생성까지 알아서 합니다. 끝나면 `.venv` 폴더가 생깁니다.

## 2. 서버 실행

```bash
cp .env.example .env          # 윈도우: copy .env.example .env
uv run uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

- 브라우저에서 http://127.0.0.1:8000/health 를 열어 `{"status":"ok"}`가 보이면 성공입니다.
- http://127.0.0.1:8000/docs 에서 API를 직접 눌러 테스트할 수 있습니다.
- 다른 터미널에서 `uv run python -m scripts.smoke_test` 를 실행하면 가족 사칭, 택배 링크, 정상 문자 세 개를 판정해 결과를 보여줍니다.
- 윈도우에서 처음 실행할 때 방화벽 허용 창이 뜨면 **허용**하세요. 폰에서 접속하려면 필요합니다.

테스트와 평가 도구:
```bash
uv run pytest                                    # 단위 테스트
uv run python -m scripts.try_judge "문자 내용"     # 서버 없이 실제 키로 판정 한 번
uv run python -m scripts.evaluate                # eval/ 문자 세트로 정확도 채점 (실제 키 사용)
uv run python -m scripts.llm_speed               # 모델·생각 단계별 응답 속도 비교
```

## 3. 판정 요청·응답 모델

`app/schemas.py`가 프론트, 백엔드 1, 백엔드 2의 약속입니다. 바꿀 게 있으면 프론트와 합의한 뒤 이 파일을 고쳐 커밋하세요.

- 요청: `device_id`, `source`(sms/kakao), `sender`, `is_contact`, `is_international`, `text`, `user_type`, `language`
- 응답: `level`(danger/caution/safe), `scam_type`, `signals`, `has_url`, `url_result`, `title`, `explanation`, `action`, `easy_sentence`, `language`, `stage_reached`, `elapsed_ms`, `incident_id`
- 화면 문구 4개(`title`, `explanation`, `action`, `easy_sentence`)는 사용자 언어로 번역된 상태로 내려갑니다.

`.env.example`의 기본값은 `USE_FAKE_JUDGE=true`라서 `/judge`가 키워드만 보고 가짜 응답을 돌려줍니다(프론트 연결 확인용). **실제 판정을 하려면 `.env`에서 `USE_FAKE_JUDGE=false`로 바꿔야 합니다.**

실제 판정은 다음 순서로 진행됩니다.

1. **빠른 신호:** 링크를 최대 3개 찾고 단축 URL을 풉니다. 최종 주소를 Safe Browsing, VirusTotal, 도메인 생성일(RDAP), 공식 도메인 목록으로 확인합니다. 외부 조회는 각 3초 제한입니다. 악성 URL 목록에 걸리면 LLM 없이 바로 '위험'입니다.
2. **LLM 판정:** Gemini가 문장의 의도를 판단합니다. 기본 모델이 6초 안에 답하지 않으면 예비 모델로 다시 시도하고, 합쳐서 10초 안에 답이 없으면 규칙 결과로 판정합니다(규칙이나 링크에 걸린 게 있으면 '주의', 없으면 통과).
3. **최종 판정:** 저장된 연락처가 보낸 문자는 평소 대화일 수 있어 최대 '주의'입니다(악성 URL은 예외로 '위험'). '위험'이면 연결된 보호자에게 FCM 푸시를 보냅니다. 문자 원문과 발신 번호는 보호자에게 보내지 않고 DB에도 저장하지 않습니다.

### 앱에서 쓰는 API

| API | 설명 |
| --- | --- |
| `GET /health` | 서버 동작 확인 |
| `POST /judge` | 의심 문자 판정 |
| `POST /judge/debug/signals` | 빠른 신호만 실행 (개발 확인용) |
| `POST /users/register` | 기기 등록 (이미 있으면 409) |
| `GET /users/{device_id}` · `PATCH /users/{device_id}` | 기기 조회 · 알림 방식·언어 변경 |
| `PUT /users/{device_id}/fcm-token` | FCM 토큰 등록 |
| `POST /guardian/pairing-code` | 보호 대상자의 6자리 연결 코드 발급 (10분 유효, 1회용) |
| `POST /guardian/connect` | 보호자가 코드를 입력해 연결 |
| `DELETE /guardian/disconnect` | 연결 해제 |
| `GET /guardian/protected/{protected_device_id}/guardians` | 보호 대상자에게 연결된 보호자 목록 |
| `GET /guardian/{guardian_device_id}/protected-users` | 보호자가 지키는 가족 목록 |
| `GET /guardian/{guardian_device_id}/notifications` | 보호자 알림 기록 |
| `GET /guardian/{guardian_device_id}/incidents` | 보호자가 볼 수 있는 사건과 이벤트 |
| `POST /incidents/{incident_id}/events` | 사건 이벤트 기록 (예: `warning_shown`) |

## 4. API 키

| 키 | 발급처 | 담당 |
| --- | --- | --- |
| `LLM_API_KEY` | Google AI Studio → Get API key (Gemini) | 백엔드 1 |
| `SAFE_BROWSING_API_KEY` | Google Cloud Console → 프로젝트 생성 → Safe Browsing API 사용 설정 → 사용자 인증 정보 → API 키 | 백엔드 1 |
| `VIRUSTOTAL_API_KEY` | virustotal.com 가입 → 프로필 → API Key | 백엔드 1 |
| `FIREBASE_CREDENTIALS_PATH` | Firebase 콘솔 → 프로젝트 설정 → 서비스 계정 → 새 비공개 키 생성 (JSON 파일 경로) | 백엔드 2 |

`.env`에 채운 뒤 확인:
```bash
uv run python -m scripts.check_keys
```
세 줄 모두 `정상`이 나오면 됩니다. `.env`는 **커밋하지 말고** 팀 메신저로만 공유하세요.

키 말고 필요하면 바꾸는 설정:

| 이름 | 설명 | 기본값 |
| --- | --- | --- |
| `USE_FAKE_JUDGE` | `true`면 가짜 판정, **실제 판정은 `false`** | `true` |
| `LLM_MODEL` | 기본 모델 | `gemini-3.5-flash-lite` |
| `LLM_FALLBACK_MODEL` | 기본 모델이 실패할 때 쓰는 예비 모델 | `gemini-flash-lite-latest` |
| `LLM_THINKING_LEVEL` | Gemini 생각 단계 | `minimal` |
| `FIREBASE_CREDENTIALS_BASE64` | 키 파일 대신 JSON을 base64로 넣을 때 (배포 환경용) | 없음 |
| `DATABASE_PATH` | SQLite 파일 경로 | `backend/shield.db` |

LLM은 Gemini에 맞춰 작성되어 있어서, 다른 LLM을 쓰려면 `app/judge/llm.py`와 `scripts/check_keys.py`의 `check_llm` 함수를 함께 바꿔야 합니다.

## 5. 통합 서버 주소

프론트가 쓸 서버를 한 대로 정합니다. 개발·시연에는 ngrok이 가장 빠릅니다.

1. ngrok.com 가입 후 설치 (맥 `brew install ngrok`, 윈도우 `winget install ngrok.ngrok`)
2. 대시보드의 authtoken 등록: `ngrok config add-authtoken <토큰>`
3. 서버를 켠 상태에서 `ngrok http 8000`
4. 나온 `https://xxxx.ngrok-free.app` 주소로 확인: `uv run python -m scripts.smoke_test https://xxxx.ngrok-free.app`
5. 주소를 프론트에 공유 (앱의 `frontend/app/src/main/java/com/hackathon/ansimmessagepractice/data/api/BackendApi.kt` → `SERVER_URL`)

무료 ngrok은 실행할 때마다 주소가 바뀔 수 있으니, 대시보드에서 무료 고정 도메인(static domain)을 하나 받아 `ngrok http --url=<고정 도메인> 8000`으로 띄우면 편합니다. 시연 당일에는 Render·Railway 같은 클라우드 배포로 옮기는 걸 권장합니다.

**Railway에 배포할 때**는 `.env`와 같은 값을 Variables에 등록하고, Volume을 마운트한 경로를 `DATABASE_PATH`로 지정하면(예: `/app/data/shield.db`) 재배포 후에도 데이터가 유지됩니다. Firebase 키는 `FIREBASE_CREDENTIALS_BASE64`로 넣으면 파일을 올리지 않아도 됩니다.

**안드로이드 에뮬레이터에서 로컬 서버에 붙을 때**는 `localhost`가 아니라 `http://10.0.2.2:8000`을 씁니다.

---

## 폴더 구조

```
app/
├─ main.py            서버 시작, 라우터 등록, /health
├─ config.py          .env 읽기
├─ database.py        SQLite 연결 (DATABASE_PATH)
├─ models.py          DB 모델: 기기, 보호자 연결, 연결 코드, 사건, 사건 이벤트, 알림 기록
├─ schemas.py         판정 요청·응답 모델 (공용)
├─ firebase.py        FCM 초기화
├─ judge/             백엔드 1: 판정
│  ├─ router.py       POST /judge (USE_FAKE_JUDGE에 따라 가짜 응답 / 실제 판정)
│  ├─ cascade.py      판정 단계 실행과 최종 판정 규칙
│  ├─ signals.py      빠른 신호 (링크·평판·도메인 생성일)
│  ├─ urls.py         링크 찾기, 단축 URL 풀기
│  ├─ url_check.py    Safe Browsing · VirusTotal · RDAP 조회
│  ├─ official_domains.py  공식 도메인 목록
│  ├─ rules.py        수법 규칙·키워드 (LLM 실패 시 안전장치)
│  ├─ llm.py          Gemini 판정
│  ├─ localize.py     LLM 문구의 언어·빈 칸·기호 점검
│  ├─ fallback_messages.py  LLM 문구가 없을 때 쓰는 고정 문구
│  └─ cache.py        외부 조회 결과 메모리 캐시 (1시간)
├─ guardian/          백엔드 2: 보호자 연결·알림
│  └─ router.py       /guardian/*, notify_guardians()
├─ user/              기기 등록·설정·FCM 토큰 (/users/*)
└─ incident/          사건 이벤트 기록 (/incidents/*)
scripts/
├─ check_keys.py      API 키 확인
├─ smoke_test.py      서버 동작 확인
├─ try_judge.py       서버 없이 판정 한 번
├─ evaluate.py        정확도 채점
└─ llm_speed.py       모델별 속도 비교
eval/                 채점용 문자 세트 (CSV)
tests/                pytest 테스트
```

## 규칙

- 패키지 추가는 `uv add <패키지>`로만 하고, `pyproject.toml`과 `uv.lock`을 같이 커밋합니다.
- 커밋 전 `uv run ruff format .` 과 `uv run ruff check .` 를 실행합니다.
- 파일을 열 때는 항상 `encoding="utf-8"`, 경로는 `pathlib.Path`를 씁니다.
- 각자 자기 폴더(`judge/`, `guardian/`)만 고치고, `schemas.py`는 합의 후 고칩니다.
- `.env`와 Firebase 키 파일은 커밋하지 않습니다.
