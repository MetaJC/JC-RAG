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
向量存储抽象基类 - 支持 ES / Milvus / Qdrant 等后端.
业务代码只依赖此接口, 由 Factory 决定具体后端实现.

@author JC
@since 2026-07-16
"""
from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Any

from .filter_expr import FilterExpr


class VectorStore(ABC):
    """向量存储抽象基类 - 所有后端实现必须遵循"""

    @property
    @abstractmethod
    def default_collection(self) -> str:
        """默认 collection/index 名称."""

    @abstractmethod
    def ensure_collection(self, name: str, dims: int) -> None:
        """确保 collection 存在, 不存在则创建."""

    @abstractmethod
    def bulk_index(
        self,
        docs: list[dict[str, Any]],
        collection: str | None = None,
    ) -> tuple[int, int]:
        """批量写入. collection 不传则用 default."""

    @abstractmethod
    def delete_by_document_id(
        self,
        document_id: str,
        collection: str | None = None,
    ) -> None: ...

    @abstractmethod
    def delete_by_ids(
        self,
        ids: list[str],
        collection: str | None = None,
    ) -> None: ...

    @abstractmethod
    def delete_by_filter(
        self,
        filter: FilterExpr,
        collection: str | None = None,
    ) -> int:
        """按过滤条件删除, 返回删除条数. 用于 kb_id 级别清理等场景."""

    @abstractmethod
    def vector_search(
        self,
        query_vector: list[float],
        collection: str | None = None,
        filter: FilterExpr | None = None,
        top_k: int = 5,
        top_n: int = 20,
    ) -> list[dict[str, Any]]: ...

    @abstractmethod
    def text_search(
        self,
        query: str,
        collection: str | None = None,
        filter: FilterExpr | None = None,
        top_k: int = 5,
    ) -> list[dict[str, Any]]: ...

    @abstractmethod
    def health_check(self) -> bool: ...

    @abstractmethod
    def backend_name(self) -> str:
        """返回后端名称 (用于日志/监控)."""

    # ---- 单文档 CRUD (L2 记忆 / 元数据场景) ----
    @abstractmethod
    def get_by_id(
        self,
        doc_id: str,
        collection: str | None = None,
    ) -> dict[str, Any] | None:
        """按主键取完整 doc, 找不到返回 None. ignore_404 内部已处理."""

    @abstractmethod
    def index_one(
        self,
        doc_id: str,
        doc: dict[str, Any],
        collection: str | None = None,
    ) -> None:
        """单条 upsert (按 doc_id)."""

    @abstractmethod
    def update_by_id(
        self,
        doc_id: str,
        partial: dict[str, Any],
        collection: str | None = None,
    ) -> None:
        """按主键局部更新 (merge), 用于 last_used_at / active=false 等小改动."""

    @abstractmethod
    def count(
        self,
        filter: "FilterExpr | None" = None,
        collection: str | None = None,
    ) -> int:
        """按 filter 统计条数, filter 为空时统计 collection 全量."""

    @abstractmethod
    def update_by_filter(
        self,
        filter: "FilterExpr",
        script_source: str,
        script_params: dict[str, Any] | None = None,
        collection: str | None = None,
    ) -> int:
        """按 filter 批量局部更新 (ES update_by_query / Milvus 等价).
        返回实际更新条数. 用于 L2 记忆软删等批量脚本场景."""
