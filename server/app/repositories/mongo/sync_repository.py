import logging
from datetime import datetime, timezone
from typing import Any

from fastapi import HTTPException
from pymongo import ASCENDING
from pymongo.database import Database

from app.core.logging import bind_extra_context
from app.schemas.sync import InitialSyncRequest, PullResponse, PushRequest, PushResultItem
from app.utils.ids import new_id

ALLOWED_ATTACHMENT_OWNER_TYPES = {"TASK_DESCRIPTION", "TASK_SOLUTION"}
logger = logging.getLogger(__name__)


def _now() -> datetime:
    return datetime.now(timezone.utc)


def _normalize_delete_flag(operation: str, is_deleted: bool) -> bool:
    return operation == "DELETE" or is_deleted


def _incoming_should_win(incoming_updated_at: datetime, existing_updated_at: datetime, *, incoming_deleted: bool, existing_deleted: bool) -> bool:
    if incoming_updated_at > existing_updated_at:
        return True
    if incoming_updated_at < existing_updated_at:
        return False
    return incoming_deleted and not existing_deleted


def _result(item: dict[str, Any], status: str) -> PushResultItem:
    return PushResultItem(clientId=item["client_id"], remoteId=item["id"], serverUpdatedAt=item["updated_at"], status=status)


def _result_for_client(item: dict[str, Any], client_id: str, status: str) -> PushResultItem:
    return PushResultItem(clientId=client_id, remoteId=item["id"], serverUpdatedAt=item["updated_at"], status=status)


def _create_skip_result(client_id: str, updated_at: datetime) -> PushResultItem:
    return PushResultItem(clientId=client_id, remoteId="", serverUpdatedAt=updated_at, status="SKIPPED")


def _set(db: Database, collection: str, doc_id: str, fields: dict[str, Any]) -> None:
    db[collection].update_one({"id": doc_id}, {"$set": fields})


def _mark_deleted(db: Database, collection: str, existing: dict[str, Any], incoming_updated_at: datetime) -> dict[str, Any]:
    existing = dict(existing)
    existing["is_deleted"] = True
    existing["updated_at"] = incoming_updated_at
    _set(db, collection, existing["id"], {"is_deleted": True, "updated_at": incoming_updated_at})
    return existing


def _validate_attachment_owner_type(owner_type: str) -> None:
    if owner_type not in ALLOWED_ATTACHMENT_OWNER_TYPES:
        raise HTTPException(status_code=400, detail=f"Invalid attachment ownerType. Expected one of {sorted(ALLOWED_ATTACHMENT_OWNER_TYPES)}, got {owner_type!r}")


def _resolve_parent_topic_id(db: Database, user_id: str, topic_remote_id: str | None, topic_client_id: str | None, *, allow_deleted: bool = False) -> str:
    topic = None
    if topic_remote_id:
        topic = db.topics.find_one({"id": topic_remote_id, "user_id": user_id})
    if topic is None and topic_client_id:
        topic = db.topics.find_one({"user_id": user_id, "client_id": topic_client_id})
    if topic is None:
        raise HTTPException(status_code=400, detail=f"Topic reference not found: remote={topic_remote_id}, client={topic_client_id}")
    if topic.get("is_deleted") and not allow_deleted:
        raise HTTPException(status_code=409, detail="Referenced topic is deleted")
    return topic["id"]


def _resolve_parent_task_id(db: Database, user_id: str, task_remote_id: str | None, task_client_id: str | None, *, allow_deleted: bool = False) -> str:
    task = None
    if task_remote_id:
        task = db.tasks.find_one({"id": task_remote_id, "user_id": user_id})
    if task is None and task_client_id:
        task = db.tasks.find_one({"user_id": user_id, "client_id": task_client_id})
    if task is None:
        raise HTTPException(status_code=400, detail=f"Task reference not found: remote={task_remote_id}, client={task_client_id}")
    if task.get("is_deleted") and not allow_deleted:
        raise HTTPException(status_code=409, detail="Referenced task is deleted")
    return task["id"]


