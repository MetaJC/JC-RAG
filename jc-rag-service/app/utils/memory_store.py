"""L2 长期事实 ES 存储封装.
<p>独立于 KAG 索引 (jc_rag_chunks), 使用 jc_chat_memory 存储用户长期记忆.
<p>所有 CRUD / 检索 / 去重都在这里, 路由层只调业务方法.

@author JC
"""
from __future__ import annotations

import logging
import re
import time
import uuid
from typing import Any

from app.core.config import Settings
from app.embeddings import embed_texts
from app.utils.es_client import get_es_client

logger = logging.getLogger(__name__)

# 敏感信息正则 (提取后脱敏; 命中即丢弃该 fact, 不入库)
_SENSITIVE_PATTERNS = [
    re.compile(r"1[3-9]\d{9}"),  # 手机号
    re.compile(r"\d{17}[\dXx]"),  # 身份证
    re.compile(r"[\w.+-]+@[\w-]+\.[\w.-]+"),  # 邮箱
    re.compile(r"(?i)password\s*[:=]\s*\S+"),
    re.compile(r"(?i)api[_-]?key\s*[:=]\s*\S+"),
]


def _is_sensitive(text: str) -> bool:
    """检查文本是否含敏感信息 (手机号/身份证/邮箱/密码/API key)."""
    if not text:
        return False
    for p in _SENSITIVE_PATTERNS:
        if p.search(text):
            return True
    return False


def ensure_memory_index(settings: Settings) -> None:
    """启动时调用: 确保 jc_chat_memory 索引存在, 不存在则建.

    mapping 设计:
      - memory_id: keyword (UUID, 业务主键)
      - user_id: keyword (强过滤, 多用户隔离)
      - session_id: keyword (来源会话, 不参与检索)
      - key: keyword (事实主键名)
      - value: text (事实正文)
      - category: keyword (分类, 用于按 category 去重)
      - source: keyword (auto/manual)
      - active: boolean (软删)
      - embedding_vector: dense_vector (cosine, dims 与 embedding 配置一致)
      - last_used_at: date (用于超量淘汰)
      - created_at / updated_at: date
    """
    client = get_es_client()
    index_name = settings.elasticsearch.memory_index_name
    if client.indices.exists(index=index_name):
        return
    # dims 走配置, 兜底 1024
    try:
        from app.embeddings import get_embedding_dimension
        dims = get_embedding_dimension(settings)
    except Exception:
        dims = int(settings.embedding.dims or 1024)
    client.indices.create(
        index=index_name,
        mappings={
            "properties": {
                "memory_id": {"type": "keyword"},
                "user_id": {"type": "keyword"},
                "session_id": {"type": "keyword"},
                "key": {"type": "keyword"},
                "value": {"type": "text", "fields": {"keyword": {"type": "keyword", "ignore_above": 256}}},
                "category": {"type": "keyword"},
                "source": {"type": "keyword"},
                "active": {"type": "boolean"},
                "embedding_vector": {
                    "type": "dense_vector",
                    "dims": dims,
                    "index": True,
                    "similarity": "cosine",
                },
                "last_used_at": {"type": "date"},
                "created_at": {"type": "date"},
                "updated_at": {"type": "date"},
            }
        },
    )
    logger.info("ensure_memory_index: created ES index '%s' (dims=%d)", index_name, dims)


def _now_iso() -> str:
    """ES 默认 date 格式: 严格 ISO 8601, 带时区."""
    from datetime import datetime, timezone
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.%fZ")


def _cosine_sim(a: list[float], b: list[float]) -> float:
    """纯 Python 余弦相似度, 用于去重比对 (向量化在 Python 侧完成, 避免再走一次 ES)."""
    if not a or not b or len(a) != len(b):
        return 0.0
    dot = sum(x * y for x, y in zip(a, b))
    na = sum(x * x for x in a) ** 0.5
    nb = sum(y * y for y in b) ** 0.5
    if na == 0 or nb == 0:
        return 0.0
    return dot / (na * nb)


