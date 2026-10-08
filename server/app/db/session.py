import logging
from sqlalchemy import create_engine
from sqlalchemy.orm import declarative_base, sessionmaker

from app.core.config import settings
from app.db.mongo import get_mongo_db

logger = logging.getLogger(__name__)

engine = create_engine(
    settings.database_url,
    future=True,
    pool_pre_ping=True,
)
SessionLocal = sessionmaker(bind=engine, autoflush=False, autocommit=False, future=True)
Base = declarative_base()


def get_sql_db():
    db = SessionLocal()
    logger.debug("SQL database session opened")
    try:
        yield db
    finally:
        db.close()
        logger.debug("SQL database session closed")


def get_db():
    if settings.database_backend == "mongodb":
        yield from get_mongo_db()
        return
    yield from get_sql_db()
