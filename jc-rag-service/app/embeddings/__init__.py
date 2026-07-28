from __future__ import annotations

import logging
import os
import time
from pathlib import Path
from functools import lru_cache
from typing import Any

logger = logging.getLogger(__name__)

LM_STUDIO_DEFAULT_URL = "http://localhost:1234"


def _check_lm_studio_available(base_url: str | None = None) -> bool:
    url = (base_url or LM_STUDIO_DEFAULT_URL).rstrip("/")
    try:
        import urllib.request
        import json
        req = urllib.request.Request(f"{url}/v1/models", method="GET")
        with urllib.request.urlopen(req, timeout=3) as resp:
            data = json.loads(resp.read().decode())
            models = data.get("data", [])
            for m in models:
                mid = (m.get("id") or "").lower()
                if "embed" in mid or "bge" in mid or "e5" in mid:
                    return True
            return len(models) > 0
    except Exception:
        return False


def _resolve_device(device: str, lm_studio_url: str | None = None) -> str:
    value = (device or "").strip().lower()
    if value and value != "auto":
        if value.startswith("dml:"):
            try:
                import torch
                import torch_directml
                dml_device = torch_directml.device(int(value.split(":", 1)[1]))
                torch.zeros(1, device=dml_device)
                return str(dml_device)
            except Exception as ex:
                raise RuntimeError(f"Selected DirectML GPU is unavailable: {ex}") from ex
        if value.startswith("cuda:"):
            try:
                import torch
                index = int(value.split(":", 1)[1])
                if not torch.cuda.is_available() or index >= torch.cuda.device_count():
                    raise RuntimeError("CUDA runtime cannot access this device")
            except ValueError:
                raise RuntimeError(f"Invalid CUDA device identifier: {value}")
        logger.info("Embedding 设备由配置指定: %s", value)
        return value
    try:
        import torch
        if torch.cuda.is_available():
            gpu_name = torch.cuda.get_device_name(0) if torch.cuda.device_count() > 0 else "unknown"
            logger.info("Embedding 启用 CUDA GPU 加速: %s", gpu_name)
            return "cuda"
    except Exception:
        pass
    try:
        import torch_directml
        # 验证 DirectML backend 真实可用 (0.2.5.dev240914 + torch 2.4.1 可能未注册 torch.privateuseone 模块)
        dml_dev = torch_directml.device()
        torch.zeros(1, device=dml_dev)
        logger.info("Embedding 启用 DirectML GPU 加速: %s", dml_dev)
        return str(dml_dev)
    except Exception as ex:
        logger.warning("DirectML backend 不可用, fallback 到 CPU: %s", ex)
    if _check_lm_studio_available(lm_studio_url):
        logger.info("Embedding 使用 LM Studio API（Intel Arc GPU 加速）")
        return "lm_studio"
    logger.info("Embedding 使用 CPU 模式（未检测到可用 GPU 或 LM Studio）")
    return "cpu"


# 同一 model_path 复用已加载实例, 忽略 device 差异.
# 原因: sentence-transformers 3.3.1 在 dml/cpu/cuda 切换时会重新加载并触发
# backend 切换, 当前 venv 下 cpu backend 加载 bge-large-zh-v1.5 会失败.
# Pro 集成场景: Java 端 payload 的 ragOverride.embedding.device 与启动时
# lifespan 探测的 device 不一致 (例如 dml → cpu), 改用 dict 缓存可彻底规避.
_model_cache: dict[tuple[str, str], Any] = {}


def _load_model(model_path: str, device: str):
    from sentence_transformers import SentenceTransformer

    if not model_path or not Path(model_path).exists():
        raise ValueError(f"embedding.model_path 不存在或不可访问：{model_path}")
    resolved_device = _resolve_device(device or "auto", None)
    if "privateuse" in resolved_device.lower() and os.environ.get("JC_RAG_ALLOW_DIRECTML_EMBEDDING") != "1":
        logger.warning("DirectML embedding disabled after runtime instability; using bounded CPU mode")
        resolved_device = "cpu"
        try:
            import torch
            torch.set_num_threads(min(4, max(1, os.cpu_count() or 1)))
        except Exception:
            pass
    cache_key = (model_path, resolved_device)
    if cache_key in _model_cache:
        cached = _model_cache[cache_key]
        logger.info(
            "Embedding 复用缓存: model=%s, 实际 device=%s, 请求 device=%s",
            model_path, getattr(cached, "device", "unknown"), device,
        )
        return cached
    # 第一次加载: 用 _resolve_device("auto") 探测的 device, 而非 Java 端传过来的 device
    # Pro 启动时 lifespan 已用 auto 探测加载过一次 (dml), 此处保证 device 一致
    if resolved_device != device:
        logger.warning(
            "Embedding 加载 device 归一化: 请求 %s → 实际 %s (避免 backend 切换崩溃)",
            device, resolved_device,
        )
    try:
        model = SentenceTransformer(model_path, device=resolved_device)
    except RuntimeError as ex:
        if "privateuse" not in resolved_device.lower() and not resolved_device.lower().startswith("dml"):
            raise
        logger.warning("Embedding DirectML load failed; falling back to CPU: %r", ex)
        resolved_device = "cpu"
        cache_key = (model_path, resolved_device)
        if cache_key in _model_cache:
            return _model_cache[cache_key]
        model = SentenceTransformer(model_path, device=resolved_device)
    _model_cache[cache_key] = model
    return model


