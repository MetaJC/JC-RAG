"""
Elasticsearch 向量存储实现.
业务代码使用 VectorStore 抽象接口, 此模块负责翻译为 ES DSL.

@author JC
@since 2026-07-16
"""
from __future__ import annotations

import logging
from typing import Any

from elasticsearch import Elasticsearch
from elasticsearch.helpers import bulk

from .base import VectorStore
from .filter_expr import FilterExpr

logger = logging.getLogger(__name__)


class ElasticsearchVectorStore(VectorStore):
    """ES 后端实现 - 兼容 ES 8.x (knn + bool.filter)."""

    def __init__(self, url: str, default_index: str) -> None:
        self._client = Elasticsearch(url)
        self._default_index = default_index

    @property
    def default_collection(self) -> str:
        return self._default_index

    def backend_name(self) -> str:
        return "elasticsearch"

    def ensure_collection(self, name: str, dims: int) -> None:
        """确保索引存在, 不存在则创建."""
        if self._client.indices.exists(index=name):
            return
        self._client.indices.create(
            index=name,
            mappings={"properties": self._build_mapping(dims)},
        )

    @staticmethod
    def _build_mapping(dims: int) -> dict[str, Any]:
        """ES 索引 mapping (与旧 es_client.py 保持一致)."""
        return {
            "chunk_id": {"type": "keyword"},
            "document_id": {"type": "keyword"},
            "kb_id": {"type": "keyword"},
            "file_name": {"type": "text", "fields": {"keyword": {"type": "keyword"}}},
            "doc_type": {"type": "keyword"},
            "security_level": {"type": "keyword"},
            "page_no": {"type": "integer"},
            "chunk_text": {"type": "text"},
            "chunk_text_keyword": {"type": "keyword", "ignore_above": 8191},
            "chunk_text_ws": {"type": "text"},
            "embedding_vector": {
                "type": "dense_vector",
                "dims": dims,
                "index": True,
                "similarity": "cosine",
            },
            "version_no": {"type": "integer"},
            "created_at": {"type": "date"},
        }

    def bulk_index(
        self,
        docs: list[dict[str, Any]],
        collection: str | None = None,
    ) -> tuple[int, int]:
        idx = collection or self._default_index
        actions = [{"_index": idx, "_source": doc} for doc in docs]
        return bulk(self._client, actions, stats_only=True)

    def delete_by_document_id(
        self,
        document_id: str,
        collection: str | None = None,
    ) -> None:
        idx = collection or self._default_index
        self._client.delete_by_query(
            index=idx,
            body={"query": {"term": {"document_id": document_id}}},
        )

    def delete_by_ids(
        self,
        ids: list[str],
        collection: str | None = None,
    ) -> None:
        idx = collection or self._default_index
        actions = [{"_op_type": "delete", "_index": idx, "_id": i} for i in ids]
        bulk(self._client, actions, stats_only=True)

    def delete_by_filter(
        self,
        filter: FilterExpr,
        collection: str | None = None,
    ) -> int:
        idx = collection or self._default_index
        result = self._client.delete_by_query(
            index=idx,
            body={"query": {"bool": {"filter": filter.to_es()}}},
            refresh=True,
        )
        return int(result.get("deleted") or 0)

    def vector_search(
        self,
        query_vector: list[float],
        collection: str | None = None,
        filter: FilterExpr | None = None,
        top_k: int = 5,
        top_n: int = 20,
    ) -> list[dict[str, Any]]:
        idx = collection or self._default_index
        body: dict[str, Any] = {
            "size": top_k,
            "query": {
                "knn": {
                    "field": "embedding_vector",
                    "query_vector": query_vector,
                    "k": top_k,
                    "num_candidates": top_n,
                }
            },
        }
        if filter:
            body["query"] = {
                "bool": {
                    "must": body["query"],
                    "filter": filter.to_es(),
                }
            }
        result = self._client.search(index=idx, body=body)
        return (result.get("hits") or {}).get("hits") or []

    def text_search(
        self,
        query: str,
        collection: str | None = None,
        filter: FilterExpr | None = None,
        top_k: int = 5,
    ) -> list[dict[str, Any]]:
        idx = collection or self._default_index
        body: dict[str, Any] = {
            "size": top_k,
            "query": {
                "bool": {
                    "filter": filter.to_es() if filter else [],
                    "should": [
                        {"match": {"chunk_text_ws": {"query": query, "operator": "or"}}},
                        {"match": {"chunk_text": {"query": query, "operator": "or"}}},
                    ],
                    "minimum_should_match": 1,
                }
            },
        }
        result = self._client.search(index=idx, body=body)
        return (result.get("hits") or {}).get("hits") or []

    def health_check(self) -> bool:
        try:
            return bool(self._client.ping())
        except Exception as e:
            logger.warning("ES health check failed: %s", e)
            return False

    # ---- 单文档 CRUD (L2 记忆 / 元数据场景) ----
    def get_by_id(
        self,
        doc_id: str,
        collection: str | None = None,
    ) -> dict[str, Any] | None:
        idx = collection or self._default_index
        try:
            resp = self._client.get(index=idx, id=doc_id, ignore=[404])
            if not resp.get("found"):
                return None
            return resp.get("_source")
        except Exception as e:
            logger.warning("ES get_by_id failed: %s", e)
            return None

    def index_one(
        self,
        doc_id: str,
        doc: dict[str, Any],
        collection: str | None = None,
    ) -> None:
        idx = collection or self._default_index
        self._client.index(index=idx, id=doc_id, document=doc, refresh=False)

    def update_by_id(
        self,
        doc_id: str,
        partial: dict[str, Any],
        collection: str | None = None,
    ) -> None:
        idx = collection or self._default_index
        self._client.update(index=idx, id=doc_id, body={"doc": partial})

    def count(
        self,
        filter: FilterExpr | None = None,
        collection: str | None = None,
    ) -> int:
        idx = collection or self._default_index
        body: dict[str, Any] = {"query": {}}
        if filter:
            body["query"] = {"bool": {"filter": filter.to_es()}}
        else:
            body["query"] = {"match_all": {}}
        result = self._client.count(index=idx, body=body)
        return int(result.get("count") or 0)

    def update_by_filter(
        self,
        filter: FilterExpr,
        script_source: str,
        script_params: dict[str, Any] | None = None,
        collection: str | None = None,
    ) -> int:
        idx = collection or self._default_index
        script: dict[str, Any] = {"source": script_source}
        if script_params:
            script["params"] = script_params
        result = self._client.update_by_query(
            index=idx,
            body={
                "query": {"bool": {"filter": filter.to_es()}},
                "script": script,
            },
            refresh=False,
        )
        return int(result.get("updated") or 0)
