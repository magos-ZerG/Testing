from datetime import datetime, timezone

from pymongo.database import Database
from pymongo.errors import DuplicateKeyError

from app.db.document import document_to_object
from app.repositories.errors import RepositoryConflictError
from app.utils.ids import new_id


def _now() -> datetime:
    return datetime.now(timezone.utc)


class MongoUserRepository:
    def __init__(self, db: Database):
        self.db = db

    def get_by_id(self, user_id: str):
        return document_to_object(self.db.users.find_one({"id": user_id}))

    def get_by_email(self, email: str):
        return document_to_object(self.db.users.find_one({"email": email}))

    def create(self, *, email: str, password_hash: str):
        user = {
            "id": new_id(),
            "email": email,
            "password_hash": password_hash,
            "created_at": _now(),
        }
        try:
            self.db.users.insert_one(user)
        except DuplicateKeyError as exc:
            raise RepositoryConflictError("User email already exists") from exc
        return document_to_object(user)


class MongoRefreshTokenRepository:
    def __init__(self, db: Database):
        self.db = db

    def create(self, *, user_id: str, token_hash: str, expires_at: datetime):
        token = {
            "id": new_id(),
            "user_id": user_id,
            "token_hash": token_hash,
            "expires_at": expires_at,
            "created_at": _now(),
            "revoked_at": None,
        }
        self.db.refresh_tokens.insert_one(token)
        return document_to_object(token)

    def get_by_hash_for_update(self, token_hash: str):
        return document_to_object(self.db.refresh_tokens.find_one({"token_hash": token_hash}))

    def revoke(self, token, revoked_at: datetime) -> None:
        self.db.refresh_tokens.update_one({"id": token.id}, {"$set": {"revoked_at": revoked_at}})

    def rollback(self) -> None:
        return None
