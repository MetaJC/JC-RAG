"""L2 长期事实路由: 提取 / 召回 / 列表 / 删除 / 清空.
<p>被 Java UserChatServiceImpl 异步 (extract) 和同步 (recall) 调用.
<p>原位置: app/api/routes.py (拆分自 2026-07-03 结构重构, 业务零变更).

@author JC
"""
from __future__ import annotations

import json
import logging
import re
from typing import Any

from fastapi import APIRouter, Query

from app.core.config import get_settings
from app.utils import memory_store

logger = logging.getLogger(__name__)
router = APIRouter()


# 提取结果 JSON 抽取 (LLM 可能包 ```json``` 围栏)
_JSON_FENCE = re.compile(r"```(?:json)?\s*([\s\S]+?)\s*```", re.IGNORECASE)
_JSON_OBJ = re.compile(r"\{[\s\S]+?\}")


def _extract_facts_json(raw: str) -> list[dict[str, Any]]:
    """从 LLM 输出中抽 JSON 数组. 容错: 先去围栏, 再尝试整体/裁剪匹配."""
    if not raw:
        return []
    text = raw.strip()
    # 优先匹配 ```json ... ```
    m = _JSON_FENCE.search(text)
    if m:
        text = m.group(1).strip()
    # 尝试整体当数组
    try:
        obj = json.loads(text)
        if isinstance(obj, list):
            return [x for x in obj if isinstance(x, dict)]
        if isinstance(obj, dict):
            # 兼容 {facts: [...]} 包装
            inner = obj.get("facts")
            if isinstance(inner, list):
                return [x for x in inner if isinstance(x, dict)]
    except Exception:
        pass
    # 尝试抽取第一个 [...] 段
    m2 = re.search(r"\[[\s\S]+\]", text)
    if m2:
        try:
            obj = json.loads(m2.group(0))
            if isinstance(obj, list):
                return [x for x in obj if isinstance(x, dict)]
        except Exception:
            pass
    # 退而求其次: 逐个找 {...} 对象
    found: list[dict[str, Any]] = []
    for m3 in _JSON_OBJ.finditer(text):
        try:
            obj = json.loads(m3.group(0))
            if isinstance(obj, dict):
                found.append(obj)
        except Exception:
            pass
    return found


def _extract_facts_response(raw: str) -> dict[str, Any]:
    """从 LLM 输出抽 {facts:[...], retract:[...]} 结构.
    <p>LLM 可能输出:
    <ul>
      <li>纯 facts 数组: [{key,value,category}, ...] -> {facts: [...], retract: []}</li>
      <li>带 retract 的对象: {facts:[...], retract:[{key,value}, ...]}</li>
    </ul>
    <p>retract 数组的元素是用户撤回/否定的事实, 系统会物理删除.
    """
    out: dict[str, Any] = {"facts": [], "retract": []}
    if not raw:
        return out
    text = raw.strip()
    m = _JSON_FENCE.search(text)
    if m:
        text = m.group(1).strip()
    # 整体尝试
    try:
        obj = json.loads(text)
        if isinstance(obj, dict):
            f = obj.get("facts")
            r = obj.get("retract")
            if isinstance(f, list):
                out["facts"] = [x for x in f if isinstance(x, dict)]
            if isinstance(r, list):
                out["retract"] = [x for x in r if isinstance(x, dict)]
            return out
        if isinstance(obj, list):
            out["facts"] = [x for x in obj if isinstance(x, dict)]
            return out
    except Exception:
        pass
    # 截取首个 {...} 段
    m2 = re.search(r"\{[\s\S]+\}", text)
    if m2:
        try:
            obj = json.loads(m2.group(0))
            if isinstance(obj, dict):
                f = obj.get("facts")
                r = obj.get("retract")
                if isinstance(f, list):
                    out["facts"] = [x for x in f if isinstance(x, dict)]
                if isinstance(r, list):
                    out["retract"] = [x for x in r if isinstance(x, dict)]
                return out
        except Exception:
            pass
    # 截取首个 [...] 段 (兼容旧纯数组)
    m3 = re.search(r"\[[\s\S]+\]", text)
    if m3:
        try:
            obj = json.loads(m3.group(0))
            if isinstance(obj, list):
                out["facts"] = [x for x in obj if isinstance(x, dict)]
                return out
        except Exception:
            pass
    # 兜底: 逐个 {...}
    facts: list[dict[str, Any]] = []
    for m4 in _JSON_OBJ.finditer(text):
        try:
            obj = json.loads(m4.group(0))
            if isinstance(obj, dict):
                facts.append(obj)
        except Exception:
            pass
    out["facts"] = facts
    return out


