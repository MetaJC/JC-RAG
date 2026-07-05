from __future__ import annotations

import logging
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
        logger.info("Embedding 启用 DirectML GPU 加速")
        return "dml"
    except Exception:
        pass
    if _check_lm_studio_available(lm_studio_url):
        logger.info("Embedding 使用 LM Studio API（Intel Arc GPU 加速）")
        return "lm_studio"
    logger.info("Embedding 使用 CPU 模式（未检测到可用 GPU 或 LM Studio）")
    return "cpu"


@lru_cache(maxsize=2)
def _load_model(model_path: str, device: str):
    from sentence_transformers import SentenceTransformer

    if not model_path or not Path(model_path).exists():
        raise ValueError(f"embedding.model_path 不存在或不可访问：{model_path}")
    return SentenceTransformer(model_path, device=device)


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
        batch_size_value = int(batch_size) if batch_size is not None else 32
    except Exception:
        batch_size_value = 32
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
    vectors = model.encode(
        texts,
        normalize_embeddings=cfg["normalizeEmbeddings"],
        batch_size=cfg["batchSize"],
        show_progress_bar=False,
    )
    cost_ms = int((time.time() - started) * 1000)
    return (
        [vec.tolist() for vec in vectors],
        {
            "modelPath": cfg["modelPath"],
            "device": cfg["device"],
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
