"""Business rules and SQL transactions tested without mocks using actual PostgreSQL."""
from datetime import datetime, timedelta, timezone

import pytest
from fastapi import HTTPException
from sqlalchemy import func, select

from app.models.study import Task, Topic, TaskSolution, FlashcardBestResult
from app.repositories.sql.sync_repository import SqlSyncRepository
from app.schemas.sync import PushRequest
from app.services.sync_service import SyncService

pytestmark = pytest.mark.integration


def ts(offset=0):
    return datetime(2026, 10, 1, 10, tzinfo=timezone.utc) + timedelta(seconds=offset)


def payload(**collections):
    return PushRequest.model_validate(collections)


def create_topic_and_task(service, user_id, tag):
    return service.apply_push(user_id, payload(
        topics=[{"clientId": f"topic-{tag}", "title": "Algebra", "operation": "CREATE", "updatedAt": ts().isoformat()}],
        tasks=[{"clientId": f"task-{tag}", "topicClientId": f"topic-{tag}", "title": "Equation", "status": "PLANNED", "operation": "CREATE", "updatedAt": ts().isoformat()}],
    ))


def test_create_topic_task_solution_persists_and_pull_has_relationships(sql_session, new_user, unique_id):
    # Arrange
    user_id = new_user().id
    tag = unique_id("chain")
    service = SyncService(SqlSyncRepository(sql_session))

    # Act
    created = create_topic_and_task(service, user_id, tag)
    solved = service.apply_push(user_id, payload(solutions=[{
        "clientId": f"solution-{tag}", "taskClientId": f"task-{tag}",
        "content": "x = 2", "operation": "CREATE", "updatedAt": ts(1).isoformat(),
    }]))
    pulled = service.pull_changes(user_id, None)

    # Assert: HTTP DTO is derived from actual SQL rows, not a stub.
    assert created["topics"][0].status == "APPLIED"
    assert created["tasks"][0].status == "APPLIED"
    assert solved["solutions"][0].status == "APPLIED"
    assert next(item for item in pulled["topics"] if item["clientId"] == f"topic-{tag}")["title"] == "Algebra"
    assert next(item for item in pulled["tasks"] if item["clientId"] == f"task-{tag}")["topicClientId"] == f"topic-{tag}"
    assert next(item for item in pulled["solutions"] if item["clientId"] == f"solution-{tag}")["content"] == "x = 2"
    assert sql_session.scalar(select(func.count()).select_from(TaskSolution).where(TaskSolution.user_id == user_id)) == 1


def test_stale_update_is_skipped_and_newer_one_is_saved(sql_session, new_user, unique_id):
    # Arrange
    user_id = new_user().id
    tag = unique_id("timestamp")
    service = SyncService(SqlSyncRepository(sql_session))
    create_topic_and_task(service, user_id, tag)

    # Act: more recent mutation first, then stale mutation.
    updated = service.apply_push(user_id, payload(topics=[{
        "clientId": f"topic-{tag}", "title": "New", "updatedAt": ts(20).isoformat(),
    }]))
    stale = service.apply_push(user_id, payload(topics=[{
        "clientId": f"topic-{tag}", "title": "Old", "updatedAt": ts(10).isoformat(),
    }]))

    # Assert
    assert updated["topics"][0].status == "APPLIED"
    assert stale["topics"][0].status == "SKIPPED"
    saved = sql_session.scalar(select(Topic).where(Topic.user_id == user_id, Topic.client_id == f"topic-{tag}"))
    assert saved.title == "New"
    assert sql_session.scalar(select(func.count()).select_from(Topic).where(Topic.user_id == user_id)) == 1


