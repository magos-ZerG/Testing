from datetime import datetime, timezone
from sqlalchemy import String, DateTime, ForeignKey, Boolean, Text, Integer, UniqueConstraint, Index, CheckConstraint
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.db.session import Base


def utcnow():
    return datetime.now(timezone.utc)


class Topic(Base):
    __tablename__ = "topics"
    __table_args__ = (
        UniqueConstraint("user_id", "client_id", name="uq_topics_user_client_id"),
        Index("ix_topics_user_updated_at", "user_id", "updated_at"),
    )

    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    client_id: Mapped[str] = mapped_column(String(128), nullable=False)
    user_id: Mapped[str] = mapped_column(ForeignKey("users.id"), nullable=False, index=True)
    title: Mapped[str] = mapped_column(String(255), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, nullable=False)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, nullable=False)
    is_deleted: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)

    tasks: Mapped[list["Task"]] = relationship(back_populates="topic")
    flashcard_best_results: Mapped[list["FlashcardBestResult"]] = relationship(back_populates="topic")


class Task(Base):
    __tablename__ = "tasks"
    __table_args__ = (
        UniqueConstraint("user_id", "client_id", name="uq_tasks_user_client_id"),
        CheckConstraint(
            "status IN ('PLANNED', 'IN_PROGRESS', 'DONE', 'ARCHIVED')",
            name="ck_tasks_status",
        ),
        Index("ix_tasks_user_updated_at", "user_id", "updated_at"),
    )

    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    client_id: Mapped[str] = mapped_column(String(128), nullable=False)
    user_id: Mapped[str] = mapped_column(ForeignKey("users.id"), nullable=False, index=True)
    topic_id: Mapped[str] = mapped_column(ForeignKey("topics.id"), nullable=False, index=True)
    title: Mapped[str] = mapped_column(String(255), nullable=False)
    description: Mapped[str | None] = mapped_column(Text, nullable=True)
    status: Mapped[str] = mapped_column(String(64), nullable=False, default="PLANNED")
    deadline_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, nullable=False)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, nullable=False)
    is_deleted: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)

    topic: Mapped["Topic"] = relationship(back_populates="tasks")
    solutions: Mapped[list["TaskSolution"]] = relationship(back_populates="task")


class TaskSolution(Base):
    __tablename__ = "task_solutions"
    __table_args__ = (
        UniqueConstraint("user_id", "client_id", name="uq_task_solutions_user_client_id"),
        Index("ix_task_solutions_user_updated_at", "user_id", "updated_at"),
    )

    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    client_id: Mapped[str] = mapped_column(String(128), nullable=False)
    user_id: Mapped[str] = mapped_column(ForeignKey("users.id"), nullable=False, index=True)
    task_id: Mapped[str] = mapped_column(ForeignKey("tasks.id"), nullable=False, index=True)
    content: Mapped[str | None] = mapped_column(Text, nullable=True)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, nullable=False)
    is_deleted: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)

    task: Mapped["Task"] = relationship(back_populates="solutions")


class Attachment(Base):
    __tablename__ = "attachments"
    __table_args__ = (
        UniqueConstraint("user_id", "client_id", name="uq_attachments_user_client_id"),
        CheckConstraint(
            "owner_type IN ('TASK_DESCRIPTION', 'TASK_SOLUTION')",
            name="ck_attachments_owner_type",
        ),
        CheckConstraint(
            "size_bytes IS NULL OR size_bytes >= 0",
            name="ck_attachments_size_bytes_non_negative",
        ),
        CheckConstraint(
            "upload_state IS NULL OR upload_state IN ('PENDING', 'UPLOADED', 'FAILED')",
            name="ck_attachments_upload_state",
        ),
        Index("ix_attachments_user_updated_at", "user_id", "updated_at"),
    )

    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    client_id: Mapped[str] = mapped_column(String(128), nullable=False)
    user_id: Mapped[str] = mapped_column(ForeignKey("users.id"), nullable=False, index=True)
    owner_type: Mapped[str] = mapped_column(String(64), nullable=False)
    owner_task_id: Mapped[str] = mapped_column(ForeignKey("tasks.id"), nullable=False, index=True)
    file_name: Mapped[str] = mapped_column(String(255), nullable=False)
    mime_type: Mapped[str | None] = mapped_column(String(255), nullable=True)
    size_bytes: Mapped[int | None] = mapped_column(Integer, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, nullable=False)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, nullable=False)
    is_deleted: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    remote_file_id: Mapped[str | None] = mapped_column(String(128), nullable=True)
    storage_key: Mapped[str | None] = mapped_column(String(512), nullable=True)
    upload_state: Mapped[str | None] = mapped_column(String(64), nullable=True)

class FlashcardBestResult(Base):
    __tablename__ = "flashcard_best_results"
    __table_args__ = (
        UniqueConstraint("user_id", "client_id", name="uq_flashcard_best_results_user_client_id"),
        UniqueConstraint("user_id", "topic_id", name="uq_flashcard_best_results_user_topic_id"),
        CheckConstraint(
            "questions_count >= 0",
            name="ck_flashcard_best_results_questions_count_non_negative",
        ),
        CheckConstraint(
            "duration_ms >= 0",
            name="ck_flashcard_best_results_duration_ms_non_negative",
        ),
        Index("ix_flashcard_best_results_user_updated_at", "user_id", "updated_at"),
        Index("ix_flashcard_best_results_topic_id", "topic_id"),
    )

    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    client_id: Mapped[str] = mapped_column(String(128), nullable=False)
    user_id: Mapped[str] = mapped_column(ForeignKey("users.id"), nullable=False, index=True)
    topic_id: Mapped[str] = mapped_column(ForeignKey("topics.id"), nullable=False, index=True)
    questions_count: Mapped[int] = mapped_column(Integer, nullable=False)
    duration_ms: Mapped[int] = mapped_column(Integer, nullable=False)
    completed_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, nullable=False)
    is_deleted: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)

    topic: Mapped["Topic"] = relationship(back_populates="flashcard_best_results")

