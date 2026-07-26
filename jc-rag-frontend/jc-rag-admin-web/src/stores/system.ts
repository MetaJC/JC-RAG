import { computed, reactive, ref } from 'vue'
import { defineStore } from 'pinia'

import { browseDirectory, fetchDevices, fetchOllamaModels, fetchRuntimeConfig, fetchServiceHealth, saveRuntimeConfig, verifyModel } from '../api/sys'
import { appDefaults } from '../config/appDefaults'
import type { BrowseDirectory, DeviceItem, HealthItem, RuntimeConfigResponse, RuntimeConfigUpdateRequest } from '../types/system'

const defaultHealthItems: HealthItem[] = [
  {
    name: 'SpringBoot',
    status: 'UP',
    endpoint: 'http://localhost:8080/api/health/basic',
    description: '统一权限、配置与业务接口入口',
  },
  {
    name: 'Python RAG',
    status: 'UNKNOWN',
    endpoint: 'http://localhost:8001/health',
    description: '文档解析、检索、模型调用服务',
  },
  {
    name: 'Ollama',
    status: 'UNKNOWN',
    endpoint: 'http://localhost:11434/api/tags',
    description: '本地离线模型运行时',
  },
  {
    name: 'Elasticsearch',
    status: 'UNKNOWN',
    endpoint: 'http://localhost:9200',
    description: '向量与全文检索存储',
  },
  {
    name: 'MinIO',
    status: 'UNKNOWN',
    endpoint: 'http://localhost:9000',
    description: '文档对象存储',
  },
]

