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
敏感词本地缓存 (Python 端) - 拉取 Java admin 模块的 /api/internal/security/words/export, 内存构建 AC 自动机。
<p>本模块为简化版 Aho-Corasick, 适合 1 万词库规模, 一次扫描 O(n+m).
<p>启动加载 + 每 5 分钟定时刷新, 失败时保留旧词库 (降级放行 + 记日志).
"""
from __future__ import annotations

import asyncio
import logging
import threading
import time
from dataclasses import dataclass, field
from typing import List, Optional

import httpx

from app.core.config import get_settings

_logger = logging.getLogger(__name__)


@dataclass
class SensitiveWord:
    word: str
    risk_level: str = "MEDIUM"  # HIGH / MEDIUM / LOW
    category: Optional[str] = None
    action: str = "DENY"  # DENY / ALERT / MASK / PASS
    word_id: Optional[int] = None


@dataclass
class FilterResult:
    action: str = "PASS"
    hits: List[SensitiveWord] = field(default_factory=list)
    original: str = ""
    masked: str = ""

    @property
    def is_deny(self) -> bool:
        return self.action == "DENY"

    @property
    def is_mask(self) -> bool:
        return self.action == "MASK"

    @property
    def is_pass(self) -> bool:
        return self.action == "PASS"


class _AhoCorasick:
    """精简版 Aho-Corasick 自动机"""

    def __init__(self) -> None:
        self._root: dict = {"_fail": None, "_out": [], "_ch": {}}

    @staticmethod
    def build(words: List[SensitiveWord]) -> "_AhoCorasick":
        ac = _AhoCorasick()
        root = ac._root
        for w in words:
            if not w.word:
                continue
            cur = root
            for ch in w.word:
                if ch not in cur["_ch"]:
                    cur["_ch"][ch] = {"_fail": None, "_out": [], "_ch": {}}
                cur = cur["ch"][ch]
            cur["_out"].append(w)
        # BFS 构建 fail 指针
        from collections import deque
        q: deque = deque()
        root["_fail"] = root
        for ch, child in root["_ch"].items():
            child["_fail"] = root
            q.append(child)
        while q:
            u = q.popleft()
            for ch, v in u["_ch"].items():
                f = u["_fail"]
                while f is not root and ch not in f["_ch"]:
                    f = f["_fail"]
                v["_fail"] = f["_ch"].get(ch, root)
                v["_out"].extend(v["_fail"]["_out"])
                q.append(v)
        return ac

    def scan(self, text: str) -> List[tuple]:
        """扫描文本, 返回 [(start, word), ...]"""
        result: List[tuple] = []
        if not text:
            return result
        cur = self._root
        for i, ch in enumerate(text):
            while cur is not self._root and ch not in cur["_ch"]:
                cur = cur["_fail"]
            cur = cur["_ch"].get(ch, self._root)
            for w in cur["_out"]:
                result.append((i - len(w.word) + 1, w))
        return result


class SensitiveWordCache:
    """Python 端敏感词缓存 + 过滤引擎 (单例)"""

    _instance: Optional["SensitiveWordCache"] = None
    _lock = threading.Lock()

    def __init__(self) -> None:
        self._engine: Optional[_AhoCorasick] = None
        self._last_reload_ts: float = 0
        self._last_words: List[SensitiveWord] = []
        self._reload_interval = 300  # 5 min

    @classmethod
    def get(cls) -> "SensitiveWordCache":
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = cls()
        return cls._instance

    def words(self) -> List[SensitiveWord]:
        return list(self._last_words)

    def _build(self, words: List[SensitiveWord]) -> None:
        self._engine = _AhoCorasick.build(words)
        self._last_words = words
        self._last_reload_ts = time.time()
        _logger.info(f"[SensitiveWord] reloaded {len(words)} words")

    async def reload_async(self) -> None:
        """从 Java admin 端拉取, 异步"""
        s = get_settings()
        admin_url = getattr(s, "admin_base_url", "http://localhost:8080")
        token = getattr(s, "admin_internal_token", "")
        url = f"{str(admin_url).rstrip('/')}/api/internal/security/words/export"
        headers = {}
        if token:
            headers["X-Internal-Token"] = str(token)
        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                resp = await client.get(url, headers=headers)
            if resp.status_code != 200:
                _logger.warning(f"[SensitiveWord] admin returned {resp.status_code}, keep prev cache")
                return
            data = resp.json()
            words: List[SensitiveWord] = []
            for m in data if isinstance(data, list) else []:
                words.append(SensitiveWord(
                    word=str(m.get("word", "")),
                    risk_level=str(m.get("riskLevel", "MEDIUM")),
                    category=m.get("category"),
                    action=str(m.get("action", "DENY")),
                    word_id=m.get("id"),
                ))
            self._build(words)
        except Exception as e:
            _logger.warning(f"[SensitiveWord] reload failed, keep prev: {e}")

    def reload_sync(self) -> None:
        """同步拉取, 启动时调用"""
        try:
            asyncio.run(self.reload_async())
        except RuntimeError:
            # 已有事件循环在跑, 用 task 走
            loop = asyncio.get_event_loop()
            if loop.is_running():
                loop.create_task(self.reload_async())
            else:
                loop.run_until_complete(self.reload_async())
        except Exception as e:
            _logger.warning(f"[SensitiveWord] sync reload failed: {e}")

    def filter(self, text: str) -> FilterResult:
        if not text or self._engine is None:
            return FilterResult(action="PASS", original=text or "", masked=text or "", hits=[])
        matches = self._engine.scan(text)
        if not matches:
            return FilterResult(action="PASS", original=text, masked=text, hits=[])
        seen: set = set()
        hits: List[SensitiveWord] = []
        top_action = "PASS"
        top_priority = 0
        masked = text
        need_mask = False
        for _, w in matches:
            if w.word in seen:
                continue
            seen.add(w.word)
            hits.append(w)
            p = 0
            if w.action == "DENY":
                p = 3
            elif w.action == "MASK":
                p = 2
            elif w.action == "ALERT":
                p = 1
            if p > top_priority:
                top_priority = p
                top_action = w.action
            if w.action == "MASK":
                need_mask = True
        if need_mask:
            for w in hits:
                if w.action == "MASK":
                    masked = masked.replace(w.word, "*" * len(w.word))
        return FilterResult(action=top_action, original=text, masked=masked, hits=hits)


# ============== PII 脱敏 (与 Java 版本对齐) ==============

import re

_ID_CARD = re.compile(r"\b\d{17}[\dXx]\b")
_MOBILE = re.compile(r"\b1[3-9]\d{9}\b")
_EMAIL = re.compile(r"\b[\w.+-]+@[\w-]+\.[a-zA-Z]{2,}\b")
_BANK_CARD = re.compile(r"\b\d{16,19}\b")


def mask_pii(text: str) -> str:
    if not text:
        return text

    def _id(m: re.Match) -> str:
        s = m.group()
        return s[:6] + "********" + s[14:]

    def _mb(m: re.Match) -> str:
        s = m.group()
        return s[:3] + "****" + s[7:]

    def _em(m: re.Match) -> str:
        s = m.group()
        i = s.find("@")
        if i <= 1:
            return s
        return s[0] + "***" + s[i:]

    def _bc(m: re.Match) -> str:
        s = m.group()
        if _ID_CARD.fullmatch(s) or _MOBILE.fullmatch(s):
            return s
        return s[:4] + "*" * (len(s) - 8) + s[-4:]

    text = _ID_CARD.sub(_id, text)
    text = _MOBILE.sub(_mb, text)
    text = _EMAIL.sub(_em, text)
    text = _BANK_CARD.sub(_bc, text)
    return text
