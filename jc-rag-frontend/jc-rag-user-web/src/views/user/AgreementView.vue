<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MarkdownIt from 'markdown-it'
import { fetchPublishedLegalDoc, type LegalDocType, type LegalDocVO } from '../../api/legal-doc'

const route = useRoute()
const router = useRouter()

const doc = ref<LegalDocVO | null>(null)
const loading = ref(false)
const error = ref<string>('')

const md = new MarkdownIt({ html: false, linkify: true, breaks: true })
const renderedHtml = computed(() => (doc.value?.contentMd ? md.render(doc.value.contentMd) : ''))

const docType = computed<LegalDocType>(() => {
  const t = (route.query.type as string) || 'USER_AGREEMENT'
  return t === 'PRIVACY_POLICY' ? 'PRIVACY_POLICY' : 'USER_AGREEMENT'
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    doc.value = await fetchPublishedLegalDoc(docType.value)
    if (!doc.value) error.value = '暂未发布该协议'
  } catch (e: any) {
    error.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

watch(docType, load)
onMounted(load)

function switchTo(t: LegalDocType) {
  router.replace({ path: '/agreement', query: { type: t } })
}

function back() {
  if (window.history.length > 1) router.back()
  else router.push('/login')
}
</script>

<template>
  <div class="agreement-wrapper">
    <div class="agreement-card">
      <div class="agreement-header">
        <el-button link @click="back">返回</el-button>
        <div class="agreement-tabs">
          <span :class="{ active: docType === 'USER_AGREEMENT' }" @click="switchTo('USER_AGREEMENT')">用户协议</span>
          <span :class="{ active: docType === 'PRIVACY_POLICY' }" @click="switchTo('PRIVACY_POLICY')">隐私政策</span>
        </div>
      </div>

      <div v-if="loading" class="agreement-loading">加载中...</div>
      <div v-else-if="error" class="agreement-error">{{ error }}</div>
      <article v-else class="agreement-body">
        <h1>{{ doc?.title }}</h1>
        <div v-if="doc?.publishedAt" class="agreement-meta">发布于 {{ doc.publishedAt }}</div>
        <div class="agreement-content markdown-body" v-html="renderedHtml"></div>
      </article>
    </div>
  </div>
</template>

<style scoped>
.agreement-wrapper {
  min-height: 100vh;
  background: linear-gradient(135deg, #0f2744 0%, #1a4a7a 100%);
  padding: 40px 20px;
}
.agreement-card {
  max-width: 820px;
  margin: 0 auto;
  background: #fff;
  border-radius: 12px;
  padding: 24px 32px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.12);
}
.agreement-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e5e7eb;
  padding-bottom: 12px;
  margin-bottom: 16px;
}
.agreement-tabs span {
  margin-left: 16px;
  font-size: 14px;
  color: #6b7280;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
}
.agreement-tabs span.active {
  color: #409eff;
  background: #ecf5ff;
  font-weight: 600;
}
.agreement-body h1 {
  font-size: 22px;
  margin: 8px 0 4px;
}
.agreement-meta {
  color: #9ca3af;
  font-size: 12px;
  margin-bottom: 16px;
}
.agreement-content {
  line-height: 1.8;
  color: #1f2937;
  font-size: 14px;
}
.agreement-content :deep(h1),
.agreement-content :deep(h2),
.agreement-content :deep(h3) {
  margin-top: 16px;
  margin-bottom: 8px;
  font-weight: 600;
}
.agreement-content :deep(p) {
  margin: 8px 0;
}
.agreement-content :deep(ul),
.agreement-content :deep(ol) {
  margin: 8px 0;
  padding-left: 24px;
}
.agreement-content :deep(code) {
  background: #f3f4f6;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
}
.agreement-loading,
.agreement-error {
  text-align: center;
  padding: 60px 0;
  color: #6b7280;
}
</style>
