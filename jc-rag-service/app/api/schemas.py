"""API Pydantic 模型集中管理.
<p>所有路由模块共享的请求/响应模型, 集中在此以避免循环导入.
<p>原位置: app/api/routes.py (拆分自 2026-07-03 结构重构, 业务零变更).

@author JC
"""
from __future__ import annotations

from typing import Any

from pydantic import BaseModel, Field


class RagEmbeddingOverride(BaseModel):
    modelPath: str | None = Field(default=None, description="嵌入模型路径")
    device: str | None = Field(default=None, description="auto/cpu/cuda")
    dims: int | None = Field(default=None, description="向量维度")
    batchSize: int | None = Field(default=None, description="编码 batch size")
    normalizeEmbeddings: bool | None = Field(default=None, description="是否归一化")
    source: str | None = Field(default=None, description="Embedding 来源: local/service")
    service: dict[str, Any] | None = Field(default=None, description="在线 Embedding 服务配置 (provider/modelName/dims/baseUrl/apiKey)")


class RagRetrievalOverride(BaseModel):
    similarityThreshold: float | None = Field(default=None, description="相似度阈值")


class RagOverride(BaseModel):
    embedding: RagEmbeddingOverride | None = None
    retrieval: RagRetrievalOverride | None = None


class RetrievalTestRequest(BaseModel):
    kbId: int | str | None = Field(default=None, description="知识库 ID")
    question: str = Field(..., description="查询文本")
    topK: int | None = Field(default=None, description="返回 TopK")
    topN: int | None = Field(default=None, description="kNN 候选数")
    ragOverride: RagOverride | None = Field(default=None, description="参数覆盖")


class HistoryMessage(BaseModel):
    """短期记忆: 单条历史消息.
    <p>由 Java 侧从 chat_message 表读取, 拼装为 messages 数组, 直接交给 LLM,
    避免 LLM 上下文断裂 (例如: 上一句"上海天气如何"问完, 下一句"明天呢" AI 知道"明天"指上海)."""
    role: str = Field(..., description="消息角色: user | assistant")
    content: str = Field(..., description="消息内容")


class ChatAskRequest(BaseModel):
    kbId: int | str | None = Field(default=None, description="知识库 ID")
    question: str = Field(..., description="用户问题")
    topK: int | None = Field(default=None, description="返回 TopK")
    topN: int | None = Field(default=None, description="kNN 候选数")
    similarityThreshold: float | None = Field(default=None, description="相似度阈值")
    providerOverride: dict[str, Any] | None = Field(default=None, description="模型提供商覆盖")
    ragOverride: RagOverride | None = Field(default=None, description="RAG 参数覆盖")
    systemPrompt: str | None = Field(default=None, description="系统提示词")
    agentId: int | None = Field(default=None, description="Agent ID")
    deepThinking: bool = Field(default=False, description="深度思考")
    attachments: list[dict[str, Any]] | None = Field(default=None, description="附件列表(base64)")
    history: list[HistoryMessage] = Field(default_factory=list, description="短期记忆: 近期对话历史 (Java 侧从 chat_message 读, 拼装为 messages 数组发给 LLM)")
    # 透传 LLM 原生 chat_template_kwargs (Qwen3 等模型用: {enable_thinking: false} 关闭思考)
    chatTemplateKwargs: dict[str, Any] | None = Field(default=None, description="LLM 原生 chat_template_kwargs 透传, 例如 {enable_thinking: false}")


class KnowledgeParseRequest(BaseModel):
    documentId: int | str
    kbId: int | str
    fileName: str
    fileExt: str | None = None
    contentType: str | None = None
    versionNo: int | None = None
    docType: str | None = None
    securityLevel: str | None = None
    minioBucket: str
    minioObjectName: str
    chunkSize: int = 700
    chunkOverlap: int = 120
    ragOverride: RagOverride | None = None


class KnowledgeDeleteRequest(BaseModel):
    kbId: int | str | None = None
    documentId: int | str | None = None
