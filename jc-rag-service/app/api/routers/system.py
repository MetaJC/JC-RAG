"""系统/运维路由: 健康检查 / 设备检测 / 资源监控 / 模型验证 / 解析进度.
<p>原位置: app/api/routes.py (拆分自 2026-07-03 结构重构, 业务零变更).

@author JC
"""
from __future__ import annotations

import logging
import platform
import time
from typing import Any

from fastapi import APIRouter, Body

from app.api.deps import _resolve_cpu_info
from app.core.config import get_settings

logger = logging.getLogger(__name__)
router = APIRouter()


@router.get("/health")
async def health() -> dict:
    settings = get_settings()
    return {
        "status": "UP",
        "service": settings.app.name,
        "version": settings.app.version,
        "activeMode": settings.providers.active_mode,
    }


@router.get("/system/devices")
async def system_devices() -> list[dict[str, Any]]:
    """检测所有可用于 Embedding 的计算设备"""
    from app.core.device_detector import detect_all_devices
    settings = get_settings()
    return detect_all_devices(settings)


_cached_cpu_name: str | None = None
_cached_gpu_names: list[str] | None = None
_gpu_metrics_cache: dict[str, dict[str, Any]] = {}
_gpu_metrics_ts: float = 0.0


def _get_cached_device_names() -> tuple[str, list[str]]:
    global _cached_cpu_name, _cached_gpu_names
    if _cached_cpu_name is not None and _cached_gpu_names is not None:
        return _cached_cpu_name, _cached_gpu_names
    cpu_name = ""
    gpu_names: list[str] = []
    try:
        if platform.system() == "Windows":
            import subprocess
            cpu_result = subprocess.check_output(
                ["powershell", "-Command",
                 "Get-CimInstance Win32_Processor | Select-Object -ExpandProperty Name"],
                text=True, timeout=8
            )
            cpu_name = cpu_result.strip()
            gpu_result = subprocess.check_output(
                ["powershell", "-Command",
                 "Get-CimInstance Win32_VideoController | Select-Object -ExpandProperty Name"],
                text=True, timeout=8
            )
            gpu_lines = [l.strip() for l in gpu_result.splitlines() if l.strip()]
            skip_kw = ["oray", "idd", "vnc", "rdp", "virtual", "remote", "mirage", "citrix", "parsec"]
            gpu_names = [n for n in gpu_lines if not any(k in n.lower() for k in skip_kw)]
        else:
            with open("/proc/cpuinfo", "r", errors="ignore") as f:
                for line in f:
                    if line.startswith("model name"):
                        cpu_name = line.split(":", 1)[1].strip()
                        break
    except Exception:
        pass
    _cached_cpu_name = cpu_name
    _cached_gpu_names = gpu_names
    return cpu_name, gpu_names


def _query_gpu_metrics_nvidia_smi() -> dict[str, dict[str, Any]]:
    """通过 nvidia-smi 获取真实 GPU 利用率和显存"""
    global _gpu_metrics_cache, _gpu_metrics_ts
    now = time.time()
    if now - _gpu_metrics_ts < 2.0 and _gpu_metrics_cache:
        return _gpu_metrics_cache
    result: dict[str, dict[str, Any]] = {}
    try:
        import subprocess
        out = subprocess.check_output(
            ["nvidia-smi",
             "--query-gpu=name,utilization.gpu,memory.used,memory.total",
             "--format=csv,noheader,nounits"],
            text=True, timeout=5
        )
        for line in out.strip().splitlines():
            parts = [p.strip() for p in line.split(",")]
            if len(parts) < 4:
                continue
            name = parts[0]
            util_pct = float(parts[1]) if parts[1] else 0.0
            mem_used_mb = float(parts[2]) if parts[2] else 0.0
            mem_total_mb = float(parts[3]) if parts[3] else 0.0
            mem_used_gb = round(mem_used_mb / 1024, 1)
            mem_total_gb = round(mem_total_mb / 1024, 1)
            result[name] = {
                "percent": round(util_pct, 1),
                "usedGb": mem_used_gb,
                "totalGb": mem_total_gb,
            }
    except Exception:
        pass
    _gpu_metrics_cache = result
    _gpu_metrics_ts = now
    return result


