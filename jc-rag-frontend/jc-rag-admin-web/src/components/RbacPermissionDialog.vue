<script setup lang="ts">
/**
 * 权限/菜单编辑 Dialog (MENU/CATALOG/BUTTON/API 共用)
 * <p>RBAC 拆 4 页面之后的共享组件, 由 RbacMenuView/RbacPermissionView 引用
 * <p>本组件只负责表单状态 + UI, 提交由父组件处理 (便于在父组件做"深度校验"等业务判断)
 *
 * @author JC
 */
import { computed, reactive, ref, watch } from 'vue'
import { QuestionFilled, Search } from '@element-plus/icons-vue'
import * as ElIcons from '@element-plus/icons-vue'
import { POPULAR_ICONS, iconZh } from '../utils/iconMap'

type PermType = 'CATALOG' | 'MENU' | 'BUTTON' | 'API'

const props = defineProps<{
  visible: boolean
  /** 编辑时传, 新建时为 null */
  editingId?: number | null
  /** 创建模式: 决定初始 permType, 也决定父级/路径/组件等区段显隐 */
  mode: PermType
  /** 上级菜单下拉数据 [{id, title, depth}] */
  flatMenuOptions: { id: number; title: string; depth: number }[]
  /** 预填的父级 id, 用于"在某个节点下新增下级" */
  initialParentId?: number | null
  /** 初始 permCode (用于编辑回显) */
  initialPermCode?: string
  /** 初始 permName */
  initialPermName?: string
  /** 初始 path */
  initialPath?: string
  /** 初始 method (API 类型用) */
  initialMethod?: string
  /** 初始 icon */
  initialIcon?: string
  /** 初始 sort */
  initialSort?: number
  /** 初始 component (MENU 类型用) */
  initialComponent?: string
  /** 初始 query (MENU 类型用) */
  initialQuery?: string
  /** 初始 permType (编辑时从后端拿) */
  initialPermType?: PermType
  /** 初始 isExternal / keepAlive / visible / status */
  initialIsExternal?: number
  initialKeepAlive?: number
  initialVisible?: number
  initialStatus?: number
}>()

const emit = defineEmits<{
  'update:visible': [v: boolean]
  submit: [form: any]
}>()

const form = reactive({
  permCode: '',
  permName: '',
  permType: 'MENU' as PermType,
  parentId: null as number | null,
  path: '',
  method: '',
  icon: '',
  sort: 0,
  component: '',
  query: '',
  isExternal: 0 as number,
  keepAlive: 0 as number,
  visible: 1 as number,
  status: 1 as number,
})

const isExternalModel = computed({
  get: () => form.isExternal,
  set: (v: number) => (form.isExternal = v),
})
const keepAliveModel = computed({
  get: () => form.keepAlive,
  set: (v: number) => (form.keepAlive = v),
})
const visibleModel = computed({
  get: () => form.visible,
  set: (v: number) => (form.visible = v),
})
const statusModel = computed({
  get: () => form.status,
  set: (v: number) => (form.status = v),
})

const showMethod = computed(() => form.permType === 'API')
const showParent = computed(
  () => form.permType === 'CATALOG' || form.permType === 'MENU'
)
const showType = computed(() => form.permType !== 'API')
const showIconSort = computed(
  () => form.permType === 'CATALOG' || form.permType === 'MENU'
)
const showNameRouter = computed(
  () => form.permType === 'CATALOG' || form.permType === 'MENU'
)
const showExternalPath = computed(() => form.permType === 'MENU')
const showComponent = computed(() => form.permType === 'MENU')
const showQueryCache = computed(() => form.permType === 'MENU')
const showVisible = computed(
  () => form.permType === 'CATALOG' || form.permType === 'MENU'
)

// ============ 图标选择器 ============
const allIconNames = computed(() =>
  Object.keys(ElIcons)
    .filter((k) => /^[A-Z]/.test(k) && k !== 'default')
    .sort()
)
const iconKeyword = ref('')
/** 标签页: popular=常用, all=全部 */
const iconTab = ref<'popular' | 'all'>('popular')
/** 图标选择 popover 显隐 */
const iconPickerVisible = ref(false)

