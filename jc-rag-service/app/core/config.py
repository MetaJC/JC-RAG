from __future__ import annotations

from functools import lru_cache
from pathlib import Path
from typing import Any

import yaml
from pydantic import BaseModel, Field


class AppConfig(BaseModel):
    name: str
    version: str
    host: str = "0.0.0.0"
    port: int = 8001


class EmbeddingConfig(BaseModel):
    model_path: str
    device: str = "auto"
    dims: int | None = None
    batch_size: int = 32
    normalize_embeddings: bool = True
    lm_studio_url: str = "http://localhost:1234"


class RetrievalConfig(BaseModel):
    top_k: int = 5
    top_n: int = 20
    similarity_threshold: float = 0.6


class ChunkConfig(BaseModel):
    size: int = 700
    overlap: int = 120


class HistoryConfig(BaseModel):
    rounds: int = 3


class OcrConfig(BaseModel):
    enabled: bool = True
    pdf_fallback: bool = True
    render_scale: float = 2.0
    text_score: float = 0.45


class ElasticsearchConfig(BaseModel):
    url: str
    index_name: str
    memory_index_name: str = "jc_chat_memory"  # L2 长期事实索引


class ProfileConfig(BaseModel):
    """L3 跨会话画像配置 (由 L2 长期事实聚合并经 LLM 提炼)"""
    enabled: bool = True
    max_skills: int = 20
    max_goals: int = 10


class MemoryConfig(BaseModel):
    """L2 长期事实 (向量记忆) 配置"""
    enabled: bool = True
    extract_top_k: int = 5
    recall_top_k: int = 5
    recall_similarity_threshold: float = 0.6
    dedup_similarity_threshold: float = 0.9
    max_per_user: int = 500
    categories: list[str] = [
        "tech_stack", "career", "project", "preference", "goal", "other"
    ]
    profile: ProfileConfig = ProfileConfig()


class ProviderDefaults(BaseModel):
    temperature: float = 0.3
    top_p: float = Field(default=0.8, alias="top_p")
    max_tokens: int = 1024
    stream: bool = True


class OllamaConfig(BaseModel):
    base_url: str
    chat_path: str
    tags_path: str
    model_name: str


class OfflineProviderConfig(BaseModel):
    provider: str
    ollama: OllamaConfig


class OnlineProviderConfig(BaseModel):
    provider: str
    base_url: str
    chat_path: str
    api_key: str = ""
    model_name: str


class ProvidersConfig(BaseModel):
    active_mode: str = "offline"
    defaults: ProviderDefaults
    offline: OfflineProviderConfig
    online: OnlineProviderConfig


class ServicesConfig(BaseModel):
    springboot_base_url: str


class MinioBucketConfig(BaseModel):
    documents: str


class MinioConfig(BaseModel):
    endpoint: str
    access_key: str
    secret_key: str
    secure: bool = False
    bucket: MinioBucketConfig


class StorageConfig(BaseModel):
    minio: MinioConfig


class Settings(BaseModel):
    app: AppConfig
    embedding: EmbeddingConfig
    retrieval: RetrievalConfig
    chunk: ChunkConfig
    history: HistoryConfig
    ocr: OcrConfig = OcrConfig()
    elasticsearch: ElasticsearchConfig
    memory: MemoryConfig = MemoryConfig()
    providers: ProvidersConfig
    services: ServicesConfig
    storage: StorageConfig


def _load_yaml_config() -> dict[str, Any]:
    config_path = Path(__file__).resolve().parents[2] / "rag_config.yml"
    with config_path.open("r", encoding="utf-8") as file:
        return yaml.safe_load(file)


@lru_cache
def get_settings() -> Settings:
    return Settings.model_validate(_load_yaml_config())