@router.post("/chat/memory/extract")
async def chat_memory_extract(request: dict[str, Any]) -> dict:
    """异步提取 fact 入库 (被 Java UserChatServiceImpl.extractMemoryAsync 调用).
    <p>请求: {userId, sessionId, messages, providerOverride, embeddingOverride}
    <p>响应: {status, inserted, updated, skipped, details, error?}
    """
    settings = get_settings()
    user_id = request.get("userId")
    session_id = request.get("sessionId")
    messages = request.get("messages") or []
    provider_override = request.get("providerOverride")
    embedding_override = request.get("embeddingOverride")
    if not user_id or not session_id or not isinstance(messages, list) or len(messages) == 0:
        return {"status": "FAILED", "message": "userId/sessionId/messages 必填"}
    if not settings.memory.enabled:
        return {"status": "SKIPPED", "message": "memory disabled in config"}

    # 1. 拼对话文本 (单条截断)
    lines: list[str] = []
    for m in messages:
        role = str(m.get("role") or "").strip()
        content = str(m.get("content") or "").strip()
        if role not in ("user", "assistant") or not content:
            continue
        if len(content) > 800:
            content = content[:800] + "..."
        lines.append(f"{'用户' if role == 'user' else 'AI'}：{content}")
    if not lines:
        return {"status": "SKIPPED", "message": "no valid messages"}
    dialog_text = "\n".join(lines)

    # 2. 拉已存在的 fact (同用户), 作为去重提示给 LLM
    existing_resp = memory_store.list_facts(settings, user_id, page=1, size=200)
    existing_lines: list[str] = []
    for it in existing_resp.get("items", []) or []:
        existing_lines.append(
            f"- [{it.get('category','other')}] {it.get('key')}: {it.get('value')}"
        )
    existing_text = "\n".join(existing_lines) if existing_lines else "(暂无)"

    # 3. 拼 LLM prompt
    categories = ", ".join(settings.memory.categories)
    prompt = (
        "请从以下用户与 AI 的最新对话中, 提取关于**该用户**的长期关键事实 (会影响未来对话). "
        "提取标准 (同时满足):\n"
        "  1) 会影响未来对话  2) 用户明确表达  3) 不易随时间失效\n"
        f"分类枚举 (强约束, 必须用其一): {categories}\n"
        "优先: 技术栈/职业/项目背景/长期偏好/明确目标\n"
        "严禁提取: 临时情绪/一次性事件/纯闲聊/敏感信息 (身份证/手机号/邮箱/密码/API key)\n"
        "**严禁编造**: 仅当用户在本对话或已知事实中明确表达, 才可提取. 不确定就不返回.\n"
        f"最多 {settings.memory.extract_top_k} 条; 已有事实 (去重用, 同类可更新):\n{existing_text}\n\n"
        f"对话内容:\n{dialog_text}\n\n"
        "严格输出 JSON 数组, 元素形如 "
        '{"key":"tech_stack","value":"Spring Cloud Alibaba","category":"tech_stack"}'
        ", 不要再加任何解释/前缀/后缀/代码围栏. 提取不到就输出 [].\n\n"
        "**矛盾事实处理**: 如果用户在对话中明确撤回/否定/改变某已知事实 "
        "(如 '我不用 X 了' / '其实我学的是 Y, 不是 X' / '我换工作了'), "
        "请在 `retract` 数组中输出 {key, value} (值与已知事实一致), "
        "系统会物理删除该 fact. 没有矛盾就输出 retract: []."
    )

    # 4. 调 LLM
    from app.llm.provider_factory import ProviderFactory
    provider = ProviderFactory(settings)
    raw = ""
    try:
        async for delta in provider.stream_generate(
            "",
            messages=[{"role": "user", "content": prompt}],
            provider_override=provider_override,
        ):
            if not delta:
                continue
            raw += delta
    except Exception as ex:
        logger.error("chat_memory_extract: LLM failed, err=%s", ex)
        return {"status": "FAILED", "message": f"LLM error: {ex}"}

    # 5. 抽 JSON (兼容两种: 纯 facts 数组, 或 {facts, retract} 对象)
    raw_obj = _extract_facts_response(raw)
    facts = raw_obj.get("facts") or []
    retracts = raw_obj.get("retract") or []
    if not facts:
        logger.info("chat_memory_extract: LLM returned no facts, raw=%s", raw[:200])
    # 处理 retract (L2 矛盾事实): 逐条物理删除
    retracted = 0
    for r in retracts:
        if not isinstance(r, dict):
            continue
        k = str(r.get("key") or "").strip()
        v = str(r.get("value") or "").strip()
        if not k or not v:
            continue
        try:
            n = memory_store.delete_fact_by_value(settings, user_id, k, v)
            retracted += n
        except Exception as ex:
            logger.warning("chat_memory_extract: retract failed, key=%s, err=%s", k, ex)

    # 6. 截到 extract_top_k
    if len(facts) > settings.memory.extract_top_k:
        facts = facts[:settings.memory.extract_top_k]

    # 7. 入库
    result = memory_store.upsert_facts(
        settings, user_id, session_id, facts, embedding_override=embedding_override
    )
    if retracted:
        result["retracted"] = retracted
    return {"status": "SUCCESS", **result}


