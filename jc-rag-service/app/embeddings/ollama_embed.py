"""Ollama embedding 后端（OpenAI 兼容 API）

通过本机 Ollama 服务生成嵌入向量，用于"无本地 GPU + 已装 Ollama"的部署场景。
Ollama 接口规范见 https://github.com/ollama/ollama/blob/main/docs/api.md#generate-embeddings
"""
from __future__ import annotations

import logging
import time
from typing import Any

import httpx

from app.core.config import get_settings

logger = logging.getLogger(__name__)


def _resolve_base_url() -> str:
    settings = get_settings()
    return (settings.providers.offline.ollama.base_url or "http://localhost:11434").rstrip("/")


def ollama_embed(
    texts: list[str],
    model_name: str,
    base_url: str | None = None,
    timeout: float = 300.0,
) -> tuple[list[list[float]], dict[str, Any]]:
    """通过 Ollama /api/embed 批量生成嵌入向量。

    优先调用 /api/embed（支持 input 数组），失败回退到逐条 /api/embeddings。
    """
    url_base = (base_url or _resolve_base_url()).rstrip("/")

    embeddings: list[list[float]] = []
    started = time.time()

    try:
        with httpx.Client(timeout=timeout, trust_env=False) as client:
            resp = client.post(
                url_base + "/api/embed",
                json={"model": model_name, "input": texts},
            )
            if resp.status_code == 404:
                raise httpx.HTTPStatusError("fallback", request=resp.request, response=resp)
            resp.raise_for_status()
            data = resp.json() or {}
            embeddings = list(data.get("embeddings") or [])
    except httpx.ConnectError:
        raise RuntimeError(
            f"无法连接 Ollama ({url_base})，请确认 Ollama 服务已启动"
        )
    except httpx.HTTPStatusError:
        # 老版本 Ollama 可能没有 /api/embed，回退到 /api/embeddings 单条
        try:
            with httpx.Client(timeout=timeout, trust_env=False) as client:
                for text in texts:
                    resp = client.post(
                        url_base + "/api/embeddings",
                        json={"model": model_name, "prompt": text},
                    )
                    resp.raise_for_status()
                    data = resp.json() or {}
                    vec = data.get("embedding") or []
                    embeddings.append(list(vec))
        except Exception as ex:
            raise RuntimeError(f"Ollama embedding 调用失败: {ex}")

    cost_ms = int((time.time() - started) * 1000)
    dims = len(embeddings[0]) if embeddings else 0

    return embeddings, {
        "modelPath": model_name,
        "device": f"ollama:{model_name}",
        "dims": dims,
        "batchSize": len(texts),
        "normalizeEmbeddings": True,
        "costMs": cost_ms,
        "backend": "ollama",
    }


def ollama_dimension(model_name: str, base_url: str | None = None) -> int:
    """探测 Ollama 嵌入模型的向量维度（发一次空字符串）。"""
    vectors, _ = ollama_embed(["test"], model_name, base_url=base_url, timeout=30.0)
    return len(vectors[0]) if vectors else 0


def ping_ollama(base_url: str | None = None, timeout: float = 2.0) -> bool:
    """检测 Ollama 是否在线。"""
    try:
        url = (base_url or _resolve_base_url()).rstrip("/") + "/api/tags"
        with httpx.Client(timeout=timeout, trust_env=False) as client:
            resp = client.get(url)
            return resp.status_code < 500
    except Exception:
        return False
