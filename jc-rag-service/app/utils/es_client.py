"""Elasticsearch 客户端封装模块"""
from __future__ import annotations

from typing import Any

from elasticsearch import Elasticsearch
from elasticsearch.helpers import bulk

from app.core.config import get_settings


def get_es_client() -> Elasticsearch:
    """获取 Elasticsearch 客户端（单例）"""
    settings = get_settings()
    return Elasticsearch(settings.elasticsearch.url)


def ensure_index() -> None:
    """确保索引存在，不存在则创建"""
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
    """执行向量检索"""
    settings = get_settings()
    client = get_es_client()
    index_name = settings.elasticsearch.index_name

    body = {
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

    if kb_filter:
        body["query"] = {
            "bool": {
                "must": body["query"],
                "filter": kb_filter,
            }
        }

    result = client.search(index=index_name, body=body)
    return (result.get("hits") or {}).get("hits") or []


def text_search(
    query: str,
    kb_filter: dict[str, Any] | None = None,
    top_k: int = 5,
) -> list[dict[str, Any]]:
    """执行全文检索"""
    settings = get_settings()
    client = get_es_client()
    index_name = settings.elasticsearch.index_name
    
    body = {
        "size": top_k,
        "query": {
            "bool": {
                "filter": kb_filter or [],
                "should": [
                    {"match": {"chunk_text_ws": {"query": query, "operator": "or"}}},
                    {"match": {"chunk_text": {"query": query, "operator": "or"}}},
                ],
                "minimum_should_match": 1,
            }
        },
    }
    
    result = client.search(index=index_name, body=body)
    return (result.get("hits") or {}).get("hits") or []


def bulk_index_documents(docs: list[dict[str, Any]]) -> tuple[int, int]:
    """批量写入文档"""
    settings = get_settings()
    client = get_es_client()
    index_name = settings.elasticsearch.index_name
    
    actions = [
        {"_index": index_name, "_source": doc}
        for doc in docs
    ]
    
    success, failed = bulk(client, actions, stats_only=True)
    return success, failed


def delete_by_document_id(document_id: str) -> None:
    """根据文档ID删除相关向量"""
    settings = get_settings()
    client = get_es_client()
    index_name = settings.elasticsearch.index_name
    
    client.delete_by_query(
        index=index_name,
        body={"query": {"term": {"document_id": document_id}}},
    )