@router.get("/system/resources")
async def system_resources() -> dict[str, Any]:
    import asyncio
    try:
        import psutil

        def _collect() -> dict[str, Any]:
            cpu_percent = psutil.cpu_percent(interval=0)
            cpu_name, gpu_names = _get_cached_device_names()

            mem = psutil.virtual_memory()
            mem_used_gb = round(mem.used / (1024 ** 3), 1)
            mem_total_gb = round(mem.total / (1024 ** 3), 1)

            cuda_metrics = _query_gpu_metrics_nvidia_smi()

            def _names_overlap(a: str, b: str) -> bool:
                al = a.lower().strip()
                bl = b.lower().strip()
                if al == bl:
                    return True
                a_is_nvidia = "nvidia" in al or "geforce" in al or "rtx" in al or "gtx" in al
                b_is_nvidia = "nvidia" in bl or "geforce" in bl or "rtx" in bl or "gtx" in bl
                if a_is_nvidia != b_is_nvidia:
                    return False
                toks_a = set(al.replace("(", "").replace(")", "").replace("®", "").replace("™", "").split())
                toks_b = set(bl.replace("(", "").replace(")", "").replace("®", "").replace("™", "").split())
                if a_is_nvidia:
                    key_nums_a = {t for t in toks_a if any(c.isdigit() for c in t) and len(t) >= 3}
                    key_nums_b = {t for t in toks_b if any(c.isdigit() for c in t) and len(t) >= 3}
                    if key_nums_a and key_nums_b and key_nums_a & key_nums_b:
                        return True
                overlap = toks_a & toks_b
                return len(overlap) >= 2

            gpus: list[dict[str, Any]] = []
            used_cuda_keys: set[str] = set()
            for gpu_name in gpu_names:
                matched_cuda: dict[str, dict[str, Any]] | None = None
                for cuda_name, metrics in cuda_metrics.items():
                    if cuda_name in used_cuda_keys:
                        continue
                    if _names_overlap(gpu_name, cuda_name):
                        matched_cuda = metrics
                        used_cuda_keys.add(cuda_name)
                        break
                if matched_cuda:
                    gpus.append({"name": gpu_name, **matched_cuda})
                else:
                    gpus.append({"name": gpu_name, "percent": 0.0, "usedGb": 0.0, "totalGb": 0.0})

            for cuda_name, metrics in cuda_metrics.items():
                if cuda_name not in used_cuda_keys:
                    gpus.append({"name": cuda_name, **metrics})

            return {
                "cpu": {"percent": cpu_percent, "name": cpu_name},
                "ram": {"percent": round(mem.percent, 1), "usedGb": mem_used_gb, "totalGb": mem_total_gb},
                "gpus": gpus,
            }

        return await asyncio.get_event_loop().run_in_executor(None, _collect)
    except ImportError:
        return {"cpu": {"percent": 0, "name": ""}, "ram": {"percent": 0, "usedGb": 0, "totalGb": 0},
                "gpus": []}


@router.post("/system/verify-model")
async def system_verify_model(body: dict[str, Any] = Body(...)) -> dict[str, Any]:
    """验证 Embedding 模型路径是否可用"""
    from sentence_transformers import SentenceTransformer
    model_path = (body.get("modelPath") or "").strip()
    if not model_path:
        return {"status": "FAILED", "message": "模型路径为空"}
    try:
        from pathlib import Path
        if not Path(model_path).exists():
            return {"status": "FAILED", "message": f"模型路径不存在: {model_path}"}
        started = time.time()
        model = SentenceTransformer(model_path, device="cpu")
        dims = model.get_sentence_embedding_dimension()
        cost = int((time.time() - started) * 1000)
        return {
            "status": "SUCCESS",
            "message": f"模型加载成功，向量维度: {dims}，耗时: {cost}ms",
            "dims": dims,
            "costMs": cost,
        }
    except Exception as ex:
        return {"status": "FAILED", "message": f"模型加载失败: {str(ex)}"}


@router.get("/system/parse-logs")
async def system_parse_logs(documentId: int | str = 0) -> dict[str, Any]:
    """获取文档解析进度（含阶段详情+处理日志）"""
    from app.core.parse_progress import get_progress_or_empty
    doc_id = int(documentId) if documentId else 0
    return get_progress_or_empty(doc_id)


@router.post("/system/cancel-parse")
async def system_cancel_parse(body: dict[str, Any] = Body(...)) -> dict[str, Any]:
    from app.core.parse_progress import cancel_task
    doc_id = int(body.get("documentId", 0))
    if cancel_task(doc_id):
        from app.utils import delete_by_document_id
        try:
            delete_by_document_id(str(doc_id))
            logger.info("Cancel: ES data deleted for doc=%s", doc_id)
        except Exception as e:
            logger.warning("Cancel: ES delete failed for doc=%s: %s", doc_id, e)
        return {"status": "SUCCESS", "message": f"文档 {doc_id} 已终止，ES 索引已回滚"}
    return {"status": "FAILED", "message": "任务未在处理中或已完成"}
