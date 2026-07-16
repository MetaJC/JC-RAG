"""
JC RAG 社区版 FastAPI app 定义 (包内入口).

@author JC
@since 2026-07-15 v1.2: 从根 main.py 迁移过来, 供商业版 in-process 集成

启动: uvicorn app.main:app --host 0.0.0.0 --port 8000
"""
from __future__ import annotations

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import Response

from app.api.routes import router
from app.api.routers.system import health_router
from app.core.config import get_settings

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s - %(message)s")

settings = get_settings()


async def initialize_community() -> None:
    """Initialize Community resources for a Community or Pro host process."""
    _log = logging.getLogger("rag-service")
    try:
        from app.utils.ocr_utils import get_ocr_engine
        get_ocr_engine()
        _log.info("OCR 引擎预初始化完成")
    except Exception as e:
        _log.warning("OCR 引擎预初始化失败: %s", e)
    try:
        from app.embeddings import get_embedding_status
        status = get_embedding_status(settings)
        _log.info("Embedding 状态: device=%s, model=%s", status.get("device"), status.get("modelPath"))
    except Exception as e:
        _log.warning("Embedding 状态检查失败: %s", e)
    # L2 长期事实: 启动时确保 jc_chat_memory 索引存在
    try:
        from app.utils.memory_store import ensure_memory_index
        ensure_memory_index(settings)
        _log.info("L2 长期记忆索引就绪: %s", settings.elasticsearch.memory_index_name)
    except Exception as e:
        _log.warning("L2 长期记忆索引初始化失败 (后续 /chat/memory/* 端点会受影响): %s", e)
    return None


@asynccontextmanager
async def _lifespan(app: FastAPI):
    await initialize_community()
    yield


class RequestLogMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next) -> Response:
        body = await request.body()
        print(
            f"[request] {request.method} {request.url.path} "
            f"content-type={request.headers.get('content-type')} "
            f"content-length={request.headers.get('content-length')} "
            f"body-len={len(body)}"
        )
        return await call_next(request)


def register_community_routes(host_app: FastAPI) -> None:
    """Register Community business routes without the host-owned health route."""
    host_app.include_router(router)


def register_community_health(host_app: FastAPI) -> None:
    """Register the health endpoint for the standalone Community service."""
    host_app.include_router(health_router)


def create_community_app() -> FastAPI:
    """Create the independently runnable Community FastAPI application."""
    community_app = FastAPI(
        title=settings.app.name,
        version=settings.app.version,
        docs_url="/docs",
        redoc_url="/redoc",
        lifespan=_lifespan,
    )
    community_app.add_middleware(RequestLogMiddleware)
    register_community_routes(community_app)
    register_community_health(community_app)
    return community_app


app = create_community_app()
