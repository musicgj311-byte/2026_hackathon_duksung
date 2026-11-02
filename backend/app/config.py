from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """.env 파일과 환경변수에서 설정을 읽습니다."""

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    llm_api_key: str = ""
    llm_model: str = "gemini-3.5-flash-lite"
    safe_browsing_api_key: str = ""
    virustotal_api_key: str = ""
    firebase_credentials_path: str = "firebase-key.json"
    use_fake_judge: bool = True


settings = Settings()