def _embed_via_lm_studio(
    texts: list[str],
    base_url: str,
    model_name: str | None = None,
) -> list[list[float]]:
    import urllib.request
    import json

    url = f"{base_url.rstrip('/')}/v1/embeddings"
    payload: dict[str, Any] = {"input": texts}
    if model_name:
        payload["model"] = model_name

    body = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(
        url, data=body, method="POST",
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(req, timeout=60) as resp:
        result = json.loads(resp.read().decode("utf-8"))

    data_list = result.get("data", [])
    sorted_data = sorted(data_list, key=lambda d: d.get("index", 0))
    return [d["embedding"] for d in sorted_data]


def _resolve_lm_studio_model(base_url: str) -> str | None:
    import urllib.request
    import json

    url = f"{base_url.rstrip('/')}/v1/models"
    req = urllib.request.Request(url, method="GET")
    with urllib.request.urlopen(req, timeout=3) as resp:
        data = json.loads(resp.read().decode("utf-8"))
    models = data.get("data", [])
    for m in models:
        mid = (m.get("id") or "").lower()
        if "embed" in mid or "bge" in mid or "e5" in mid:
            return m.get("id")
    if models:
        return models[0].get("id")
    return None


def _resolve_embedding_override(settings, embedding_override: dict[str, Any] | None) -> dict[str, Any]:
    override = embedding_override if isinstance(embedding_override, dict) else {}
    model_path = str(override.get("modelPath") or settings.embedding.model_path or "").strip()
    lm_studio_url = str(
        override.get("lmStudioUrl")
        or getattr(settings.embedding, "lm_studio_url", "")
        or LM_STUDIO_DEFAULT_URL
    ).strip()
    device = _resolve_device(
        str(override.get("device") or settings.embedding.device or "auto"),
        lm_studio_url=lm_studio_url,
    )
    dims = override.get("dims", settings.embedding.dims)
    try:
        dims_value = int(dims) if dims is not None and str(dims).strip() != "" else None
    except Exception:
        dims_value = None
    batch_size = override.get("batchSize", settings.embedding.batch_size)
    try:
        batch_size_value = min(8, max(1, int(batch_size))) if batch_size is not None else 8
    except Exception:
        batch_size_value = 8
    normalize = override.get("normalizeEmbeddings", settings.embedding.normalize_embeddings)
    normalize_value = bool(normalize) if normalize is not None else True
    return {
        "modelPath": model_path,
        "device": device,
        "dims": dims_value,
        "batchSize": batch_size_value,
        "normalizeEmbeddings": normalize_value,
        "lmStudioUrl": lm_studio_url,
    }


def get_embedding_dimension(settings, embedding_override: dict[str, Any] | None = None) -> int:
    cfg = _resolve_embedding_override(settings, embedding_override)
    if cfg["dims"] and int(cfg["dims"]) > 0:
        return int(cfg["dims"])
    if cfg["device"] == "lm_studio":
        return 1024
    model = _load_model(cfg["modelPath"], cfg["device"])
    return int(model.get_sentence_embedding_dimension())


def embed_texts(
    texts: list[str],
    settings,
    embedding_override: dict[str, Any] | None = None,
) -> tuple[list[list[float]], dict[str, Any]]:
    cfg = _resolve_embedding_override(settings, embedding_override)
    device = cfg["device"]
    started = time.time()

    if device == "lm_studio":
        base_url = cfg["lmStudioUrl"]
        model_name = _resolve_lm_studio_model(base_url)
        logger.info("Embedding 通过 LM Studio API 调用: url=%s, model=%s", base_url, model_name)
        vectors = _embed_via_lm_studio(texts, base_url, model_name)
        dims = len(vectors[0]) if vectors else cfg.get("dims", 1024)
        cost_ms = int((time.time() - started) * 1000)
        return (
            vectors,
            {
                "modelPath": f"LM Studio ({base_url})",
                "device": "lm_studio",
                "dims": dims,
                "costMs": cost_ms,
                "batchSize": cfg["batchSize"],
                "normalizeEmbeddings": cfg["normalizeEmbeddings"],
            },
        )

    model = _load_model(cfg["modelPath"], cfg["device"])
    try:
        vectors = model.encode(
            texts,
            normalize_embeddings=cfg["normalizeEmbeddings"],
            batch_size=cfg["batchSize"],
            show_progress_bar=False,
        )
    except RuntimeError as ex:
        actual_device = str(getattr(model, "device", cfg["device"]))
        if "privateuse" not in actual_device.lower() and not actual_device.lower().startswith("dml"):
            raise
        logger.warning("Embedding DirectML inference failed; retrying on CPU: %r", ex)
        model = _load_model(cfg["modelPath"], "cpu")
        vectors = model.encode(texts, normalize_embeddings=cfg["normalizeEmbeddings"], batch_size=cfg["batchSize"], show_progress_bar=False)
    cost_ms = int((time.time() - started) * 1000)
    return (
        [vec.tolist() for vec in vectors],
        {
            "modelPath": cfg["modelPath"],
            "device": str(getattr(model, "device", cfg["device"])),
            "dims": int(model.get_sentence_embedding_dimension()),
            "costMs": cost_ms,
            "batchSize": cfg["batchSize"],
            "normalizeEmbeddings": cfg["normalizeEmbeddings"],
        },
    )


def get_embedding_status(settings, embedding_override: dict[str, Any] | None = None) -> dict[str, Any]:
    cfg = _resolve_embedding_override(settings, embedding_override)
    try:
        dims = get_embedding_dimension(settings, embedding_override)
        return {
            "enabled": True,
            "modelPath": cfg["modelPath"],
            "device": cfg["device"],
            "dims": dims,
            "batchSize": cfg["batchSize"],
            "normalizeEmbeddings": cfg["normalizeEmbeddings"],
        }
    except Exception as ex:
        return {
            "enabled": False,
            "modelPath": cfg["modelPath"],
            "device": cfg["device"],
            "error": str(ex),
        }
