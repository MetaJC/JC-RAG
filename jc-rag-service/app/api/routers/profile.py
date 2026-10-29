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


"""L3 跨会话画像路由: 由 L2 长期事实聚合并提炼成结构化画像.
<p>被 Java UserMemoryProfileServiceImpl.extractProfileAsync 调用.
<p>原位置: 新增 (L3 阶段, 2026-07-03 启动).

@author JC
"""
from __future__ import annotations

import json
import logging
import re
from typing import Any

from fastapi import APIRouter

from app.core.config import get_settings
from app.utils import memory_store

logger = logging.getLogger(__name__)
router = APIRouter()


# JSON 抽取 (复用 L2 风格, 兼容 LLM 加围栏)
_JSON_FENCE = re.compile(r"```(?:json)?\s*([\s\S]+?)\s*```", re.IGNORECASE)


def _extract_profile_json(raw: str) -> dict[str, Any] | None:
    """从 LLM 输出中抽 profile JSON 对象. 容错: 围栏 / 整体 / 截取首个 {...}."""
    if not raw:
        return None
    text = raw.strip()
    m = _JSON_FENCE.search(text)
    if m:
        text = m.group(1).strip()
    # 整体尝试
    try:
        obj = json.loads(text)
        if isinstance(obj, dict):
            return obj
    except Exception:
        pass
    # 截取首个 {...} (单行/多行均支持)
    m2 = re.search(r"\{[\s\S]+\}", text)
    if m2:
        try:
            obj = json.loads(m2.group(0))
            if isinstance(obj, dict):
                return obj
        except Exception:
            pass
    return None


def _normalize_profile(raw: dict[str, Any], max_skills: int, max_goals: int) -> dict[str, Any]:
    """把 LLM 输出规整成五段结构: domain / role / skills / preferences / goals."""
    out: dict[str, Any] = {
        "domain": None,
        "role": None,
        "skills": [],
        "preferences": {},
        "goals": [],
    }
    if not isinstance(raw, dict):
        return out

    # domain / role: 字符串, 截断
    for k in ("domain", "role"):
        v = raw.get(k)
        if isinstance(v, str) and v.strip():
            out[k] = v.strip()[:120]

    # skills: 字符串数组, 去空去重
    skills = raw.get("skills")
    if isinstance(skills, list):
        seen: set[str] = set()
        norm: list[str] = []
        for s in skills:
            if not isinstance(s, str):
                continue
            t = s.strip()
            if not t or t in seen:
                continue
            seen.add(t)
            norm.append(t[:80])
            if len(norm) >= max_skills:
                break
        out["skills"] = norm

    # preferences: 字符串 -> 字符串 map, 去空
    prefs = raw.get("preferences")
    if isinstance(prefs, dict):
        clean: dict[str, str] = {}
        for k, v in prefs.items():
            if not isinstance(k, str) or not isinstance(v, str):
                continue
            kk = k.strip()
            vv = v.strip()
            if not kk or not vv:
                continue
            clean[kk[:32]] = vv[:200]
        out["preferences"] = clean

    # goals: 字符串数组
    goals = raw.get("goals")
    if isinstance(goals, list):
        seen_g: set[str] = set()
        norm_g: list[str] = []
        for g in goals:
            if not isinstance(g, str):
                continue
            t = g.strip()
            if not t or t in seen_g:
                continue
            seen_g.add(t)
            norm_g.append(t[:120])
            if len(norm_g) >= max_goals:
                break
        out["goals"] = norm_g
    return out


