<!--
  Copyright 2024-2026 OpenJC
  
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  
      http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  
  本文件来源于 JC RAG Community Edition
  开源地址: https://gitee.com/Open-JC/jc-rag | https://github.com/MetaJC/JC-RAG
  二次开发请保留此出处声明。
 -->


<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { FolderOpened, InfoFilled, RefreshRight } from '@element-plus/icons-vue'

import { useSystemStore } from '../stores/system'
import { appDefaults } from '../config/appDefaults'

const systemStore = useSystemStore()

/** 提供商 logo 映射 */
const PROVIDER_MAP: Record<string, { name: string; color: string }> = {
  qwen: { name: 'Q', color: '#6366f1' },
  deepseek: { name: 'D', color: '#0ea5e9' },
  llama: { name: 'Me', color: '#0668e1' },
  mistral: { name: 'Mi', color: '#f97316' },
  gemma: { name: 'G', color: '#10b981' },
  phi: { name: 'Ph', color: '#8b5cf6' },
  glm: { name: 'Z', color: '#ef4444' },
  yi: { name: 'Y', color: '#ec4899' },
  gpt: { name: 'O', color: '#10a37f' },
  claude: { name: 'An', color: '#d97706' },
  gemini: { name: 'Go', color: '#4285f4' },
  codellama: { name: 'Me', color: '#0668e1' },
  llama3: { name: 'Me', color: '#0668e1' },
  llama4: { name: 'Me', color: '#0668e1' },
}

function getProviderInfo(modelName: string): { name: string; color: string } {
  const lower = modelName.toLowerCase()
  for (const [key, info] of Object.entries(PROVIDER_MAP)) {
    if (lower.includes(key)) return info
  }
  return { name: modelName.charAt(0).toUpperCase(), color: '#6b7280' }
}

/**
 * 模型能力判断。
 * <p>优先取后端 capabilities；为空时按模型名规则兜底推断。
 * <p>推断规则：含 vl/vision/-v-/4v/gpt-4v → 视觉；含 r1/reasoning/o1/qwq/think → 推理；含 coder/code/functioncall/tools → 工具。
 * @author JC
 */
function inferCapabilities(modelName: string): string[] {
  const lower = (modelName || '').toLowerCase()
  const caps: string[] = []
  if (/(?:^|[-:/.])(?:vl|vision|gpt-4-?v|4v|llava|llava-?[-:/.]|qwen2-?vl|qwen-vl|gemma-?3|mantica)/i.test(lower)
      || /\bvl\b/.test(lower) || lower.includes('vision')) {
    caps.push('vision')
  }
  if (/\b(r1|reasoning|think(?:ing)?|o1|qwq|magpie|deepseek-?r|skywork-?o)\b/.test(lower)) {
    caps.push('thinking')
  }
  if (/(?:coder|code-?llama|code-?qwen|function[-_]?call|tools?|hermes-?tool)/i.test(lower)) {
    caps.push('tools')
  }
  return caps
}

function getCapabilities(modelName: string): string[] {
  const fromBackend = systemStore.offlineModelDetails[modelName]
  if (Array.isArray(fromBackend) && fromBackend.length > 0) return fromBackend
  return inferCapabilities(modelName)
}

function hasVision(modelName: string): boolean {
  return getCapabilities(modelName).includes('vision')
}

function hasReasoning(modelName: string): boolean {
  return getCapabilities(modelName).includes('thinking')
}

function hasTool(modelName: string): boolean {
  return getCapabilities(modelName).includes('tools')
}

/** 下拉框选项数据 */
interface ModelOption {
  name: string
  provider: { name: string; color: string }
  vision: boolean
  reasoning: boolean
  tool: boolean
}