const popularIcons = computed(() =>
  POPULAR_ICONS.filter((n) => ElIcons[n as keyof typeof ElIcons])
)
/** 搜索命中的图标 (支持中英文 + 组件名), 跨"常用 / 全部"两个 tab */
const searchHits = computed(() => {
  const kw = iconKeyword.value.trim().toLowerCase()
  if (!kw) return [] as string[]
  return allIconNames.value.filter((n) => {
    if (n.toLowerCase().includes(kw)) return true
    const zh = iconZh(n)
    if (zh && zh.toLowerCase().includes(kw)) return true
    return false
  })
})
/** 当前 tab 要展示的图标列表 */
const visibleIcons = computed(() => {
  if (iconKeyword.value.trim()) return searchHits.value
  return iconTab.value === 'popular' ? popularIcons.value : allIconNames.value
})
function pickIcon(name: string) {
  form.icon = name
  iconPickerVisible.value = false
  iconKeyword.value = ''
  iconTab.value = 'popular'
}
function clearIcon() {
  form.icon = ''
}

function reset() {
  form.permCode = ''
  form.permName = ''
  form.permType = props.mode
  form.parentId = null
  form.path = ''
  form.method = ''
  form.icon = ''
  form.sort = 0
  form.component = ''
  form.query = ''
  form.isExternal = 0
  form.keepAlive = 0
  form.visible = 1
  form.status = 1
  iconKeyword.value = ''
  iconTab.value = 'popular'
  iconPickerVisible.value = false
}

watch(
  () => props.visible,
  (v) => {
    if (!v) return
    if (props.editingId) {
      // 编辑: 回显
      form.permCode = props.initialPermCode || ''
      form.permName = props.initialPermName || ''
      form.permType = (props.initialPermType || props.mode) as PermType
      form.parentId = props.initialParentId ?? null
      form.path = props.initialPath || ''
      form.method = props.initialMethod || ''
      form.icon = props.initialIcon || ''
      form.sort = props.initialSort ?? 0
      form.component = props.initialComponent || ''
      form.query = props.initialQuery || ''
      form.isExternal = props.initialIsExternal ?? 0
      form.keepAlive = props.initialKeepAlive ?? 0
      form.visible = props.initialVisible ?? 1
      form.status = props.initialStatus ?? 1
    } else {
      // 新建
      reset()
      if (props.initialParentId) form.parentId = props.initialParentId
    }
  }
)

function close() {
  emit('update:visible', false)
}

function onSubmit() {
  emit('submit', { ...form })
}

