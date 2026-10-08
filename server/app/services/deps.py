import logging
from typing import Any

from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.core.config import settings
from app.core.logging import bind_auth_context, bind_extra_context, bind_user_context
from app.core.security import decode_token
from app.db.session import get_db
from app.models.user import User
from app.repositories.mongo.attachment_repository import MongoAttachmentRepository
from app.repositories.mongo.sync_repository import MongoSyncRepository
from app.repositories.mongo.user_repository import MongoRefreshTokenRepository, MongoUserRepository
from app.repositories.protocols import AttachmentRepository, RefreshTokenRepository, SyncRepository, UserRepository
from app.repositories.sql.attachment_repository import SqlAttachmentRepository
from app.repositories.sql.sync_repository import SqlSyncRepository
from app.repositories.sql.user_repository import SqlRefreshTokenRepository, SqlUserRepository
from app.services.attachment_service import AttachmentService
from app.services.auth_service import AuthService
from app.services.sync_service import SyncService

bearer_scheme = HTTPBearer(auto_error=False)
logger = logging.getLogger(__name__)


def get_user_repository(db: Any = Depends(get_db)) -> UserRepository:
    if settings.database_backend == "mongodb":
        return MongoUserRepository(db)
    return SqlUserRepository(db)


def get_refresh_token_repository(db: Any = Depends(get_db)) -> RefreshTokenRepository:
    if settings.database_backend == "mongodb":
        return MongoRefreshTokenRepository(db)
    return SqlRefreshTokenRepository(db)


def get_sync_repository(db: Any = Depends(get_db)) -> SyncRepository:
    if settings.database_backend == "mongodb":
        return MongoSyncRepository(db)
    return SqlSyncRepository(db)


def get_attachment_repository(db: Any = Depends(get_db)) -> AttachmentRepository:
    if settings.database_backend == "mongodb":
        return MongoAttachmentRepository(db)
    return SqlAttachmentRepository(db)


def get_auth_service(
    users: UserRepository = Depends(get_user_repository),
    refresh_tokens: RefreshTokenRepository = Depends(get_refresh_token_repository),
) -> AuthService:
    return AuthService(users, refresh_tokens)


def get_sync_service(repository: SyncRepository = Depends(get_sync_repository)) -> SyncService:
    return SyncService(repository)


def get_attachment_service(repository: AttachmentRepository = Depends(get_attachment_repository)) -> AttachmentService:
    return AttachmentService(repository)


def get_current_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
    users: UserRepository = Depends(get_user_repository),
) -> User:
    if credentials is None:
        logger.warning("Authorization failed: missing bearer token")
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Missing bearer token")

    try:
        payload = decode_token(str(credentials.credentials))
    except Exception:
        logger.warning("Authorization failed: invalid token payload")
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token")

    bind_auth_context(subject=str(payload.get("sub") or "-"), token_type=str(payload.get("type") or "-"))
    bind_extra_context(auth_scheme=credentials.scheme)

    if payload.get("type") != "access" or not payload.get("sub"):
        logger.warning("Authorization failed: invalid token type")
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token type")

    user = users.get_by_id(payload["sub"])
    if user is None:
        logger.warning("Authorization failed: user not found | user_id=%s", payload.get("sub"))
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="User not found")

    bind_user_context(user_id=str(user.id), user_email=str(user.email))
    logger.debug("Authorization resolved current user")
    return user
