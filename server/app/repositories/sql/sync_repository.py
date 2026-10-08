import logging
from datetime import datetime, timezone

from fastapi import HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.logging import bind_extra_context
from app.models.study import Attachment, FlashcardBestResult, Task, TaskSolution, Topic
from app.schemas.sync import InitialSyncRequest, PullResponse, PushRequest, PushResultItem
from app.utils.ids import new_id

ALLOWED_ATTACHMENT_OWNER_TYPES = {"TASK_DESCRIPTION", "TASK_SOLUTION"}
logger = logging.getLogger(__name__)


def _now() -> datetime:
    return datetime.now(timezone.utc)


def _normalize_delete_flag(operation: str, is_deleted: bool) -> bool:
    return operation == "DELETE" or is_deleted


def _incoming_should_win(
    incoming_updated_at: datetime,
    existing_updated_at: datetime,
    *,
    incoming_deleted: bool,
    existing_deleted: bool,
) -> bool:
    if incoming_updated_at > existing_updated_at:
        return True
    if incoming_updated_at < existing_updated_at:
        return False
    return incoming_deleted and not existing_deleted


def _mark_deleted(existing, incoming_updated_at: datetime) -> None:
    existing.is_deleted = True
    existing.updated_at = incoming_updated_at


def _validate_attachment_owner_type(owner_type: str) -> None:
    if owner_type not in ALLOWED_ATTACHMENT_OWNER_TYPES:
        raise HTTPException(
            status_code=400,
            detail=(
                "Invalid attachment ownerType. "
                f"Expected one of {sorted(ALLOWED_ATTACHMENT_OWNER_TYPES)}, got {owner_type!r}"
            ),
        )


def _resolve_parent_topic_id(
    db: Session,
    user_id: str,
    topic_remote_id: str | None,
    topic_client_id: str | None,
    *,
    allow_deleted: bool = False,
) -> str:
    topic = None
    if topic_remote_id:
        topic = db.get(Topic, topic_remote_id)
        if topic and topic.user_id != user_id:
            topic = None
    if topic is None and topic_client_id:
        topic = db.execute(
            select(Topic).where(Topic.user_id == user_id, Topic.client_id == topic_client_id)
        ).scalar_one_or_none()

    if topic is None:
        raise HTTPException(
            status_code=400,
            detail=f"Topic reference not found: remote={topic_remote_id}, client={topic_client_id}",
        )
    if topic.is_deleted and not allow_deleted:
        raise HTTPException(status_code=409, detail="Referenced topic is deleted")
    return topic.id


def _resolve_parent_task_id(
    db: Session,
    user_id: str,
    task_remote_id: str | None,
    task_client_id: str | None,
    *,
    allow_deleted: bool = False,
) -> str:
    task = None
    if task_remote_id:
        task = db.get(Task, task_remote_id)
        if task and task.user_id != user_id:
            task = None
    if task is None and task_client_id:
        task = db.execute(
            select(Task).where(Task.user_id == user_id, Task.client_id == task_client_id)
        ).scalar_one_or_none()

    if task is None:
        raise HTTPException(
            status_code=400,
            detail=f"Task reference not found: remote={task_remote_id}, client={task_client_id}",
        )
    if task.is_deleted and not allow_deleted:
        raise HTTPException(status_code=409, detail="Referenced task is deleted")
    return task.id


def _result(item, status: str) -> PushResultItem:
    return PushResultItem(
        clientId=item.client_id,
        remoteId=item.id,
        serverUpdatedAt=item.updated_at,
        status=status,
    )


def _result_for_client(item, client_id: str, status: str) -> PushResultItem:
    return PushResultItem(
        clientId=client_id,
        remoteId=item.id,
        serverUpdatedAt=item.updated_at,
        status=status,
    )


def _create_skip_result(client_id: str, updated_at: datetime) -> PushResultItem:
    return PushResultItem(
        clientId=client_id,
        remoteId="",
        serverUpdatedAt=updated_at,
        status="SKIPPED",
    )


def _cascade_delete_topic(db: Session, topic_id: str, deleted_at: datetime) -> None:
    tasks = db.execute(select(Task).where(Task.topic_id == topic_id)).scalars().all()
    for task in tasks:
        if task.updated_at <= deleted_at or not task.is_deleted:
            _cascade_delete_task(db, task, deleted_at)

    flashcard_results = db.execute(
        select(FlashcardBestResult).where(FlashcardBestResult.topic_id == topic_id)
    ).scalars().all()
    for result in flashcard_results:
        if result.updated_at <= deleted_at or not result.is_deleted:
            result.is_deleted = True
            result.updated_at = deleted_at


