import logging
from contextvars import ContextVar, Token
from logging.handlers import RotatingFileHandler
from pathlib import Path
from typing import Any

from app.core.config import settings

_REQUEST_LOG_FORMAT = (
    '%(asctime)s | %(levelname)s | %(name)s '
    '| request_id=%(request_id)s | method=%(method)s | path=%(path)s '
    '| route=%(route)s | path_params=%(path_params)s '
    '| client=%(client)s | user_agent=%(user_agent)s '
    '| user_id=%(user_id)s | user_email=%(user_email)s '
    '| auth_sub=%(auth_sub)s | token_type=%(token_type)s '
    '| context=%(context)s | %(message)s'
)
_LOGGING_CONFIGURED = False

_request_id_ctx: ContextVar[str] = ContextVar('request_id', default='-')
_method_ctx: ContextVar[str] = ContextVar('method', default='-')
_path_ctx: ContextVar[str] = ContextVar('path', default='-')
_route_ctx: ContextVar[str] = ContextVar('route', default='-')
_path_params_ctx: ContextVar[str] = ContextVar('path_params', default='-')
_client_ctx: ContextVar[str] = ContextVar('client', default='-')
_user_agent_ctx: ContextVar[str] = ContextVar('user_agent', default='-')
_user_id_ctx: ContextVar[str] = ContextVar('user_id', default='-')
_user_email_ctx: ContextVar[str] = ContextVar('user_email', default='-')
_auth_sub_ctx: ContextVar[str] = ContextVar('auth_sub', default='-')
_token_type_ctx: ContextVar[str] = ContextVar('token_type', default='-')
_extra_context_ctx: ContextVar[dict[str, str]] = ContextVar('extra_context', default={})


class RequestContextFilter(logging.Filter):
    def filter(self, record: logging.LogRecord) -> bool:
        record.request_id = _request_id_ctx.get()
        record.method = _method_ctx.get()
        record.path = _path_ctx.get()
        record.route = _route_ctx.get()
        record.path_params = _path_params_ctx.get()
        record.client = _client_ctx.get()
        record.user_agent = _user_agent_ctx.get()
        record.user_id = _user_id_ctx.get()
        record.user_email = _user_email_ctx.get()
        record.auth_sub = _auth_sub_ctx.get()
        record.token_type = _token_type_ctx.get()
        extra_context = _extra_context_ctx.get()
        record.context = _format_extra_context(extra_context)
        return True


_REQUEST_CONTEXT_FILTER = RequestContextFilter()


def _build_handler(handler: logging.Handler, level: int, formatter: logging.Formatter) -> logging.Handler:
    handler.setLevel(level)
    handler.setFormatter(formatter)
    handler.addFilter(_REQUEST_CONTEXT_FILTER)
    return handler


def _normalize_context_value(value: Any) -> str:
    if value is None:
        return '-'
    normalized = str(value).strip()
    return normalized or '-'


def _format_extra_context(values: dict[str, str]) -> str:
    if not values:
        return '-'
    return ','.join(f'{key}={values[key]}' for key in sorted(values))


def _serialize_mapping(values: dict[str, Any]) -> str:
    cleaned = {
        str(key): _normalize_context_value(value)
        for key, value in values.items()
        if value is not None and _normalize_context_value(value) != '-'
    }
    return _format_extra_context(cleaned)