const modelOptions = computed<ModelOption[]>(() => {
  const list = (systemStore.offlineModelOptions || []).map(name => ({
    name,
    provider: getProviderInfo(name),
    vision: hasVision(name),
    reasoning: hasReasoning(name),
    tool: hasTool(name),
  }))
  // 确保当前选中值也在列表中
  const current = systemStore.form.offlineModel
  if (current && !list.some(o => o.name === current)) {
    list.unshift({
      name: current,
      provider: getProviderInfo(current),
      vision: hasVision(current),
      reasoning: hasReasoning(current),
      tool: hasTool(current),
    })
  }
  return list
})

const runtimeConfig = computed(() => systemStore.runtimeConfig)
const healthItems = computed(() => systemStore.healthItems)
const loading = computed(() => systemStore.loading)
const saving = computed(() => systemStore.saving)
const devices = computed(() => systemStore.devices)
const devicesLoading = computed(() => systemStore.devicesLoading)
const modeLabel = computed(() => (systemStore.activeMode === 'online' ? '在线模型模式' : '离线模型模式'))
const effectiveModeLabel = computed(() =>
  runtimeConfig.value?.model?.activeMode === 'online' ? '在线模型模式（生效中）' : '离线模型模式（生效中）',
)
const effectiveRetrievalLabel = computed(() => 'ES 向量检索（embedding 生效）+ 文本回退')

const activeSettingTab = ref('readonly')

const embeddingDeviceRef = ref(systemStore.form.embeddingDevice)

watch(embeddingDeviceRef, (val) => {
  systemStore.form.embeddingDevice = val
})

watch(
  () => systemStore.form.embeddingDevice,
  (val) => {
    if (val !== embeddingDeviceRef.value) {
      embeddingDeviceRef.value = val
    }
  },
)

const showLmstudioUrl = computed(() => embeddingDeviceRef.value === 'lmstudio')

async function onSave() {
  try {
    await systemStore.saveSettings()
    ElMessage.success('配置已保存')
  } catch (error) {
    console.error(error)
    ElMessage.error('配置保存失败，请检查后端日志')
  }
}

async function onRefreshHealth() {
  try {
    await systemStore.refreshServiceHealth()
    ElMessage.success('服务状态已刷新')
  } catch (error) {
    console.error(error)
    ElMessage.error('刷新失败，请检查服务连接')
  }
}

onMounted(() => {
  void systemStore.loadRuntimeConfig()
})
</script>