const title = computed(() => (props.editingId ? '编辑菜单' : '添加菜单'))
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="title"
    width="760px"
    destroy-on-close
    align-center
    @update:model-value="emit('update:visible', $event)"
    @close="close"
  >
    <el-form label-position="left" label-width="120px" class="perm-form">
      <el-form-item v-if="showParent">
        <template #label>
          <span class="label-cell-star"></span>
          <span class="label-cell-text">上级菜单</span>
          <span class="label-cell-help"></span>
        </template>
        <el-select v-model="form.parentId" clearable placeholder="不选则为顶级菜单" style="width: 100%">
          <el-option label="(顶级菜单)" :value="null" />
          <el-option
            v-for="m in flatMenuOptions"
            :key="m.id"
            :label="'— '.repeat(m.depth) + m.title"
            :value="m.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item v-if="showType">
        <template #label>
          <span class="label-cell-star"></span>
          <span class="label-cell-text">菜单类型</span>
          <span class="label-cell-help"></span>
        </template>
        <el-radio-group v-model="form.permType">
          <el-radio value="CATALOG">目录</el-radio>
          <el-radio value="MENU">菜单</el-radio>
          <el-radio value="BUTTON">按钮</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-row v-if="showIconSort" :gutter="20">
        <el-col :span="12">
          <el-form-item>
            <template #label>
              <span class="label-cell-star"></span>
              <span class="label-cell-text">菜单图标</span>
              <span class="label-cell-help"></span>
            </template>
            <el-popover
              v-model:visible="iconPickerVisible"
              placement="bottom-start"
              :width="460"
              trigger="manual"
              popper-class="icon-picker-pop"
            >
              <template #reference>
                <!-- 包裹层: 阻止 dialog 打开时 el-input 自动 focus 误触 popover -->
                <div class="icon-picker-trigger" @click.stop="iconPickerVisible = !iconPickerVisible">
                  <el-input
                    :model-value="form.icon"
                    placeholder="点击选择图标"
                    clearable
                    @clear="clearIcon"
                    readonly
                    style="cursor: pointer"
                  >
                    <template #prefix>
                      <el-icon v-if="form.icon" :size="16"><component :is="form.icon" /></el-icon>
                      <el-icon v-else :size="16" color="#c0c4cc"><Search /></el-icon>
                    </template>
                  </el-input>
                </div>
              </template>
              <div class="icon-picker">
                <el-input
                  v-model="iconKeyword"
                  placeholder="搜索图标 (中文 / 英文 / 组件名, 如 用户 / User)"
                  clearable
                  size="small"
                  style="margin-bottom: 8px"
                >
                  <template #prefix><el-icon><Search /></el-icon></template>
                </el-input>
                <el-tabs v-model="iconTab" v-if="!iconKeyword.trim()" class="icon-picker-tabs">
                  <el-tab-pane label="常用" name="popular" />
                  <el-tab-pane :label="`全部 (${allIconNames.length})`" name="all" />
                </el-tabs>
                <div class="icon-picker-meta">
                  <template v-if="iconKeyword.trim()">
                    搜索 "{{ iconKeyword }}" · 命中 {{ searchHits.length }} 个
                  </template>
                  <template v-else>
                    共 {{ visibleIcons.length }} 个图标
                    <span v-if="iconTab === 'popular'" class="icon-picker-meta-tip">置顶展示后台常用图标</span>
                  </template>
                </div>
                <div class="icon-picker-grid">
                  <el-tooltip
                    v-for="name in visibleIcons"
                    :key="name"
                    :content="iconZh(name) ? `${iconZh(name)} (${name})` : name"
                    placement="top"
                  >
                    <div
                      class="icon-cell"
                      :class="{ active: form.icon === name }"
                      @click="pickIcon(name)"
                    >
                      <el-icon :size="20"><component :is="(ElIcons as any)[name]" /></el-icon>
                      <span class="icon-cell-name">{{ iconZh(name) || name }}</span>
                    </div>
                  </el-tooltip>
                  <div v-if="visibleIcons.length === 0" class="icon-picker-empty">
                    没有匹配 "{{ iconKeyword }}" 的图标
                  </div>
                </div>
              </div>
            </el-popover>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item>
            <template #label>
              <span class="label-cell-star">*</span>
              <span class="label-cell-text">显示排序</span>
              <span class="label-cell-help"></span>
            </template>
            <el-input-number
              v-model="form.sort"
              :min="0"
              :max="9999"
              controls-position="right"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row v-if="showNameRouter" :gutter="20">
        <el-col :span="12">
          <el-form-item>
            <template #label>
              <span class="label-cell-star">*</span>
              <span class="label-cell-text">菜单名称</span>
              <span class="label-cell-help"></span>
            </template>
            <el-input v-model="form.permName" placeholder="请输入菜单名称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item>
            <template #label>
              <span class="label-cell-star"></span>
              <span class="label-cell-text">路由名称</span>
              <el-tooltip content="前端 vue-router 的 name 字段, 与权限字符保持一致即可, 路由跳转时使用" placement="top">
                <span class="label-cell-help"><el-icon><QuestionFilled /></el-icon></span>
              </el-tooltip>
            </template>
            <el-input :model-value="form.permCode" placeholder="请输入路由名称" disabled />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row v-if="showExternalPath" :gutter="20">
        <el-col :span="12">
          <el-form-item>
            <template #label>
              <span class="label-cell-star"></span>
              <span class="label-cell-text">是否外链</span>
              <el-tooltip content="路由地址是否为外部链接; 选是时路由地址需以 http:// 或 https:// 开头, 点击会打开新窗口" placement="top">
                <span class="label-cell-help"><el-icon><QuestionFilled /></el-icon></span>
              </el-tooltip>
            </template>
            <el-radio-group v-model="isExternalModel">
              <el-radio :value="1">是</el-radio>
              <el-radio :value="0">否</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item>
            <template #label>
              <span class="label-cell-star">*</span>
              <span class="label-cell-text">路由地址</span>
              <el-tooltip content="前端访问路径, 如 /system/user; 访问时浏览器 URL 由此路径决定, 必须以 / 开头" placement="top">
                <span class="label-cell-help"><el-icon><QuestionFilled /></el-icon></span>
              </el-tooltip>
            </template>
            <el-input v-model="form.path" placeholder="请输入路由地址" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row v-if="showMethod" :gutter="20">
        <el-col :span="12">
          <el-form-item>
            <template #label>
              <span class="label-cell-star"></span>
              <span class="label-cell-text">请求方法</span>
              <span class="label-cell-help"></span>
            </template>
            <el-select v-model="form.method">
              <el-option label="GET" value="GET" />
              <el-option label="POST" value="POST" />
              <el-option label="PUT" value="PUT" />
              <el-option label="DELETE" value="DELETE" />
              <el-option label="PATCH" value="PATCH" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item>
            <template #label>
              <span class="label-cell-star">*</span>
              <span class="label-cell-text">请求路径</span>
              <span class="label-cell-help"></span>
            </template>
            <el-input v-model="form.path" placeholder="如 /api/sys/user" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col v-if="showComponent" :span="12">
          <el-form-item>
            <template #label>
              <span class="label-cell-star"></span>
              <span class="label-cell-text">组件路径</span>
              <el-tooltip content="前端 Vue 组件路径, 如 system/user/index; 用于前端动态加载页面组件, 目录类型可不填" placement="top">
                <span class="label-cell-help"><el-icon><QuestionFilled /></el-icon></span>
              </el-tooltip>
            </template>
            <el-input v-model="form.component" placeholder="请输入组件路径" />
          </el-form-item>
        </el-col>
        <el-col :span="showComponent ? 12 : 24">
          <el-form-item>
            <template #label>
              <span class="label-cell-star">*</span>
              <span class="label-cell-text">权限字符</span>
              <el-tooltip content="后端权限标识, 如 sys:rbac:manage; 后端 @PreAuthorize 注解按此校验, 是路由名称" placement="top">
                <span class="label-cell-help"><el-icon><QuestionFilled /></el-icon></span>
              </el-tooltip>
            </template>
            <el-input
              v-model="form.permCode"
              :disabled="Boolean(editingId)"
              placeholder="如 sys:rbac:manage"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row v-if="showQueryCache" :gutter="20">
        <el-col :span="12">
          <el-form-item>
            <template #label>
              <span class="label-cell-star"></span>
              <span class="label-cell-text">路由参数</span>
              <el-tooltip content="路由 query 参数, 格式为 key=value, 多个用 & 连接, 如 id=1&type=2; 用于路由跳转时携带额外参数" placement="top">
                <span class="label-cell-help"><el-icon><QuestionFilled /></el-icon></span>
              </el-tooltip>
            </template>
            <el-input v-model="form.query" placeholder="如 id=1&type=2" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item>
            <template #label>
              <span class="label-cell-star"></span>
              <span class="label-cell-text">是否缓存</span>
              <el-tooltip content="是否启用 keep-alive 缓存页面; 选缓存时切换路由不会刷新组件, 适合列表/详情等需要保留状态的页面" placement="top">
                <span class="label-cell-help"><el-icon><QuestionFilled /></el-icon></span>
              </el-tooltip>
            </template>
            <el-radio-group v-model="keepAliveModel">
              <el-radio :value="1">缓存</el-radio>
              <el-radio :value="0">不缓存</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col v-if="showVisible" :span="12">
          <el-form-item>
            <template #label>
              <span class="label-cell-star"></span>
              <span class="label-cell-text">显示状态</span>
              <span class="label-cell-help"></span>
            </template>
            <el-radio-group v-model="visibleModel">
              <el-radio :value="1">显示</el-radio>
              <el-radio :value="0">隐藏</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="showVisible ? 12 : 24">
          <el-form-item>
            <template #label>
              <span class="label-cell-star"></span>
              <span class="label-cell-text">菜单状态</span>
              <span class="label-cell-help"></span>
            </template>
            <el-radio-group v-model="statusModel">
              <el-radio :value="1">正常</el-radio>
              <el-radio :value="0">停用</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button @click="close">取 消</el-button>
      <el-button type="primary" @click="onSubmit">确 定</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.perm-form .el-form-item {
  margin-bottom: 18px;
}