@router.post("/chat/memory/recall")
async def chat_memory_recall(request: dict[str, Any]) -> dict:
    """同步召回 fact (被 Java UserChatServiceImpl.buildPayload 调用).
    <p>请求: {userId, question, topK?, providerOverride, embeddingOverride}
    <p>响应: {status, items, formattedPrompt}
    """
    settings = get_settings()
    user_id = request.get("userId")
    question = str(request.get("question") or "").strip()
    top_k = request.get("topK")
    embedding_override = request.get("embeddingOverride")
    if not user_id or not question:
        return {"status": "FAILED", "message": "userId/question 必填"}
    if not settings.memory.enabled:
        return {"status": "SKIPPED", "items": [], "formattedPrompt": ""}

    try:
        k = int(top_k) if top_k else settings.memory.recall_top_k
    except Exception:
        k = settings.memory.recall_top_k
    items = memory_store.recall_facts(settings, user_id, question, top_k=k,
                                      embedding_override=embedding_override)
    formatted = ""
    if items:
        lines = ["[用户长期记忆 (L2) — 来自历史会话, AI 可引用]"]
        for i, it in enumerate(items, 1):
            lines.append(f"{i}. [{it.get('category','other')}] {it.get('key')}: {it.get('value')}")
        formatted = "\n".join(lines)
    return {"status": "SUCCESS", "items": items, "formattedPrompt": formatted}


@router.get("/chat/memory/list")
async def chat_memory_list(
    userId: str = Query(..., description="用户 ID"),
    page: int = Query(1, ge=1, description="页码"),
    size: int = Query(50, ge=1, le=200, description="每页条数"),
) -> dict:
    """前端"我的记忆"页面: 列本用户所有 active fact."""
    settings = get_settings()
    if not settings.memory.enabled:
        return {"status": "SKIPPED", "total": 0, "items": [], "page": page, "size": size}
    data = memory_store.list_facts(settings, userId, page=page, size=size)
    return {"status": "SUCCESS", **data}


@router.post("/chat/memory/delete")
async def chat_memory_delete(request: dict[str, Any]) -> dict:
    """软删单条 fact (按 memory_id). 校验归属, 跨用户删除会被拒."""
    settings = get_settings()
    user_id = request.get("userId")
    memory_id = str(request.get("memoryId") or "").strip()
    if not user_id or not memory_id:
        return {"status": "FAILED", "message": "userId/memoryId 必填"}
    ok = memory_store.delete_fact(settings, user_id, memory_id)
    return {"status": "SUCCESS" if ok else "FAILED", "deleted": ok}


@router.post("/chat/memory/clear")
async def chat_memory_clear(request: dict[str, Any]) -> dict:
    """清空本用户所有 fact.
    <p>请求: {userId, hard?}
    <p>hard 默认 false = 软删 (active=false, 保留 ES doc);
    <p>hard=true = 物理删除 (GDPR 被遗忘权).
    """
    settings = get_settings()
    user_id = request.get("userId")
    hard = bool(request.get("hard", False))
    if not user_id:
        return {"status": "FAILED", "message": "userId 必填"}
    n = memory_store.clear_facts(settings, user_id, hard=hard)
    return {"status": "SUCCESS", "cleared": n, "hard": hard}


@router.post("/chat/memory/forget")
async def chat_memory_forget(request: dict[str, Any]) -> dict:
    """被遗忘权: 物理删除本用户在 jc_chat_memory 中的所有 doc (不留痕).
    <p>请求: {userId}
    <p>响应: {status, removed}
    <p>与 /chat/memory/clear?hard=true 等价, 但语义更明确 (GDPR / 个保法).
    """
    settings = get_settings()
    user_id = request.get("userId")
    if not user_id:
        return {"status": "FAILED", "message": "userId 必填"}
    n = memory_store.clear_facts(settings, user_id, hard=True)
    return {"status": "SUCCESS", "removed": n}


@router.post("/chat/memory/retract")
async def chat_memory_retract(request: dict[str, Any]) -> dict:
    """撤回 (物理删) 1 条已知 fact, 用于 L2 矛盾事实处理.
    <p>请求: {userId, key, value}
    <p>响应: {status, removed}
    """
    settings = get_settings()
    user_id = request.get("userId")
    key = str(request.get("key") or "").strip()
    value = str(request.get("value") or "").strip()
    if not user_id or not key or not value:
        return {"status": "FAILED", "message": "userId/key/value 必填"}
    n = memory_store.delete_fact_by_value(settings, user_id, key, value)
    return {"status": "SUCCESS", "removed": n}