def _cascade_delete_task(db: Session, task: Task, deleted_at: datetime) -> None:
    task.is_deleted = True
    task.updated_at = deleted_at

    solutions = db.execute(select(TaskSolution).where(TaskSolution.task_id == task.id)).scalars().all()
    for solution in solutions:
        if solution.updated_at <= deleted_at or not solution.is_deleted:
            solution.is_deleted = True
            solution.updated_at = deleted_at

    attachments = db.execute(select(Attachment).where(Attachment.owner_task_id == task.id)).scalars().all()
    for attachment in attachments:
        if attachment.updated_at <= deleted_at or not attachment.is_deleted:
            attachment.is_deleted = True
            attachment.updated_at = deleted_at


def _apply_topic_update(existing: Topic, item) -> None:
    existing.title = item.title
    existing.updated_at = item.updatedAt
    existing.is_deleted = False


def _apply_task_update(db: Session, user_id: str, existing: Task, item, incoming_deleted: bool) -> None:
    if incoming_deleted:
        _cascade_delete_task(db, existing, item.updatedAt)
        return

    topic_id = _resolve_parent_topic_id(db, user_id, item.topicRemoteId, item.topicClientId)
    existing.topic_id = topic_id
    existing.title = item.title
    existing.description = item.description
    existing.status = item.status
    existing.deadline_at = item.deadlineAt
    existing.updated_at = item.updatedAt
    existing.is_deleted = False


def _apply_solution_update(db: Session, user_id: str, existing: TaskSolution, item, incoming_deleted: bool) -> None:
    if incoming_deleted:
        _mark_deleted(existing, item.updatedAt)
        return

    task_id = _resolve_parent_task_id(db, user_id, item.taskRemoteId, item.taskClientId)
    existing.task_id = task_id
    existing.content = item.content
    existing.updated_at = item.updatedAt
    existing.is_deleted = False


def _apply_attachment_update(db: Session, user_id: str, existing: Attachment, item, incoming_deleted: bool) -> None:
    if incoming_deleted:
        _mark_deleted(existing, item.updatedAt)
        return

    if item.ownerType is not None:
        _validate_attachment_owner_type(item.ownerType)
        existing.owner_type = item.ownerType

    owner_task_id = _resolve_parent_task_id(db, user_id, item.ownerTaskRemoteId, item.ownerTaskClientId)
    existing.owner_task_id = owner_task_id
    existing.file_name = item.fileName
    existing.mime_type = item.mimeType
    existing.size_bytes = item.sizeBytes
    existing.updated_at = item.updatedAt
    existing.is_deleted = False
    existing.remote_file_id = item.remoteFileId
    existing.upload_state = item.uploadState


def _flashcard_result_is_better(
    new_questions_count: int,
    new_duration_ms: int,
    existing_questions_count: int,
    existing_duration_ms: int,
) -> bool:
    if new_questions_count != existing_questions_count:
        return new_questions_count > existing_questions_count
    return new_duration_ms < existing_duration_ms


def _apply_flashcard_best_result_update(
    db: Session,
    user_id: str,
    existing: FlashcardBestResult,
    item,
    incoming_deleted: bool,
) -> None:
    if incoming_deleted:
        _mark_deleted(existing, item.updatedAt)
        return

    topic_id = _resolve_parent_topic_id(db, user_id, item.topicRemoteId, item.topicClientId)
    incoming_is_better = _flashcard_result_is_better(
        item.questionsCount,
        item.durationMs,
        existing.questions_count,
        existing.duration_ms,
    )

    if incoming_is_better or existing.is_deleted:
        existing.topic_id = topic_id
        existing.questions_count = item.questionsCount
        existing.duration_ms = item.durationMs
        existing.completed_at = item.completedAt
        existing.updated_at = item.updatedAt
        existing.is_deleted = False
    elif item.updatedAt > existing.updated_at:
        existing.updated_at = item.updatedAt


def _find_flashcard_best_result_by_topic(
    db: Session,
    user_id: str,
    topic_id: str,
) -> FlashcardBestResult | None:
    return db.execute(
        select(FlashcardBestResult).where(
            FlashcardBestResult.user_id == user_id,
            FlashcardBestResult.topic_id == topic_id,
        )
    ).scalar_one_or_none()


