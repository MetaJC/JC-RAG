/*
 * Copyright 2024-2026 OpenJC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 本文件来源于 JC RAG Community Edition
 * 开源地址: https://gitee.com/Open-JC/jc-rag | https://github.com/MetaJC/JC-RAG
 * 二次开发请保留此出处声明。
 */


export interface HealthItem {
  name: string
  status: 'UP' | 'DOWN' | 'UNKNOWN'
  endpoint: string
  description: string
}

export interface RuntimeConfigResponse {
  app: {
    name: string
    version: string
    deploymentMode: string
  }
  model: {
    activeMode: 'online' | 'offline' | string
    defaults: {
      temperature: number
      topP: number
      maxTokens: number
    }
    offline: {
      provider: string
      ollama: {
        baseUrl: string
        chatPath: string
        tagsPath: string
        defaultModel: string
      }
    }
    online: {
      baseUrl: string
      chatPath: string
      modelName: string
      apiKeyConfigured: boolean
    }
  }
  rag: {
    embedding: {
      source: 'local' | 'service' | string
      modelPath: string
      device: string
      dims: number
      batchSize: number
      normalizeEmbeddings: boolean
      service: {
        provider: string
        modelName: string
        dims: number
        baseUrl: string
        apiKeyConfigured: boolean
      }
    }
    retrieval: {
      topK: number
      topN: number
      similarityThreshold: number
    }
    chunk: {
      size: number
      overlap: number
    }
    history: {
      rounds: number
    }
  }
  service: {
    pythonRagBaseUrl: string
    ollama: {
      baseUrl: string
      chatPath: string
      tagsPath: string
    }
  }
}

export interface RuntimeConfigUpdateRequest {
  activeMode: string
  offlineModel: string
  ollamaBaseUrl: string
  ollamaChatPath: string
  ollamaTagsPath: string
  onlineBaseUrl: string
  onlineModelName: string
  onlineApiKey: string
  embeddingModelPath: string
  embeddingDevice: string
  embeddingDims: number
  embeddingBatchSize: number
  embeddingNormalizeEmbeddings: boolean
  embeddingSource?: string
  embeddingServiceProvider?: string
  embeddingServiceModel?: string
  embeddingServiceDims?: number
  temperature: number
  topP: number
  maxTokens: number
  topK: number
  topN: number
  similarityThreshold: number
  chunkSize: number
  chunkOverlap: number
  lmstudioBaseUrl?: string
}

export interface DeviceItem {
  id: string
  name: string
  type: 'gpu' | 'cpu' | 'llm_studio' | 'ollama' | 'other'
  performanceRank: number
  available: boolean
  detail: string
}

export interface BrowseDirectory {
  name: string
  path: string
}

export interface BrowseDirectoryResult {
  currentPath: string
  parentPath: string | null
  directories: BrowseDirectory[]
}

/** 模型验证结果, 与后端 EmbeddingVerifyVO 对齐 */
export interface ModelVerifyResult {
  valid?: boolean
  message: string
  modelPath?: string
  sizeBytes?: number
}

export interface DocumentParseStage {
  stageId: string
  stageName: string
  status: 'pending' | 'running' | 'completed' | 'failed'
  progress: number
  order: number
  deviceInfo?: string
  startedAt?: string | null
  completedAt?: string | null
}

export interface DocumentParseProgress {
  documentId: number
  fileName: string
  fileExt: string
  fileSize: number
  overallProgress: number
  parseStatus: string
  currentStage: string
  stages: DocumentParseStage[]
  logs: ParseLogEntry[]
}

export interface ParseLogEntry {
  timestamp: string
  stageId: string
  level: 'info' | 'warn' | 'error'
  message: string
  deviceInfo?: string
}
