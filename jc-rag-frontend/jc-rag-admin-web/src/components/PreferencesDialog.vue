<script setup lang="ts">
/**
 * 偏好设置 Dialog (齿轮 - 偏好设置)。
 * <p>左右布局: 左侧设置项导航, 右侧当前项内容。
 * <p>第一阶段仅实现: 记忆文件位置; 其他项预留, 点击不报错。
 *
 * @author JC
 */
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Bell,
  Brush,
  ChatLineSquare,
  Clock,
  Cpu,
  DataAnalysis,
  Delete,
  Folder,
  FolderOpened,
  InfoFilled,
  Memo,
  Operation,
  Reading,
  RefreshRight,
  Setting,
} from '@element-plus/icons-vue'

import { DEFAULT_MEMORY_FILE_PATH, type UserPreferenceUpdateParam } from '../api/preferences'
import { useUserPreferenceStore } from '../stores/preferences'

const props = defineProps<{ visible: boolean }>()
const emit = defineEmits<{ (e: 'update:visible', v: boolean): void }>()

const dialogVisible = computed({
  get: () => props.visible,
  set: (v) => emit('update:visible', v),
})

const store = useUserPreferenceStore()

/* ============== 左侧设置项 (含占位) ============== */

interface PreferenceItem {
  key: string
  label: string
  icon: unknown
  /** 留空表示尚未实现 */
  implemented?: boolean
  /** 仅做视觉提示 (true = 已实现但仅展示/后续阶段) */
  comingSoon?: boolean
}

const items: PreferenceItem[] = [
  { key: 'memory-file-path', label: '记忆文件位置', icon: Folder, implemented: true },
  { key: 'theme', label: '主题外观', icon: Brush, comingSoon: true },
  { key: 'language', label: '界面语言', icon: Memo, comingSoon: true },
  { key: 'default-model', label: '默认模型', icon: Cpu, comingSoon: true },
  { key: 'chat-style', label: '回答风格', icon: ChatLineSquare, comingSoon: true },
  { key: 'font-size', label: '字号与排版', icon: Reading, comingSoon: true },
  { key: 'shortcut', label: '快捷键', icon: Operation, comingSoon: true },
  { key: 'notification', label: '消息通知', icon: Bell, comingSoon: true },
  { key: 'history-retention', label: '历史保留时长', icon: Clock, comingSoon: true },
  { key: 'usage-stats', label: '使用统计与报告', icon: DataAnalysis, comingSoon: true },
]

const activeKey = ref('memory-file-path')
const currentItem = computed(() => items.find((i) => i.key === activeKey.value))

function selectItem(key: string) {
  activeKey.value = key
}

/* ============== 记忆文件位置 (已实现) ============== */

const form = reactive({
  memoryFilePath: '',
})
const saving = ref(false)

async function loadForm() {
  await store.load()
  form.memoryFilePath = store.memoryFilePath
}

watch(
  () => props.visible,
  (v) => {
    if (v) loadForm()
  },
  { immediate: true }
)

