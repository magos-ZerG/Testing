from datetime import datetime

from sqlalchemy.orm import Session

from app.models.study import Attachment


class SqlAttachmentRepository:
    def __init__(self, db: Session):
        self.db = db

    def get_by_id(self, attachment_id: str) -> Attachment | None:
        return self.db.get(Attachment, attachment_id)

    def update_upload_metadata(
        self,
        attachment_id: str,
        *,
        remote_file_id: str,
        storage_key: str,
        size_bytes: int,
        mime_type: str | None,
        updated_at: datetime,
    ) -> Attachment:
        attachment = self.db.get(Attachment, attachment_id)
        if attachment is None:
            raise LookupError(attachment_id)
        attachment.remote_file_id = remote_file_id
        attachment.storage_key = storage_key
        attachment.size_bytes = size_bytes
        attachment.mime_type = mime_type
        attachment.upload_state = "UPLOADED"
        attachment.updated_at = updated_at
        self.db.commit()
        self.db.refresh(attachment)
        return attachment

    def rollback(self) -> None:
        self.db.rollback()
