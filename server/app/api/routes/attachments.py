import logging
from pathlib import Path

from fastapi import APIRouter, Depends, File, HTTPException, Request, UploadFile
from fastapi.responses import FileResponse

from app.api.route_logging import LoggedRoute
from app.core.config import settings
from app.core.logging import bind_extra_context
from app.models.study import utcnow
from app.models.user import User
from app.services.attachment_service import AttachmentService
from app.services.deps import get_attachment_service, get_current_user
from app.services.storage_service import resolve_download_path, save_uploaded_file

router = APIRouter(prefix="/api/v1/attachments", tags=["attachments"], route_class=LoggedRoute)
logger = logging.getLogger(__name__)


@router.post("/{attachment_id}/upload")
async def upload_attachment(
    attachment_id: str,
    request: Request,
    file: UploadFile = File(...),
    current_user: User = Depends(get_current_user),
    attachments: AttachmentService = Depends(get_attachment_service),
):
    bind_extra_context(attachment_id=attachment_id, filename=file.filename, content_type=file.content_type)
    attachment = attachments.get_attachment(attachment_id)
    if attachment is None or attachment.user_id != current_user.id:
        logger.warning("Upload rejected: attachment not found | attachment_id=%s | user_id=%s", attachment_id, current_user.id)
        raise HTTPException(status_code=404, detail="Attachment not found")
    if attachment.is_deleted:
        logger.warning("Upload rejected: deleted attachment | attachment_id=%s | user_id=%s", attachment_id, current_user.id)
        raise HTTPException(status_code=409, detail="Deleted attachment cannot be uploaded")

    bind_extra_context(owner_task_id=attachment.owner_task_id, owner_type=attachment.owner_type)

    content_length = request.headers.get("content-length")
    if content_length is not None:
        try:
            if int(content_length) > settings.max_upload_size_bytes:
                logger.warning(
                    "Upload rejected: content-length too large | attachment_id=%s | content_length=%s | max_size_bytes=%s",
                    attachment_id,
                    content_length,
                    settings.max_upload_size_bytes,
                )
                raise HTTPException(status_code=413, detail=f"File too large. Maximum size is {settings.max_upload_size_bytes} bytes")
        except ValueError:
            logger.warning("Upload request has invalid content-length | attachment_id=%s | content_length=%s", attachment_id, content_length)

    remote_file_id, storage_key, size = await save_uploaded_file(attachment_id, file)

    try:
        attachment = attachments.mark_uploaded(
            attachment_id,
            remote_file_id=remote_file_id,
            storage_key=storage_key,
            size_bytes=size,
            mime_type=file.content_type,
            updated_at=utcnow(),
        )
    except Exception:
        attachments.rollback()
        (Path(settings.file_storage_dir) / storage_key).unlink(missing_ok=True)
        logger.exception("Attachment DB update failed after file save | attachment_id=%s | storage_key=%s", attachment_id, storage_key)
        raise

    bind_extra_context(remote_file_id=attachment.remote_file_id, storage_key=attachment.storage_key, size_bytes=attachment.size_bytes)
    logger.info("Attachment uploaded | attachment_id=%s | user_id=%s | size_bytes=%s", attachment.id, current_user.id, attachment.size_bytes)

    return {
        "id": attachment.id,
        "remoteFileId": attachment.remote_file_id,
        "storageKey": attachment.storage_key,
        "sizeBytes": attachment.size_bytes,
        "uploadState": attachment.upload_state,
        "updatedAt": attachment.updated_at,
    }


@router.get("/{attachment_id}/download")
def download_attachment(
    attachment_id: str,
    current_user: User = Depends(get_current_user),
    attachments: AttachmentService = Depends(get_attachment_service),
):
    bind_extra_context(attachment_id=attachment_id)
    attachment = attachments.get_attachment(attachment_id)
    if attachment is None or attachment.user_id != current_user.id or attachment.is_deleted or not attachment.storage_key:
        logger.warning("Download rejected: attachment file not found | attachment_id=%s | user_id=%s", attachment_id, current_user.id)
        raise HTTPException(status_code=404, detail="Attachment file not found")

    bind_extra_context(
        owner_task_id=attachment.owner_task_id,
        owner_type=attachment.owner_type,
        storage_key=attachment.storage_key,
        remote_file_id=attachment.remote_file_id,
    )
    path = resolve_download_path(str(attachment.storage_key))
    logger.info("Attachment download started | attachment_id=%s | user_id=%s | storage_key=%s", attachment.id, current_user.id, attachment.storage_key)
    return FileResponse(path=path, filename=str(attachment.file_name), media_type=attachment.mime_type or "application/octet-stream")
