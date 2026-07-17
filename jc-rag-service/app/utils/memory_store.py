"""L2 长期事实向量存储封装.
<p>独立于 KAG 索引 (jc_rag_chunks), 使用 jc_chat_memory 存储用户长期记忆.
<p>所有 CRUD / 检索 / 去重都走 VectorStore 抽象层 (后端无关).

@author JC
"""
from __future__ import annotations

import logging
import re
import uuid
from typing import Any

from app.core.config import Settings
from app.embeddings import embed_texts
from app.vector_store import FilterExpr, get_vector_store

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


def ensure_memory_index(settings: Settings) -> None:
    """启动时调用: 确保 jc_chat_memory 索引存在, 不存在则建.

    <p>支持多后端: ES 走 es_client, Milvus (Pro 注入) 走 ensure_memory_collection.
    <p>其他后端若无 memory schema 支持则 warn 跳过.
    """
    from app.vector_store import get_vector_store

    index_name = settings.elasticsearch.memory_index_name
    vs = get_vector_store()

    if vs.backend_name() == "elasticsearch":
        # ES 后端: 沿用旧路径直接 create index (mapping 与 chunks 不同)
        from app.utils.es_client import get_es_client

        client = get_es_client()
        if client.indices.exists(index=index_name):
            return
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
        return

    # Milvus / 其他后端: duck typing 调 ensure_memory_collection
    if hasattr(vs, "ensure_memory_collection"):
        try:
            from app.embeddings import get_embedding_dimension
            dims = get_embedding_dimension(settings)
        except Exception:
            dims = int(settings.embedding.dims or 1024)
        vs.ensure_memory_collection(index_name, dims)
        logger.info(
            "ensure_memory_index: created %s collection '%s' (dims=%d)",
            vs.backend_name(), index_name, dims,
        )
        return

    logger.warning(
        "ensure_memory_index: backend=%s 不支持 memory collection, 跳过 (L2 记忆功能不可用)",
        vs.backend_name(),
    )


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
        vs = get_vector_store()
        index_name = settings.elasticsearch.memory_index_name
        # knn 必须传 query_vector, 用全零向量以走通接口, 再用 filter 强约束
        # 实际由 filter (user_id + active + category) 决定命中, 不依赖相似度
        try:
            from app.embeddings import get_embedding_dimension
            dims = get_embedding_dimension(settings)
        except Exception:
            dims = int(settings.embedding.dims or 1024)
        zero_vec = [0.0] * dims
        f_user = FilterExpr.eq("user_id", str(user_id))
        f_active = FilterExpr.eq("active", True)
        f_cats = FilterExpr.in_("category", categories)
        combined = f_user.and_(f_active).and_(f_cats)
        hits = vs.vector_search(
            zero_vec,
            collection=index_name,
            filter=combined,
            top_k=1000,
            top_n=1000,
        )
        for hit in hits:
            src = hit.get("_source") or {}
            cat = src.get("category", "other")
            existing_by_cat.setdefault(cat, []).append(src)
    except Exception as e:
        logger.warning("upsert_facts: fetch existing failed, err=%s", e)

    # 4. 去重比对 + 写入
    vs = get_vector_store()
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
                    vs.update_by_id(
                        doc_id,
                        {
                            "value": fact["value"],
                            "key": fact["key"],
                            "source": "auto",
                            "embedding_vector": vec,
                            "last_used_at": now_iso,
                            "updated_at": now_iso,
                        },
                        collection=index_name,
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
            vs.index_one(memory_id, doc, collection=index_name)
            inserted += 1
            details.append({"action": "inserted", "memory_id": memory_id})
        except Exception as e:
            logger.warning("upsert_facts: write one fact failed, err=%s", e)
            skipped += 1
            details.append({"action": "skipped", "error": str(e)})

    # 5. 超量淘汰: 超过 max_per_user, 按 last_used_at 升序软删最旧的
    try:
        max_per = settings.memory.max_per_user
        f_user = FilterExpr.eq("user_id", str(user_id))
        f_active = FilterExpr.eq("active", True)
        total = vs.count(filter=f_user.and_(f_active), collection=index_name)
        if total > max_per:
            over = total - max_per
            # 找最旧的 over 条
            try:
                from app.embeddings import get_embedding_dimension
                dims = get_embedding_dimension(settings)
            except Exception:
                dims = int(settings.embedding.dims or 1024)
            zero_vec = [0.0] * dims
            # 拉一批 (按 updated_at asc 排序, 抽象层暂不暴露 sort, 走全量后 Python 侧排序)
            all_hits = vs.vector_search(
                zero_vec,
                collection=index_name,
                filter=f_user.and_(f_active),
                top_k=max(over * 2, 50),
                top_n=max(over * 2, 50),
            )
            all_hits.sort(key=lambda h: (h.get("_source") or {}).get("last_used_at") or "")
            evict_ids = [(h.get("_source") or {}).get("memory_id") for h in all_hits[:over]]
            now_iso2 = _now_iso()
            for mid in evict_ids:
                if mid:
                    vs.update_by_id(
                        mid,
                        {"active": False, "updated_at": now_iso2},
                        collection=index_name,
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

    vs = get_vector_store()
    index_name = settings.elasticsearch.memory_index_name
    threshold = settings.memory.recall_similarity_threshold
    try:
        f_user = FilterExpr.eq("user_id", str(user_id))
        f_active = FilterExpr.eq("active", True)
        resp_hits = vs.vector_search(
            query_vec,
            collection=index_name,
            filter=f_user.and_(f_active),
            top_k=k,
            top_n=max(k * 10, 50),
        )
    except Exception as e:
        logger.warning("recall_facts: vector_search failed, err=%s", e)
        return []

    out: list[dict[str, Any]] = []
    for hit in resp_hits:
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
                    vs.update_by_id(
                        mid,
                        {"last_used_at": now_iso},
                        collection=index_name,
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
    vs = get_vector_store()
    index_name = settings.elasticsearch.memory_index_name
    page = max(1, int(page))
    size = max(1, min(200, int(size)))
    try:
        f_user = FilterExpr.eq("user_id", str(user_id))
        f_active = FilterExpr.eq("active", True)
        # 抽象层不暴露 sort, Python 侧拉一批后 sort 截断
        try:
            from app.embeddings import get_embedding_dimension
            dims = get_embedding_dimension(settings)
        except Exception:
            dims = int(settings.embedding.dims or 1024)
        zero_vec = [0.0] * dims
        all_hits = vs.vector_search(
            zero_vec,
            collection=index_name,
            filter=f_user.and_(f_active),
            top_k=settings.memory.max_per_user,
            top_n=settings.memory.max_per_user,
        )
    except Exception as e:
        logger.warning("list_facts: vector_search failed, err=%s", e)
        return {"total": 0, "items": [], "page": page, "size": size}
    all_hits.sort(
        key=lambda h: (h.get("_source") or {}).get("updated_at") or "",
        reverse=True,
    )
    from_ = (page - 1) * size
    page_hits = all_hits[from_:from_ + size]
    items = []
    for hit in page_hits:
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
    total = len(all_hits)
    return {"total": int(total), "items": items, "page": page, "size": size}


def delete_fact(settings: Settings, user_id: str | int, memory_id: str) -> bool:
    """软删单条 fact (按 memory_id, 必须属本用户). 找不到返回 False."""
    if not memory_id:
        return False
    vs = get_vector_store()
    index_name = settings.elasticsearch.memory_index_name
    try:
        src = vs.get_by_id(memory_id, collection=index_name)
        if src is None:
            return False
        if str(src.get("user_id")) != str(user_id):
            logger.warning("delete_fact: user mismatch, want=%s, doc=%s", user_id, src.get("user_id"))
            return False
        vs.update_by_id(
            memory_id,
            {"active": False, "updated_at": _now_iso()},
            collection=index_name,
        )
        return True
    except Exception as e:
        logger.warning("delete_fact: failed, err=%s", e)
        return False


def clear_facts(settings: Settings, user_id: str | int, hard: bool = False) -> int:
    """清空本用户所有 active fact.
    <p>默认软删 (active=false, 保留 doc 便于审计);
    <p>hard=True 物理删除 (GDPR 被遗忘权, 走 delete_by_query).
    """
    vs = get_vector_store()
    index_name = settings.elasticsearch.memory_index_name
    try:
        f_user = FilterExpr.eq("user_id", str(user_id))
        if hard:
            return vs.delete_by_filter(f_user, collection=index_name)
        f_active = FilterExpr.eq("active", True)
        now = _now_iso()
        return vs.update_by_filter(
            f_user.and_(f_active),
            script_source="ctx._source.active = false; ctx._source.updated_at = params.now",
            script_params={"now": now},
            collection=index_name,
        )
    except Exception as e:
        logger.warning("clear_facts: failed, err=%s", e)
        return 0


def delete_fact_by_value(settings: Settings, user_id: str | int, key: str, value: str) -> int:
    """按 (user_id, key, value) 物理删 fact (用于 L2 矛盾事实: 用户撤回某事实).
    <p>返回实际删除条数.
    <p>实现: 走 vector_search 拉一批, Python 侧用 value 字段精确匹配后 delete_by_ids.
    """
    if not key or not value:
        return 0
    vs = get_vector_store()
    index_name = settings.elasticsearch.memory_index_name
    try:
        # 走 filter: user_id + key, value 用 ES 全文 match (后续 Python 侧再精确比 value)
        # 抽象层 text_search 走的是 chunk_text 字段, 与 L2 记忆 value 字段不匹配, 故用 vector_search
        try:
            from app.embeddings import get_embedding_dimension
            dims = get_embedding_dimension(settings)
        except Exception:
            dims = int(settings.embedding.dims or 1024)
        zero_vec = [0.0] * dims
        f_user = FilterExpr.eq("user_id", str(user_id))
        f_key = FilterExpr.eq("key", key)
        hits = vs.vector_search(
            zero_vec,
            collection=index_name,
            filter=f_user.and_(f_key),
            top_k=500,
            top_n=500,
        )
        # 二次精确匹配 value (避免 match 全文带来的误匹配)
        match_ids = [
            (h.get("_source") or {}).get("memory_id")
            for h in hits
            if (h.get("_source") or {}).get("value") == value
        ]
        match_ids = [mid for mid in match_ids if mid]
        if not match_ids:
            return 0
        return vs.delete_by_ids(match_ids, collection=index_name)
    except Exception as e:
        logger.warning("delete_fact_by_value: failed, err=%s", e)
        return 0
