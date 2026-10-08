"""add flashcard best results

Revision ID: 20260508_0002
Revises: 20260409_0001
Create Date: 2026-05-08 00:00:00

"""
from alembic import op
import sqlalchemy as sa

revision = "20260508_0002"
down_revision = "20260409_0001"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "flashcard_best_results",
        sa.Column("id", sa.String(length=36), nullable=False),
        sa.Column("client_id", sa.String(length=128), nullable=False),
        sa.Column("user_id", sa.String(length=36), nullable=False),
        sa.Column("topic_id", sa.String(length=36), nullable=False),
        sa.Column("questions_count", sa.Integer(), nullable=False),
        sa.Column("duration_ms", sa.Integer(), nullable=False),
        sa.Column("completed_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("is_deleted", sa.Boolean(), nullable=False),
        sa.CheckConstraint(
            "questions_count >= 0",
            name="ck_flashcard_best_results_questions_count_non_negative",
        ),
        sa.CheckConstraint(
            "duration_ms >= 0",
            name="ck_flashcard_best_results_duration_ms_non_negative",
        ),
        sa.ForeignKeyConstraint(["topic_id"], ["topics.id"]),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"]),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("user_id", "client_id", name="uq_flashcard_best_results_user_client_id"),
        sa.UniqueConstraint("user_id", "topic_id", name="uq_flashcard_best_results_user_topic_id"),
    )
    op.create_index("ix_flashcard_best_results_user_id", "flashcard_best_results", ["user_id"], unique=False)
    op.create_index("ix_flashcard_best_results_topic_id", "flashcard_best_results", ["topic_id"], unique=False)
    op.create_index(
        "ix_flashcard_best_results_user_updated_at",
        "flashcard_best_results",
        ["user_id", "updated_at"],
        unique=False,
    )


def downgrade() -> None:
    op.drop_index("ix_flashcard_best_results_user_updated_at", table_name="flashcard_best_results")
    op.drop_index("ix_flashcard_best_results_topic_id", table_name="flashcard_best_results")
    op.drop_index("ix_flashcard_best_results_user_id", table_name="flashcard_best_results")
    op.drop_table("flashcard_best_results")
