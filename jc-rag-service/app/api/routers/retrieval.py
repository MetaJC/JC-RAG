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


"""检索/Embedding 路由: 检索测试 / Embedding 状态 / 模型列表.
<p>原位置: app/api/routes.py (拆分自 2026-07-03 结构重构, 业务零变更).

@author JC
"""
from __future__ import annotations

import logging
from typing import Any

from fastapi import APIRouter, Body

from app.api.deps import (
    _build_kb_filter,
    _build_query_vector,
    _extract_embedding_override,
    _to_ws_tokens,
)
from app.api.schemas import RetrievalTestRequest
from app.core.config import get_settings
from app.embeddings import get_embedding_status
from app.llm.provider_factory import ProviderFactory
from app.vector_store import get_vector_store

logger = logging.getLogger(__name__)
router = APIRouter()


@router.get("/embedding/status")
async def embedding_status() -> dict:
    settings = get_settings()
    return get_embedding_status(settings)


@router.post("/embedding/encode")
async def embedding_encode(payload: dict[str, Any] = Body(...)) -> dict:
    settings = get_settings()
    text = str(payload.get("text") or "").strip()
    if not text:
        return {"status": "FAILED", "message": "text 不能为空"}

    from app.embeddings import embed_texts
    vectors, meta = embed_texts([text], settings)
    preview = vectors[0][:8] if vectors and vectors[0] else []
    return {"status": "SUCCESS", "meta": meta, "preview": preview}


@router.post("/retrieval/test")
async def retrieval_test(request: RetrievalTestRequest) -> dict:
    settings = get_settings()
    payload = request.model_dump(exclude_none=True)

    question = str(payload.get("question") or "").strip()
    kb_filter = _build_kb_filter(payload.get("kbId"))
    top_k = int(payload.get("topK") or settings.retrieval.top_k)
    top_n = int(payload.get("topN") or settings.retrieval.top_n)
    embedding_override = _extract_embedding_override(payload)
    query_vector = _build_query_vector(question, settings, embedding_override)

    vector_hits: list[dict[str, Any]] = []
    vector_error: str | None = None

    try:
        vs = get_vector_store()
        vector_hits = vs.vector_search(
            query_vector,
            filter=kb_filter,
            top_k=top_k,
            top_n=top_n,
        )
    except Exception as ex:
        vector_error = str(ex)

    text_hits: list[dict[str, Any]] = []
    try:
        vs = get_vector_store()
        question_ws = _to_ws_tokens(str(question))
        text_hits = vs.text_search(question_ws, filter=kb_filter, top_k=top_k)
    except Exception:
        text_hits = []

    def _brief(hit: dict[str, Any]) -> dict[str, Any]:
        source = hit.get("_source") or {}
        text = str(source.get("chunk_text") or "")
        return {
            "score": hit.get("_score"),
            "kbId": source.get("kb_id"),
            "documentId": source.get("document_id"),
            "chunkId": source.get("chunk_id"),
            "fileName": source.get("file_name"),
            "excerpt": text[:160],
        }

    return {
        "status": "SUCCESS",
        "embedding": get_embedding_status(settings, embedding_override),
        "vector": {
            "count": len(vector_hits),
            "error": vector_error,
            "hits": [_brief(h) for h in vector_hits],
        },
        "text": {
            "count": len(text_hits),
            "hits": [_brief(h) for h in text_hits],
        },
    }


@router.get("/providers/models")
async def providers_models() -> dict:
    settings = get_settings()
    provider_factory = ProviderFactory(settings)
    return await provider_factory.list_models()