/**
 * label 三段式布局: * | 文字 | 问号
 *
 * 设计:
 *   - 用 inline-flex (而非 grid) 兼容性更好
 *   - 容器 120px 宽, 高度 32px 与 input 同高
 *   - 三个 cell 用固定 flex-basis 分配宽度:
 *     * cell: 14px (固定, 无 * 时空 cell, 仍占位)
 *     text cell: 自适应 (1fr, 文字左对齐)
 *     ? cell: 18px (固定, 无 ? 时空 cell, 仍占位)
 *   - 所有 cell 内 content 水平和垂直居中
 *   - 所有 label 文字起点永远对齐 (从 * cell 之后开始)
 *   - 文字与 * / ? 之间有 4px gap (由 cell 固定宽度 + 自然间距)
 */
.perm-form .el-form-item__label {
  display: inline-flex !important;
  align-items: center !important;
  width: 120px !important;
  height: 32px !important;
  font-size: 14px !important;
  line-height: 1 !important;
  color: #374151 !important;
  white-space: nowrap !important;
  box-sizing: border-box !important;
  padding: 0 !important;
  margin: 0 !important;
  text-align: left !important;
  vertical-align: middle !important;
}

/* 第一格: * 标记, 固定 14px 宽, 居中
 * 用 padding-top: 2px 微微下推, 让 * 字符与文字水平中线对齐
 * (因为 * 字符在字体中天然偏高) */
