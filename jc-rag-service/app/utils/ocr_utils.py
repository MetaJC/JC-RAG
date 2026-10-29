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


"""OCR 工具模块"""
from __future__ import annotations

import logging
import os
from typing import Any

os.environ.setdefault("OMP_NUM_THREADS", "2")
os.environ.setdefault("OMP_THREAD_LIMIT", "2")
os.environ.setdefault("MKL_NUM_THREADS", "2")

logger = logging.getLogger(__name__)

_ocr_engine = None
_ocr_runtime_device: str | None = None


def _create_ocr_engine(force_cpu: bool = False):
    from rapidocr_onnxruntime import RapidOCR
    if force_cpu:
        return RapidOCR()
    _, use_gpu_flags = _detect_gpu_provider()
    return RapidOCR(**use_gpu_flags)


def get_ocr_execution_device() -> str:
    """Return the provider that will actually execute OCR, for progress logs."""
    if _ocr_runtime_device:
        return _ocr_runtime_device
    providers, _ = _detect_gpu_provider()
    if "CUDAExecutionProvider" in providers:
        try:
            import torch
            return f"CUDA ({torch.cuda.get_device_name(0)})"
        except Exception:
            return "CUDA GPU"
    if "DmlExecutionProvider" in providers:
        try:
            from app.core.config import get_settings
            from app.core.device_detector import detect_all_devices
            devices = detect_all_devices(get_settings())
            device = next((item for item in devices if item.get("available") and str(item.get("id", "")).startswith("dml:")), None)
            if device:
                return str(device["name"])
        except Exception:
            pass
        return "DirectML GPU"
    return "CPU"


def _detect_gpu_provider() -> tuple[list[str], dict[str, bool]]:
    """检测可用的 GPU 加速方式，优先级: CUDA(NVIDIA独显) > DirectML > CPU"""
    providers: list[str] = []
    use_gpu_flags: dict[str, bool] = {}
    try:
        import onnxruntime as ort
        available = ort.get_available_providers()
        if "CUDAExecutionProvider" in available:
            from onnxruntime import InferenceSession
            test_model = None
            try:
                import importlib.util
                spec = importlib.util.find_spec("rapidocr_onnxruntime")
                if spec and spec.submodule_search_locations:
                    from pathlib import Path
                    model_dir = Path(spec.submodule_search_locations[0]) / "models"
                    det_models = list(model_dir.glob("ch_PP-OCRv4_det_infer.onnx"))
                    if det_models:
                        test_model = str(det_models[0])
            except Exception:
                pass
            cuda_actually_works = False
            if test_model:
                try:
                    sess = InferenceSession(test_model, providers=["CUDAExecutionProvider", "CPUExecutionProvider"])
                    cuda_actually_works = sess.get_providers()[0] == "CUDAExecutionProvider"
                except Exception:
                    pass
            if cuda_actually_works:
                providers.append("CUDAExecutionProvider")
                use_gpu_flags = {"det_use_cuda": True, "cls_use_cuda": True, "rec_use_cuda": True}
                logger.info("RapidOCR 启用 CUDA GPU 加速（NVIDIA 独显优先）")
            else:
                logger.warning("CUDAExecutionProvider 已注册但实际不可用，尝试 DirectML")
        if not providers and "DmlExecutionProvider" in available:
            providers.append("DmlExecutionProvider")
            use_gpu_flags = {"det_use_dml": True, "cls_use_dml": True, "rec_use_dml": True}
            logger.info("RapidOCR 启用 DirectML GPU 加速")
        if not providers:
            logger.info("未检测到 GPU 加速 Provider，OCR 使用 CPU 模式")
    except Exception as e:
        logger.warning("OCR GPU 检测异常: %s", e)
    return providers, use_gpu_flags


def get_ocr_engine():
    """获取 OCR 引擎实例（单例），自动检测 GPU 加速"""
    global _ocr_engine
    if _ocr_engine is not None:
        return _ocr_engine

    from app.core.config import get_settings

    settings = get_settings()
    if not bool(settings.ocr.enabled):
        raise ValueError("OCR 功能未启用")

    try:
        import rapidocr_onnxruntime  # noqa: F401
    except Exception as ex:
        raise ValueError(
            "OCR 依赖未安装，请先执行 pip install -r requirements.txt 以启用扫描版 PDF 解析"
        ) from ex

    _ocr_engine = _create_ocr_engine()
    return _ocr_engine


def _ocr_result_line_key(item: Any) -> tuple[float, float]:
    """用于 OCR 结果排序的 key 函数"""
    box = item[0] if isinstance(item, (list, tuple)) and item else None
    if not isinstance(box, (list, tuple)) or not box:
        return (0.0, 0.0)
    
    xs: list[float] = []
    ys: list[float] = []
    
    for point in box:
        if isinstance(point, (list, tuple)) and len(point) >= 2:
            try:
                xs.append(float(point[0]))
                ys.append(float(point[1]))
            except Exception:
                continue
    
    if not xs or not ys:
        return (0.0, 0.0)
    
    return (sum(ys) / len(ys), sum(xs) / len(xs))


def extract_text_from_ocr_result(result: Any, min_score: float = 0.45) -> str:
    """从 OCR 结果中提取文本"""
    if not isinstance(result, list):
        return ""
    
    lines: list[str] = []
    sorted_items = sorted(result, key=_ocr_result_line_key)
    
    for item in sorted_items:
        if not isinstance(item, (list, tuple)) or len(item) < 2:
            continue
        
        text = str(item[1] or "").strip()
        if not text:
            continue
        
        score = item[2] if len(item) > 2 else None
        try:
            score_value = float(score) if score is not None else 1.0
        except Exception:
            score_value = 1.0
        
        if score_value < min_score:
            continue
        
        lines.append(text)
    
    return "\n".join(lines).strip()


def ocr_pdf_page_text(file_path: str, page_index: int, render_scale: float = 2.0, min_score: float = 0.45) -> str:
    global _ocr_engine, _ocr_runtime_device
    ocr_engine = get_ocr_engine()

    try:
        import fitz
    except Exception as ex:
        raise ValueError(
            "缺少 PDF OCR 渲染依赖 PyMuPDF，请先执行 pip install -r requirements.txt"
        ) from ex

    doc = fitz.open(file_path)
    pixmap = None
    image_bytes = None
    try:
        page = doc.load_page(page_index)
        matrix = fitz.Matrix(render_scale, render_scale)
        pixmap = page.get_pixmap(matrix=matrix, alpha=False)
        image_bytes = pixmap.tobytes("png")

        try:
            result, _ = ocr_engine(image_bytes)
        except Exception as ex:
            # Some DirectML/driver combinations initialise successfully but
            # fail during inference. Continue the document on CPU instead of
            # failing the entire parse job.
            logger.warning("OCR GPU inference failed on page %s; falling back to CPU: %s", page_index + 1, ex)
            _ocr_engine = _create_ocr_engine(force_cpu=True)
            _ocr_runtime_device = "CPU（DirectML 推理失败后回退）"
            result, _ = _ocr_engine(image_bytes)
        return extract_text_from_ocr_result(result, min_score)
    finally:
        pixmap = None
        image_bytes = None
        doc.close()
