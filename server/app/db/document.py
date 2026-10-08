from types import SimpleNamespace
from typing import Any


def document_to_object(document: dict[str, Any] | None):
    if document is None:
        return None
    data = dict(document)
    data.pop("_id", None)
    return SimpleNamespace(**data)