def upsert_facts(
    settings: Settings,
    user_id: str | int,
    session_id: str | int,
    facts: list[dict[str, Any]],
    embedding_override: dict[str, Any] | None = None,
) -> dict[str, Any]:
    """LLM 提取出的 fact 批量入库, 含去重.

    Args:
        settings: 全局配置
        user_id: 用户 ID
        session_id: 来源会话 ID
        facts: 形如 [{"key":"...","value":"...","category":"..."}]
        embedding_override: 嵌入覆盖 (同 chat)

    Returns:
        {"inserted": N, "updated": M, "skipped": K, "details": [...]}
    """
    if not facts:
        return {"inserted": 0, "updated": 0, "skipped": 0, "details": []}

    # 1. 过滤敏感 + 截断
    cleaned: list[dict[str, Any]] = []
    for f in facts:
        if not isinstance(f, dict):
            continue
        key = str(f.get("key") or "").strip()
        value = str(f.get("value") or "").strip()
        category = str(f.get("category") or "other").strip()
        if not key or not value:
            continue
        if category not in settings.memory.categories:
            category = "other"
        if _is_sensitive(value) or _is_sensitive(key):
            logger.info("upsert_facts: skip sensitive fact, key=%s", key)
            continue
        if len(value) > 500:
            value = value[:500] + "..."
        cleaned.append({"key": key, "value": value, "category": category})
    if not cleaned:
        return {"inserted": 0, "updated": 0, "skipped": 0, "details": []}

    # 2. 向量化 (一次调用, 高效)
    texts = [f"{f['key']}: {f['value']}" for f in cleaned]
    try:
        vectors, _ = embed_texts(texts, settings, embedding_override)
    except Exception as e:
        logger.warning("upsert_facts: embed failed, skip all, err=%s", e)
        return {"inserted": 0, "updated": 0, "skipped": len(cleaned), "details": []}

    # 3. 按 category 分组, 查同 category 现存 fact (含 active=true 的)
    categories = list({f["category"] for f in cleaned})
    existing_by_cat: dict[str, list[dict[str, Any]]] = {c: [] for c in categories}
    try:
        client = get_es_client()
        index_name = settings.elasticsearch.memory_index_name
        resp = client.search(
            index=index_name,
            body={
                "size": 1000,  # 单类目上限
                "query": {
                    "bool": {
                        "filter": [
                            {"term": {"user_id": str(user_id)}},
                            {"term": {"active": True}},
                            {"terms": {"category": categories}},
                        ]
                    }
                },
            },
        )
        for hit in (resp.get("hits") or {}).get("hits") or []:
            src = hit.get("_source") or {}
            cat = src.get("category", "other")
            existing_by_cat.setdefault(cat, []).append(src)
    except Exception as e:
        logger.warning("upsert_facts: fetch existing failed, err=%s", e)

    # 4. 去重比对 + 写入
    client = get_es_client()
    index_name = settings.elasticsearch.memory_index_name
    inserted = 0
    updated = 0
    skipped = 0
    details: list[dict[str, Any]] = []
    now_iso = _now_iso()
    dedup_threshold = settings.memory.dedup_similarity_threshold

    for fact, vec in zip(cleaned, vectors):
        try:
            # 同 category 找最相似的
            best: tuple[float, dict[str, Any] | None] = (0.0, None)
            for ex in existing_by_cat.get(fact["category"], []):
                ex_vec = ex.get("embedding_vector")
                if not ex_vec:
                    continue
                sim = _cosine_sim(vec, ex_vec)
                if sim > best[0]:
                    best = (sim, ex)

            if best[1] is not None and best[0] >= dedup_threshold:
                # update: 覆盖 value, 保留 memory_id
                doc_id = best[1].get("memory_id")
                if doc_id:
                    client.update(
                        index=index_name,
                        id=doc_id,
                        body={
                            "doc": {
                                "value": fact["value"],
                                "key": fact["key"],
                                "source": "auto",
                                "embedding_vector": vec,
                                "last_used_at": now_iso,
                                "updated_at": now_iso,
                            }
                        },
                    )
                    updated += 1
                    details.append({"action": "updated", "memory_id": doc_id, "sim": round(best[0], 4)})
                    continue
            # 新增
            memory_id = uuid.uuid4().hex
            doc = {
                "memory_id": memory_id,
                "user_id": str(user_id),
                "session_id": str(session_id),
                "key": fact["key"],
                "value": fact["value"],
                "category": fact["category"],
                "source": "auto",
                "active": True,
                "embedding_vector": vec,
                "last_used_at": now_iso,
                "created_at": now_iso,
                "updated_at": now_iso,
            }
            client.index(index=index_name, id=memory_id, document=doc, refresh=False)
            inserted += 1
            details.append({"action": "inserted", "memory_id": memory_id})
        except Exception as e:
            logger.warning("upsert_facts: write one fact failed, err=%s", e)
            skipped += 1
            details.append({"action": "skipped", "error": str(e)})

    # 5. 超量淘汰: 超过 max_per_user, 按 last_used_at 升序软删最旧的
    try:
        max_per = settings.memory.max_per_user
        count_resp = client.count(
            index=index_name,
            body={"query": {"bool": {"filter": [
                {"term": {"user_id": str(user_id)}},
                {"term": {"active": True}},
            ]}}},
        )
        total = int(count_resp.get("count") or 0)
        if total > max_per:
            over = total - max_per
            old_resp = client.search(
                index=index_name,
                body={
                    "size": over,
                    "query": {"bool": {"filter": [
                        {"term": {"user_id": str(user_id)}},
                        {"term": {"active": True}},
                    ]}},
                    "sort": [{"last_used_at": {"order": "asc"}}],
                },
            )
            for hit in (old_resp.get("hits") or {}).get("hits") or []:
                mid = (hit.get("_source") or {}).get("memory_id")
                if mid:
                    client.update(
                        index=index_name,
                        id=mid,
                        body={"doc": {"active": False, "updated_at": _now_iso()}},
                    )
            logger.info("upsert_facts: capped to max_per_user, evicted=%d, user=%s", over, user_id)
    except Exception as e:
        logger.warning("upsert_facts: eviction check failed, err=%s", e)

    logger.info("upsert_facts: user=%s, inserted=%d, updated=%d, skipped=%d",
                user_id, inserted, updated, skipped)
    return {"inserted": inserted, "updated": updated, "skipped": skipped, "details": details}


