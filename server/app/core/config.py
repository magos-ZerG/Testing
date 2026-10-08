import secrets
from pathlib import Path

from pydantic import field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    database_backend: str = "postgresql"
    database_url: str = "postgresql+psycopg://studymate:studymate@localhost:5432/studymate"
    mongo_url: str = "mongodb://studymate:studymate@localhost:27017/studymate?authSource=admin"
    mongo_database: str = "studymate"
    jwt_secret: str = secrets.token_urlsafe(48)
    jwt_algorithm: str = "HS256"
    access_token_expire_minutes: int = 60
    refresh_token_expire_days: int = 30
    file_storage_dir: str = "./storage/files"
    max_upload_size_bytes: int = 50 * 1024 * 1024

    log_file_path: str = "./logs/studymate-server.log"
    log_level: str = "INFO"
    log_max_bytes: int = 5 * 1024 * 1024
    log_backup_count: int = 5
    run_migrations_on_startup: bool = True

    model_config = SettingsConfigDict(
        env_file=".env",
        env_prefix="",
        case_sensitive=False,
    )

    @field_validator("database_backend")
    @classmethod
    def validate_database_backend(cls, value: str) -> str:
        normalized = value.strip().lower()
        if normalized not in {"postgresql", "mongo", "mongodb"}:
            raise ValueError("DATABASE_BACKEND must be one of: postgresql, mongo, mongodb")
        return "mongodb" if normalized == "mongo" else normalized

    @field_validator("database_url")
    @classmethod
    def validate_database_url(cls, value: str) -> str:
        normalized = value.strip()
        if not normalized:
            raise ValueError("DATABASE_URL must not be empty")
        if not normalized.startswith(("postgresql+psycopg://", "postgresql://")):
            raise ValueError(
                "DATABASE_URL must point to PostgreSQL and start with postgresql+psycopg:// or postgresql://"
            )
        return normalized

    @field_validator("mongo_url")
    @classmethod
    def validate_mongo_url(cls, value: str) -> str:
        normalized = value.strip()
        if not normalized:
            raise ValueError("MONGO_URL must not be empty")
        if not normalized.startswith(("mongodb://", "mongodb+srv://")):
            raise ValueError("MONGO_URL must start with mongodb:// or mongodb+srv://")
        return normalized

    @field_validator("mongo_database")
    @classmethod
    def validate_mongo_database(cls, value: str) -> str:
        normalized = value.strip()
        if not normalized:
            raise ValueError("MONGO_DATABASE must not be empty")
        return normalized

    @field_validator("jwt_secret")
    @classmethod
    def validate_jwt_secret(cls, value: str) -> str:
        insecure_values = {"", "change_me", "secret", "default", "jwt_secret"}
        if value.strip().lower() in insecure_values:
            raise ValueError("JWT_SECRET must be set to a strong unique secret")
        if len(value) < 32:
            raise ValueError("JWT_SECRET must be at least 32 characters long")
        return value


settings = Settings()
Path(settings.file_storage_dir).mkdir(parents=True, exist_ok=True)
Path(settings.log_file_path).parent.mkdir(parents=True, exist_ok=True)
