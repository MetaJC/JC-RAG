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
      modelPath: string
      device: string
      dims: number
      batchSize: number
      normalizeEmbeddings: boolean
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

export interface ModelVerifyResult {
  status: 'SUCCESS' | 'FAILED' | 'loading'
  message: string
  detail?: string
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