def recall_facts(
    settings: Settings,
    user_id: str | int,
    query: str,
    top_k: int | None = None,
    embedding_override: dict[str, Any] | None = None,
) -> list[dict[str, Any]]:
    """按 query 向量检索本用户 top_k 事实, 排除软删.

    Returns:
        [{"memory_id", "key", "value", "category", "source", "score"}]
    """
    if not query or not query.strip():
        return []
    k = top_k or settings.memory.recall_top_k
    try:
        vectors, _ = embed_texts([query.strip()], settings, embedding_override)
        query_vec = vectors[0]
    except Exception as e:
        logger.warning("recall_facts: embed failed, err=%s", e)
        return []

    client = get_es_client()
    index_name = settings.elasticsearch.memory_index_name
    threshold = settings.memory.recall_similarity_threshold
    try:
        resp = client.search(
            index=index_name,
            body={
                "size": k,
                "knn": {
                    "field": "embedding_vector",
                    "query_vector": query_vec,
                    "k": k,
                    "num_candidates": max(k * 10, 50),
                    "filter": [
                        {"term": {"user_id": str(user_id)}},
                        {"term": {"active": True}},
                    ],
                },
            },
        )
    except Exception as e:
        logger.warning("recall_facts: ES search failed, err=%s", e)
        return []

    out: list[dict[str, Any]] = []
    for hit in (resp.get("hits") or {}).get("hits") or []:
        score = float(hit.get("_score") or 0.0)
        if score < threshold:
            continue
        src = hit.get("_source") or {}
        out.append({
            "memory_id": src.get("memory_id"),
            "key": src.get("key"),
            "value": src.get("value"),
            "category": src.get("category"),
            "source": src.get("source"),
            "score": round(score, 4),
        })
    # 命中后, 异步刷一下 last_used_at (不阻塞主流程)
    if out:
        try:
            now_iso = _now_iso()
            for item in out:
                mid = item.get("memory_id")
                if mid:
                    client.update(
                        index=index_name,
                        id=mid,
                        body={"doc": {"last_used_at": now_iso}},
                    )
        except Exception:
            pass
    return out


