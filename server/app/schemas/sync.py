from datetime import datetime, timezone
from typing import Literal

from pydantic import BaseModel, Field, field_validator, model_validator


ALLOWED_TASK_STATUSES = {"PLANNED", "IN_PROGRESS", "DONE", "ARCHIVED"}

Operation = Literal["CREATE", "UPDATE", "DELETE"]


def _require_timezone(value: datetime) -> datetime:
    if value.tzinfo is None:
        raise ValueError("datetime must contain timezone")
    return value.astimezone(timezone.utc)


class _PushItemBase(BaseModel):
    clientId: str = Field(min_length=1, max_length=128)
    updatedAt: datetime
    isDeleted: bool = False
    operation: Operation = "UPDATE"

    @field_validator("updatedAt")
    @classmethod
    def validate_updated_at(cls, value: datetime) -> datetime:
        return _require_timezone(value)

    def is_delete_operation(self) -> bool:
        return self.operation == "DELETE" or self.isDeleted


class TopicPushItem(_PushItemBase):
    title: str | None = Field(default=None, max_length=255)

    @model_validator(mode="after")
    def validate_required_fields(self):
        if not self.is_delete_operation() and not self.title:
            raise ValueError("title is required for CREATE/UPDATE topic operations")
        return self


class TaskPushItem(_PushItemBase):
    topicClientId: str | None = Field(default=None, max_length=128)
    topicRemoteId: str | None = Field(default=None, max_length=128)
    title: str | None = Field(default=None, max_length=255)
    description: str | None = None
    status: str = "PLANNED"
    deadlineAt: datetime | None = None

    @field_validator("deadlineAt")
    @classmethod
    def validate_deadline_at(cls, value: datetime | None) -> datetime | None:
        if value is None:
            return None
        return _require_timezone(value)

    @model_validator(mode="after")
    def validate_required_fields(self):
        if self.is_delete_operation():
            return self
        if not self.title:
            raise ValueError("title is required for CREATE/UPDATE task operations")
        if not self.topicClientId and not self.topicRemoteId:
            raise ValueError("topicClientId or topicRemoteId is required for CREATE/UPDATE task operations")
        if self.status not in ALLOWED_TASK_STATUSES:
            raise ValueError(
                f"status must be one of: {', '.join(sorted(ALLOWED_TASK_STATUSES))}"
            )
        return self


class SolutionPushItem(_PushItemBase):
    taskClientId: str | None = Field(default=None, max_length=128)
    taskRemoteId: str | None = Field(default=None, max_length=128)
    content: str | None = None

    @model_validator(mode="after")
    def validate_required_fields(self):
        if self.is_delete_operation():
            return self
        if not self.taskClientId and not self.taskRemoteId:
            raise ValueError("taskClientId or taskRemoteId is required for CREATE/UPDATE solution operations")
        return self


class AttachmentMetadataPushItem(_PushItemBase):
    ownerType: str | None = Field(default=None, max_length=64)
    ownerTaskClientId: str | None = Field(default=None, max_length=128)
    ownerTaskRemoteId: str | None = Field(default=None, max_length=128)
    fileName: str | None = Field(default=None, max_length=255)
    mimeType: str | None = Field(default=None, max_length=255)
    sizeBytes: int | None = Field(default=None, ge=0)
    remoteFileId: str | None = Field(default=None, max_length=128)
    uploadState: str | None = Field(default=None, max_length=64)

    @model_validator(mode="after")
    def validate_required_fields(self):
        if self.is_delete_operation():
            return self
        if not self.ownerType:
            raise ValueError("ownerType is required for CREATE/UPDATE attachment operations")
        if not self.fileName:
            raise ValueError("fileName is required for CREATE/UPDATE attachment operations")
        if not self.ownerTaskClientId and not self.ownerTaskRemoteId:
            raise ValueError(
                "ownerTaskClientId or ownerTaskRemoteId is required for CREATE/UPDATE attachment operations"
            )
        return self