@router.post("/chat/profile/extract")
async def chat_profile_extract(request: dict[str, Any]) -> dict:
    """从 L2 长期事实聚合并经 LLM 提炼成结构化画像 (L3 跨会话画像).
    <p>请求: {userId, maxSkills?, maxGoals?}
    <p>响应: {status, profile, factCount, modelName, message?}
    """
    settings = get_settings()
    user_id = request.get("userId")
    if not user_id:
        return {"status": "FAILED", "message": "userId 必填"}
    if not settings.memory.enabled:
        return {"status": "SKIPPED", "message": "memory disabled in config"}
    if not settings.memory.profile.enabled:
        return {"status": "SKIPPED", "message": "L3 profile disabled in config"}

    max_skills = int(request.get("maxSkills") or settings.memory.profile.max_skills)
    max_goals = int(request.get("maxGoals") or settings.memory.profile.max_goals)

    # 1. 拉本用户所有 active fact
    try:
        fact_resp = memory_store.list_facts(settings, user_id, page=1, size=200)
        items = fact_resp.get("items", []) or []
    except Exception as ex:
        logger.warning("chat_profile_extract: list_facts failed, err=%s", ex)
        return {"status": "FAILED", "message": f"fetch facts failed: {ex}"}

    fact_count = len(items)
    if fact_count == 0:
        return {"status": "SUCCESS", "profile": None, "factCount": 0, "modelName": ""}

    # 2. 按 category 分组, 拼成事实文本
    by_cat: dict[str, list[str]] = {}
    for it in items:
        cat = str(it.get("category") or "other")
        line = f"- {it.get('key')}: {it.get('value')}"
        by_cat.setdefault(cat, []).append(line)
    fact_text = "\n".join(
        f"[{cat}]\n" + "\n".join(lines) for cat, lines in by_cat.items()
    )

    # 3. 拼 LLM prompt
    prompt = (
        "你是一名用户画像分析师, 请根据以下该用户的长期关键事实, 提炼出**结构化画像** (用于跨会话个性化回答).\n"
        "严格输出 JSON, 严格按以下五段结构 (字段名固定, 顺序不限):\n"
        '  "domain":  string|null    // 从事领域 (例: "Java 后端开发" / "高校教师"), 简洁 1 句\n'
        '  "role":    string|null    // 当前角色 (例: "高级工程师" / "博士生"), 简洁 1 句\n'
        '  "skills":  string[]        // 关键技术栈 / 能力, 短词优先, 不超过 max_skills 条\n'
        '  "preferences": object     // 长期偏好 (例: {"code_style":"阿里规约","answer_length":"详细"})\n'
        '  "goals":   string[]        // 明确长期目标, 不超过 max_goals 条\n\n'
        "规则:\n"
        "  1. 仅基于给定事实, 严禁编造. 信息不足的字段填 null 或空数组/对象.\n"
        "  2. 同一语义合并 (例: 'Spring Cloud' 与 'Spring Cloud Alibaba' 合并为更具体的那个).\n"
        "  3. skills 优先技术名词 / 框架 / 语言, 排除一次性的临时项目.\n"
        "  4. goals 是用户明确表达的长期目标, 不是临时任务.\n"
        "  5. 只输出 JSON, 不要任何解释/前缀/后缀/代码围栏.\n\n"
        f"max_skills = {max_skills}, max_goals = {max_goals}\n"
        f"用户长期事实 (共 {fact_count} 条, 按 category 分组):\n{fact_text}\n"
    )

    # 4. 调 LLM
    from app.llm.provider_factory import ProviderFactory
    provider = ProviderFactory(settings)
    model_name = ""
    raw = ""
    try:
        async for delta in provider.stream_generate(
            "",
            messages=[{"role": "user", "content": prompt}],
            provider_override=None,
        ):
            if not delta:
                continue
            raw += delta
        # 从 provider 拿当前 model 名 (供 Java 端落库审计)
        try:
            model_name = str(getattr(provider, "active_model_name", "") or "")
        except Exception:
            model_name = ""
    except Exception as ex:
        logger.error("chat_profile_extract: LLM failed, err=%s", ex)
        return {"status": "FAILED", "message": f"LLM error: {ex}"}

    # 5. 抽 JSON + 规整
    parsed = _extract_profile_json(raw)
    if parsed is None:
        logger.info("chat_profile_extract: LLM returned no JSON, raw=%s", raw[:200])
        return {"status": "SUCCESS", "profile": None, "factCount": fact_count, "modelName": model_name,
                "message": "LLM 返回非 JSON, 跳过本轮 (旧画像保留)"}

    profile = _normalize_profile(parsed, max_skills=max_skills, max_goals=max_goals)
    return {
        "status": "SUCCESS",
        "profile": profile,
        "factCount": fact_count,
        "modelName": model_name,
    }