def list_facts(
    settings: Settings,
    user_id: str | int,
    page: int = 1,
    size: int = 50,
) -> dict[str, Any]:
    """列出本用户所有 active 事实 (前端"我的记忆"页面用)."""
    client = get_es_client()
    index_name = settings.elasticsearch.memory_index_name
    page = max(1, int(page))
    size = max(1, min(200, int(size)))
    from_ = (page - 1) * size
    try:
        resp = client.search(
            index=index_name,
            body={
                "from": from_,
                "size": size,
                "query": {"bool": {"filter": [
                    {"term": {"user_id": str(user_id)}},
                    {"term": {"active": True}},
                ]}},
                "sort": [{"updated_at": {"order": "desc"}}],
            },
        )
    except Exception as e:
        logger.warning("list_facts: ES search failed, err=%s", e)
        return {"total": 0, "items": [], "page": page, "size": size}
    items = []
    for hit in (resp.get("hits") or {}).get("hits") or []:
        src = hit.get("_source") or {}
        items.append({
            "memory_id": src.get("memory_id"),
            "key": src.get("key"),
            "value": src.get("value"),
            "category": src.get("category"),
            "source": src.get("source"),
            "created_at": src.get("created_at"),
            "updated_at": src.get("updated_at"),
            "last_used_at": src.get("last_used_at"),
        })
    total = (resp.get("hits") or {}).get("total", {}).get("value", len(items))
    return {"total": int(total), "items": items, "page": page, "size": size}


def delete_fact(settings: Settings, user_id: str | int, memory_id: str) -> bool:
    """软删单条 fact (按 memory_id, 必须属本用户). 找不到返回 False."""
    if not memory_id:
        return False
    client = get_es_client()
    index_name = settings.elasticsearch.memory_index_name
    try:
        # 先按 _id 拿, 校验 user_id
        doc = client.get(index=index_name, id=memory_id, ignore=[404])
        if not doc.get("found"):
            return False
        src = doc.get("_source") or {}
        if str(src.get("user_id")) != str(user_id):
            logger.warning("delete_fact: user mismatch, want=%s, doc=%s", user_id, src.get("user_id"))
            return False
        client.update(
            index=index_name,
            id=memory_id,
            body={"doc": {"active": False, "updated_at": _now_iso()}},
        )
        return True
    except Exception as e:
        logger.warning("delete_fact: failed, err=%s", e)
        return False


def clear_facts(settings: Settings, user_id: str | int, hard: bool = False) -> int:
    """清空本用户所有 active fact.
    <p>默认软删 (active=false, 保留 ES doc 便于审计);
    <p>hard=True 物理删除 (GDPR 被遗忘权, 走 delete_by_query).
    """
    client = get_es_client()
    index_name = settings.elasticsearch.memory_index_name
    try:
        if hard:
            resp = client.delete_by_query(
                index=index_name,
                body={"query": {"bool": {"filter": [
                    {"term": {"user_id": str(user_id)}},
                ]}}},
                refresh=False,
            )
            return int(resp.get("deleted") or 0)
        resp = client.update_by_query(
            index=index_name,
            body={
                "query": {"bool": {"filter": [
                    {"term": {"user_id": str(user_id)}},
                    {"term": {"active": True}},
                ]}},
                "script": {
                    "source": "ctx._source.active = false; ctx._source.updated_at = params.now",
                    "params": {"now": _now_iso()},
                },
            },
            refresh=False,
        )
        return int(resp.get("updated") or 0)
    except Exception as e:
        logger.warning("clear_facts: failed, err=%s", e)
        return 0


def delete_fact_by_value(settings: Settings, user_id: str | int, key: str, value: str) -> int:
    """按 (user_id, key, value) 物理删 fact (用于 L2 矛盾事实: 用户撤回某事实).
    <p>返回实际删除条数.
    """
    if not key or not value:
        return 0
    client = get_es_client()
    index_name = settings.elasticsearch.memory_index_name
    try:
        resp = client.delete_by_query(
            index=index_name,
            body={"query": {"bool": {"filter": [
                {"term": {"user_id": str(user_id)}},
                {"term": {"key": key}},
                {"match": {"value": value}},
            ]}}},
            refresh=False,
        )
        return int(resp.get("deleted") or 0)
    except Exception as e:
        logger.warning("delete_fact_by_value: failed, err=%s", e)
        return 0
