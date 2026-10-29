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


"""RAG Agent - 基于检索的问答 Agent"""
from __future__ import annotations

import json
from typing import Any, Dict, List, Optional

from app.core.config import get_settings
from app.embeddings import embed_texts
from app.llm.provider_factory import ProviderFactory
from app.vector_store import FilterExpr
from app.vector_store import get_vector_store

from .agent_base import ToolUsingAgent, AgentFactory


class RagAgent(ToolUsingAgent):
    """RAG 专用 Agent"""
    
    def __init__(self):
        super().__init__(
            name="rag_agent",
            description="基于知识库检索的问答助手，用于回答与知识库相关的问题",
            tool_names=["rag_search", "doc_lookup", "answer_generate"]
        )
    
    def run(self, prompt: str, **kwargs) -> str:
        """执行 RAG 问答"""
        settings = get_settings()
        kb_id = kwargs.get("kb_id")
        
        # 1. 向量化问题
        vectors, _ = embed_texts([prompt], settings)
        query_vector = vectors[0] if vectors else []
        
        if not query_vector:
            return "无法生成查询向量"
        
        # 2. 向量检索
        kb_filter = FilterExpr.eq("kb_id", str(kb_id)) if kb_id else None
        vs = get_vector_store()
        hits = vs.vector_search(
            query_vector,
            filter=kb_filter,
            top_k=settings.retrieval.top_k,
        )
        
        if not hits:
            # 没有检索到结果，直接调用大模型
            return self._generate_answer(prompt, [], settings)
        
        # 3. 构建上下文
        contexts = []
        for hit in hits:
            source = hit.get("_source") or {}
            contexts.append({
                "content": source.get("chunk_text", ""),
                "file_name": source.get("file_name", ""),
                "score": hit.get("_score", 0),
            })
        
        # 4. 生成回答
        return self._generate_answer(prompt, contexts, settings)
    
    def _generate_answer(self, prompt: str, contexts: List[Dict[str, Any]], settings) -> str:
        """生成回答"""
        provider_factory = ProviderFactory(settings)
        
        # 构建提示词
        context_text = "\n\n".join([
            f"【来源: {ctx['file_name']}】\n{ctx['content']}"
            for ctx in contexts
        ])
        
        system_prompt = f"""
你是军事领域RAG问答助手。请根据以下知识片段回答用户问题。

知识片段：
{context_text}

如果知识片段不足以回答，可以结合你的知识自由作答。
"""
        
        # 调用模型
        answer = provider_factory.generate(
            prompt=prompt,
            system_prompt=system_prompt,
            temperature=settings.providers.defaults.temperature,
            max_tokens=settings.providers.defaults.max_tokens
        )
        
        return answer


# 注册 RAG Agent
AgentFactory.register_agent(RagAgent())
