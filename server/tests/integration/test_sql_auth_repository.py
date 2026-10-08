"""Real repositories + PostgreSQL: duplicate handling, refresh rotation and service auth."""
from datetime import datetime, timezone

import pytest
from fastapi import HTTPException
from sqlalchemy import func, select

from app.core.security import hash_refresh_token
from app.models.user import RefreshToken, User
from app.repositories.sql.user_repository import SqlRefreshTokenRepository, SqlUserRepository
from app.schemas.auth import LoginRequest, RegisterRequest
from app.services.auth_service import AuthService

pytestmark = pytest.mark.integration


def test_register_login_refresh_logout_persists_and_revokes_tokens(sql_session, unique_id):
    # Arrange: the real service and SQL repositories share an isolated database session.
    users = SqlUserRepository(sql_session)
    tokens = SqlRefreshTokenRepository(sql_session)
    auth = AuthService(users, tokens)
    email = f"{unique_id('auth')}@example.com"
    password = "TestPassword123!"

    # Act: register, login, rotate refresh token and revoke the rotated token.
    user = auth.register_user(RegisterRequest(email=email.upper(), password=password))
    first = auth.login_user(LoginRequest(email=email, password=password))
    rotated = auth.refresh_access_token(first["refreshToken"])
    auth.logout_by_refresh_token(rotated["refreshToken"])

    # Assert: persisted user and two revoked token records (not an in-memory fake).
    assert sql_session.get(User, user.id).email == email
    assert first["accessToken"] and rotated["accessToken"]
    assert first["refreshToken"] != rotated["refreshToken"]
    rows = sql_session.execute(select(RefreshToken).where(RefreshToken.user_id == user.id)).scalars().all()
    assert len(rows) == 2
    assert all(row.revoked_at is not None for row in rows)
    with pytest.raises(HTTPException) as error:
        auth.refresh_access_token(first["refreshToken"])
    assert error.value.status_code == 401


def test_duplicate_registration_returns_conflict_without_extra_row(sql_session, unique_id):
    # Arrange
    repo = SqlUserRepository(sql_session)
    auth = AuthService(repo, SqlRefreshTokenRepository(sql_session))
    email = f"{unique_id('duplicate')}@example.com"
    registration = RegisterRequest(email=email, password="TestPassword123!")
    auth.register_user(registration)

    # Act
    with pytest.raises(HTTPException) as error:
        auth.register_user(registration)

    # Assert
    assert error.value.status_code == 409
    assert sql_session.scalar(select(func.count()).select_from(User).where(User.email == email)) == 1


def test_wrong_password_does_not_create_session(sql_session, unique_id):
    # Arrange
    auth = AuthService(SqlUserRepository(sql_session), SqlRefreshTokenRepository(sql_session))
    email = f"{unique_id('bad-password')}@example.com"
    user = auth.register_user(RegisterRequest(email=email, password="GoodPassword123!"))

    # Act
    with pytest.raises(HTTPException) as error:
        auth.login_user(LoginRequest(email=email, password="WrongPassword123!"))

    # Assert
    assert error.value.status_code == 401
    assert sql_session.scalar(select(func.count()).select_from(RefreshToken).where(RefreshToken.user_id == user.id)) == 0