def _cascade_delete_task(db: Database, task: dict[str, Any], deleted_at: datetime) -> None:
    _set(db, "tasks", task["id"], {"is_deleted": True, "updated_at": deleted_at})
    for solution in db.task_solutions.find({"task_id": task["id"]}):
        if solution["updated_at"] <= deleted_at or not solution.get("is_deleted", False):
            _set(db, "task_solutions", solution["id"], {"is_deleted": True, "updated_at": deleted_at})
    for attachment in db.attachments.find({"owner_task_id": task["id"]}):
        if attachment["updated_at"] <= deleted_at or not attachment.get("is_deleted", False):
            _set(db, "attachments", attachment["id"], {"is_deleted": True, "updated_at": deleted_at})


def _cascade_delete_topic(db: Database, topic_id: str, deleted_at: datetime) -> None:
    for task in db.tasks.find({"topic_id": topic_id}):
        if task["updated_at"] <= deleted_at or not task.get("is_deleted", False):
            _cascade_delete_task(db, task, deleted_at)
    for result in db.flashcard_best_results.find({"topic_id": topic_id}):
        if result["updated_at"] <= deleted_at or not result.get("is_deleted", False):
            _set(db, "flashcard_best_results", result["id"], {"is_deleted": True, "updated_at": deleted_at})


def _flashcard_result_is_better(new_questions_count: int, new_duration_ms: int, existing_questions_count: int, existing_duration_ms: int) -> bool:
    if new_questions_count != existing_questions_count:
        return new_questions_count > existing_questions_count
    return new_duration_ms < existing_duration_ms


def _apply_flashcard_best_result_update(db: Database, user_id: str, existing: dict[str, Any], item, incoming_deleted: bool) -> dict[str, Any]:
    if incoming_deleted:
        return _mark_deleted(db, "flashcard_best_results", existing, item.updatedAt)
    topic_id = _resolve_parent_topic_id(db, user_id, item.topicRemoteId, item.topicClientId)
    incoming_is_better = _flashcard_result_is_better(item.questionsCount, item.durationMs, existing["questions_count"], existing["duration_ms"])
    existing = dict(existing)
    if incoming_is_better or existing.get("is_deleted", False):
        fields = {"topic_id": topic_id, "questions_count": item.questionsCount, "duration_ms": item.durationMs, "completed_at": item.completedAt, "updated_at": item.updatedAt, "is_deleted": False}
        existing.update(fields)
        _set(db, "flashcard_best_results", existing["id"], fields)
    elif item.updatedAt > existing["updated_at"]:
        existing["updated_at"] = item.updatedAt
        _set(db, "flashcard_best_results", existing["id"], {"updated_at": item.updatedAt})
    return existing