<template>
  <div>
      <header class="page-header">
        <div>
          <h1>系统设置</h1>
          <p>统一管理模型模式、Ollama、本地嵌入模型和 RAG 运行参数。</p>
        </div>
        <div class="header-actions">
          <el-tag type="success">{{ modeLabel }}</el-tag>
          <el-button type="primary" :loading="saving" @click="onSave">保存配置</el-button>
        </div>
      </header>

      <section class="summary-grid">
        <el-card class="summary-card" shadow="hover">
          <template #header>当前模型模式</template>
          <div class="summary-value">{{ effectiveModeLabel }}</div>
          <div class="summary-desc">以保存后的后端运行时配置为准</div>
        </el-card>
        <el-card class="summary-card" shadow="hover">
          <template #header>默认离线模型</template>
          <div class="summary-value">{{ runtimeConfig?.model?.offline?.ollama?.defaultModel ?? '-' }}</div>
          <div class="summary-desc">Ollama /api/tags 中的模型 name</div>
        </el-card>
        <el-card class="summary-card" shadow="hover">
          <template #header>检索方式</template>
          <div class="summary-value compact">{{ effectiveRetrievalLabel }}</div>
          <div class="summary-desc">启用 embedding 后需对已上传文档执行重建索引</div>
        </el-card>
      </section>

      <section class="tab-section">
        <el-card class="panel-card" shadow="never">
          <el-tabs v-model="activeSettingTab">
            <el-tab-pane label="当前生效配置" name="readonly">
              <el-descriptions :column="1" border>
                <el-descriptions-item label="模型模式">
                  {{ runtimeConfig?.model?.activeMode ?? '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="离线 Ollama">
                  {{ runtimeConfig?.service?.ollama?.baseUrl ?? '-' }}{{ runtimeConfig?.service?.ollama?.chatPath ?? '' }}
                </el-descriptions-item>
                <el-descriptions-item label="离线默认模型">
                  {{ runtimeConfig?.model?.offline?.ollama?.defaultModel ?? '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="在线接口">
                  {{ runtimeConfig?.model?.online?.baseUrl ?? '-' }}{{ runtimeConfig?.model?.online?.chatPath ?? '' }}
                </el-descriptions-item>
                <el-descriptions-item label="在线模型名称">
                  {{ runtimeConfig?.model?.online?.modelName ?? '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="在线 API Key">
                  <el-tag :type="runtimeConfig?.model?.online?.apiKeyConfigured ? 'success' : 'warning'">
                    {{ runtimeConfig?.model?.online?.apiKeyConfigured ? '已配置（不回显）' : '未配置' }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="Python RAG 服务">
                  {{ runtimeConfig?.service?.pythonRagBaseUrl ?? '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="分块参数">
                  size={{ runtimeConfig?.rag?.chunk?.size ?? '-' }}, overlap={{ runtimeConfig?.rag?.chunk?.overlap ?? '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="Embedding 参数">
                  dims={{ runtimeConfig?.rag?.embedding?.dims ?? '-' }}, batchSize={{ runtimeConfig?.rag?.embedding?.batchSize ?? '-' }},
                  normalize={{ runtimeConfig?.rag?.embedding?.normalizeEmbeddings ?? '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="召回参数">
                  topK={{ runtimeConfig?.rag?.retrieval?.topK ?? '-' }}, topN={{ runtimeConfig?.rag?.retrieval?.topN ?? '-' }},
                  threshold={{ runtimeConfig?.rag?.retrieval?.similarityThreshold ?? '-' }}
                </el-descriptions-item>
              </el-descriptions>
            </el-tab-pane>

            <el-tab-pane label="模型配置" name="model">
              <el-form label-position="top" v-loading="loading">
                <el-form-item label="模型模式">
                  <el-segmented v-model="systemStore.form.activeMode" :options="[
                    { label: '离线模型', value: 'offline' },
                    { label: '在线模型', value: 'online' },
                  ]" />
                </el-form-item>

                <div class="model-cards">
                  <el-card class="model-card" shadow="hover">
                    <template #header>
                      <div class="model-card-title">
                        <span>离线模型配置</span>
                        <el-tag v-if="systemStore.form.activeMode === 'offline'" type="success" size="small">当前使用</el-tag>
                      </div>
                    </template>
                    <el-form-item label="Ollama 地址">
                      <el-input v-model="systemStore.form.ollamaBaseUrl" />
                    </el-form-item>
                    <el-form-item label="聊天接口">
                      <el-input v-model="systemStore.form.ollamaChatPath" />
                    </el-form-item>
                    <el-form-item label="模型列表接口">
                      <el-input v-model="systemStore.form.ollamaTagsPath" />
                    </el-form-item>
                    <el-form-item label="默认离线模型">
                      <div class="inline-row">
                        <el-select
                          v-model="systemStore.form.offlineModel"
                          placeholder="选择 Ollama 模型"
                          :loading="systemStore.offlineModelLoading"
                          filterable
                          allow-create
                          class="offline-model-select"
                          :popper-class="'offline-model-popper'"
                        >
                          <el-option
                            v-for="opt in modelOptions"
                            :key="opt.name"
                            :label="opt.name"
                            :value="opt.name"
                          >
                            <div class="model-option-row">
                              <span class="model-provider-logo" :style="{ background: opt.provider.color }">{{ opt.provider.name }}</span>
                              <span class="model-option-name">{{ opt.name }}</span>
                              <span class="model-capability-icons">
                                <el-tooltip v-if="opt.vision" content="支持视觉/图片" placement="top" :show-after="200">
                                  <span class="cap-icon vision-icon">
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                                  </span>
                                </el-tooltip>
                                <el-tooltip v-if="opt.reasoning" content="支持深度推理" placement="top" :show-after="200">
                                  <span class="cap-icon reasoning-icon">
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2a4 4 0 0 1 4 4c0 1.95-1.4 3.58-3.25 3.93V12h3.75a2.5 2.5 0 0 1 2.5 2.5v1.75c1.85.35 3.25 1.98 3.25 3.93a4 4 0 1 1-7.25-2.33V14.5a.5.5 0 0 0-.5-.5h-9a.5.5 0 0 0-.5.5v3.35A4 4 0 1 1 2.25 14.5a2.5 2.5 0 0 1 2.5-2.5h3.75V9.93A4.002 4.002 0 0 1 12 2z"/></svg>
                                  </span>
                                </el-tooltip>
                                <el-tooltip v-if="opt.tool" content="支持工具调用" placement="top" :show-after="200">
                                  <span class="cap-icon tool-icon">
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/></svg>
                                  </span>
                                </el-tooltip>
                              </span>
                            </div>
                          </el-option>
                        </el-select>
                        <el-tooltip content="刷新模型列表" placement="top" :show-after="200">
                          <el-button :icon="RefreshRight" :loading="systemStore.offlineModelLoading" @click="systemStore.refreshOfflineModels" circle />
                        </el-tooltip>
                      </div>
                    </el-form-item>
                  </el-card>

                  <el-card class="model-card" shadow="hover">
                    <template #header>
                      <div class="model-card-title">
                        <span>在线模型配置</span>
                        <el-tag v-if="systemStore.form.activeMode === 'online'" type="success" size="small">当前使用</el-tag>
                      </div>
                    </template>
                    <el-form-item label="在线接口地址">
                      <el-input v-model="systemStore.form.onlineBaseUrl" />
                    </el-form-item>
                    <el-form-item label="在线模型名称">
                      <el-input v-model="systemStore.form.onlineModelName" />
                    </el-form-item>
                    <el-form-item label="在线 API Key（留空表示不修改）">
                      <el-input v-model="systemStore.form.onlineApiKey" type="password" show-password placeholder="可选" />
                    </el-form-item>
                    <el-form-item label="API Key 已配置">
                      <el-tag :type="runtimeConfig?.model.online.apiKeyConfigured ? 'success' : 'warning'">
                        {{ runtimeConfig?.model.online.apiKeyConfigured ? '已配置' : '未配置' }}
                      </el-tag>
                    </el-form-item>
                  </el-card>
                </div>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="参数配置" name="params">
              <el-form label-position="top" v-loading="loading">
                <div class="params-grid">
                  <el-card class="param-card" shadow="hover">
                    <template #header>生成参数</template>
                    <el-form-item>
                      <template #label>
                        <span class="label-with-help">
                          Temperature
                          <el-tooltip content="控制回答随机性：越大越发散；越小越稳定。常用 0.2~0.7。" placement="top" :show-after="150">
                            <el-icon class="help-icon"><InfoFilled /></el-icon>
                          </el-tooltip>
                        </span>
                      </template>
                      <el-input-number v-model="systemStore.form.temperature" :min="0" :max="2" :step="0.1" />
                    </el-form-item>
                    <el-form-item>
                      <template #label>
                        <span class="label-with-help">
                          Top P
                          <el-tooltip content="核采样阈值：越小越保守，越大越多样。通常与 Temperature 配合使用（0.7~0.95 常见）。" placement="top" :show-after="150">
                            <el-icon class="help-icon"><InfoFilled /></el-icon>
                          </el-tooltip>
                        </span>
                      </template>
                      <el-input-number v-model="systemStore.form.topP" :min="0" :max="1" :step="0.05" />
                    </el-form-item>
                    <el-form-item>
                      <template #label>
                        <span class="label-with-help">
                          Max Tokens
                          <el-tooltip content="限制单次回答的最大输出长度。数值越大回答越长，但延迟/成本也更高。" placement="top" :show-after="150">
                            <el-icon class="help-icon"><InfoFilled /></el-icon>
                          </el-tooltip>
                        </span>
                      </template>
                      <el-input-number v-model="systemStore.form.maxTokens" :min="128" :max="8192" :step="128" />
                    </el-form-item>
                  </el-card>

                  <el-card class="param-card" shadow="hover">
                    <template #header>Embedding 参数</template>
                    <el-form-item>
                      <template #label>
                        <span class="label-with-help">
                          Embedding 模型路径
                          <el-tooltip content="sentence-transformers 模型目录（本地路径）。修改后需要对已上传文档执行重建索引。" placement="top" :show-after="150">
                            <el-icon class="help-icon"><InfoFilled /></el-icon>
                          </el-tooltip>
                        </span>
                      </template>
                      <div class="model-path-row">
                        <div class="model-path-input-wrap">
                          <el-input
                            v-model="systemStore.form.embeddingModelPath"
                            class="model-path-input"
                          >
                            <template #suffix>
                              <el-tooltip content="浏览目录" placement="top" :show-after="200">
                                <el-icon
                                  class="model-path-browse-icon"
                                  @click="systemStore.openBrowse()"
                                >
                                  <FolderOpened />
                                </el-icon>
                              </el-tooltip>
                            </template>
                          </el-input>
                        </div>
                        <el-button
                          :loading="systemStore.modelVerifyStatus === 'loading'"
                          title="验证模型"
                          @click="systemStore.verifyModel(systemStore.form.embeddingModelPath)"
                        >验证</el-button>
                        <el-tag
                          v-if="systemStore.modelVerifyStatus === 'success'"
                          type="success"
                          size="large"
                          effect="dark"
                        >✓</el-tag>
                        <el-tag
                          v-else-if="systemStore.modelVerifyStatus === 'error'"
                          type="danger"
                          size="default"
                          effect="dark"
                        >✗ {{ systemStore.modelVerifyMessage?.slice(0, 40) || '验证失败' }}</el-tag>
                      </div>
                    </el-form-item>
                    <el-form-item>
                      <template #label>
                        <span class="label-with-help">
                          Embedding 设备
                          <el-tooltip
                            content="Auto 自动选择性能最优设备（独显 > 集显 > MPS > LM Studio > Ollama > CPU）；LM Studio / Ollama 需先启动本地服务并加载嵌入模型。"
                            placement="top"
                            :show-after="150"
                          >
                            <el-icon class="help-icon"><InfoFilled /></el-icon>
                          </el-tooltip>
                        </span>
                      </template>
                      <div class="device-row">
                        <el-select
                          v-model="embeddingDeviceRef"
                          :loading="devicesLoading"
                          placeholder="请选择设备"
                          class="device-select"
                          popper-class="device-select-popper"
                        >
                          <el-option
                            v-for="d in devices"
                            :key="d.id"
                            :label="d.name"
                            :value="d.id"
                          >
                            <el-tooltip
                              :content="d.name"
                              placement="right"
                              :show-after="300"
                              :disabled="(d.name || '').length < 24"
                            >
                              <span class="device-option-label">{{ d.name }}</span>
                            </el-tooltip>
                          </el-option>
                        </el-select>
                        <el-tooltip content="重新检测当前可用 Embedding 设备" placement="top">
                          <el-button
                            :icon="RefreshRight"
                            :loading="devicesLoading"
                            @click="systemStore.refreshDevices()"
                            circle
                          />
                        </el-tooltip>
                      </div>
                    </el-form-item>
                    <el-form-item v-if="showLmstudioUrl">
                      <template #label>
                        <span class="label-with-help">
                          LM Studio 接口地址
                          <el-tooltip :content="`LM Studio 本地 API 服务地址，默认为 ${appDefaults.lmstudioUrl}`" placement="top" :show-after="150">
                            <el-icon class="help-icon"><InfoFilled /></el-icon>
                          </el-tooltip>
                        </span>
                      </template>
                      <el-input v-model="systemStore.form.lmstudioBaseUrl" placeholder="http://localhost:1234" />
                    </el-form-item>
                    <el-form-item>
                      <template #label>
                        <span class="label-with-help">
                          向量维度（dims）
                          <el-tooltip content="ES dense_vector 的维度。建议与模型维度一致（例如 bge-large-zh-v1.5 为 1024）。0 表示自动从模型读取。" placement="top" :show-after="150">
                            <el-icon class="help-icon"><InfoFilled /></el-icon>
                          </el-tooltip>
                        </span>
                      </template>
                      <el-input-number v-model="systemStore.form.embeddingDims" :min="0" :max="4096" :step="1" />
                    </el-form-item>
                    <el-form-item>
                      <template #label>
                        <span class="label-with-help">
                          Embedding Batch Size
                          <el-tooltip content="向量化批大小。越大通常越快，但更占内存/显存；CPU 环境建议 16~64。" placement="top" :show-after="150">
                            <el-icon class="help-icon"><InfoFilled /></el-icon>
                          </el-tooltip>
                        </span>
                      </template>
                      <el-input-number v-model="systemStore.form.embeddingBatchSize" :min="1" :max="256" :step="1" />
                    </el-form-item>
                    <el-form-item>
                      <template #label>
                        <span class="label-with-help">
                          Normalize Embeddings
                          <el-tooltip content="是否归一化向量。cosine 相似度检索推荐开启；关闭可用于某些模型/检索策略。" placement="top" :show-after="150">
                            <el-icon class="help-icon"><InfoFilled /></el-icon>
                          </el-tooltip>
                        </span>
                      </template>
                      <el-switch v-model="systemStore.form.embeddingNormalizeEmbeddings" />
                    </el-form-item>
                  </el-card>

                  <el-card class="param-card" shadow="hover">
                    <template #header>RAG 参数</template>
                    <el-form-item>
                      <template #label>
                        <span class="label-with-help">
                          Top K
                          <el-tooltip content="返回给模型的检索片段条数（最终命中数）。值越大召回越多，但上下文更长、生成更慢。" placement="top" :show-after="150">
                            <el-icon class="help-icon"><InfoFilled /></el-icon>
                          </el-tooltip>
                        </span>
                      </template>
                      <el-input-number v-model="systemStore.form.topK" :min="1" :max="20" />
                    </el-form-item>
                    <el-form-item>
                      <template #label>
                        <span class="label-with-help">
                          候选召回数 Top N
                          <el-tooltip content="kNN 的候选集合大小（num_candidates）。通常 Top N ≥ Top K；值越大召回更充分，但检索更慢。" placement="top" :show-after="150">
                            <el-icon class="help-icon"><InfoFilled /></el-icon>
                          </el-tooltip>
                        </span>
                      </template>
                      <el-input-number v-model="systemStore.form.topN" :min="1" :max="100" />
                    </el-form-item>
                    <el-form-item>
                      <template #label>
                        <span class="label-with-help">
                          相似度阈值
                          <el-tooltip content="过滤低相关片段。越高越严格（可能导致无命中），越低越宽松（可能引入噪声）。常用 0.4~0.7。" placement="top" :show-after="150">
                            <el-icon class="help-icon"><InfoFilled /></el-icon>
                          </el-tooltip>
                        </span>
                      </template>
                      <el-input-number v-model="systemStore.form.similarityThreshold" :min="0" :max="1" :step="0.05" />
                    </el-form-item>
                    <el-form-item>
                      <template #label>
                        <span class="label-with-help">
                          Chunk Size
                          <el-tooltip content="文档切块长度（字符数）。越大上下文更完整，但召回粒度变粗；越小粒度更细但块数更多。" placement="top" :show-after="150">
                            <el-icon class="help-icon"><InfoFilled /></el-icon>
                          </el-tooltip>
                        </span>
                      </template>
                      <el-input-number v-model="systemStore.form.chunkSize" :min="100" :max="2000" :step="50" />
                    </el-form-item>
                    <el-form-item>
                      <template #label>
                        <span class="label-with-help">
                          Chunk Overlap
                          <el-tooltip content="相邻切块的重叠长度（字符数），用于减少切分断句带来的信息丢失。一般为 Chunk Size 的 10%~30%。" placement="top" :show-after="150">
                            <el-icon class="help-icon"><InfoFilled /></el-icon>
                          </el-tooltip>
                        </span>
                      </template>
                      <el-input-number v-model="systemStore.form.chunkOverlap" :min="0" :max="500" :step="10" />
                    </el-form-item>
                  </el-card>
                </div>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="服务状态" name="services">
              <div class="service-grid">
                <el-card class="service-card" shadow="hover">
                  <template #header>
                    <div class="panel-header-actions">
                      <span>服务连接状态</span>
                      <el-button size="small" @click="onRefreshHealth">刷新状态</el-button>
                    </div>
                  </template>
                  <el-table :data="healthItems" stripe>
                    <el-table-column prop="name" label="服务名称" min-width="140" />
                    <el-table-column label="状态" min-width="100">
                      <template #default="{ row }">
                        <el-tag :type="row.status === 'UP' ? 'success' : row.status === 'DOWN' ? 'danger' : 'info'">
                          {{ row.status }}
                        </el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column prop="endpoint" label="地址" min-width="280" />
                    <el-table-column prop="description" label="说明" min-width="220" />
                  </el-table>
                </el-card>
                <el-card class="service-card" shadow="hover">
                  <template #header>部署说明</template>
                  <el-descriptions :column="1" border>
                    <el-descriptions-item label="离线模型接口">
                      {{ runtimeConfig?.service.ollama.baseUrl }}{{ runtimeConfig?.service.ollama.chatPath }}
                    </el-descriptions-item>
                    <el-descriptions-item label="Python RAG 服务">
                      {{ runtimeConfig?.service.pythonRagBaseUrl }}
                    </el-descriptions-item>
                    <el-descriptions-item label="嵌入设备模式">
                      {{ runtimeConfig?.rag.embedding.device }}
                    </el-descriptions-item>
                    <el-descriptions-item label="部署策略">
                      {{ runtimeConfig?.app.deploymentMode }}
                    </el-descriptions-item>
                  </el-descriptions>
                </el-card>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </section>

    <el-dialog
      v-model="systemStore.browseVisible"
      title="选择模型目录"
      width="600px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div class="browse-path-bar">
        <el-button
          :disabled="!systemStore.browseParentPath"
          :icon="FolderOpened"
          @click="systemStore.loadBrowseDirectory(systemStore.browseParentPath!)"
          title="返回上级"
        >上级目录</el-button>
        <el-input v-model="systemStore.browseCurrentPath" readonly />
      </div>
      <div v-if="systemStore.browseError" class="browse-error">{{ systemStore.browseError }}</div>
      <div class="browse-list" v-loading="systemStore.browseLoading">
        <div
          v-for="dir in systemStore.browseDirectories"
          :key="dir.path"
          class="browse-item"
          @dblclick="systemStore.selectBrowsePath(dir.path)"
        >
          <el-button
            type="primary"
            link
            class="browse-item-link"
            @click="systemStore.loadBrowseDirectory(dir.path)"
          >{{ dir.name }}</el-button>
          <el-button
            size="small"
            type="success"
            plain
            @click.stop="systemStore.selectBrowsePath(dir.path)"
          >选择</el-button>
        </div>
        <el-empty v-if="systemStore.browseDirectories.length === 0 && !systemStore.browseLoading" description="该目录下无子目录" />
      </div>
      <template #footer>
        <el-input
          v-model="systemStore.form.embeddingModelPath"
          placeholder="或直接输入完整路径"
          style="margin-bottom: 8px"
        />
        <el-button @click="systemStore.browseVisible = false">取消</el-button>
        <el-button type="primary" @click="systemStore.selectBrowsePath(systemStore.form.embeddingModelPath)">使用此路径</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.label-with-help {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.help-icon {
  color: #6b7280;
}

.inline-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.offline-model-select {
  flex: 1;
  min-width: 240px;
  width: 100%;
}

.model-option-row {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  min-height: 28px;
}

.model-provider-logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  flex-shrink: 0;
  line-height: 1;
}

.model-option-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.model-capability-icons {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.cap-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 4px;
}

.vision-icon { color: #10b981; background: #ecfdf5; }
.reasoning-icon { color: #8b5cf6; background: #f5f3ff; }
.tool-icon { color: #f59e0b; background: #fffbeb; }

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-header h1 {
  margin: 0 0 8px;
  font-size: 28px;
}

.page-header p {
  margin: 0;
  color: #6b7280;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.summary-card,
.panel-card {
  border-radius: 16px;
}

.summary-value {
  font-size: 20px;
  font-weight: 700;
  color: #0f2744;
}

.summary-value.compact {
  font-size: 14px;
  line-height: 1.6;
  word-break: break-all;
}

.summary-desc {
  margin-top: 8px;
  color: #6b7280;
  font-size: 13px;
}

.tab-section {
  margin-bottom: 16px;
}

.model-cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-top: 16px;
}

.model-card {
  border-radius: 12px;
}

.model-card-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.params-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.param-card {
  border-radius: 12px;
}

.service-grid {
  display: grid;
  grid-template-columns: 1.3fr 0.7fr;
  gap: 16px;
}

.service-card {
  border-radius: 12px;
}

.panel-header-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.service-controls {
  display: flex;
  gap: 8px;
  justify-content: space-between;
}

.model-path-row {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.model-path-row .el-tag {
  white-space: nowrap;
  font-weight: 600;
}

.model-path-input-wrap {
  flex: 1;
  min-width: 0;
}

.model-path-input :deep(.el-input__wrapper) {
  padding-right: 4px;
}

.model-path-browse-icon {
  cursor: pointer;
  font-size: 16px;
  color: var(--el-text-color-secondary);
  padding: 4px;
  border-radius: 4px;
  transition: opacity 0.2s ease, background-color 0.15s ease, color 0.15s ease;
  opacity: 0;
  pointer-events: none;
}

.model-path-input :deep(.el-input__wrapper):hover .model-path-browse-icon,
.model-path-input :deep(.el-input__wrapper.is-focus) .model-path-browse-icon {
  opacity: 1;
  pointer-events: auto;
}

.model-path-browse-icon:hover {
  color: var(--el-color-primary);
  background-color: var(--el-fill-color);
}

.device-row {
  display: flex;
  gap: 12px;
  align-items: center;
  width: 100%;
}

.device-select {
  flex: 1;
  min-width: 0;
}

.device-select :deep(.el-select__wrapper) {
  width: 100%;
}

.device-option-label {
  display: block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.browse-path-bar {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 12px;
}

.browse-error {
  color: var(--el-color-danger);
  padding: 8px 0;
  font-size: 13px;
}

.browse-list {
  max-height: 320px;
  overflow-y: auto;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  padding: 8px;
}

.browse-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.15s;
}

.browse-item:hover {
  background: var(--el-fill-color-light);
}

.browse-item-link {
  font-size: 14px;
}
</style>

<style>
/* 非 scoped：el-select 的 popper 被 teleport 到 body，scoped 无法命中 */
.device-select-popper {
  min-width: 240px !important;
  max-width: 520px !important;
}

.device-select-popper .el-select-dropdown__item {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  padding-right: 16px;
}

.offline-model-popper {
  min-width: 320px !important;
  max-width: 560px !important;
}

.offline-model-popper .el-select-dropdown__item {
  height: auto;
  padding: 6px 12px;
}
</style>
