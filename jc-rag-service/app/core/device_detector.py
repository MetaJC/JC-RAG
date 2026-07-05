"""
设备自动检测模块。

检测所有可用于 Embedding/OCR 的加速设备，按性能从高到低排序：
独显(NVIDIA) > 集显(Intel Arc / DirectML) > LM Studio > Ollama > CPU
"""
from __future__ import annotations

import logging
import platform
import re
import subprocess
from dataclasses import dataclass
from typing import Any

logger = logging.getLogger(__name__)


@dataclass
class DeviceInfo:
    id: str
    name: str
    type: str
    performance_rank: int
    available: bool
    detail: str


def _resolve_cpu_name() -> str:
    system = platform.system()
    if system == "Windows":
        try:
            result = subprocess.check_output(
                ["powershell", "-Command", "Get-CimInstance Win32_Processor | Select-Object -ExpandProperty Name"],
                text=True, timeout=5
            )
            name = result.strip()
            if name:
                return name
        except Exception:
            pass
        try:
            result = subprocess.check_output(
                ["powershell", "-Command", "(Get-ItemProperty 'HKLM:\\HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0').ProcessorNameString"],
                text=True, timeout=5
            )
            name = result.strip()
            if name:
                return name
        except Exception:
            pass
    elif system == "Linux":
        try:
            with open("/proc/cpuinfo", "r") as f:
                for line in f:
                    if "model name" in line:
                        return line.split(":", 1)[1].strip()
        except Exception:
            pass
    elif system == "Darwin":
        try:
            result = subprocess.check_output(["sysctl", "-n", "machdep.cpu.brand_string"], text=True, timeout=5)
            return result.strip()
        except Exception:
            pass
    raw = platform.processor() or "CPU"
    if raw and raw != "CPU":
        return raw
    return f"Unknown {platform.machine()}"


def _calc_cuda_rank(name: str) -> int:
    name_lower = name.lower()
    if re.search(r'50[5-9]0', name_lower):
        return 1
    if re.search(r'4090|4080|4070|4060', name_lower):
        return 2
    if re.search(r'3090|3080|3070|3060', name_lower):
        return 3
    if 'titan' in name_lower:
        return 2
    if re.search(r'20\d0', name_lower) or 'quadro' in name_lower:
        return 4
    return 5


def _detect_windows_gpus() -> list[DeviceInfo]:
    """Windows 原生 GPU 枚举（无需 PyTorch），用于列出硬件但不承诺可用性"""
    devices: list[DeviceInfo] = []
    try:
        ps_cmd = (
            "Get-CimInstance Win32_VideoController | "
            "Select-Object Name, AdapterRAM, DriverVersion | "
            "ConvertTo-Json"
        )
        result = subprocess.check_output(["powershell", "-Command", ps_cmd], text=True, timeout=8)
        if result.strip():
            import json
            data = json.loads(result)
            if isinstance(data, dict):
                data = [data]
            seen_names = set()
            for idx, gpu in enumerate(data):
                name = (gpu.get("Name") or "").strip()
                if not name or name in seen_names:
                    continue
                name_lower = name.lower()
                skip_keywords = ["oray", "idddriver", "vnc", "rdp", "virtual", "remote", "mirage", "citrix", "parsec"]
                if any(k in name_lower for k in skip_keywords):
                    continue
                seen_names.add(name)
                ram_bytes = gpu.get("AdapterRAM", 0) or 0
                ram_gb = ram_bytes / (1024**3) if ram_bytes else 0
                ram_str = f" ({ram_gb:.1f} GB)" if ram_gb > 0 else ""
                detail = f"{name}{ram_str}"
                rank = 20
                if any(k in name.lower() for k in ["nvidia", "rtx", "gtx", "quadro"]):
                    rank = 10
                elif "arc" in name.lower():
                    rank = 12
                elif any(k in name.lower() for k in ["intel", "uhd", "iris", "hd graphics"]):
                    rank = 18
                elif any(k in name.lower() for k in ["amd", "radeon"]):
                    rank = 14
                devices.append(DeviceInfo(
                    id=f"win_gpu_{idx}", name=name, type="gpu",
                    performance_rank=rank, available=False,
                    detail=f"{name}{ram_str}"
                ))
    except Exception:
        pass
    return devices


