from datetime import datetime

from app.repositories.protocols import SyncRepository
from app.schemas.sync import InitialSyncRequest, PushRequest


class SyncService:
    def __init__(self, repository: SyncRepository):
        self.repository = repository

    def apply_push(self, user_id: str, payload: PushRequest):
        return self.repository.apply_push(user_id, payload)

    def pull_changes(self, user_id: str, since: datetime | None):
        return self.repository.pull_changes(user_id, since)

    def handle_initial_sync(self, user_id: str, payload: InitialSyncRequest):
        return self.repository.handle_initial_sync(user_id, payload)
