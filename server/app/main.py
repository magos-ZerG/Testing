import logging
import time
import uuid
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from .api.routes.attachments import router as attachments_router
from .api.routes.auth import router as auth_router
from .api.routes.sync import router as sync_router
from .core.config import settings
from .core.logging import bind_request_context, configure_logging, reset_request_context
from .db.migrations import run_migrations

configure_logging()
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(curr_app: FastAPI):
    logger.info('Application startup initiated')
    if settings.run_migrations_on_startup:
        logger.info('Running database migrations on startup')
        run_migrations()
        logger.info('Database migrations finished')
    yield
    logger.info('Application shutdown completed')


app = FastAPI(
    title='StudyMate Server API',
    version='1.1.0',
    description='Server API for StudyMate',
    lifespan=lifespan,
)


def _log_response(logger_: logging.Logger, status_code: int, message: str, *args) -> None:
    if status_code >= 500:
        logger_.error(message, *args)
    elif status_code >= 400:
        logger_.warning(message, *args)
    else:
        logger_.info(message, *args)


@app.middleware('http')
async def log_requests(request: Request, call_next):
    request_id = request.headers.get('X-Request-ID') or uuid.uuid4().hex[:12]
    start = time.perf_counter()
    client = request.client
    client_host = client.host if client is not None else 'unknown'
    user_agent = request.headers.get('User-Agent', '-')

    context_tokens = bind_request_context(
        request_id=request_id,
        method=request.method,
        path=request.url.path,
        client=client_host,
        user_agent=user_agent,
    )

    logger.info('Request started')

    try:
        try:
            response = await call_next(request)
        except Exception:  # noqa: BLE001
            duration_ms = (time.perf_counter() - start) * 1000
            logger.exception(
                'Request failed with unhandled exception | duration_ms=%.2f',
                duration_ms,
            )
            response = JSONResponse(
                status_code=500,
                content={'detail': 'Internal server error', 'requestId': request_id},
            )

        duration_ms = (time.perf_counter() - start) * 1000

        _log_response(
            logger,
            response.status_code,
            'Request completed | status=%s | duration_ms=%.2f',
            response.status_code,
            duration_ms,
        )

        response.headers['X-Request-ID'] = request_id
        return response

    finally:
        reset_request_context(context_tokens)


app.include_router(auth_router)
app.include_router(sync_router)
app.include_router(attachments_router)


@app.get('/health')
def health():
    return {'status': 'ok'}


@app.get('/')
def root():
    return {
        'name': 'StudyMate Server API',
        'docs': '/docs',
        'health': '/health',
    }
