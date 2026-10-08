import logging
from functools import lru_cache

from pymongo import MongoClient, ASCENDING
from pymongo.database import Database

from app.core.config import settings

logger = logging.getLogger(__name__)


@lru_cache(maxsize=1)
def get_mongo_client() -> MongoClient:
    return MongoClient(settings.mongo_url, uuidRepresentation="standard", tz_aware=True)


def get_mongo_database() -> Database:
    return get_mongo_client()[settings.mongo_database]


def init_mongo_indexes() -> None:
    db = get_mongo_database()

    db.users.create_index([("email", ASCENDING)], unique=True)
    db.refresh_tokens.create_index([("token_hash", ASCENDING)], unique=True)
    db.refresh_tokens.create_index([("user_id", ASCENDING)])
    db.refresh_tokens.create_index([("expires_at", ASCENDING)])

    for name in ("topics", "tasks", "task_solutions", "attachments", "flashcard_best_results"):
        db[name].create_index([("user_id", ASCENDING), ("client_id", ASCENDING)], unique=True)
        db[name].create_index([("user_id", ASCENDING), ("updated_at", ASCENDING)])

    db.tasks.create_index([("topic_id", ASCENDING)])
    db.task_solutions.create_index([("task_id", ASCENDING)])
    db.attachments.create_index([("owner_task_id", ASCENDING)])
    db.flashcard_best_results.create_index([("user_id", ASCENDING), ("topic_id", ASCENDING)], unique=True)
    logger.info("MongoDB indexes are ready")


def get_mongo_db():
    yield get_mongo_database()
