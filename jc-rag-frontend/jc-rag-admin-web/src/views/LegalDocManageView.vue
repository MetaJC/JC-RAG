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
/**
 * 用户协议 / 隐私政策 管理 (A 端管理后台 -> 用户设置)。
 * <p>支持 Markdown 编辑, 上传 word/markdown 自动解析, 草稿/发布, 实时预览。
 *
 * @author JC
 */
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Upload, View } from '@element-plus/icons-vue'
import { marked } from 'marked'

import {
  getLegalDoc,
  listLegalDocs,
  publishLegalDoc,
  saveLegalDoc,
  uploadLegalDocFile,
  type LegalDocStatus,
  type LegalDocType,
  type LegalDocVO,
} from '../api/sys'

const DOC_TYPES: { key: LegalDocType; title: string; defaultTitle: string }[] = [
  { key: 'USER_AGREEMENT', title: '用户协议', defaultTitle: '用户协议' },
  { key: 'PRIVACY_POLICY', title: '隐私政策', defaultTitle: '隐私政策' },
]

const activeType = ref<LegalDocType>('USER_AGREEMENT')
const form = reactive<{ title: string; contentMd: string; status: LegalDocStatus }>({
  title: '',
  contentMd: '',
  status: 'DRAFT',
})
const current = ref<LegalDocVO | null>(null)
const loading = ref(false)
const saving = ref(false)
const publishing = ref(false)
const previewVisible = ref(false)

const previewHtml = computed(() => marked.parse(form.contentMd || '', { breaks: true }))

async function load(type: LegalDocType) {
  loading.value = true
  try {
    const data = await getLegalDoc(type)
    current.value = data
    form.title = data.title || DOC_TYPES.find((d) => d.key === type)?.defaultTitle || ''
    form.contentMd = data.contentMd || ''
    form.status = (data.status as LegalDocStatus) || 'DRAFT'
  } catch (e: any) {
    ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function save(status: LegalDocStatus) {
  if (!form.title.trim()) {
    ElMessage.warning('请填写标题'); return
  }
  saving.value = true
  try {
    await saveLegalDoc({
      docType: activeType.value,
      title: form.title.trim(),
      contentMd: form.contentMd,
      status,
    })
    form.status = status
    ElMessage.success(status === 'PUBLISHED' ? '已发布' : '草稿已保存')
    await load(activeType.value)
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function publish() {
  try {
    await ElMessageBox.confirm('发布后用户端会立即看到该协议内容, 是否继续?', '确认发布', {
      type: 'warning',
    })
  } catch {
    return
  }
  publishing.value = true
  try {
    await publishLegalDoc(activeType.value)
    form.status = 'PUBLISHED'
    ElMessage.success('发布成功')
    await load(activeType.value)
  } catch (e: any) {
    ElMessage.error(e?.message || '发布失败')
  } finally {
    publishing.value = false
  }
}

function onUploadChange(file: File) {
  const name = file.name.toLowerCase()
  if (!(name.endsWith('.md') || name.endsWith('.markdown') || name.endsWith('.docx') || name.endsWith('.txt'))) {
    ElMessage.warning('仅支持 .md / .markdown / .docx / .txt 文件')
    return
  }
  uploadLegalDocFile(file)
    .then((md) => {
      form.contentMd = md
      ElMessage.success('解析完成, 请检查内容后再保存')
    })
    .catch((e) => ElMessage.error(e?.message || '解析失败'))
}

const statusTag = computed(() => {
  if (form.status === 'PUBLISHED') return { label: '已发布', type: 'success' as const }
  return { label: '草稿', type: 'info' as const }
})

watch(activeType, (t) => load(t))
onMounted(async () => {
  // 触发第一次加载 + 预热列表
  await load(activeType.value)
  listLegalDocs().catch(() => {/* 静默 */})
})
</script>

<template>
  <div class="legal-doc-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">用户设置 / 协议管理</h2>
        <p class="page-desc">维护用户协议与隐私政策, 支持富文本 (Markdown) 编辑及 Word 上传解析</p>
      </div>
      <div class="header-right">
        <el-tag :type="statusTag.type" effect="dark">{{ statusTag.label }}</el-tag>
      </div>
    </div>

    <div class="type-tabs">
      <div
        v-for="t in DOC_TYPES"
        :key="t.key"
        class="type-tab"
        :class="{ active: activeType === t.key }"
        @click="activeType = t.key"
      >
        {{ t.title }}
      </div>
    </div>

    <el-card v-loading="loading" shadow="never" class="editor-card">
      <el-form label-position="top">
        <el-form-item label="协议标题">
          <el-input v-model="form.title" placeholder="请输入协议标题" maxlength="255" show-word-limit />
        </el-form-item>
        <el-form-item label="协议内容 (Markdown)">
          <div class="editor-toolbar">
            <el-upload
              :show-file-list="false"
              :auto-upload="true"
              :http-request="(opts: any) => onUploadChange(opts.file)"
              accept=".md,.markdown,.docx,.txt"
            >
              <el-button :icon="Upload">上传 Word / Markdown</el-button>
            </el-upload>
            <el-button :icon="View" @click="previewVisible = true">预览</el-button>
          </div>
          <el-input
            v-model="form.contentMd"
            type="textarea"
            :rows="20"
            placeholder="支持 Markdown 语法, 可直接粘贴或上传 .md / .docx 文件"
            resize="vertical"
            class="md-textarea"
          />
        </el-form-item>
        <el-form-item v-if="current?.publishedAt" label="最近发布">
          <span class="meta-text">{{ current.publishedAt }}</span>
        </el-form-item>
        <el-form-item>
          <el-button :icon="Document" :loading="saving" @click="save('DRAFT')">保存草稿</el-button>
          <el-button type="primary" :loading="publishing" @click="publish">立即发布</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-dialog v-model="previewVisible" :title="form.title || '预览'" width="760px" top="6vh">
      <div class="preview-body" v-html="previewHtml"></div>
    </el-dialog>
  </div>
</template>

<style scoped>
.legal-doc-page {
  padding: 0 4px;
}
.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 16px;
}
.page-title {
  margin: 0 0 4px;
  font-size: 20px;
  font-weight: 600;
}
.page-desc {
  margin: 0;
  color: #6b7280;
  font-size: 13px;
}
.type-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  border-bottom: 1px solid #e5e7eb;
}
.type-tab {
  padding: 8px 16px;
  cursor: pointer;
  font-size: 14px;
  color: #6b7280;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
}
.type-tab:hover {
  color: #409eff;
}
.type-tab.active {
  color: #409eff;
  border-bottom-color: #409eff;
  font-weight: 600;
}
.editor-card {
  border-radius: 8px;
}
.editor-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}
.md-textarea :deep(textarea) {
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 13px;
  line-height: 1.6;
}
.meta-text {
  color: #6b7280;
  font-size: 13px;
}
.preview-body {
  padding: 0 8px;
  line-height: 1.8;
  font-size: 14px;
  color: #1f2937;
  max-height: 70vh;
  overflow-y: auto;
}
.preview-body :deep(h1),
.preview-body :deep(h2),
.preview-body :deep(h3) {
  margin: 16px 0 8px;
  font-weight: 600;
}
.preview-body :deep(p) {
  margin: 8px 0;
}
.preview-body :deep(ul),
.preview-body :deep(ol) {
  margin: 8px 0;
  padding-left: 24px;
}
.preview-body :deep(code) {
  background: #f3f4f6;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
}
.preview-body :deep(pre) {
  background: #1f2937;
  color: #e5e7eb;
  padding: 12px 16px;
  border-radius: 6px;
  overflow-x: auto;
}
</style>