def apply_push(db: Database, user_id: str, payload: PushRequest):
    bind_extra_context(sync_operation="push", sync_topics=len(payload.topics), sync_tasks=len(payload.tasks), sync_solutions=len(payload.solutions), sync_attachments=len(payload.attachmentsMetadata), sync_flashcard_best_results=len(payload.flashcardBestResults))
    logger.info("Mongo sync push started | user_id=%s", user_id)
    topic_results, task_results, solution_results, attachment_results, flashcard_results = [], [], [], [], []

    for item in payload.topics:
        incoming_deleted = _normalize_delete_flag(item.operation, item.isDeleted)
        existing = db.topics.find_one({"user_id": user_id, "client_id": item.clientId})
        if existing is None:
            if incoming_deleted:
                topic_results.append(_create_skip_result(item.clientId, item.updatedAt)); continue
            obj = {"id": new_id(), "client_id": item.clientId, "user_id": user_id, "title": item.title, "created_at": item.updatedAt, "updated_at": item.updatedAt, "is_deleted": False}
            db.topics.insert_one(obj); topic_results.append(_result(obj, "APPLIED")); continue
        if _incoming_should_win(item.updatedAt, existing["updated_at"], incoming_deleted=incoming_deleted, existing_deleted=existing.get("is_deleted", False)):
            if incoming_deleted:
                existing = _mark_deleted(db, "topics", existing, item.updatedAt); _cascade_delete_topic(db, existing["id"], item.updatedAt)
            else:
                fields = {"title": item.title, "updated_at": item.updatedAt, "is_deleted": False}; existing.update(fields); _set(db, "topics", existing["id"], fields)
            topic_results.append(_result(existing, "APPLIED")); continue
        topic_results.append(_result(existing, "SKIPPED"))

    for item in payload.tasks:
        incoming_deleted = _normalize_delete_flag(item.operation, item.isDeleted)
        existing = db.tasks.find_one({"user_id": user_id, "client_id": item.clientId})
        if existing is None:
            if incoming_deleted and not (item.topicRemoteId or item.topicClientId):
                task_results.append(_create_skip_result(item.clientId, item.updatedAt)); continue
            topic_id = _resolve_parent_topic_id(db, user_id, item.topicRemoteId, item.topicClientId, allow_deleted=incoming_deleted)
            obj = {"id": new_id(), "client_id": item.clientId, "user_id": user_id, "topic_id": topic_id, "title": item.title or "", "description": item.description, "status": item.status, "deadline_at": item.deadlineAt, "created_at": item.updatedAt, "updated_at": item.updatedAt, "is_deleted": incoming_deleted}
            db.tasks.insert_one(obj); task_results.append(_result(obj, "APPLIED")); continue
        if _incoming_should_win(item.updatedAt, existing["updated_at"], incoming_deleted=incoming_deleted, existing_deleted=existing.get("is_deleted", False)):
            if incoming_deleted:
                _cascade_delete_task(db, existing, item.updatedAt); existing.update({"is_deleted": True, "updated_at": item.updatedAt})
            else:
                topic_id = _resolve_parent_topic_id(db, user_id, item.topicRemoteId, item.topicClientId)
                fields = {"topic_id": topic_id, "title": item.title, "description": item.description, "status": item.status, "deadline_at": item.deadlineAt, "updated_at": item.updatedAt, "is_deleted": False}; existing.update(fields); _set(db, "tasks", existing["id"], fields)
            task_results.append(_result(existing, "APPLIED")); continue
        task_results.append(_result(existing, "SKIPPED"))

    for item in payload.solutions:
        incoming_deleted = _normalize_delete_flag(item.operation, item.isDeleted)
        existing = db.task_solutions.find_one({"user_id": user_id, "client_id": item.clientId})
        if existing is None:
            if incoming_deleted and not (item.taskRemoteId or item.taskClientId):
                solution_results.append(_create_skip_result(item.clientId, item.updatedAt)); continue
            task_id = _resolve_parent_task_id(db, user_id, item.taskRemoteId, item.taskClientId, allow_deleted=incoming_deleted)
            obj = {"id": new_id(), "client_id": item.clientId, "user_id": user_id, "task_id": task_id, "content": item.content, "updated_at": item.updatedAt, "is_deleted": incoming_deleted}
            db.task_solutions.insert_one(obj); solution_results.append(_result(obj, "APPLIED")); continue
        if _incoming_should_win(item.updatedAt, existing["updated_at"], incoming_deleted=incoming_deleted, existing_deleted=existing.get("is_deleted", False)):
            if incoming_deleted:
                existing = _mark_deleted(db, "task_solutions", existing, item.updatedAt)
            else:
                task_id = _resolve_parent_task_id(db, user_id, item.taskRemoteId, item.taskClientId)
                fields = {"task_id": task_id, "content": item.content, "updated_at": item.updatedAt, "is_deleted": False}; existing.update(fields); _set(db, "task_solutions", existing["id"], fields)
            solution_results.append(_result(existing, "APPLIED")); continue
        solution_results.append(_result(existing, "SKIPPED"))

    for item in payload.attachmentsMetadata:
        incoming_deleted = _normalize_delete_flag(item.operation, item.isDeleted)
        existing = db.attachments.find_one({"user_id": user_id, "client_id": item.clientId})
        if existing is None:
            if incoming_deleted and not (item.ownerTaskRemoteId or item.ownerTaskClientId):
                attachment_results.append(_create_skip_result(item.clientId, item.updatedAt)); continue
            if item.ownerType is None:
                raise HTTPException(status_code=400, detail="ownerType is required when creating an attachment")
            _validate_attachment_owner_type(item.ownerType)
            owner_task_id = _resolve_parent_task_id(db, user_id, item.ownerTaskRemoteId, item.ownerTaskClientId, allow_deleted=incoming_deleted)
            obj = {"id": new_id(), "client_id": item.clientId, "user_id": user_id, "owner_type": item.ownerType, "owner_task_id": owner_task_id, "file_name": item.fileName or "", "mime_type": item.mimeType, "size_bytes": item.sizeBytes, "created_at": item.updatedAt, "updated_at": item.updatedAt, "is_deleted": incoming_deleted, "remote_file_id": item.remoteFileId, "storage_key": None, "upload_state": item.uploadState}
            db.attachments.insert_one(obj); attachment_results.append(_result(obj, "APPLIED")); continue
        if _incoming_should_win(item.updatedAt, existing["updated_at"], incoming_deleted=incoming_deleted, existing_deleted=existing.get("is_deleted", False)):
            if incoming_deleted:
                existing = _mark_deleted(db, "attachments", existing, item.updatedAt)
            else:
                if item.ownerType is not None:
                    _validate_attachment_owner_type(item.ownerType)
                owner_task_id = _resolve_parent_task_id(db, user_id, item.ownerTaskRemoteId, item.ownerTaskClientId)
                fields = {"owner_type": item.ownerType or existing.get("owner_type"), "owner_task_id": owner_task_id, "file_name": item.fileName, "mime_type": item.mimeType, "size_bytes": item.sizeBytes, "updated_at": item.updatedAt, "is_deleted": False, "remote_file_id": item.remoteFileId, "upload_state": item.uploadState}; existing.update(fields); _set(db, "attachments", existing["id"], fields)
            attachment_results.append(_result(existing, "APPLIED")); continue
        attachment_results.append(_result(existing, "SKIPPED"))

    for item in payload.flashcardBestResults:
        incoming_deleted = _normalize_delete_flag(item.operation, item.isDeleted)
        existing = db.flashcard_best_results.find_one({"user_id": user_id, "client_id": item.clientId})
        if existing is None:
            if incoming_deleted:
                flashcard_results.append(_create_skip_result(item.clientId, item.updatedAt)); continue
            topic_id = _resolve_parent_topic_id(db, user_id, item.topicRemoteId, item.topicClientId)
            existing_by_topic = db.flashcard_best_results.find_one({"user_id": user_id, "topic_id": topic_id})
            if existing_by_topic is not None:
                existing_by_topic = _apply_flashcard_best_result_update(db, user_id, existing_by_topic, item, False)
                flashcard_results.append(_result_for_client(existing_by_topic, item.clientId, "APPLIED")); continue
            obj = {"id": new_id(), "client_id": item.clientId, "user_id": user_id, "topic_id": topic_id, "questions_count": item.questionsCount, "duration_ms": item.durationMs, "completed_at": item.completedAt, "updated_at": item.updatedAt, "is_deleted": False}
            db.flashcard_best_results.insert_one(obj); flashcard_results.append(_result(obj, "APPLIED")); continue
        if incoming_deleted:
            if _incoming_should_win(item.updatedAt, existing["updated_at"], incoming_deleted=True, existing_deleted=existing.get("is_deleted", False)):
                existing = _mark_deleted(db, "flashcard_best_results", existing, item.updatedAt); flashcard_results.append(_result(existing, "APPLIED"))
            else:
                flashcard_results.append(_result(existing, "SKIPPED"))
            continue
        existing = _apply_flashcard_best_result_update(db, user_id, existing, item, False)
        flashcard_results.append(_result(existing, "APPLIED"))

    logger.info("Mongo sync push completed")
    return {"topics": topic_results, "tasks": task_results, "solutions": solution_results, "attachmentsMetadata": attachment_results, "flashcardBestResults": flashcard_results}


