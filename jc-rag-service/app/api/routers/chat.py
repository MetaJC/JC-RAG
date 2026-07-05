"""聊天核心路由: 流式问答 / 同步问答 / 会话标题.
<p>原位置: app/api/routes.py (拆分自 2026-07-03 结构重构, 业务零变更).

@author JC
"""
from __future__ import annotations

import json
import logging
from typing import Any

from fastapi import APIRouter
from fastapi.responses import StreamingResponse

from app.api.deps import (
    _build_kb_filter,
    _build_query_vector,
    _extract_embedding_override,
)
from app.api.schemas import ChatAskRequest
from app.core.config import get_settings

logger = logging.getLogger(__name__)
router = APIRouter()


@router.post("/chat/ask/stream")
async def chat_ask_stream(request: ChatAskRequest):
    """SSE 流式问答：检索 → 拼 prompt → 流式生成（支持短期记忆 history）"""
    from app.llm.provider_factory import ProviderFactory
    from app.utils import text_search, vector_search

    settings = get_settings()
    payload = request.model_dump(exclude_none=True)
    question = str(payload.get("question") or "").strip()
    kb_id = payload.get("kbId")
    kb_filter = _build_kb_filter(kb_id)
    top_k = int(payload.get("topK") or settings.retrieval.top_k)
    top_n = int(payload.get("topN") or settings.retrieval.top_n)
    similarity_threshold = float(
        payload.get("similarityThreshold") or settings.retrieval.similarity_threshold
    )
    embedding_override = _extract_embedding_override(payload)
    query_vector = _build_query_vector(question, settings, embedding_override)
    provider_override = payload.get("providerOverride")
    system_prompt = payload.get("systemPrompt")
    agent_id = payload.get("agentId")
    deep_thinking = bool(payload.get("deepThinking"))
    attachments = payload.get("attachments") or []
    history = payload.get("history") or []
    chat_template_kwargs = payload.get("chatTemplateKwargs")
    # 用户级 prompt 片段 (只影响 user 层, 不可触及 system 层, 由 Java 端校验后下发)
    user_prompt_suffix = payload.get("userPromptSuffix") or ""

    def _apply_user_suffix(msgs: list[dict]) -> list[dict]:
        if not user_prompt_suffix:
            return msgs
        for i in range(len(msgs) - 1, -1, -1):
            if msgs[i].get("role") == "user":
                content = msgs[i].get("content", "")
                msgs[i]["content"] = f"{user_prompt_suffix}\n\n{content}" if content else user_prompt_suffix
                break
        return msgs

    async def _gen():
        try:
            context_lines: list[str] = []
            references: list[dict] = []
            if kb_id is not None:
                try:
                    vec_hits = vector_search(query_vector, kb_filter, top_k, top_n)
                    for h in vec_hits:
                        src = h.get("_source") or {}
                        text = str(src.get("chunk_text") or "").strip()
                        if text:
                            context_lines.append(text)
                            references.append({
                                "chunkId": src.get("chunk_id"),
                                "documentId": src.get("document_id"),
                                "fileName": src.get("file_name"),
                                "score": h.get("_score"),
                                "excerpt": text[:160],
                            })
                    if not vec_hits:
                        try:
                            text_hits = text_search(question, kb_filter, top_k)
                            for h in text_hits:
                                src = h.get("_source") or {}
                                text = str(src.get("chunk_text") or "").strip()
                                if text and text not in context_lines:
                                    context_lines.append(text)
                                    references.append({
                                        "chunkId": src.get("chunk_id"),
                                        "documentId": src.get("document_id"),
                                        "fileName": src.get("file_name"),
                                        "score": h.get("_score"),
                                        "excerpt": text[:160],
                                    })
                        except Exception:
                            pass
                except Exception as ex:
                    logger.warning("vector_search failed: %s", str(ex))

            yield f"data: {json.dumps({'type': 'thinking', 'message': '正在准备回答...'}, ensure_ascii=False)}\n\n"

            rag_prompt = ""
            if context_lines:
                ctx_text = "\n\n".join(context_lines[:top_k])
                rag_prompt = (
                    f"以下是知识库中可能与问题相关的内容片段，请基于这些内容并结合你的知识回答。"
                    f"如果内容与问题无关，直接回答用户问题即可。\n\n"
                    f"参考内容：\n{ctx_text}\n\n"
                    f"用户问题：{question}"
                )
            else:
                rag_prompt = question

            provider = ProviderFactory(settings)
            messages: list[dict[str, Any]] = []
            if system_prompt:
                messages.append({"role": "system", "content": system_prompt})
            for h in history:
                role = str(h.get("role") or "user").strip()
                content = str(h.get("content") or "").strip()
                if role not in ("user", "assistant", "system"):
                    continue
                if not content:
                    continue
                messages.append({"role": role, "content": content})
            messages.append({"role": "user", "content": rag_prompt})
            _apply_user_suffix(messages)

            # [DIAG] 记录最终发给 LLM 的 messages (前 200 字符)
            try:
                _diag = []
                for _m in messages:
                    _diag.append({"role": _m.get("role"), "content_preview": str(_m.get("content",""))[:200]})
                logger.info("[DIAG] LLM_messages=%s", json.dumps(_diag, ensure_ascii=False))
            except Exception:
                pass

            full_content = ""
            async for delta in provider.stream_generate(
                "",
                messages=messages,
                provider_override=provider_override,
                chat_template_kwargs=chat_template_kwargs,
            ):
                if not delta:
                    continue
                # PII 脱敏: 流式输出时实时遮蔽
                try:
                    from app.security.sensitive_cache import mask_pii
                    delta = mask_pii(delta)
                except Exception:
                    pass
                full_content += delta
                yield f"data: {json.dumps({'type': 'delta', 'delta': delta}, ensure_ascii=False)}\n\n"
                # 高危输出命中: 中断流
                try:
                    from app.security.sensitive_cache import SensitiveWordCache
                    _r = SensitiveWordCache.get().filter(delta)
                    if _r.is_deny and delta:
                        yield f"data: {json.dumps({'type': 'error', 'error': '输出包含敏感内容, 已中断'}, ensure_ascii=False)}\n\n"
                        return
                except Exception:
                    pass

            # 流结束: 对 full_content 做一次完整敏感词扫描
            try:
                from app.security.sensitive_cache import SensitiveWordCache
                _fr = SensitiveWordCache.get().filter(full_content)
                if _fr.is_deny:
                    return
                if _fr.is_mask and _fr.masked != full_content:
                    yield f"data: {json.dumps({'type': 'output_mask', 'original_len': len(full_content), 'masked': _fr.masked}, ensure_ascii=False)}\n\n"
            except Exception:
                pass

            yield f"data: {json.dumps({'type': 'done', 'references': references, 'fullContent': full_content}, ensure_ascii=False)}\n\n"
        except Exception as ex:
            logger.error("chat_ask_stream failed: %s", str(ex), exc_info=True)
            yield f"data: {json.dumps({'type': 'error', 'error': str(ex)}, ensure_ascii=False)}\n\n"

    return StreamingResponse(_gen(), media_type="text/event-stream")


