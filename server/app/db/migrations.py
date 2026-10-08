from pathlib import Path

from alembic import command
from alembic.config import Config

from app.core.config import settings


def run_migrations() -> None:
    if settings.database_backend == "mongodb":
        from app.db.mongo import init_mongo_indexes

        init_mongo_indexes()
        return

    alembic_ini_path = Path(__file__).resolve().parents[2] / "alembic.ini"
    cfg = Config(alembic_ini_path)
    cfg.set_main_option("sqlalchemy.url", settings.database_url)
    command.upgrade(cfg, "head")
