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
 * 记忆文件查看/编辑 弹窗 (独立组件, 不嵌套在 PreferencesDialog 内).
 * <p>由 PreferencesDialog 通过 v-model 控制显示.
 * <p>支持两种模式:
 * <ul>
 *   <li>view: 只读, 可切换到 edit</li>
 *   <li>edit: 可编辑, 调 PUT 写盘</li>
 * </ul>
 *
 * @author JC
 */
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Setting } from '@element-plus/icons-vue'
import { fetchMemoryFile, saveMemoryFile, type MemoryFileVO } from '../api/preference'

const props = defineProps<{
  modelValue: boolean
  /** 初始模式: view 只读, edit 可编辑 */
  mode?: 'view' | 'edit'
}>()
const emit = defineEmits<{ (e: 'update:modelValue', v: boolean): void }>()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const editorMode = ref<'view' | 'edit'>(props.mode || 'view')
watch(() => props.mode, (v) => { if (v) editorMode.value = v })

const editorContent = ref('')
const editorLoading = ref(false)
const editorSaving = ref(false)
const editorMeta = ref<MemoryFileVO | null>(null)
const editorDirty = ref(false)

/** 拉取记忆文件 (会自动初始化默认模板) */
async function loadMemoryFile(): Promise<MemoryFileVO | null> {
  editorLoading.value = true
  try {
    const vo = await fetchMemoryFile()
    editorMeta.value = vo
    editorContent.value = vo.content || ''
    editorDirty.value = false
    if (vo.autoInitialized) {
      ElMessage.info('已自动创建默认模板, 你可以直接编辑')
    }
    return vo
  } catch (e) {
    console.error('[memory-editor] load failed:', e)
    ElMessage.error('读取记忆文件失败')
    return null
  } finally {
    editorLoading.value = false
  }
}

// 弹窗打开时主动拉取
watch(dialogVisible, (v) => {
  if (v) {
    loadMemoryFile()
  } else {
    // 关闭时清空 dirty
    editorDirty.value = false
  }
})

/** 保存编辑 */
async function onSave() {
  if (!editorDirty.value) {
    ElMessage.info('内容未变更')
    return
  }
  editorSaving.value = true
  try {
    const vo = await saveMemoryFile(editorContent.value)
    editorMeta.value = vo
    editorDirty.value = false
    ElMessage.success(`已保存 (${vo.bytes ?? 0} 字节)`)
  } catch (e) {
    console.error(e)
    ElMessage.error('保存失败')
  } finally {
    editorSaving.value = false
  }
}

/** 关闭前确认 (有未保存修改) */
function onClose() {
  if (editorDirty.value && editorMode.value === 'edit') {
    ElMessageBox.confirm('有未保存的修改, 确定关闭?', '提示', {
      confirmButtonText: '关闭',
      cancelButtonText: '继续编辑',
      type: 'warning',
    })
      .then(() => {
        dialogVisible.value = false
        editorDirty.value = false
      })
      .catch(() => { /* 用户取消, 继续编辑 */ })
  } else {
    dialogVisible.value = false
  }
}

/** 切换到编辑模式 */
function switchToEdit() {
  editorMode.value = 'edit'
  editorDirty.value = false
}
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    :title="editorMode === 'view' ? '记忆文件 (只读)' : '记忆文件 (编辑)'"
    width="720px"
    destroy-on-close
    append-to-body
    :close-on-click-modal="false"
    @close="onClose"
  >
    <div class="editor-meta" v-if="editorMeta">
      <el-text type="info" size="small">
        <strong>原始路径:</strong> <code>{{ editorMeta.rawPath || '-' }}</code><br />
        <strong>磁盘路径:</strong> <code>{{ editorMeta.resolvedPath || '-' }}</code><br />
        <strong>大小:</strong> {{ editorMeta.bytes ?? 0 }} 字节
        <span v-if="editorMeta.lastModified"> · 最后修改: {{ editorMeta.lastModified }}</span>
        <span v-if="editorMeta.autoInitialized"> · <el-tag size="small" type="warning">自动初始化</el-tag></span>
      </el-text>
    </div>
    <el-input
      v-model="editorContent"
      type="textarea"
      :rows="20"
      :readonly="editorMode === 'view'"
      :placeholder="editorLoading ? '加载中...' : '空文件'"
      resize="vertical"
      style="margin-top: 12px; font-family: ui-monospace, SFMono-Regular, Consolas, monospace;"
      @input="editorDirty = true"
    />
    <template #footer>
      <template v-if="editorMode === 'view'">
        <el-button @click="onClose">关闭</el-button>
        <el-button type="primary" @click="switchToEdit">切换到编辑</el-button>
      </template>
      <template v-else>
        <el-button @click="onClose">取消</el-button>
        <el-button
          type="primary"
          :icon="Setting"
          :loading="editorSaving"
          :disabled="!editorDirty"
          @click="onSave"
        >保存到磁盘</el-button>
      </template>
    </template>
  </el-dialog>
</template>

<style scoped>
.editor-meta {
  padding: 8px 12px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
  line-height: 1.8;
}
</style>