@router.post("/chat/ask")
async def chat_ask(request: ChatAskRequest) -> dict:
    """同步问答（被 Java UserChatServiceImpl.callPythonAsk 调用）"""
    from app.llm.provider_factory import ProviderFactory
    from app.utils import text_search, vector_search

    settings = get_settings()
    payload = request.model_dump(exclude_none=True)
    question = str(payload.get("question") or "").strip()
    kb_id = payload.get("kbId")
    kb_filter = _build_kb_filter(kb_id)
    top_k = int(payload.get("topK") or settings.retrieval.top_k)
    top_n = int(payload.get("topN") or settings.retrieval.top_n)
    similarity_threshold = float(
        payload.get("similarityThreshold") or settings.retrieval.similarity_threshold
    )
    embedding_override = _extract_embedding_override(payload)
    query_vector = _build_query_vector(question, settings, embedding_override)
    provider_override = payload.get("providerOverride")
    system_prompt = payload.get("systemPrompt")
    agent_id = payload.get("agentId")
    deep_thinking = bool(payload.get("deepThinking"))
    attachments = payload.get("attachments") or []
    history = payload.get("history") or []
    chat_template_kwargs = payload.get("chatTemplateKwargs")

    context_lines: list[str] = []
    references: list[dict] = []
    if kb_id is not None:
        try:
            vec_hits = vector_search(query_vector, kb_filter, top_k, top_n)
            for h in vec_hits:
                src = h.get("_source") or {}
                text = str(src.get("chunk_text") or "").strip()
                if text:
                    context_lines.append(text)
                    references.append({
                        "chunkId": src.get("chunk_id"),
                        "documentId": src.get("document_id"),
                        "fileName": src.get("file_name"),
                        "score": h.get("_score"),
                        "excerpt": text[:160],
                    })
            if not vec_hits:
                try:
                    text_hits = text_search(question, kb_filter, top_k)
                    for h in text_hits:
                        src = h.get("_source") or {}
                        text = str(src.get("chunk_text") or "").strip()
                        if text and text not in context_lines:
                            context_lines.append(text)
                            references.append({
                                "chunkId": src.get("chunk_id"),
                                "documentId": src.get("document_id"),
                                "fileName": src.get("file_name"),
                                "score": h.get("_score"),
                                "excerpt": text[:160],
                            })
                except Exception:
                    pass
        except Exception as ex:
            logger.warning("vector_search failed: %s", str(ex))

    rag_prompt = ""
    if context_lines:
        ctx_text = "\n\n".join(context_lines[:top_k])
        rag_prompt = (
            f"以下是知识库中可能与问题相关的内容片段，请基于这些内容并结合你的知识回答。"
            f"如果内容与问题无关，直接回答用户问题即可。\n\n"
            f"参考内容：\n{ctx_text}\n\n"
            f"用户问题：{question}"
        )
    else:
        rag_prompt = question

    provider = ProviderFactory(settings)
    messages: list[dict[str, Any]] = []
    if system_prompt:
        messages.append({"role": "system", "content": system_prompt})
    for h in history:
        role = str(h.get("role") or "user").strip()
        content = str(h.get("content") or "").strip()
        if role not in ("user", "assistant", "system"):
            continue
        if not content:
            continue
        messages.append({"role": role, "content": content})
    messages.append({"role": "user", "content": rag_prompt})
    _apply_user_suffix(messages)

    full_content = ""
    try:
        async for delta in provider.stream_generate(
            "",
            messages=messages,
            provider_override=provider_override,
            chat_template_kwargs=chat_template_kwargs,
        ):
            if not delta:
                continue
            full_content += delta
    except Exception as ex:
        logger.error("chat_ask LLM failed: %s", str(ex), exc_info=True)
        return {"status": "FAILED", "message": str(ex), "references": references}

    return {
        "status": "SUCCESS",
        "content": full_content,
        "references": references,
    }