def apply_push(db: Session, user_id: str, payload: PushRequest):
    bind_extra_context(
        sync_operation='push',
        sync_topics=len(payload.topics),
        sync_tasks=len(payload.tasks),
        sync_solutions=len(payload.solutions),
        sync_attachments=len(payload.attachmentsMetadata),
        sync_flashcard_best_results=len(payload.flashcardBestResults),
    )
    logger.info(
        "Sync push started | user_id=%s | topics=%s | tasks=%s | solutions=%s | attachments=%s | flashcard_best_results=%s",
        user_id,
        len(payload.topics),
        len(payload.tasks),
        len(payload.solutions),
        len(payload.attachmentsMetadata),
        len(payload.flashcardBestResults),
    )
    topic_results = []
    task_results = []
    solution_results = []
    attachment_results = []
    flashcard_best_result_results = []

    for item in payload.topics:
        incoming_deleted = _normalize_delete_flag(item.operation, item.isDeleted)
        existing = db.execute(
            select(Topic).where(Topic.user_id == user_id, Topic.client_id == item.clientId)
        ).scalar_one_or_none()

        if existing is None:
            if incoming_deleted:
                topic_results.append(_create_skip_result(item.clientId, item.updatedAt))
                continue
            obj = Topic(
                id=new_id(),
                client_id=item.clientId,
                user_id=user_id,
                title=item.title,
                created_at=item.updatedAt,
                updated_at=item.updatedAt,
                is_deleted=False,
            )
            db.add(obj)
            db.flush()
            topic_results.append(_result(obj, "APPLIED"))
            continue

        if _incoming_should_win(
            item.updatedAt,
            existing.updated_at,
            incoming_deleted=incoming_deleted,
            existing_deleted=existing.is_deleted,
        ):
            if incoming_deleted:
                _mark_deleted(existing, item.updatedAt)
                _cascade_delete_topic(db, existing.id, item.updatedAt)
            else:
                _apply_topic_update(existing, item)
            topic_results.append(_result(existing, "APPLIED"))
            continue

        topic_results.append(_result(existing, "SKIPPED"))

    db.flush()

    for item in payload.tasks:
        incoming_deleted = _normalize_delete_flag(item.operation, item.isDeleted)
        existing = db.execute(
            select(Task).where(Task.user_id == user_id, Task.client_id == item.clientId)
        ).scalar_one_or_none()

        if existing is None:
            if incoming_deleted:
                if not item.topicRemoteId and not item.topicClientId:
                    task_results.append(_create_skip_result(item.clientId, item.updatedAt))
                    continue
                topic_id = _resolve_parent_topic_id(
                    db,
                    user_id,
                    item.topicRemoteId,
                    item.topicClientId,
                    allow_deleted=True,
                )
                obj = Task(
                    id=new_id(),
                    client_id=item.clientId,
                    user_id=user_id,
                    topic_id=topic_id,
                    title=item.title or "",
                    description=item.description,
                    status=item.status,
                    deadline_at=item.deadlineAt,
                    created_at=item.updatedAt,
                    updated_at=item.updatedAt,
                    is_deleted=True,
                )
                db.add(obj)
                db.flush()
                task_results.append(_result(obj, "APPLIED"))
                continue

            topic_id = _resolve_parent_topic_id(db, user_id, item.topicRemoteId, item.topicClientId)
            obj = Task(
                id=new_id(),
                client_id=item.clientId,
                user_id=user_id,
                topic_id=topic_id,
                title=item.title,
                description=item.description,
                status=item.status,
                deadline_at=item.deadlineAt,
                created_at=item.updatedAt,
                updated_at=item.updatedAt,
                is_deleted=False,
            )
            db.add(obj)
            db.flush()
            task_results.append(_result(obj, "APPLIED"))
            continue

        if _incoming_should_win(
            item.updatedAt,
            existing.updated_at,
            incoming_deleted=incoming_deleted,
            existing_deleted=existing.is_deleted,
        ):
            _apply_task_update(db, user_id, existing, item, incoming_deleted)
            task_results.append(_result(existing, "APPLIED"))
            continue

        task_results.append(_result(existing, "SKIPPED"))

    db.flush()

    for item in payload.solutions:
        incoming_deleted = _normalize_delete_flag(item.operation, item.isDeleted)
        existing = db.execute(
            select(TaskSolution).where(TaskSolution.user_id == user_id, TaskSolution.client_id == item.clientId)
        ).scalar_one_or_none()

        if existing is None:
            if incoming_deleted:
                if not item.taskRemoteId and not item.taskClientId:
                    solution_results.append(_create_skip_result(item.clientId, item.updatedAt))
                    continue
                task_id = _resolve_parent_task_id(
                    db,
                    user_id,
                    item.taskRemoteId,
                    item.taskClientId,
                    allow_deleted=True,
                )
                obj = TaskSolution(
                    id=new_id(),
                    client_id=item.clientId,
                    user_id=user_id,
                    task_id=task_id,
                    content=item.content,
                    updated_at=item.updatedAt,
                    is_deleted=True,
                )
                db.add(obj)
                db.flush()
                solution_results.append(_result(obj, "APPLIED"))
                continue

            task_id = _resolve_parent_task_id(db, user_id, item.taskRemoteId, item.taskClientId)
            obj = TaskSolution(
                id=new_id(),
                client_id=item.clientId,
                user_id=user_id,
                task_id=task_id,
                content=item.content,
                updated_at=item.updatedAt,
                is_deleted=False,
            )
            db.add(obj)
            db.flush()
            solution_results.append(_result(obj, "APPLIED"))
            continue

        if _incoming_should_win(
            item.updatedAt,
            existing.updated_at,
            incoming_deleted=incoming_deleted,
            existing_deleted=existing.is_deleted,
        ):
            _apply_solution_update(db, user_id, existing, item, incoming_deleted)
            solution_results.append(_result(existing, "APPLIED"))
            continue

        solution_results.append(_result(existing, "SKIPPED"))

    db.flush()

    for item in payload.attachmentsMetadata:
        incoming_deleted = _normalize_delete_flag(item.operation, item.isDeleted)
        existing = db.execute(
            select(Attachment).where(Attachment.user_id == user_id, Attachment.client_id == item.clientId)
        ).scalar_one_or_none()

        if existing is None:
            if incoming_deleted:
                if not item.ownerTaskRemoteId and not item.ownerTaskClientId:
                    attachment_results.append(_create_skip_result(item.clientId, item.updatedAt))
                    continue
                if item.ownerType is None:
                    raise HTTPException(
                        status_code=400,
                        detail="ownerType is required when creating an attachment",
                    )
                _validate_attachment_owner_type(item.ownerType)
                owner_task_id = _resolve_parent_task_id(
                    db,
                    user_id,
                    item.ownerTaskRemoteId,
                    item.ownerTaskClientId,
                    allow_deleted=True,
                )
                obj = Attachment(
                    id=new_id(),
                    client_id=item.clientId,
                    user_id=user_id,
                    owner_type=item.ownerType,
                    owner_task_id=owner_task_id,
                    file_name=item.fileName or "",
                    mime_type=item.mimeType,
                    size_bytes=item.sizeBytes,
                    created_at=item.updatedAt,
                    updated_at=item.updatedAt,
                    is_deleted=True,
                    remote_file_id=item.remoteFileId,
                    upload_state=item.uploadState,
                )
                db.add(obj)
                db.flush()
                attachment_results.append(_result(obj, "APPLIED"))
                continue

            if item.ownerType is None:
                raise HTTPException(
                    status_code=400,
                    detail="ownerType is required when creating an attachment tombstone",
                )
            _validate_attachment_owner_type(item.ownerType)
            owner_task_id = _resolve_parent_task_id(db, user_id, item.ownerTaskRemoteId, item.ownerTaskClientId)
            obj = Attachment(
                id=new_id(),
                client_id=item.clientId,
                user_id=user_id,
                owner_type=item.ownerType,
                owner_task_id=owner_task_id,
                file_name=item.fileName,
                mime_type=item.mimeType,
                size_bytes=item.sizeBytes,
                created_at=item.updatedAt,
                updated_at=item.updatedAt,
                is_deleted=False,
                remote_file_id=item.remoteFileId,
                upload_state=item.uploadState,
            )
            db.add(obj)
            db.flush()
            attachment_results.append(_result(obj, "APPLIED"))
            continue

        if _incoming_should_win(
            item.updatedAt,
            existing.updated_at,
            incoming_deleted=incoming_deleted,
            existing_deleted=existing.is_deleted,
        ):
            _apply_attachment_update(db, user_id, existing, item, incoming_deleted)
            attachment_results.append(_result(existing, "APPLIED"))
            continue

        attachment_results.append(_result(existing, "SKIPPED"))

    db.flush()

    for item in payload.flashcardBestResults:
        incoming_deleted = _normalize_delete_flag(item.operation, item.isDeleted)
        existing = db.execute(
            select(FlashcardBestResult).where(
                FlashcardBestResult.user_id == user_id,
                FlashcardBestResult.client_id == item.clientId,
            )
        ).scalar_one_or_none()

        if existing is None:
            if incoming_deleted:
                flashcard_best_result_results.append(_create_skip_result(item.clientId, item.updatedAt))
                continue

            topic_id = _resolve_parent_topic_id(db, user_id, item.topicRemoteId, item.topicClientId)
            existing_by_topic = _find_flashcard_best_result_by_topic(db, user_id, topic_id)
            if existing_by_topic is not None:
                _apply_flashcard_best_result_update(
                    db,
                    user_id,
                    existing_by_topic,
                    item,
                    incoming_deleted=False,
                )
                flashcard_best_result_results.append(_result_for_client(existing_by_topic, item.clientId, "APPLIED"))
                continue

            obj = FlashcardBestResult(
                id=new_id(),
                client_id=item.clientId,
                user_id=user_id,
                topic_id=topic_id,
                questions_count=item.questionsCount,
                duration_ms=item.durationMs,
                completed_at=item.completedAt,
                updated_at=item.updatedAt,
                is_deleted=False,
            )
            db.add(obj)
            db.flush()
            flashcard_best_result_results.append(_result(obj, "APPLIED"))
            continue

        if incoming_deleted:
            if _incoming_should_win(
                item.updatedAt,
                existing.updated_at,
                incoming_deleted=True,
                existing_deleted=existing.is_deleted,
            ):
                _mark_deleted(existing, item.updatedAt)
                flashcard_best_result_results.append(_result(existing, "APPLIED"))
            else:
                flashcard_best_result_results.append(_result(existing, "SKIPPED"))
            continue

        _apply_flashcard_best_result_update(db, user_id, existing, item, incoming_deleted=False)
        flashcard_best_result_results.append(_result(existing, "APPLIED"))

    db.commit()
    bind_extra_context(
        applied_topics=sum(1 for x in topic_results if x.status == "APPLIED"),
        applied_tasks=sum(1 for x in task_results if x.status == "APPLIED"),
        applied_solutions=sum(1 for x in solution_results if x.status == "APPLIED"),
        applied_attachments=sum(1 for x in attachment_results if x.status == "APPLIED"),
        applied_flashcard_best_results=sum(1 for x in flashcard_best_result_results if x.status == "APPLIED"),
    )
    logger.info("Sync push completed")

    return {
        "topics": topic_results,
        "tasks": task_results,
        "solutions": solution_results,
        "attachmentsMetadata": attachment_results,
        "flashcardBestResults": flashcard_best_result_results,
    }