export const useSystemStore = defineStore('system', () => {
  const loading = ref(false)
  const saving = ref(false)
  const runtimeConfig = ref<RuntimeConfigResponse | null>(null)
  const healthItems = ref<HealthItem[]>(defaultHealthItems)
  const activeMenu = ref('system-settings')
  const offlineModelOptions = ref<string[]>([])
  const offlineModelDetails = ref<Record<string, string[]>>({}) // name -> capabilities
  const offlineModelLoading = ref(false)

  const devices = ref<DeviceItem[]>([])
  const devicesLoading = ref(false)

  const modelVerifyStatus = ref<'success' | 'error' | 'loading' | ''>('')
  const modelVerifyMessage = ref('')

  const browseVisible = ref(false)
  const browseParentPath = ref<string | null>(null)
  const browseCurrentPath = ref('')
  const browseError = ref('')
  const browseLoading = ref(false)
  const browseDirectories = ref<BrowseDirectory[]>([])

  const form = reactive<RuntimeConfigUpdateRequest>({
    activeMode: 'offline',
    offlineModel: 'qwen1.5-1.8b-gguf',
    ollamaBaseUrl: appDefaults.ollamaUrl,
    ollamaChatPath: appDefaults.ollamaChatPath,
    ollamaTagsPath: appDefaults.ollamaTagsPath,
    onlineBaseUrl: '',
    onlineModelName: '',
    onlineApiKey: '',
    embeddingModelPath: appDefaults.embeddingModelPath,
    embeddingDevice: 'auto',
    embeddingDims: 1024,
    embeddingBatchSize: 32,
    embeddingNormalizeEmbeddings: true,
    temperature: 0.3,
    topP: 0.8,
    maxTokens: 1024,
    topK: 5,
    topN: 20,
    similarityThreshold: 0.6,
    chunkSize: 700,
    chunkOverlap: 120,
    lmstudioBaseUrl: appDefaults.lmstudioUrl,
  })

  const activeMode = computed(() => form.activeMode)

  function bindForm(config: RuntimeConfigResponse | null) {
    if (!config) return
    form.activeMode = config.model.activeMode
    form.offlineModel = config.model.offline.ollama.defaultModel
    form.ollamaBaseUrl = config.model.offline.ollama.baseUrl
    form.ollamaChatPath = config.model.offline.ollama.chatPath
    form.ollamaTagsPath = config.model.offline.ollama.tagsPath
    form.onlineBaseUrl = config.model.online.baseUrl
    form.onlineModelName = config.model.online.modelName
    form.onlineApiKey = ''
    form.embeddingModelPath = config.rag.embedding.modelPath
    form.embeddingDevice = config.rag.embedding.device
    form.embeddingDims = config.rag.embedding.dims
    form.embeddingBatchSize = config.rag.embedding.batchSize
    form.embeddingNormalizeEmbeddings = config.rag.embedding.normalizeEmbeddings
    form.temperature = config.model.defaults.temperature
    form.topP = config.model.defaults.topP
    form.maxTokens = config.model.defaults.maxTokens
    form.topK = config.rag.retrieval.topK
    form.topN = config.rag.retrieval.topN
    form.similarityThreshold = config.rag.retrieval.similarityThreshold
    form.chunkSize = config.rag.chunk.size
    form.chunkOverlap = config.rag.chunk.overlap
  }

  async function loadRuntimeConfig(loadAuxiliary = true) {
    loading.value = true
    try {
      runtimeConfig.value = await fetchRuntimeConfig()
      bindForm(runtimeConfig.value)
      if (loadAuxiliary) {
        await Promise.all([
          refreshOfflineModels(),
          refreshDevices(),
          refreshServiceHealth(),
        ])
      }
    } finally {
      loading.value = false
    }
  }

  async function refreshOfflineModels() {
    offlineModelLoading.value = true
    try {
      const data = await fetchOllamaModels()
      const names: string[] = []
      const details: Record<string, string[]> = {}
      for (const item of data.models ?? []) {
        if (typeof item === 'string') {
          names.push(item)
          continue
        }
        if (item && typeof item === 'object' && typeof item.name === 'string') {
          names.push(item.name)
          // 提取 capabilities
          const caps = Array.isArray(item.capabilities) ? item.capabilities.map(String) : []
          details[item.name] = caps
        }
      }
      const unique = Array.from(new Set(names)).filter((it) => it && it.trim().length > 0)
      offlineModelOptions.value = unique
      offlineModelDetails.value = details
    } finally {
      offlineModelLoading.value = false
    }
  }

  async function refreshServiceHealth() {
    try {
      healthItems.value = await fetchServiceHealth()
    } catch {
      /* health check best-effort */
    }
  }

  async function refreshDevices() {
    devicesLoading.value = true
    try {
      devices.value = await fetchDevices()
    } catch {
      devices.value = [{ id: 'auto', name: 'Auto（自动选择最优设备）', type: 'other', performanceRank: 999, available: true, detail: '设备检测失败，仅 Auto 可用' }]
    } finally {
      devicesLoading.value = false
    }
  }

  async function verifyModelAction(modelPath: string) {
    modelVerifyStatus.value = 'loading'
    modelVerifyMessage.value = ''
    try {
      const result = await verifyModel(modelPath)
      if (result.valid === true) {
        modelVerifyStatus.value = 'success'
        modelVerifyMessage.value = result.message || '模型验证通过'
      } else {
        modelVerifyStatus.value = 'error'
        modelVerifyMessage.value = result.message || '模型验证失败'
      }
    } catch (ex: unknown) {
      modelVerifyStatus.value = 'error'
      modelVerifyMessage.value = (ex as Error)?.message || '验证请求失败'
    }
  }

  function openBrowse() {
    browseError.value = ''
    browseDirectories.value = []
    if (form.embeddingModelPath) {
      loadBrowseDirectory(form.embeddingModelPath)
    } else {
      loadBrowseDirectory('D:\\')
    }
    browseVisible.value = true
  }

  async function loadBrowseDirectory(dirPath: string) {
    browseLoading.value = true
    browseError.value = ''
    try {
      const result = await browseDirectory(dirPath)
      browseCurrentPath.value = result.currentPath
      browseParentPath.value = result.parentPath
      browseDirectories.value = result.directories
    } catch (ex: unknown) {
      browseError.value = '无法访问目录: ' + ((ex as Error)?.message || '未知错误')
    } finally {
      browseLoading.value = false
    }
  }

  function selectBrowsePath(dirPath: string) {
    form.embeddingModelPath = dirPath
    browseVisible.value = false
  }

  async function saveSettings() {
    saving.value = true
    try {
      await saveRuntimeConfig({ ...form })
      // save 端点返回 R<Void>（data=null），不要 bindForm(null)；重新拉一次拿最新值
      const fresh = await fetchRuntimeConfig()
      runtimeConfig.value = fresh
      bindForm(fresh)
      await Promise.all([refreshServiceHealth(), refreshOfflineModels()])
    } finally {
      saving.value = false
    }
  }

  function setActiveMenu(menu: string) {
    activeMenu.value = menu
  }

  return {
    activeMode,
    activeMenu,
    form,
    healthItems,
    loading,
    offlineModelLoading,
    offlineModelOptions,
    offlineModelDetails,
    saving,
    runtimeConfig,
    devices,
    devicesLoading,
    modelVerifyStatus,
    modelVerifyMessage,
    browseVisible,
    browseParentPath,
    browseCurrentPath,
    browseError,
    browseLoading,
    browseDirectories,
    loadRuntimeConfig,
    refreshOfflineModels,
    refreshServiceHealth,
    refreshDevices,
    verifyModel: verifyModelAction,
    openBrowse,
    loadBrowseDirectory,
    selectBrowsePath,
    saveSettings,
    setActiveMenu,
  }
})
