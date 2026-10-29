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


"""运行时配置路由.
<p>原位置: app/api/routes.py (拆分自 2026-07-03 结构重构, 业务零变更).

@author JC
"""
from __future__ import annotations

from fastapi import APIRouter

from app.core.config import get_settings

router = APIRouter()


@router.get("/runtime/config")
async def runtime_config() -> dict:
    """运行时配置: 供前端读取切换 provider/embedding 等参数"""
    settings = get_settings()
    return {
        "providers": {
            "activeMode": settings.providers.active_mode,
            "ollama": {
                "host": settings.providers.ollama.host,
                "models": settings.providers.ollama.models,
            },
            "openaiCompatible": {
                "models": settings.providers.openai_compatible.models,
            },
        },
        "embedding": {
            "modelPath": settings.embedding.model_path,
            "device": settings.embedding.device,
            "dims": settings.embedding.dims,
            "batchSize": settings.embedding.batch_size,
            "normalizeEmbeddings": settings.embedding.normalize_embeddings,
        },
        "retrieval": {
            "topK": settings.retrieval.top_k,
            "topN": settings.retrieval.top_n,
            "similarityThreshold": settings.retrieval.similarity_threshold,
        },
        "chunk": {
            "size": settings.chunk.size,
            "overlap": settings.chunk.overlap,
        },
    }
