"""All DB fixtures work exclusively with the lab2 disposable PostgreSQL container.

No fixture overrides a production database URI. Running integration tests without
LAB2_TEST_DATABASE_URL is deliberately an error, not a fallback to server/.env.
"""
import os
from datetime import datetime, timezone
from uuid import uuid4

import pytest
import requests
from sqlalchemy import create_engine, text
from sqlalchemy.orm import Session


@pytest.fixture(scope="session")
def db_engine():
    url = os.environ.get("LAB2_TEST_DATABASE_URL", "")
    assert url.startswith("postgresql+psycopg://"), (
        "LAB2_TEST_DATABASE_URL must be explicitly set to the disposable lab2 PostgreSQL"
    )
    # An explicit marker prevents accidental use against regular study/production DB.
    assert "/studymate_lab2" in url.split("?", 1)[0], "Refusing non-lab2 database"
    engine = create_engine(url, pool_pre_ping=True)
    with engine.connect() as connection:
        name = connection.execute(text("SELECT current_database() ")).scalar_one()
        assert name == "studymate_lab2", f"Refusing database {name!r}"
    yield engine
    engine.dispose()


@pytest.fixture
def sql_session(db_engine):
    """Test writes are committed by production repositories; suite isolation is DB/container-level."""
    with Session(db_engine) as session:
        yield session
        session.rollback()


@pytest.fixture
def new_user(sql_session):
    from app.repositories.sql.user_repository import SqlUserRepository

    def make_user():
        return SqlUserRepository(sql_session).create(
            email=f"integration-{uuid4().hex}@example.com", password_hash="not-used"
        )

    return make_user


@pytest.fixture(scope="session")
def api_url():
    return os.environ.get("LAB2_API_URL", "http://api:8000").rstrip("/")


@pytest.fixture(scope="session")
def api_ready(api_url):
    response = requests.get(f"{api_url}/health", timeout=10)
    response.raise_for_status()
    assert response.json() == {"status": "ok"}
    return api_url


@pytest.fixture
def unique_id():
    return lambda prefix: f"{prefix}-{uuid4().hex}"


@pytest.fixture
def now_utc():
    return lambda: datetime.now(timezone.utc)