def pull_changes(db: Session, user_id: str, since: datetime | None):
    bind_extra_context(sync_operation='pull', sync_since=since.isoformat() if since is not None else '-')
    logger.info("Sync pull started | user_id=%s | since=%s", user_id, since)
    now = _now()

    topic_stmt = select(Topic).where(Topic.user_id == user_id)
    task_stmt = select(Task).where(Task.user_id == user_id)
    sol_stmt = select(TaskSolution).where(TaskSolution.user_id == user_id)
    att_stmt = select(Attachment).where(Attachment.user_id == user_id)
    flashcard_stmt = select(FlashcardBestResult).where(FlashcardBestResult.user_id == user_id)

    if since is not None:
        topic_stmt = topic_stmt.where(Topic.updated_at > since)
        task_stmt = task_stmt.where(Task.updated_at > since)
        sol_stmt = sol_stmt.where(TaskSolution.updated_at > since)
        att_stmt = att_stmt.where(Attachment.updated_at > since)
        flashcard_stmt = flashcard_stmt.where(FlashcardBestResult.updated_at > since)

    topics = db.execute(topic_stmt.order_by(Topic.updated_at.asc())).scalars().all()
    tasks = db.execute(task_stmt.order_by(Task.updated_at.asc())).scalars().all()
    solutions = db.execute(sol_stmt.order_by(TaskSolution.updated_at.asc())).scalars().all()
    attachments = db.execute(att_stmt.order_by(Attachment.updated_at.asc())).scalars().all()
    flashcard_results = db.execute(
        flashcard_stmt.order_by(FlashcardBestResult.updated_at.asc())
    ).scalars().all()

    task_topic_ids = {x.topic_id for x in tasks}
    flashcard_topic_ids = {x.topic_id for x in flashcard_results}
    all_topic_ids = task_topic_ids | flashcard_topic_ids
    task_owner_ids = {x.owner_task_id for x in attachments}
    solution_task_ids = {x.task_id for x in solutions}
    referenced_task_ids = task_owner_ids | solution_task_ids

    topic_map = {}
    if all_topic_ids:
        for topic in db.execute(select(Topic).where(Topic.id.in_(all_topic_ids))).scalars().all():
            topic_map[topic.id] = topic

    task_map = {}
    if referenced_task_ids:
        for task in db.execute(select(Task).where(Task.id.in_(referenced_task_ids))).scalars().all():
            task_map[task.id] = task

    bind_extra_context(
        pulled_topics=len(topics),
        pulled_tasks=len(tasks),
        pulled_solutions=len(solutions),
        pulled_attachments=len(attachments),
        pulled_flashcard_best_results=len(flashcard_results),
    )
    logger.info("Sync pull completed")

    return {
        "serverTime": now,
        "topics": [
            {
                "remoteId": x.id,
                "clientId": x.client_id,
                "title": x.title,
                "createdAt": x.created_at,
                "updatedAt": x.updated_at,
                "isDeleted": x.is_deleted,
            }
            for x in topics
        ],
        "tasks": [
            {
                "remoteId": x.id,
                "clientId": x.client_id,
                "topicRemoteId": x.topic_id,
                "topicClientId": topic_map[x.topic_id].client_id,
                "title": x.title,
                "description": x.description,
                "status": x.status,
                "deadlineAt": x.deadline_at,
                "createdAt": x.created_at,
                "updatedAt": x.updated_at,
                "isDeleted": x.is_deleted,
            }
            for x in tasks
            if x.topic_id in topic_map
        ],
        "solutions": [
            {
                "remoteId": x.id,
                "clientId": x.client_id,
                "taskRemoteId": x.task_id,
                "taskClientId": task_map[x.task_id].client_id,
                "content": x.content,
                "updatedAt": x.updated_at,
                "isDeleted": x.is_deleted,
            }
            for x in solutions
            if x.task_id in task_map
        ],
        "attachmentsMetadata": [
            {
                "remoteId": x.id,
                "clientId": x.client_id,
                "ownerType": x.owner_type,
                "ownerTaskRemoteId": x.owner_task_id,
                "ownerTaskClientId": task_map[x.owner_task_id].client_id,
                "fileName": x.file_name,
                "mimeType": x.mime_type,
                "sizeBytes": x.size_bytes,
                "createdAt": x.created_at,
                "updatedAt": x.updated_at,
                "isDeleted": x.is_deleted,
                "remoteFileId": x.remote_file_id,
                "storageKey": x.storage_key,
                "uploadState": x.upload_state,
            }
            for x in attachments
            if x.owner_task_id in task_map
        ],
        "flashcardBestResults": [
            {
                "remoteId": x.id,
                "clientId": x.client_id,
                "topicRemoteId": x.topic_id,
                "topicClientId": topic_map[x.topic_id].client_id,
                "questionsCount": x.questions_count,
                "durationMs": x.duration_ms,
                "completedAt": x.completed_at,
                "updatedAt": x.updated_at,
                "isDeleted": x.is_deleted,
            }
            for x in flashcard_results
            if x.topic_id in topic_map
        ],
        "nextSince": now,
    }


def handle_initial_sync(db: Session, user_id: str, payload: InitialSyncRequest):
    bind_extra_context(sync_operation='initial', pull_since=payload.pullSince.isoformat() if payload.pullSince else '-')
    logger.info("Initial sync started")
    push_result = apply_push(db, user_id, payload.push)
    pull_result = pull_changes(db, user_id, payload.pullSince)
    logger.info("Initial sync completed")
    return {
        "push": push_result,
        "pull": PullResponse.model_validate(pull_result),
    }



class SqlSyncRepository:
    def __init__(self, db: Session):
        self.db = db

    def apply_push(self, user_id: str, payload: PushRequest):
        return apply_push(self.db, user_id, payload)

    def pull_changes(self, user_id: str, since: datetime | None):
        return pull_changes(self.db, user_id, since)

    def handle_initial_sync(self, user_id: str, payload: InitialSyncRequest):
        return handle_initial_sync(self.db, user_id, payload)
