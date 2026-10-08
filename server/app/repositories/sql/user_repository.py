from datetime import datetime

from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.models.user import RefreshToken, User
from app.repositories.errors import RepositoryConflictError
from app.utils.ids import new_id


class SqlUserRepository:
    def __init__(self, db: Session):
        self.db = db

    def get_by_id(self, user_id: str) -> User | None:
        return self.db.get(User, user_id)

    def get_by_email(self, email: str) -> User | None:
        return self.db.execute(select(User).where(User.email == email)).scalar_one_or_none()

    def create(self, *, email: str, password_hash: str) -> User:
        user = User(id=new_id(), email=email, password_hash=password_hash)
        self.db.add(user)
        try:
            self.db.commit()
        except IntegrityError as exc:
            self.db.rollback()
            raise RepositoryConflictError("User email already exists") from exc
        self.db.refresh(user)
        return user


class SqlRefreshTokenRepository:
    def __init__(self, db: Session):
        self.db = db

    def create(self, *, user_id: str, token_hash: str, expires_at: datetime) -> RefreshToken:
        token = RefreshToken(id=new_id(), user_id=user_id, token_hash=token_hash, expires_at=expires_at)
        self.db.add(token)
        self.db.commit()
        return token

    def get_by_hash_for_update(self, token_hash: str) -> RefreshToken | None:
        return self.db.execute(
            select(RefreshToken).where(RefreshToken.token_hash == token_hash).with_for_update()
        ).scalar_one_or_none()

    def revoke(self, token: RefreshToken, revoked_at: datetime) -> None:
        token.revoked_at = revoked_at
        self.db.commit()

    def rollback(self) -> None:
        self.db.rollback()
