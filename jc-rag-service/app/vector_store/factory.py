# Copyright 2024-2026 OpenJC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# 本文件来源于 JC RAG Community Edition
# 开源地址: https://gitee.com/Open-JC/jc-rag | https://github.com/MetaJC/JC-RAG
# 二次开发请保留此出处声明。


"""
向量存储工厂 - 根据配置返回对应后端实例 (单例).
后端切换无需改业务代码, 仅修改 rag_config.yml 的 vector_store.backend.

@author JC
@since 2026-07-16
"""
from __future__ import annotations

import logging
from typing import Optional

from app.core.config import get_settings

from .base import VectorStore

logger = logging.getLogger(__name__)

_store: Optional[VectorStore] = None


def get_vector_store() -> VectorStore:
    """获取当前激活的向量存储 (单例)."""
    global _store
    if _store is not None:
        return _store

    settings = get_settings()
    backend = settings.vector_store.backend
    logger.info("Initializing vector store backend: %s", backend)

    if backend == "elasticsearch":
        from .elasticsearch_store import ElasticsearchVectorStore
        _store = ElasticsearchVectorStore(
            url=settings.vector_store.elasticsearch.url,
            default_index=settings.vector_store.elasticsearch.default_index,
        )
    elif backend == "milvus":
        from .milvus_store import MilvusVectorStore
        cfg = settings.vector_store.milvus
        _store = MilvusVectorStore(
            host=cfg.host,
            port=cfg.port,
            user=cfg.user,
            password=cfg.password,
            default_collection=cfg.default_collection,
        )
    else:
        raise ValueError(f"Unknown vector store backend: {backend}")

    logger.info("Vector store ready: %s", _store.backend_name())
    return _store


def reset_vector_store() -> None:
    """测试时用, 重置单例."""
    global _store
    _store = None


def set_vector_store(instance) -> None:
    """外部注入向量存储实例 (供 Pro 集成时使用).

    <p>典型场景: Pro 启动时根据 pro_config.yml 构造 MilvusVectorStore,
    调用本函数覆盖 community 默认单例, 业务代码无感知.
    <p>支持运行时切换 (e.g. 配置变更后重启) - 旧引用仍然指向旧实例, 需重启服务.
    <p>使用 duck typing 而非 isinstance 检查, 因为 Pro 的 VectorStore 类与 community 同源独立维护.
    """
    global _store
    if not hasattr(instance, "backend_name") or not hasattr(instance, "vector_search"):
        raise TypeError(
            f"Expected VectorStore-like object (need backend_name/vector_search), "
            f"got {type(instance).__name__}"
        )
    backend = instance.backend_name()
    if _store is not None:
        logger.info(
            "Overriding existing vector store (%s) with %s",
            _store.backend_name(), backend,
        )
    _store = instance
    logger.info("Vector store injected: %s", backend)
