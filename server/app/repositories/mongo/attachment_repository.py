from datetime import datetime

from pymongo.database import Database

from app.db.document import document_to_object


class MongoAttachmentRepository:
    def __init__(self, db: Database):
        self.db = db

    def get_by_id(self, attachment_id: str):
        return document_to_object(self.db.attachments.find_one({"id": attachment_id}))

    def update_upload_metadata(
        self,
        attachment_id: str,
        *,
        remote_file_id: str,
        storage_key: str,
        size_bytes: int,
        mime_type: str | None,
        updated_at: datetime,
    ):
        self.db.attachments.update_one(
            {"id": attachment_id},
            {"$set": {
                "remote_file_id": remote_file_id,
                "storage_key": storage_key,
                "size_bytes": size_bytes,
                "mime_type": mime_type,
                "upload_state": "UPLOADED",
                "updated_at": updated_at,
            }},
        )
        return self.get_by_id(attachment_id)

    def rollback(self) -> None:
        return None
