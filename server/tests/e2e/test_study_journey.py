"""No GUI. All calls go via the running FastAPI server and real database."""
from datetime import datetime, timedelta, timezone
from uuid import uuid4

import pytest
import requests

pytestmark = pytest.mark.e2e


def check(response, status=200):
    assert response.status_code == status, f"{response.request.method} {response.url}: {response.status_code} {response.text[:500]}"
    return response.json()


def auth_headers(access):
    return {"Authorization": f"Bearer {access}"}


def timestamp(offset):
    return (datetime.now(timezone.utc) + timedelta(seconds=offset)).isoformat()


def test_student_mvp_register_login_sync_task_update_delete_logout(api_ready):
    """Demonstration: registration -> authentication -> learning data sync -> logout."""
    api = api_ready
    unique = uuid4().hex
    email = f"e2e-{unique}@example.com"
    password = "StudyMateTest123!"
    topic = f"topic-{unique}"
    task = f"task-{unique}"
    solution = f"solution-{unique}"
    http = requests.Session()
    http.headers["X-Request-ID"] = f"lab2-e2e-{unique[:12]}"

    # Arrange: unique user, dedicated disposable DB restored by compose cleanup.
    user = check(http.post(f"{api}/api/v1/auth/register", json={"email": email, "password": password}, timeout=15))
    tokens = check(http.post(f"{api}/api/v1/auth/login", json={"email": email, "password": password}, timeout=15))
    http.headers.update(auth_headers(tokens["accessToken"]))
    assert check(http.get(f"{api}/api/v1/auth/me", timeout=15))["id"] == user["id"]

    # Act: one initial sync creates a topic, task and solution in one HTTP request.
    initial = check(http.post(f"{api}/api/v1/sync/initial", json={"push": {
        "topics": [{"clientId": topic, "title": "Programming", "operation": "CREATE", "updatedAt": timestamp(0)}],
        "tasks": [{"clientId": task, "topicClientId": topic, "title": "Integration labs", "description": "Task from mobile client", "status": "PLANNED", "operation": "CREATE", "updatedAt": timestamp(1)}],
        "solutions": [{"clientId": solution, "taskClientId": task, "content": "First attempt", "operation": "CREATE", "updatedAt": timestamp(2)}],
    }}, timeout=15))

    # Assert first server-to-client state.
    assert initial["push"]["topics"][0]["status"] == "APPLIED"
    assert initial["push"]["tasks"][0]["status"] == "APPLIED"
    assert initial["push"]["solutions"][0]["status"] == "APPLIED"
    pulled = check(http.get(f"{api}/api/v1/sync/pull", timeout=15))
    assert next(row for row in pulled["topics"] if row["clientId"] == topic)["title"] == "Programming"
    assert next(row for row in pulled["tasks"] if row["clientId"] == task)["status"] == "PLANNED"

    # Act: change the task and then remove its topic (cascade tombstone).
    result = check(http.post(f"{api}/api/v1/sync/push", json={"tasks": [{
        "clientId": task, "topicClientId": topic, "title": "Integration labs: done",
        "description": "Reviewed", "status": "DONE", "updatedAt": timestamp(10), "operation": "UPDATE"
    }]}, timeout=15))
    assert result["tasks"][0]["status"] == "APPLIED"
    assert next(row for row in check(http.get(f"{api}/api/v1/sync/pull", timeout=15))["tasks"] if row["clientId"] == task)["status"] == "DONE"
    deleted = check(http.post(f"{api}/api/v1/sync/push", json={"topics": [{
        "clientId": topic, "operation": "DELETE", "updatedAt": timestamp(20)
    }]}, timeout=15))
    assert deleted["topics"][0]["status"] == "APPLIED"
    final_state = check(http.get(f"{api}/api/v1/sync/pull", timeout=15))
    assert next(row for row in final_state["topics"] if row["clientId"] == topic)["isDeleted"] is True
    assert next(row for row in final_state["tasks"] if row["clientId"] == task)["isDeleted"] is True
    assert next(row for row in final_state["solutions"] if row["clientId"] == solution)["isDeleted"] is True

    # Act/Assert: refresh rotates and logout invalidates refresh sessions.
    refresh = check(http.post(f"{api}/api/v1/auth/refresh", json={"refreshToken": tokens["refreshToken"]}, timeout=15))
    check(http.post(f"{api}/api/v1/auth/logout", json={"refreshToken": refresh["refreshToken"]}, timeout=15))
    check(http.post(f"{api}/api/v1/auth/refresh", json={"refreshToken": refresh["refreshToken"]}, timeout=15), 401)
    http.close()


def test_http_user_isolation_and_unauthorized_sync(api_ready):
    api = api_ready
    id_ = uuid4().hex
    one = requests.Session()
    two = requests.Session()
    try:
        check(one.get(f"{api}/api/v1/sync/pull", timeout=15), 401)
        for session, suffix in ((one, "one"), (two, "two")):
            email = f"{suffix}-{id_}@example.com"
            check(session.post(f"{api}/api/v1/auth/register", json={"email": email, "password": "StudyMateTest123!"}, timeout=15))
            tokens = check(session.post(f"{api}/api/v1/auth/login", json={"email": email, "password": "StudyMateTest123!"}, timeout=15))
            session.headers.update(auth_headers(tokens["accessToken"]))
        check(one.post(f"{api}/api/v1/sync/push", json={"topics": [{
            "clientId": f"private-{id_}", "title": "For user one", "operation": "CREATE", "updatedAt": timestamp(1)
        }]}, timeout=15))
        result = check(two.get(f"{api}/api/v1/sync/pull", timeout=15))
        assert not any(row["clientId"] == f"private-{id_}" for row in result["topics"])
    finally:
        one.close()
        two.close()


def test_file_attachment_roundtrip_through_http_and_storage(api_ready):
    api = api_ready
    id_ = uuid4().hex
    file_content = b"StudyMate lab2 storage roundtrip\x00\x01\x02"
    with requests.Session() as session:
        email = f"file-{id_}@example.com"
        check(session.post(f"{api}/api/v1/auth/register", json={"email": email, "password": "StudyMateTest123!"}, timeout=15))
        tokens = check(session.post(f"{api}/api/v1/auth/login", json={"email": email, "password": "StudyMateTest123!"}, timeout=15))
        session.headers.update(auth_headers(tokens["accessToken"]))
        created = check(session.post(f"{api}/api/v1/sync/push", json={
            "topics": [{"clientId": f"topic-{id_}", "title": "Files", "updatedAt": timestamp(0)}],
            "tasks": [{"clientId": f"task-{id_}", "topicClientId": f"topic-{id_}", "title": "Attach document", "updatedAt": timestamp(1)}],
            "attachmentsMetadata": [{
                "clientId": f"attachment-{id_}", "ownerType": "TASK_DESCRIPTION", "ownerTaskClientId": f"task-{id_}",
                "fileName": "proof.bin", "mimeType": "application/octet-stream", "sizeBytes": len(file_content), "updatedAt": timestamp(2),
            }],
        }, timeout=15))
        attachment_id = created["attachmentsMetadata"][0]["remoteId"]
        uploaded = check(session.post(f"{api}/api/v1/attachments/{attachment_id}/upload", files={
            "file": ("proof.bin", file_content, "application/octet-stream")
        }, timeout=15))
        assert uploaded["uploadState"] == "UPLOADED"
        assert uploaded["sizeBytes"] == len(file_content)
        downloaded = session.get(f"{api}/api/v1/attachments/{attachment_id}/download", timeout=15)
        assert downloaded.status_code == 200
        assert downloaded.content == file_content
