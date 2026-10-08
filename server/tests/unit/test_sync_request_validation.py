"""Fast feedback: request validation unit tests, no DB, HTTP, or mocks."""
from datetime import datetime, timezone
from uuid import uuid4

import pytest
from pydantic import ValidationError

from app.schemas.sync import PushRequest, InitialSyncRequest

pytestmark = pytest.mark.unit


def valid_time():
    return datetime.now(timezone.utc).isoformat()


def test_empty_push_is_valid():
    result = PushRequest.model_validate({})
    assert not result.topics and not result.tasks


def test_missing_topic_title_is_rejected():
    with pytest.raises(ValidationError, match="title is required"):
        PushRequest.model_validate({"topics": [{"clientId": "test", "updatedAt": valid_time()}]})


def test_task_without_parent_is_rejected():
    with pytest.raises(ValidationError, match="topicClientId or topicRemoteId"):
        PushRequest.model_validate({"tasks": [{"clientId": "test", "title": "Task", "updatedAt": valid_time()}]})


def test_invalid_status_is_rejected():
    with pytest.raises(ValidationError, match="status must be one of"):
        PushRequest.model_validate({"tasks": [{"clientId": "test", "topicClientId": "topic", "title": "Task", "status": "NO_STATUS", "updatedAt": valid_time()}]})


def test_naive_timestamp_is_rejected():
    with pytest.raises(ValidationError, match="timezone"):
        PushRequest.model_validate({"topics": [{"clientId": "topic", "title": "Topic", "updatedAt": "2026-10-08T10:00:00"}]})


def test_delete_does_not_require_title():
    item = PushRequest.model_validate({"topics": [{"clientId": "topic", "operation": "DELETE", "updatedAt": valid_time()}]}).topics[0]
    assert item.is_delete_operation()


def test_best_result_requires_score_and_completion_time():
    with pytest.raises(ValidationError, match="questionsCount"):
        PushRequest.model_validate({"flashcardBestResults": [{"clientId": "score", "topicClientId": "topic", "updatedAt": valid_time()}]})


def test_initial_sync_rejects_naive_pull_since():
    with pytest.raises(ValidationError, match="timezone"):
        InitialSyncRequest.model_validate({"pullSince": "2026-10-08T10:00:00"})
