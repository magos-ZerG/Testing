import logging
from pathlib import Path

from fastapi import HTTPException, UploadFile

from app.core.config import settings
from app.utils.ids import new_id

CHUNK_SIZE = 1024 * 1024
logger = logging.getLogger(__name__)


def build_attachment_path(attachment_id: str, original_name: str) -> tuple[str, Path]:
    safe_name = original_name.replace("/", "_").replace("\\", "_")
    remote_file_id = new_id()
    relative_key = f"{attachment_id}/{remote_file_id}_{safe_name}"
    full_path = Path(settings.file_storage_dir) / relative_key
    full_path.parent.mkdir(parents=True, exist_ok=True)
    return remote_file_id, full_path


async def save_uploaded_file(attachment_id: str, file: UploadFile) -> tuple[str, str, int]:
    remote_file_id, full_path = build_attachment_path(attachment_id, str(file.filename))
    size = 0

    try:
        with full_path.open("wb") as output:
            while True:
                chunk = await file.read(CHUNK_SIZE)
                if not chunk:
                    break

                next_size = size + len(chunk)

                if next_size > settings.max_upload_size_bytes:
                    logger.warning(
                        "Upload rejected: file too large | attachment_id=%s | size_bytes=%s | max_size_bytes=%s",
                        attachment_id,
                        next_size,
                        settings.max_upload_size_bytes,
                    )
                    raise HTTPException(
                        status_code=413,
                        detail=f"File too large. Maximum size is {settings.max_upload_size_bytes} bytes",
                    )

                output.write(chunk)
                size = next_size

    except Exception:
        full_path.unlink(missing_ok=True)
        raise

    base_dir = Path(settings.file_storage_dir).resolve()
    storage_key = full_path.resolve().relative_to(base_dir).as_posix()

    logger.info(
        "File saved | attachment_id=%s | storage_key=%s | size_bytes=%s",
        attachment_id,
        storage_key,
        size,
    )

    return remote_file_id, storage_key, size


def resolve_download_path(storage_key: str) -> Path:
    base_dir = Path(settings.file_storage_dir).resolve()
    path = (base_dir / storage_key).resolve()

    try:
        path.relative_to(base_dir)
    except ValueError:
        logger.warning("Download failed: invalid storage key | storage_key=%s", storage_key)
        raise HTTPException(status_code=400, detail="Invalid storage key")

    if not path.is_file():
        logger.warning("Download failed: stored file not found | storage_key=%s", storage_key)
        raise HTTPException(status_code=404, detail="Stored file not found")

    return path
