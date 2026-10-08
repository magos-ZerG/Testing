"""initial schema

Revision ID: 20260409_0001
Revises: 
Create Date: 2026-04-09 00:00:00

"""
from alembic import op
import sqlalchemy as sa

revision = "20260409_0001"
down_revision = None
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "users",
        sa.Column("id", sa.String(length=36), nullable=False),
        sa.Column("email", sa.String(length=255), nullable=False),
        sa.Column("password_hash", sa.String(length=255), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_users_email", "users", ["email"], unique=True)

    op.create_table(
        "refresh_tokens",
        sa.Column("id", sa.String(length=36), nullable=False),
        sa.Column("user_id", sa.String(length=36), nullable=False),
        sa.Column("token_hash", sa.String(length=64), nullable=False),
        sa.Column("expires_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("revoked_at", sa.DateTime(timezone=True), nullable=True),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"]),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_refresh_tokens_token_hash", "refresh_tokens", ["token_hash"], unique=True)
    op.create_index("ix_refresh_tokens_user_id", "refresh_tokens", ["user_id"], unique=False)

    op.create_table(
        "topics",
        sa.Column("id", sa.String(length=36), nullable=False),
        sa.Column("client_id", sa.String(length=128), nullable=False),
        sa.Column("user_id", sa.String(length=36), nullable=False),
        sa.Column("title", sa.String(length=255), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("is_deleted", sa.Boolean(), nullable=False),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"]),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("user_id", "client_id", name="uq_topics_user_client_id"),
    )
    op.create_index("ix_topics_user_id", "topics", ["user_id"], unique=False)
    op.create_index("ix_topics_user_updated_at", "topics", ["user_id", "updated_at"], unique=False)

    op.create_table(
        "tasks",
        sa.Column("id", sa.String(length=36), nullable=False),
        sa.Column("client_id", sa.String(length=128), nullable=False),
        sa.Column("user_id", sa.String(length=36), nullable=False),
        sa.Column("topic_id", sa.String(length=36), nullable=False),
        sa.Column("title", sa.String(length=255), nullable=False),
        sa.Column("description", sa.Text(), nullable=True),
        sa.Column("status", sa.String(length=64), nullable=False),
        sa.Column("deadline_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("is_deleted", sa.Boolean(), nullable=False),
        sa.CheckConstraint(
            "status IN ('PLANNED', 'IN_PROGRESS', 'DONE', 'ARCHIVED')",
            name="ck_tasks_status",
        ),
        sa.ForeignKeyConstraint(["topic_id"], ["topics.id"]),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"]),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("user_id", "client_id", name="uq_tasks_user_client_id"),
    )
    op.create_index("ix_tasks_topic_id", "tasks", ["topic_id"], unique=False)
    op.create_index("ix_tasks_user_id", "tasks", ["user_id"], unique=False)
    op.create_index("ix_tasks_user_updated_at", "tasks", ["user_id", "updated_at"], unique=False)

    op.create_table(
        "task_solutions",
        sa.Column("id", sa.String(length=36), nullable=False),
        sa.Column("client_id", sa.String(length=128), nullable=False),
        sa.Column("user_id", sa.String(length=36), nullable=False),
        sa.Column("task_id", sa.String(length=36), nullable=False),
        sa.Column("content", sa.Text(), nullable=True),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("is_deleted", sa.Boolean(), nullable=False),
        sa.ForeignKeyConstraint(["task_id"], ["tasks.id"]),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"]),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("user_id", "client_id", name="uq_task_solutions_user_client_id"),
    )
    op.create_index("ix_task_solutions_task_id", "task_solutions", ["task_id"], unique=False)
    op.create_index("ix_task_solutions_user_id", "task_solutions", ["user_id"], unique=False)
    op.create_index("ix_task_solutions_user_updated_at", "task_solutions", ["user_id", "updated_at"], unique=False)

    op.create_table(
        "attachments",
        sa.Column("id", sa.String(length=36), nullable=False),
        sa.Column("client_id", sa.String(length=128), nullable=False),
        sa.Column("user_id", sa.String(length=36), nullable=False),
        sa.Column("owner_type", sa.String(length=64), nullable=False),
        sa.Column("owner_task_id", sa.String(length=36), nullable=False),
        sa.Column("file_name", sa.String(length=255), nullable=False),
        sa.Column("mime_type", sa.String(length=255), nullable=True),
        sa.Column("size_bytes", sa.Integer(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("is_deleted", sa.Boolean(), nullable=False),
        sa.Column("remote_file_id", sa.String(length=128), nullable=True),
        sa.Column("storage_key", sa.String(length=512), nullable=True),
        sa.Column("upload_state", sa.String(length=64), nullable=True),
        sa.CheckConstraint(
            "owner_type IN ('TASK_DESCRIPTION', 'TASK_SOLUTION')",
            name="ck_attachments_owner_type",
        ),
        sa.CheckConstraint(
            "size_bytes IS NULL OR size_bytes >= 0",
            name="ck_attachments_size_bytes_non_negative",
        ),
        sa.CheckConstraint(
            "upload_state IS NULL OR upload_state IN ('PENDING', 'UPLOADED', 'FAILED')",
            name="ck_attachments_upload_state",
        ),
        sa.ForeignKeyConstraint(["owner_task_id"], ["tasks.id"]),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"]),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("user_id", "client_id", name="uq_attachments_user_client_id"),
    )
    op.create_index("ix_attachments_owner_task_id", "attachments", ["owner_task_id"], unique=False)
    op.create_index("ix_attachments_user_id", "attachments", ["user_id"], unique=False)
    op.create_index("ix_attachments_user_updated_at", "attachments", ["user_id", "updated_at"], unique=False)


def downgrade() -> None:
    op.drop_index("ix_attachments_user_updated_at", table_name="attachments")
    op.drop_index("ix_attachments_user_id", table_name="attachments")
    op.drop_index("ix_attachments_owner_task_id", table_name="attachments")
    op.drop_table("attachments")

    op.drop_index("ix_task_solutions_user_updated_at", table_name="task_solutions")
    op.drop_index("ix_task_solutions_user_id", table_name="task_solutions")
    op.drop_index("ix_task_solutions_task_id", table_name="task_solutions")
    op.drop_table("task_solutions")

    op.drop_index("ix_tasks_user_updated_at", table_name="tasks")
    op.drop_index("ix_tasks_user_id", table_name="tasks")
    op.drop_index("ix_tasks_topic_id", table_name="tasks")
    op.drop_table("tasks")

    op.drop_index("ix_topics_user_updated_at", table_name="topics")
    op.drop_index("ix_topics_user_id", table_name="topics")
    op.drop_table("topics")

    op.drop_index("ix_refresh_tokens_user_id", table_name="refresh_tokens")
    op.drop_index("ix_refresh_tokens_token_hash", table_name="refresh_tokens")
    op.drop_table("refresh_tokens")

    op.drop_index("ix_users_email", table_name="users")
    op.drop_table("users")