class FlashcardBestResultPushItem(_PushItemBase):
    topicClientId: str | None = Field(default=None, max_length=128)
    topicRemoteId: str | None = Field(default=None, max_length=128)
    questionsCount: int | None = Field(default=None, ge=0)
    durationMs: int | None = Field(default=None, ge=0)
    completedAt: datetime | None = None

    @field_validator("completedAt")
    @classmethod
    def validate_completed_at(cls, value: datetime | None) -> datetime | None:
        if value is None:
            return None
        return _require_timezone(value)

    @model_validator(mode="after")
    def validate_required_fields(self):
        if self.is_delete_operation():
            return self
        if not self.topicClientId and not self.topicRemoteId:
            raise ValueError("topicClientId or topicRemoteId is required for CREATE/UPDATE flashcard best result operations")
        if self.questionsCount is None:
            raise ValueError("questionsCount is required for CREATE/UPDATE flashcard best result operations")
        if self.durationMs is None:
            raise ValueError("durationMs is required for CREATE/UPDATE flashcard best result operations")
        if self.completedAt is None:
            raise ValueError("completedAt is required for CREATE/UPDATE flashcard best result operations")
        return self


class PushRequest(BaseModel):
    topics: list[TopicPushItem] = Field(default_factory=list)
    tasks: list[TaskPushItem] = Field(default_factory=list)
    solutions: list[SolutionPushItem] = Field(default_factory=list)
    attachmentsMetadata: list[AttachmentMetadataPushItem] = Field(default_factory=list)
    flashcardBestResults: list[FlashcardBestResultPushItem] = Field(default_factory=list)


class PushResultItem(BaseModel):
    clientId: str
    remoteId: str
    serverUpdatedAt: datetime
    status: str


class PushResponse(BaseModel):
    topics: list[PushResultItem]
    tasks: list[PushResultItem]
    solutions: list[PushResultItem]
    attachmentsMetadata: list[PushResultItem]
    flashcardBestResults: list[PushResultItem]


class TopicPullItem(BaseModel):
    remoteId: str
    clientId: str
    title: str
    createdAt: datetime
    updatedAt: datetime
    isDeleted: bool


class TaskPullItem(BaseModel):
    remoteId: str
    clientId: str
    topicRemoteId: str
    topicClientId: str
    title: str
    description: str | None = None
    status: str
    deadlineAt: datetime | None = None
    createdAt: datetime
    updatedAt: datetime
    isDeleted: bool


class SolutionPullItem(BaseModel):
    remoteId: str
    clientId: str
    taskRemoteId: str
    taskClientId: str
    content: str | None = None
    updatedAt: datetime
    isDeleted: bool


class AttachmentPullItem(BaseModel):
    remoteId: str
    clientId: str
    ownerType: str
    ownerTaskRemoteId: str
    ownerTaskClientId: str
    fileName: str
    mimeType: str | None = None
    sizeBytes: int | None = None
    createdAt: datetime
    updatedAt: datetime
    isDeleted: bool
    remoteFileId: str | None = None
    storageKey: str | None = None
    uploadState: str | None = None


class FlashcardBestResultPullItem(BaseModel):
    remoteId: str
    clientId: str
    topicRemoteId: str
    topicClientId: str
    questionsCount: int
    durationMs: int
    completedAt: datetime
    updatedAt: datetime
    isDeleted: bool


class PullResponse(BaseModel):
    serverTime: datetime
    topics: list[TopicPullItem]
    tasks: list[TaskPullItem]
    solutions: list[SolutionPullItem]
    attachmentsMetadata: list[AttachmentPullItem]
    flashcardBestResults: list[FlashcardBestResultPullItem]
    nextSince: datetime


class InitialSyncRequest(BaseModel):
    push: PushRequest = Field(default_factory=PushRequest)
    pullSince: datetime | None = None

    @field_validator("pullSince")
    @classmethod
    def validate_pull_since(cls, value: datetime | None) -> datetime | None:
        if value is None:
            return None
        return _require_timezone(value)


class InitialSyncResponse(BaseModel):
    push: PushResponse
    pull: PullResponse