def _changed_query(user_id: str, since: datetime | None) -> dict[str, Any]:
    query: dict[str, Any] = {"user_id": user_id}
    if since is not None:
        query["updated_at"] = {"$gt": since}
    return query


def _by_id_map(db: Database, collection: str, ids: set[str]) -> dict[str, dict[str, Any]]:
    if not ids:
        return {}
    return {x["id"]: x for x in db[collection].find({"id": {"$in": list(ids)}})}


def pull_changes(db: Database, user_id: str, since: datetime | None):
    bind_extra_context(sync_operation="pull", sync_since=since.isoformat() if since is not None else "-")
    now = _now()
    sort = [("updated_at", ASCENDING)]
    topics = list(db.topics.find(_changed_query(user_id, since)).sort(sort))
    tasks = list(db.tasks.find(_changed_query(user_id, since)).sort(sort))
    solutions = list(db.task_solutions.find(_changed_query(user_id, since)).sort(sort))
    attachments = list(db.attachments.find(_changed_query(user_id, since)).sort(sort))
    flashcard_results = list(db.flashcard_best_results.find(_changed_query(user_id, since)).sort(sort))
    topic_map = _by_id_map(db, "topics", {x["topic_id"] for x in tasks} | {x["topic_id"] for x in flashcard_results})
    task_map = _by_id_map(db, "tasks", {x["owner_task_id"] for x in attachments} | {x["task_id"] for x in solutions})
    logger.info("Mongo sync pull completed")
    return {
        "serverTime": now,
        "topics": [{"remoteId": x["id"], "clientId": x["client_id"], "title": x["title"], "createdAt": x["created_at"], "updatedAt": x["updated_at"], "isDeleted": x["is_deleted"]} for x in topics],
        "tasks": [{"remoteId": x["id"], "clientId": x["client_id"], "topicRemoteId": x["topic_id"], "topicClientId": topic_map[x["topic_id"]]["client_id"], "title": x["title"], "description": x.get("description"), "status": x["status"], "deadlineAt": x.get("deadline_at"), "createdAt": x["created_at"], "updatedAt": x["updated_at"], "isDeleted": x["is_deleted"]} for x in tasks if x["topic_id"] in topic_map],
        "solutions": [{"remoteId": x["id"], "clientId": x["client_id"], "taskRemoteId": x["task_id"], "taskClientId": task_map[x["task_id"]]["client_id"], "content": x.get("content"), "updatedAt": x["updated_at"], "isDeleted": x["is_deleted"]} for x in solutions if x["task_id"] in task_map],
        "attachmentsMetadata": [{"remoteId": x["id"], "clientId": x["client_id"], "ownerType": x["owner_type"], "ownerTaskRemoteId": x["owner_task_id"], "ownerTaskClientId": task_map[x["owner_task_id"]]["client_id"], "fileName": x["file_name"], "mimeType": x.get("mime_type"), "sizeBytes": x.get("size_bytes"), "createdAt": x["created_at"], "updatedAt": x["updated_at"], "isDeleted": x["is_deleted"], "remoteFileId": x.get("remote_file_id"), "storageKey": x.get("storage_key"), "uploadState": x.get("upload_state")} for x in attachments if x["owner_task_id"] in task_map],
        "flashcardBestResults": [{"remoteId": x["id"], "clientId": x["client_id"], "topicRemoteId": x["topic_id"], "topicClientId": topic_map[x["topic_id"]]["client_id"], "questionsCount": x["questions_count"], "durationMs": x["duration_ms"], "completedAt": x["completed_at"], "updatedAt": x["updated_at"], "isDeleted": x["is_deleted"]} for x in flashcard_results if x["topic_id"] in topic_map],
        "nextSince": now,
    }


def handle_initial_sync(db: Database, user_id: str, payload: InitialSyncRequest):
    bind_extra_context(sync_operation="initial", pull_since=payload.pullSince.isoformat() if payload.pullSince else "-")
    push_result = apply_push(db, user_id, payload.push)
    pull_result = pull_changes(db, user_id, payload.pullSince)
    return {"push": push_result, "pull": PullResponse.model_validate(pull_result)}



class MongoSyncRepository:
    def __init__(self, db):
        self.db = db

    def apply_push(self, user_id: str, payload):
        return apply_push(self.db, user_id, payload)

    def pull_changes(self, user_id: str, since):
        return pull_changes(self.db, user_id, since)

    def handle_initial_sync(self, user_id: str, payload):
        return handle_initial_sync(self.db, user_id, payload)