def configure_logging() -> None:
    global _LOGGING_CONFIGURED

    root_logger = logging.getLogger()
    if _LOGGING_CONFIGURED:
        return

    log_level = getattr(logging, settings.log_level.upper(), logging.INFO)
    log_path = Path(settings.log_file_path)
    log_path.parent.mkdir(parents=True, exist_ok=True)

    formatter = logging.Formatter(_REQUEST_LOG_FORMAT)

    file_handler = _build_handler(
        RotatingFileHandler(
            filename=log_path,
            maxBytes=settings.log_max_bytes,
            backupCount=settings.log_backup_count,
            encoding='utf-8',
        ),
        log_level,
        formatter,
    )

    stream_handler = _build_handler(logging.StreamHandler(), log_level, formatter)

    root_logger.handlers.clear()
    root_logger.filters.clear()
    root_logger.setLevel(log_level)
    root_logger.addHandler(file_handler)
    root_logger.addHandler(stream_handler)

    for logger_name in ('uvicorn', 'uvicorn.access', 'uvicorn.error', 'fastapi'):
        target_logger = logging.getLogger(logger_name)
        target_logger.handlers.clear()
        target_logger.propagate = True

    logging.captureWarnings(True)
    _LOGGING_CONFIGURED = True


def bind_request_context(*, request_id: str, method: str, path: str, client: str, user_agent: str) \
        -> dict[str, Token[Any]]:
    return {
        'request_id': _request_id_ctx.set(request_id),
        'method': _method_ctx.set(method),
        'path': _path_ctx.set(path),
        'route': _route_ctx.set('-'),
        'path_params': _path_params_ctx.set('-'),
        'client': _client_ctx.set(client),
        'user_agent': _user_agent_ctx.set(user_agent),
        'user_id': _user_id_ctx.set('-'),
        'user_email': _user_email_ctx.set('-'),
        'auth_sub': _auth_sub_ctx.set('-'),
        'token_type': _token_type_ctx.set('-'),
        'extra_context': _extra_context_ctx.set({}),
    }


def bind_route_context(*, route: str | None, path_params: dict[str, Any] | None) -> dict[str, Token[Any]]:
    return {
        'route': _route_ctx.set(_normalize_context_value(route)),
        'path_params': _path_params_ctx.set(_serialize_mapping(path_params or {})),
    }


def bind_user_context(*, user_id: str | None, user_email: str | None = None) -> None:
    _user_id_ctx.set(_normalize_context_value(user_id))
    _user_email_ctx.set(_normalize_context_value(user_email))


def bind_auth_context(*, subject: str | None, token_type: str | None) -> None:
    _auth_sub_ctx.set(_normalize_context_value(subject))
    _token_type_ctx.set(_normalize_context_value(token_type))


def bind_extra_context(**kwargs: Any) -> Token[Any]:
    current = dict(_extra_context_ctx.get())
    for key, value in kwargs.items():
        normalized_key = str(key).strip()
        if not normalized_key:
            continue
        normalized_value = _normalize_context_value(value)
        if normalized_value == '-':
            current.pop(normalized_key, None)
        else:
            current[normalized_key] = normalized_value
    return _extra_context_ctx.set(current)


def reset_extra_context(token: Token[Any]) -> None:
    _extra_context_ctx.reset(token)


def reset_route_context(tokens: dict[str, Token[Any]]) -> None:
    _path_params_ctx.reset(tokens['path_params'])
    _route_ctx.reset(tokens['route'])


def reset_request_context(tokens: dict[str, Token[Any]]) -> None:
    _extra_context_ctx.reset(tokens['extra_context'])
    _token_type_ctx.reset(tokens['token_type'])
    _auth_sub_ctx.reset(tokens['auth_sub'])
    _user_email_ctx.reset(tokens['user_email'])
    _user_id_ctx.reset(tokens['user_id'])
    _user_agent_ctx.reset(tokens['user_agent'])
    _client_ctx.reset(tokens['client'])
    _path_params_ctx.reset(tokens['path_params'])
    _route_ctx.reset(tokens['route'])
    _path_ctx.reset(tokens['path'])
    _method_ctx.reset(tokens['method'])
    _request_id_ctx.reset(tokens['request_id'])


__all__ = [
    'bind_auth_context',
    'bind_extra_context',
    'bind_request_context',
    'bind_route_context',
    'bind_user_context',
    'configure_logging',
    'reset_extra_context',
    'reset_request_context',
    'reset_route_context',
]