def test_topic_delete_cascades_to_task_and_solution(sql_session, new_user, unique_id):
    # Arrange
    user_id = new_user().id
    tag = unique_id("delete")
    service = SyncService(SqlSyncRepository(sql_session))
    create_topic_and_task(service, user_id, tag)
    service.apply_push(user_id, payload(solutions=[{
        "clientId": f"solution-{tag}", "taskClientId": f"task-{tag}", "content": "Done", "updatedAt": ts(1).isoformat()
    }]))

    # Act
    service.apply_push(user_id, payload(topics=[{
        "clientId": f"topic-{tag}", "operation": "DELETE", "updatedAt": ts(30).isoformat(),
    }]))

    # Assert
    topic = sql_session.scalar(select(Topic).where(Topic.user_id == user_id, Topic.client_id == f"topic-{tag}"))
    task = sql_session.scalar(select(Task).where(Task.user_id == user_id, Task.client_id == f"task-{tag}"))
    solution = sql_session.scalar(select(TaskSolution).where(TaskSolution.user_id == user_id, TaskSolution.client_id == f"solution-{tag}"))
    assert topic.is_deleted and task.is_deleted and solution.is_deleted
    assert all(row["isDeleted"] for row in service.pull_changes(user_id, None)["tasks"])


def test_failing_batch_rolls_back_all_its_previous_inserts(sql_session, new_user, unique_id):
    # Arrange
    user_id = new_user().id
    tag = unique_id("atomic")
    service = SyncService(SqlSyncRepository(sql_session))

    # Act: first entry is valid, but task references a non-existent topic.
    with pytest.raises(HTTPException) as error:
        service.apply_push(user_id, payload(
            topics=[{"clientId": f"topic-{tag}", "title": "Should roll back", "updatedAt": ts().isoformat()}],
            tasks=[{"clientId": f"task-{tag}", "title": "Invalid parent", "topicClientId": f"absent-{tag}", "updatedAt": ts().isoformat()}]
        ))
    sql_session.rollback()  # Request-scoped dependency would close/rollback on exception.

    # Assert
    assert error.value.status_code == 400
    assert sql_session.scalar(select(func.count()).select_from(Topic).where(Topic.user_id == user_id)) == 0


def test_users_cannot_reference_each_others_topics(sql_session, new_user, unique_id):
    # Arrange
    owner = new_user().id
    stranger = new_user().id
    tag = unique_id("private")
    service = SyncService(SqlSyncRepository(sql_session))
    topic_id = create_topic_and_task(service, owner, tag)["topics"][0].remoteId

    # Act
    with pytest.raises(HTTPException) as error:
        service.apply_push(stranger, payload(tasks=[{
            "clientId": f"intruder-{tag}", "title": "Leak", "topicRemoteId": topic_id, "updatedAt": ts(3).isoformat()
        }]))
    sql_session.rollback()

    # Assert
    assert error.value.status_code == 400
    assert sql_session.scalar(select(func.count()).select_from(Task).where(Task.user_id == stranger)) == 0


def test_flashcard_best_result_merges_highest_score(sql_session, new_user, unique_id):
    # Arrange
    user_id = new_user().id
    tag = unique_id("cards")
    service = SyncService(SqlSyncRepository(sql_session))
    create_topic_and_task(service, user_id, tag)

    # Act
    def push_score(count, duration, offset):
        return service.apply_push(user_id, payload(flashcardBestResults=[{
            "clientId": f"best-{tag}", "topicClientId": f"topic-{tag}",
            "questionsCount": count, "durationMs": duration, "completedAt": ts(offset).isoformat(),
            "updatedAt": ts(offset).isoformat(),
        }]))
    push_score(10, 5000, 1)
    push_score(8, 1000, 2)
    push_score(10, 4500, 3)

    # Assert
    result = sql_session.scalar(select(FlashcardBestResult).where(FlashcardBestResult.user_id == user_id))
    assert result.questions_count == 10
    assert result.duration_ms == 4500
    assert sql_session.scalar(select(func.count()).select_from(FlashcardBestResult).where(FlashcardBestResult.user_id == user_id)) == 1