.perm-form .label-cell-star {
  flex: 0 0 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #f56c6c;
  font-weight: 600;
  font-size: 14px;
  line-height: 1;
  height: 100%;
  text-align: center;
  padding-top: 2px;
}

/* 第二格: label 文字, 自适应内容宽度 (只比文字宽 2-3px)
 * 所有 label 文字都是 4 个汉字, 自然宽度一致 -> 文字起点永远对齐
 * padding: 0 3px 让文字与 * 和 ? 之间各留 3px 边距 */
.perm-form .label-cell-text {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  padding: 0 3px;
  color: #374151;
  font-weight: 500;
  font-size: 14px;
  line-height: 1.4;
  height: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 第三格: 问号图标, 固定 14px 宽, 居中
 * 宽度小一点 -> 问号与文字之间间距小 */
.perm-form .label-cell-help {
  flex: 0 0 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
  font-size: 14px;
  height: 100%;
  text-align: center;
}

/* 问号 icon 自身样式 */
.perm-form .label-cell-help .el-icon {
  font-size: 14px;
  cursor: help;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.icon-picker {
  width: 100%;
}

.icon-picker-trigger {
  cursor: pointer;
  display: block;
  width: 100%;
}

.icon-picker-meta {
  font-size: 12px;
  color: #9ca3af;
  margin-bottom: 8px;
}

.icon-picker-meta-tip {
  margin-left: 6px;
  color: #6b7280;
  font-size: 11px;
}

.icon-picker-tabs {
  margin-bottom: 8px;
}
.icon-picker-tabs :deep(.el-tabs__nav-wrap::after) {
  background: #e5e7eb;
}
.icon-picker-tabs :deep(.el-tabs__item) {
  font-size: 12px;
  padding: 0 8px;
  height: 28px;
  line-height: 28px;
}

.icon-picker-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 4px;
  max-height: 320px;
  overflow-y: auto;
  padding: 2px;
}

.icon-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 52px;
  border-radius: 4px;
  cursor: pointer;
  color: #4b5563;
  transition: background 0.15s, color 0.15s;
  padding: 4px 2px;
}

.icon-cell:hover {
  background: #eff6ff;
  color: #2563eb;
}

.icon-cell.active {
  background: #2563eb;
  color: #fff;
}

.icon-cell-name {
  font-size: 10px;
  margin-top: 2px;
  line-height: 1.2;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}

.icon-picker-empty {
  grid-column: 1 / -1;
  text-align: center;
  color: #9ca3af;
  font-size: 12px;
  padding: 16px 0;
}
</style>