def _detect_directml_devices() -> list[DeviceInfo]:
    devices: list[DeviceInfo] = []
    try:
        import torch_directml
    except ImportError:
        logger.debug("torch-directml not installed")
        return devices
    except Exception as e:
        logger.warning(f"torch-directml 导入失败（版本不兼容？）: {e}")
        devices.append(DeviceInfo(
            id="dml:error", name="DirectML（不可用）", type="gpu",
            performance_rank=200, available=False,
            detail=f"torch-directml 已安装但导入失败，请升级至与 PyTorch 匹配的版本: {e}"
        ))
        return devices

    try:
        count = torch_directml.device_count()
        for i in range(count):
            name = torch_directml.device_name(i) if hasattr(torch_directml, 'device_name') else f"DirectML Device {i}"
            devices.append(DeviceInfo(
                id=f"dml:{i}", name=name, type="gpu",
                performance_rank=8, available=True,
                detail=f"DirectML {i}: {name}"
            ))
    except Exception as e:
        logger.warning(f"DirectML device enum failed: {e}")
    return devices


def _detect_torch_devices() -> list[DeviceInfo]:
    devices: list[DeviceInfo] = []
    try:
        import torch

        if torch.cuda.is_available():
            count = torch.cuda.device_count()
            for i in range(count):
                try:
                    name = torch.cuda.get_device_name(i)
                    props = torch.cuda.get_device_properties(i)
                    mem_gb = props.total_memory / (1024**3)
                    detail = f"CUDA {i}: {name} ({mem_gb:.1f} GB)"
                    rank = _calc_cuda_rank(name)
                    devices.append(DeviceInfo(
                        id=f"cuda:{i}", name=name, type="gpu",
                        performance_rank=rank, available=True, detail=detail
                    ))
                except Exception:
                    devices.append(DeviceInfo(
                        id=f"cuda:{i}", name=f"CUDA Device {i}", type="gpu",
                        performance_rank=10, available=True, detail="CUDA device"
                    ))
        else:
            logger.info("torch.cuda not available")

        if hasattr(torch, 'xpu') and torch.xpu.is_available():
            try:
                count = torch.xpu.device_count()
                for i in range(count):
                    name = torch.xpu.get_device_name(i) if hasattr(torch.xpu, 'get_device_name') else f"XPU Device {i}"
                    devices.append(DeviceInfo(
                        id=f"xpu:{i}", name=name, type="gpu",
                        performance_rank=7, available=True,
                        detail=f"Intel XPU {i}: {name}"
                    ))
            except Exception:
                devices.append(DeviceInfo(
                    id="xpu:0", name="Intel Arc GPU", type="gpu",
                    performance_rank=7, available=True,
                    detail="Intel Arc (XPU) via PyTorch"
                ))

        if hasattr(torch.backends, 'mps') and torch.backends.mps.is_available():
            devices.append(DeviceInfo(
                id="mps", name="Apple MPS (Metal)",
                type="gpu", performance_rank=15, available=True,
                detail="Apple Metal Performance Shaders"
            ))

    except ImportError:
        logger.info("PyTorch not installed, skipping torch device detection")

    return devices