@router.post("/chat/title")
async def chat_title(request: dict[str, Any]) -> dict:
    """生成会话标题（基于首轮问答）"""
    from app.llm.provider_factory import ProviderFactory

    question = str(request.get("question") or "").strip()
    answer = str(request.get("answer") or "").strip()
    if not question:
        return {"status": "FAILED", "message": "question 不能为空"}

    settings = get_settings()
    provider = ProviderFactory(settings)
    prompt = (
        f"请根据以下对话生成一个不超过 20 字的会话标题，不要带标点符号和引号。\n"
        f"用户问题：{question}\n"
        f"AI 回答：{answer[:200]}\n"
        f"标题："
    )
    title = ""
    try:
        async for delta in provider.stream_generate(
            "",
            messages=[{"role": "user", "content": prompt}],
            provider_override=request.get("providerOverride"),
        ):
            if not delta:
                continue
            title += delta
    except Exception as ex:
        logger.error("chat_title failed: %s", str(ex))
        return {"status": "FAILED", "message": str(ex)}

    title = title.strip().splitlines()[0][:30] if title else ""
    return {"status": "SUCCESS", "title": title}


@router.post("/chat/summary")
async def chat_summary(request: dict[str, Any]) -> dict:
    """生成会话摘要 (L1 摘要压缩, 被 Java UserChatServiceImpl.generateSummaryAsync 调用).
    <p>接收多轮对话 (messages: [{role, content}]), 返回一段压缩后的中文摘要.
    <p>降级策略: LLM 调用失败 3 次后, 由 Java 侧走"取前 5 轮直接拼接"兜底, 不在本端点处理.
    """
    from app.llm.provider_factory import ProviderFactory

    messages_raw = request.get("messages") or []
    provider_override = request.get("providerOverride")
    if not isinstance(messages_raw, list) or len(messages_raw) == 0:
        return {"status": "FAILED", "message": "messages 不能为空"}

    # 拼装对话文本 (截断每条 content, 避免单条超长拖垮 LLM)
    lines: list[str] = []
    for m in messages_raw:
        role = str(m.get("role") or "").strip()
        content = str(m.get("content") or "").strip()
        if role not in ("user", "assistant"):
            continue
        if not content:
            continue
        # 单条最多 1500 字, 超过截断
        if len(content) > 1500:
            content = content[:1500] + "..."
        lines.append(f"{'用户' if role == 'user' else 'AI'}：{content}")
    if not lines:
        return {"status": "FAILED", "message": "messages 内容为空"}

    dialog_text = "\n".join(lines)
    prompt = (
        "请将以下多轮对话压缩为一段简洁的中文摘要，要求：\n"
        "1) 用第三人称叙述；\n"
        "2) 保留关键事实、用户偏好、未完成的任务或待澄清的问题；\n"
        "3) 控制在 300 字以内；\n"
        "4) 不要复述寒暄、客套与重复内容。\n\n"
        f"对话内容：\n{dialog_text}\n\n"
        "摘要："
    )

    settings = get_settings()
    provider = ProviderFactory(settings)
    summary = ""
    try:
        async for delta in provider.stream_generate(
            "",
            messages=[{"role": "user", "content": prompt}],
            provider_override=provider_override,
        ):
            if not delta:
                continue
            summary += delta
    except Exception as ex:
        logger.error("chat_summary LLM failed: %s", str(ex))
        return {"status": "FAILED", "message": str(ex)}

    summary = summary.strip()
    # 取首行 (避免模型输出多段)
    summary = summary.splitlines()[0] if summary else ""
    if len(summary) > 600:
        summary = summary[:600] + "..."
    if not summary:
        return {"status": "FAILED", "message": "LLM 返回空摘要"}
    return {"status": "SUCCESS", "summary": summary}
