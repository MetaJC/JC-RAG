"""
向量存储抽象层 - 社区版副本 (与 pro 同源, 手动保持一致).

@author JC
@since 2026-07-16
"""
from __future__ import annotations

from .base import VectorStore
from .factory import get_vector_store, reset_vector_store, set_vector_store
from .filter_expr import FilterCondition, FilterExpr, OrGroup

__all__ = [
    "FilterCondition",
    "FilterExpr",
    "OrGroup",
    "VectorStore",
    "get_vector_store",
    "reset_vector_store",
    "set_vector_store",
]
