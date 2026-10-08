from datetime import datetime

from fastapi import APIRouter, Depends, HTTPException, Query

from app.api.route_logging import LoggedRoute
from app.models.user import User
from app.schemas.sync import InitialSyncRequest, InitialSyncResponse, PullResponse, PushRequest, PushResponse
from app.services.deps import get_current_user, get_sync_service
from app.services.sync_service import SyncService

router = APIRouter(prefix="/api/v1/sync", tags=["sync"], route_class=LoggedRoute)


@router.post("/push", response_model=PushResponse)
def push(
    payload: PushRequest,
    current_user: User = Depends(get_current_user),
    sync: SyncService = Depends(get_sync_service),
):
    return sync.apply_push(str(current_user.id), payload)


@router.get("/pull", response_model=PullResponse)
def pull(
    since: datetime | None = Query(default=None),
    current_user: User = Depends(get_current_user),
    sync: SyncService = Depends(get_sync_service),
):
    if since is not None and since.tzinfo is None:
        raise HTTPException(status_code=400, detail="since must contain timezone")

    return sync.pull_changes(str(current_user.id), since)


@router.post("/initial", response_model=InitialSyncResponse)
def initial_sync(
    payload: InitialSyncRequest,
    current_user: User = Depends(get_current_user),
    sync: SyncService = Depends(get_sync_service),
):
    return sync.handle_initial_sync(str(current_user.id), payload)
