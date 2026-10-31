# 백엔드 환경설정

판정 API(백엔드 1)와 보호자 연결·알림 API(백엔드 2)를 담은 FastAPI 서버입니다.

## Day 1 체크리스트

- [ ] 1. uv 설치, 저장소 클론, `uv sync` 성공
- [ ] 2. `uv run uvicorn ...`으로 양쪽 모두 서버 실행 확인
- [ ] 3. `app/schemas.py` 판정 요청·응답 모델 확정
- [ ] 4. `.env` 공유, API 키 동작 확인
- [ ] 5. 통합 서버 주소 정해서 프론트에 공유

---

## 1. 설치와 클론

**맥 (백엔드 1)**
```bash
brew install uv git
git clone <저장소 주소>
cd shield-backend
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
git clone <저장소 주소>
cd shield-backend
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
- 다른 터미널에서 `uv run python -m scripts.smoke_test` 를 실행하면 가족 사칭(danger), 택배 링크(caution), 정상(safe) 세 문자가 판정됩니다.
- 윈도우에서 처음 실행할 때 방화벽 허용 창이 뜨면 **허용**하세요. 폰에서 접속하려면 필요합니다.

## 3. 판정 요청·응답 모델

`app/schemas.py`가 프론트 1, 백엔드 1, 백엔드 2의 약속입니다. Day 1 회의에서 아래를 확인하고, 바꿀 게 있으면 이 파일을 고친 뒤 커밋하세요.

- 요청: `device_id`, `source`(sms/kakao), `sender`, `is_contact`, `is_international`, `text`, `user_type`, `language`
- 응답: `level`(danger/caution/safe), `scam_type`, `signals`, `has_url`, `url_result`, `title`, `explanation`, `action`, `easy_sentence`, `language`, `stage_reached`, `elapsed_ms`
- 화면 문구 4개(`title`, `explanation`, `action`, `easy_sentence`)는 사용자 언어로 번역된 상태로 내려갑니다.

지금 `/judge`는 `USE_FAKE_JUDGE=true`라서 키워드만 보고 가짜 응답을 돌려줍니다. 프론트 1은 이걸로 먼저 연결 작업을 하면 됩니다.

## 4. API 키

| 키 | 발급처 | 담당 |
| --- | --- | --- |
| `LLM_API_KEY` | 사용할 LLM 콘솔 (Claude면 console.anthropic.com) | 백엔드 1 |
| `SAFE_BROWSING_API_KEY` | Google Cloud Console → 프로젝트 생성 → Safe Browsing API 사용 설정 → 사용자 인증 정보 → API 키 | 백엔드 1 |
| `VIRUSTOTAL_API_KEY` | virustotal.com 가입 → 프로필 → API Key | 백엔드 1 |
| `FIREBASE_CREDENTIALS_PATH` | Day 2에 FCM 세팅하며 발급 | 백엔드 2 |

`.env`에 채운 뒤 확인:
```bash
uv run python -m scripts.check_keys
```
세 줄 모두 `정상`이 나오면 됩니다. `.env`는 **커밋하지 말고** 팀 메신저로만 공유하세요.

다른 LLM을 쓰면 `scripts/check_keys.py`의 `check_llm` 함수만 바꾸면 됩니다.

## 5. 통합 서버 주소

프론트가 쓸 서버를 한 대로 정합니다. Day 1~2는 ngrok이 가장 빠릅니다.

1. ngrok.com 가입 후 설치 (맥 `brew install ngrok`, 윈도우 `winget install ngrok.ngrok`)
2. 대시보드의 authtoken 등록: `ngrok config add-authtoken <토큰>`
3. 서버를 켠 상태에서 `ngrok http 8000`
4. 나온 `https://xxxx.ngrok-free.app` 주소로 확인: `uv run python -m scripts.smoke_test https://xxxx.ngrok-free.app`
5. 주소를 프론트에 공유

무료 ngrok은 실행할 때마다 주소가 바뀔 수 있으니, 대시보드에서 무료 고정 도메인(static domain)을 하나 받아 `ngrok http --url=<고정 도메인> 8000`으로 띄우면 편합니다. 시연 당일에는 Render·Railway 같은 클라우드 배포로 옮기는 걸 권장합니다.

**안드로이드 에뮬레이터에서 로컬 서버에 붙을 때**는 `localhost`가 아니라 `http://10.0.2.2:8000`을 씁니다.

---

## 폴더 구조

```
app/
├─ main.py            서버 시작, 라우터 등록, /health
├─ config.py          .env 읽기
├─ schemas.py         판정 요청·응답 모델 (공용)
├─ judge/             백엔드 1: 판정
│  └─ router.py       POST /judge (지금은 가짜 응답)
└─ guardian/          백엔드 2: 보호자 연결·알림
   └─ router.py       /guardian/*, notify_guardians()
scripts/
├─ check_keys.py      API 키 확인
└─ smoke_test.py      서버 동작 확인
```

## 규칙

- 패키지 추가는 `uv add <패키지>`로만 하고, `pyproject.toml`과 `uv.lock`을 같이 커밋합니다.
- 커밋 전 `uv run ruff format .` 과 `uv run ruff check .` 를 실행합니다.
- 파일을 열 때는 항상 `encoding="utf-8"`, 경로는 `pathlib.Path`를 씁니다.
- 각자 자기 폴더(`judge/`, `guardian/`)만 고치고, `schemas.py`는 합의 후 고칩니다.