def _detect_external_services(settings) -> list[DeviceInfo]:
    devices: list[DeviceInfo] = []
    import urllib.request
    import json

    try:
        lm_studio_url = getattr(settings, 'lm_studio_base_url', 'http://localhost:1234') or 'http://localhost:1234'
        req = urllib.request.Request(f"{lm_studio_url.rstrip('/')}/v1/models", method="GET")
        with urllib.request.urlopen(req, timeout=3) as resp:
            data = json.loads(resp.read().decode())
            models = data.get("data", [])
            model_names = [m.get("id", "unknown") for m in models] if models else ["unknown"]
            devices.append(DeviceInfo(
                id="lm_studio", name="LM Studio", type="llm_studio",
                performance_rank=50, available=True,
                detail=f"LM Studio ({lm_studio_url}) - 模型: {', '.join(model_names[:3])}"
            ))
    except Exception:
        devices.append(DeviceInfo(
            id="lm_studio", name="LM Studio", type="llm_studio",
            performance_rank=50, available=False,
            detail="LM Studio 未连接"
        ))

    try:
        ollama_url = getattr(settings, 'ollama_base_url', 'http://localhost:11434') or 'http://localhost:11434'
        req = urllib.request.Request(f"{ollama_url.rstrip('/')}/api/tags", method="GET")
        with urllib.request.urlopen(req, timeout=3) as resp:
            data = json.loads(resp.read().decode())
            models = data.get("models", [])
            model_names = [m.get("name", "unknown") for m in models] if models else ["unknown"]
            devices.append(DeviceInfo(
                id="ollama", name="Ollama", type="ollama",
                performance_rank=60, available=True,
                detail=f"Ollama ({ollama_url}) - 模型: {', '.join(model_names[:3])}"
            ))
    except Exception:
        devices.append(DeviceInfo(
            id="ollama", name="Ollama", type="ollama",
            performance_rank=60, available=False,
            detail="Ollama 未连接"
        ))

    return devices


def detect_all_devices(settings) -> list[dict[str, Any]]:
    result: list[DeviceInfo] = []

    torch_devices = _detect_torch_devices()
    dml_devices = _detect_directml_devices()
    win_gpus = _detect_windows_gpus()
    torch_ids = {d.id for d in torch_devices}
    dml_ids = {d.id for d in dml_devices}

    def _gpu_name_match(dev_name: str, wmi_name: str) -> bool:
        tn = dev_name.lower()
        wn = wmi_name.lower()
        if tn == wn:
            return True
        if tn in wn or wn in tn:
            return True
        tokens_t = set(re.findall(r'[a-z0-9]+', tn))
        tokens_w = set(re.findall(r'[a-z0-9]+', wn))
        overlap = tokens_t & tokens_w
        return len(overlap) >= 3

    used_wmi = set()
    for td in torch_devices:
        result.append(td)
        for wg in win_gpus:
            if wg.id in used_wmi:
                continue
            if _gpu_name_match(td.name, wg.name):
                if td.name.strip().lower() != wg.name.strip().lower():
                    result.append(DeviceInfo(
                        id=f"{td.id}_hw", name=wg.name, type=td.type,
                        performance_rank=td.performance_rank, available=td.available,
                        detail=f"{td.detail}（WMI: {wg.name}）"
                    ))
                used_wmi.add(wg.id)
                break

    for dd in dml_devices:
        result.append(dd)
        for wg in win_gpus:
            if wg.id in used_wmi:
                continue
            if _gpu_name_match(dd.name, wg.name):
                used_wmi.add(wg.id)
                break

    for wg in win_gpus:
        if wg.id in used_wmi:
            continue
        filter_keywords = ["oray", "vnc", "rdp", "idd", "virtual", "remote", "mirage", "citrix"]
        if any(k in wg.name.lower() for k in filter_keywords):
            continue
        wg.performance_rank = 90
        wg.detail = f"{wg.detail}（未检测到加速框架，需安装对应驱动）"
        result.append(wg)

    result.extend(_detect_external_services(settings))

    cpu_name = _resolve_cpu_name()
    result.append(DeviceInfo(
        id="cpu", name=cpu_name, type="cpu",
        performance_rank=80, available=True,
        detail=f"{cpu_name} ({platform.machine()})"
    ))

    auto_device = DeviceInfo(
        id="auto", name="Auto（自动选择最优设备）", type="other",
        performance_rank=999, available=True,
        detail="根据性能从高到低自动选择可用设备"
    )

    sorted_devices = sorted(result, key=lambda d: d.performance_rank)
    sorted_devices.append(auto_device)

    return [
        {
            "id": d.id,
            "name": d.name,
            "type": d.type,
            "performanceRank": d.performance_rank,
            "available": d.available,
            "detail": d.detail,
        }
        for d in sorted_devices
    ]


def resolve_best_device() -> str | None:
    torch_devices = _detect_torch_devices()
    dml_devices = _detect_directml_devices()
    all_gpu = torch_devices + dml_devices
    for d in sorted(all_gpu, key=lambda x: x.performance_rank):
        if d.available and d.type == "gpu":
            return d.id
    return None