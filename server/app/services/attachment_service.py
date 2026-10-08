from datetime import datetime

from app.repositories.protocols import AttachmentRepository


class AttachmentService:
    def __init__(self, repository: AttachmentRepository):
        self.repository = repository

    def get_attachment(self, attachment_id: str):
        return self.repository.get_by_id(attachment_id)

    def mark_uploaded(
        self,
        attachment_id: str,
        *,
        remote_file_id: str,
        storage_key: str,
        size_bytes: int,
        mime_type: str | None,
        updated_at: datetime,
    ):
        return self.repository.update_upload_metadata(
            attachment_id,
            remote_file_id=remote_file_id,
            storage_key=storage_key,
            size_bytes=size_bytes,
            mime_type=mime_type,
            updated_at=updated_at,
        )

    def rollback(self) -> None:
        self.repository.rollback()
