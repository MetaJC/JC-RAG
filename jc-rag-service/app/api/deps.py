"""API 共享私有辅助函数.
<p>被多个 router 模块调用的工具函数, 集中管理以避免循环导入.
<p>原位置: app/api/routes.py (拆分自 2026-07-03 结构重构, 业务零变更).

@author JC
"""
from __future__ import annotations

import platform
from typing import Any

from app.embeddings import embed_texts
from app.vector_store.filter_expr import FilterExpr


def _build_kb_filter(kb_id: int | str | None) -> FilterExpr | None:
    """构建知识库过滤条件 - 返回后端无关的 FilterExpr."""
    if kb_id is None:
        return None
    return FilterExpr.eq("kb_id", str(kb_id))


def _extract_embedding_override(payload: Any) -> dict[str, Any] | None:
    """提取嵌入参数覆盖"""
    if not isinstance(payload, dict):
        return None
    rag_override = payload.get("ragOverride")
    if not isinstance(rag_override, dict):
        return None
    embedding = rag_override.get("embedding")
    if not isinstance(embedding, dict):
        return None
    return embedding


def _build_query_vector(question: str, settings, embedding_override: dict[str, Any] | None) -> list[float]:
    """构建查询向量"""
    vectors, _ = embed_texts([question], settings, embedding_override)
    return vectors[0] if vectors else []


def _to_ws_tokens(text: str) -> str:
    """转换为分词文本（预留接口）"""
    return text


def _resolve_cpu_info() -> str:
    try:
        from app.core.device_detector import _resolve_cpu_name
        return f"CPU ({_resolve_cpu_name()})"
    except Exception:
        return f"CPU ({platform.processor() or 'Unknown'})"
