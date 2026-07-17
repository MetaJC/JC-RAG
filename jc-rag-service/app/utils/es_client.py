"""Elasticsearch 客户端封装模块 (兼容层, 已废弃 - 新代码请走 VectorStore 抽象层).
<p>本模块仅保留以下用途:
<ul>
  <li>启动时建索引 (ensure_memory_index 等特殊 schema 场景)</li>
  <li>外部脚本 / 测试代码兼容</li>
</ul>
<p>业务代码应使用 <code>app.vector_store.get_vector_store()</code>, 后端切换无需改业务代码.
<p>所有旧 API 已添加 DeprecationWarning, 后续版本将删除.

@author JC
@deprecated 2026-07-16: 请使用 app.vector_store.get_vector_store()
"""
from __future__ import annotations

import warnings
from typing import Any

from elasticsearch import Elasticsearch

from app.core.config import get_settings


def _warn_deprecated(api_name: str) -> None:
    """统一 DeprecationWarning 入口, 控制噪音."""
    warnings.warn(
        f"app.utils.es_client.{api_name} 已废弃, 请改用 app.vector_store.get_vector_store()",
        DeprecationWarning,
        stacklevel=3,
    )


def get_es_client() -> Elasticsearch:
    """获取 Elasticsearch 客户端 (兼容层, 启动建索引专用).

    @deprecated: 业务代码请使用 get_vector_store().
    """
    settings = get_settings()
    return Elasticsearch(settings.elasticsearch.url)


def ensure_index() -> None:
    """确保 KAG chunks 索引存在 (兼容层, 启动时仍可使用).

    @deprecated: 业务代码请使用 vs.ensure_collection().
    """
    _warn_deprecated("ensure_index")
    from app.embeddings import get_embedding_dimension

    settings = get_settings()
    client = get_es_client()
    index_name = settings.elasticsearch.index_name
    dims = get_embedding_dimension(settings)

    if client.indices.exists(index=index_name):
        _ensure_mappings(client, index_name, dims)
        return

    client.indices.create(
        index=index_name,
        mappings={
            "properties": {
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
        },
    )


def _ensure_mappings(client: Elasticsearch, index_name: str, dims: int) -> None:
    """确保映射存在，不存在则添加"""
    try:
        mapping = client.indices.get_mapping(index=index_name)
        properties = (mapping.get(index_name, {}).get("mappings", {}) or {}).get("properties", {}) or {}

        if "chunk_text_ws" not in properties:
            client.indices.put_mapping(
                index=index_name,
                properties={"chunk_text_ws": {"type": "text"}},
            )

        if "embedding_vector" not in properties:
            client.indices.put_mapping(
                index=index_name,
                properties={
                    "embedding_vector": {
                        "type": "dense_vector",
                        "dims": dims,
                        "index": True,
                        "similarity": "cosine",
                    }
                },
            )
    except Exception:
        pass


def vector_search(
    query_vector: list[float],
    kb_filter: dict[str, Any] | None = None,
    top_k: int = 5,
    top_n: int = 20,
) -> list[dict[str, Any]]:
    """执行向量检索 (兼容层).

    @deprecated: 请使用 get_vector_store().vector_search(query_vector, filter=FilterExpr, ...).
    """
    _warn_deprecated("vector_search")
    from app.vector_store import FilterExpr, get_vector_store

    flt = None
    if kb_filter:
        # 兼容旧 dict filter: 期望 {"term": {"kb_id": "..."}} 形式, 翻译为 FilterExpr
        # 仅做最简实现: term / terms
        items: list[Any] = []
        if "term" in kb_filter:
            for field, value in kb_filter["term"].items():
                items.append(FilterExpr.eq(field, value))
        if "terms" in kb_filter:
            for field, value in kb_filter["terms"].items():
                items.append(FilterExpr.in_(field, value))
        if items:
            flt = items[0]
            for it in items[1:]:
                flt = flt.and_(it)
    return get_vector_store().vector_search(query_vector, filter=flt, top_k=top_k, top_n=top_n)


def text_search(
    query: str,
    kb_filter: dict[str, Any] | None = None,
    top_k: int = 5,
) -> list[dict[str, Any]]:
    """执行全文检索 (兼容层).

    @deprecated: 请使用 get_vector_store().text_search(query, filter=FilterExpr, ...).
    """
    _warn_deprecated("text_search")
    from app.vector_store import FilterExpr, get_vector_store

    flt = None
    if kb_filter:
        items: list[Any] = []
        if "term" in kb_filter:
            for field, value in kb_filter["term"].items():
                items.append(FilterExpr.eq(field, value))
        if "terms" in kb_filter:
            for field, value in kb_filter["terms"].items():
                items.append(FilterExpr.in_(field, value))
        if items:
            flt = items[0]
            for it in items[1:]:
                flt = flt.and_(it)
    return get_vector_store().text_search(query, filter=flt, top_k=top_k)


def bulk_index_documents(docs: list[dict[str, Any]]) -> tuple[int, int]:
    """批量写入文档 (兼容层).

    @deprecated: 请使用 get_vector_store().bulk_index(docs).
    """
    _warn_deprecated("bulk_index_documents")
    return get_vector_store().bulk_index(docs)


def delete_by_document_id(document_id: str) -> None:
    """根据文档ID删除相关向量 (兼容层).

    @deprecated: 请使用 get_vector_store().delete_by_document_id(document_id).
    """
    _warn_deprecated("delete_by_document_id")
    get_vector_store().delete_by_document_id(document_id)
