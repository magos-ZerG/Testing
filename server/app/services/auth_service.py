import logging
from datetime import datetime, timedelta, timezone

from fastapi import HTTPException, status

from app.core.config import settings
from app.core.logging import bind_auth_context, bind_extra_context, bind_user_context
from app.core.security import (
    create_access_token,
    create_refresh_token,
    hash_password,
    hash_refresh_token,
    verify_password,
)
from app.repositories.errors import RepositoryConflictError
from app.repositories.protocols import RefreshTokenRepository, UserRepository
from app.schemas.auth import LoginRequest, RegisterRequest

logger = logging.getLogger(__name__)


def _as_utc(value: datetime) -> datetime:
    if value.tzinfo is None:
        return value.replace(tzinfo=timezone.utc)
    return value.astimezone(timezone.utc)


def _token_fingerprint(token: str) -> str:
    digest = hash_refresh_token(token)
    return digest[:12]


def normalize_email(email: str) -> str:
    return email.strip().lower()


class AuthService:
    def __init__(self, users: UserRepository, refresh_tokens: RefreshTokenRepository):
        self.users = users
        self.refresh_tokens = refresh_tokens

    def register_user(self, payload: RegisterRequest):
        email = normalize_email(str(payload.email))
        bind_extra_context(email=email)

        try:
            user = self.users.create(email=email, password_hash=hash_password(payload.password))
        except RepositoryConflictError:
            logger.warning("Registration rejected: email already exists | email=%s", email)
            raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Email already registered")

        bind_user_context(user_id=str(user.id), user_email=str(user.email))
        logger.info("User registered | user_id=%s | email=%s", user.id, user.email)
        return user

    def login_user(self, payload: LoginRequest):
        email = normalize_email(str(payload.email))
        bind_extra_context(email=email)
        user = self.users.get_by_email(email)
        if not user or not verify_password(payload.password, user.password_hash):
            logger.warning("Login failed: invalid credentials | email=%s", email)
            raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid credentials")

        access_token = create_access_token(user.id)
        refresh_token = create_refresh_token()
        self.refresh_tokens.create(
            user_id=user.id,
            token_hash=hash_refresh_token(refresh_token),
            expires_at=datetime.now(timezone.utc) + timedelta(days=settings.refresh_token_expire_days),
        )

        bind_user_context(user_id=str(user.id), user_email=str(user.email))
        bind_auth_context(subject=str(user.id), token_type="access")
        bind_extra_context(refresh_token_fp=_token_fingerprint(refresh_token))
        logger.info("User logged in")
        return {"accessToken": access_token, "refreshToken": refresh_token, "tokenType": "bearer"}

    def refresh_access_token(self, raw_refresh_token: str):
        bind_extra_context(refresh_token_fp=_token_fingerprint(raw_refresh_token))
        token_hash = hash_refresh_token(raw_refresh_token)
        record = self.refresh_tokens.get_by_hash_for_update(token_hash)
        now = datetime.now(timezone.utc)

        if record is None or record.revoked_at is not None or _as_utc(record.expires_at) < now:
            logger.warning("Refresh token rejected")
            rollback = getattr(self.refresh_tokens, "rollback", None)
            if rollback is not None:
                rollback()
            raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid refresh token")

        access_token = create_access_token(record.user_id)
        new_refresh_token = create_refresh_token()
        self.refresh_tokens.revoke(record, now)
        self.refresh_tokens.create(
            user_id=record.user_id,
            token_hash=hash_refresh_token(new_refresh_token),
            expires_at=now + timedelta(days=settings.refresh_token_expire_days),
        )

        bind_user_context(user_id=str(record.user_id))
        bind_auth_context(subject=str(record.user_id), token_type="refresh")
        bind_extra_context(new_refresh_token_fp=_token_fingerprint(new_refresh_token))
        logger.info("Access token refreshed")
        return {"accessToken": access_token, "refreshToken": new_refresh_token, "tokenType": "bearer"}

    def logout_by_refresh_token(self, raw_refresh_token: str):
        bind_extra_context(refresh_token_fp=_token_fingerprint(raw_refresh_token))
        token_hash = hash_refresh_token(raw_refresh_token)
        record = self.refresh_tokens.get_by_hash_for_update(token_hash)
        if record and record.revoked_at is None:
            self.refresh_tokens.revoke(record, datetime.now(timezone.utc))
            bind_user_context(user_id=str(record.user_id))
            bind_auth_context(subject=str(record.user_id), token_type="refresh")
            logger.info("User logged out by refresh token")
        else:
            logger.warning("Logout requested with missing or already revoked refresh token")