async function onSaveMemoryPath() {
  if (!form.memoryFilePath || !form.memoryFilePath.trim()) {
    ElMessage.warning('路径不能为空')
    return
  }
  saving.value = true
  try {
    const payload: UserPreferenceUpdateParam = { memoryFilePath: form.memoryFilePath.trim() }
    const vo = await store.save(payload)
    if (vo?.memoryFilePath) form.memoryFilePath = vo.memoryFilePath
    ElMessage.success('已保存')
  } catch (e) {
    console.error(e)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function onResetMemoryPath() {
  try {
    await ElMessageBox.confirm(
      `将恢复为默认路径 ${DEFAULT_MEMORY_FILE_PATH}, 是否继续?`,
      '恢复默认',
      { confirmButtonText: '恢复', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  saving.value = true
  try {
    const vo = await store.resetMemoryFilePath()
    if (vo?.memoryFilePath) form.memoryFilePath = vo.memoryFilePath
    ElMessage.success('已恢复默认')
  } catch (e) {
    console.error(e)
    ElMessage.error('恢复失败')
  } finally {
    saving.value = false
  }
}

function onOpenFolder() {
  ElMessage.info('该能力依赖桌面端能力, 当前为 Web 端, 请手动打开上述路径')
}

/* ============== 关闭 ============== */

function onClose() {
  dialogVisible.value = false
}
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    title="偏好设置"
    width="760px"
    destroy-on-close
    append-to-body
    @closed="activeKey = 'memory-file-path'"
  >
    <div class="pref-layout">
      <!-- 左侧: 设置项导航 -->
      <aside class="pref-aside">
        <el-menu :default-active="activeKey" @select="selectItem">
          <el-menu-item v-for="it in items" :key="it.key" :index="it.key">
            <el-icon><component :is="it.icon" /></el-icon>
            <span>{{ it.label }}</span>
            <el-tag
              v-if="!it.implemented"
              size="small"
              type="info"
              effect="plain"
              class="pref-tag"
            >敬请期待</el-tag>
          </el-menu-item>
        </el-menu>
      </aside>

      <!-- 右侧: 当前项内容 -->
      <section class="pref-content">
        <header class="pref-header">
          <h3>{{ currentItem?.label }}</h3>
          <p class="pref-desc" v-if="currentItem?.key === 'memory-file-path'">
            你的长期记忆 (关键事实/偏好) 将保存在此 Markdown 文件中, 可用任意编辑器查看与编辑。
          </p>
          <p class="pref-desc" v-else>
            <el-icon><InfoFilled /></el-icon>
            该偏好项正在规划中, 后续版本开放。
          </p>
        </header>

        <!-- 仅记忆文件位置已实现 -->
        <el-form v-if="currentItem?.key === 'memory-file-path'" label-position="top">
          <el-form-item label="记忆文件路径 (留空将使用默认)">
            <el-input
              v-model="form.memoryFilePath"
              :placeholder="DEFAULT_MEMORY_FILE_PATH"
              clearable
            >
              <template #prefix>
                <el-icon><Folder /></el-icon>
              </template>
              <template #append>
                <el-button :icon="FolderOpened" @click="onOpenFolder">打开目录</el-button>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-text type="info" size="small">
              默认位置: <code>{{ DEFAULT_MEMORY_FILE_PATH }}</code><br />
              支持 <code>~</code> 代表用户主目录。系统会自动创建中间目录。
            </el-text>
          </el-form-item>
        </el-form>

        <el-empty
          v-else
          :image-size="80"
          description="该偏好项尚未实现, 后续版本开放"
        />
      </section>
    </div>

    <template #footer>
      <template v-if="currentItem?.key === 'memory-file-path'">
        <el-button :icon="RefreshRight" @click="onResetMemoryPath">恢复默认</el-button>
        <el-button :icon="Delete" @click="onClose">取消</el-button>
        <el-button type="primary" :icon="Setting" :loading="saving" @click="onSaveMemoryPath">
          保存
        </el-button>
      </template>
      <template v-else>
        <el-button type="primary" @click="onClose">关闭</el-button>
      </template>
    </template>
  </el-dialog>
</template>

<style scoped>
.pref-layout {
  display: flex;
  gap: 16px;
  min-height: 420px;
}
.pref-aside {
  width: 200px;
  flex: 0 0 200px;
  border-right: 1px solid var(--el-border-color-lighter);
  padding-right: 8px;
}
.pref-content {
  flex: 1;
  min-width: 0;
  padding-left: 8px;
}
.pref-header h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
}
.pref-desc {
  margin: 0 0 20px 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.pref-tag-soon {
  margin-left: auto;
  font-size: 10px;
  height: 18px;
  line-height: 18px;
  padding: 0 6px;
}
:deep(.el-menu-item) {
  display: flex;
  align-items: center;
  gap: 6px;
}
</style>
