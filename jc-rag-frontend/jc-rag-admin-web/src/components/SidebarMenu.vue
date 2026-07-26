<script setup lang="ts">
/**
 * 侧边栏菜单项 (递归组件, 支持无限层级)
 * <p>由 MainLayout 渲染顶级节点, 本组件递归处理子节点
 * <p>折叠态下, 有一级子菜单的父项用 el-popover 弹出浮层, 无子菜单的项直接 go
 *
 * @author JC
 */
import { computed, ref, watch, type PropType } from 'vue'
import { useRoute } from 'vue-router'

export interface SidebarMenuItem {
  path: string
  label: string
  short: string
  /** Element Plus 图标组件名 (如 'User'); 空串=未设置 */
  icon: string
  depth: number
  children?: SidebarMenuItem[]
}

const props = defineProps({
  item: { type: Object as PropType<SidebarMenuItem>, required: true },
  collapsed: { type: Boolean, default: false },
  isActive: { type: Function as PropType<(p: string) => boolean>, required: true },
  openKeys: { type: Object as PropType<Set<string>>, required: true },
  toggle: { type: Function as PropType<(item: SidebarMenuItem) => void>, required: true },
  go: { type: Function as PropType<(path: string) => void>, required: true },
  /** 折叠态下, 当前打开 popover 的父菜单 path; null = 全部关闭 (用于互斥) */
  activePopover: { type: String as PropType<string | null>, default: null },
  /** 父组件回调, 通知切换 popover 显隐 */
  onPopoverOpen: { type: Function as PropType<(path: string | null) => void>, required: true },
})

const route = useRoute()

/** 当前菜单项是否在当前路由路径下 (含子项命中)
 * <p>用 computed 监听 route.path, props.item 变化, 自己递归检查
 * <p>不依赖 prop 传递, 不依赖父组件状态, 一定准确
 */
function ownsRoutePath(item: SidebarMenuItem, target: string): boolean {
  if (item.path === target) return true
  for (const c of item.children ?? []) {
    if (ownsRoutePath(c, target)) return true
  }
  return false
}

const isTopActive = computed(() => {
  if (props.item.depth !== 0) return false
  return ownsRoutePath(props.item, route.path)
})

/** 折叠态 + 有子菜单 = 走 popover 模式 */
const usePopover = computed(
  () => props.collapsed && !!props.item.children?.length,
)

/** 当前 popover 是否打开 (受父组件 activePopover 控制, 实现互斥) */
const popoverVisible = ref(false)
watch(
  () => props.activePopover,
  (val) => {
    popoverVisible.value = val === props.item.path
  },
  { immediate: true },
)

function onPopoverTriggerClick() {
  if (popoverVisible.value) {
    props.onPopoverOpen(null)
  } else {
    props.onPopoverOpen(props.item.path)
  }
}

function onPopoverItemClick(child: SidebarMenuItem) {
  props.onPopoverOpen(null)
  props.go(child.path)
}
</script>

<template>
  <div class="nav-block" :class="`depth-${Math.min(item.depth, 2)}`">
    <!-- 折叠态 + 有子菜单: el-popover 包裹, 点击右侧弹出浮层 -->
    <el-popover
      v-if="usePopover"
      v-model:visible="popoverVisible"
      placement="right-start"
      :width="200"
      trigger="manual"
      :show-arrow="false"
      popper-class="jc-sidebar-popover"
      :hide-after="0"
    >
      <template #reference>
        <button
          class="nav-item nav-top is-collapsed-parent"
          :class="{ active: isTopActive }"
          :title="item.label"
          @click="onPopoverTriggerClick"
        >
          <span class="nav-icon" :class="{ 'is-empty': !item.icon }">
            <el-icon v-if="item.icon" :size="16"><component :is="item.icon" /></el-icon>
          </span>
        </button>
      </template>
      <div class="popover-menu">
        <div class="popover-title">{{ item.label }}</div>
        <button
          v-for="child in item.children"
          :key="child.path"
          class="popover-item"
          :class="{ active: isActive(child.path) }"
          @click="onPopoverItemClick(child)"
        >
          <el-icon v-if="child.icon" :size="14" class="popover-item-icon"><component :is="child.icon" /></el-icon>
          <span class="popover-item-label">{{ child.label }}</span>
        </button>
      </div>
    </el-popover>

    <!-- 折叠态 + 无子菜单: 简单 button, 直接 go -->
    <button
      v-else-if="collapsed"
      class="nav-item"
      :class="[ item.depth === 0 ? 'nav-top' : { 'nav-sub': true, 'nav-sub-d1': item.depth === 1, 'nav-sub-d2': item.depth === 2 }, { active: item.depth === 0 ? isTopActive : isActive(item.path) } ]"
      :title="item.label"
      @click="go(item.path)"
    >
      <span class="nav-icon" :class="{ 'is-empty': !item.icon }">
        <el-icon v-if="item.icon" :size="16"><component :is="item.icon" /></el-icon>
      </span>
    </button>

    <!-- 展开态: 原有逻辑 (图标 + 文字 + 箭头) -->
    <button
      v-else
      class="nav-item"
      :class="[
        item.depth === 0
          ? 'nav-top'
          : { 'nav-sub': true, 'nav-sub-d1': item.depth === 1, 'nav-sub-d2': item.depth === 2 },
        { active: item.depth === 0 ? isTopActive : isActive(item.path), open: openKeys.has(item.path) },
      ]"
      :title="collapsed ? item.label : ''"
      @click="item.children?.length ? toggle(item) : go(item.path)"
    >
      <span class="nav-icon" :class="{ 'is-empty': !item.icon }">
        <el-icon v-if="item.icon" :size="16"><component :is="item.icon" /></el-icon>
      </span>
      <span class="nav-label">{{ item.label }}</span>
      <svg
        v-if="item.children?.length"
        class="nav-caret"
        :class="{ open: openKeys.has(item.path) }"
        width="14"
        height="14"
        viewBox="0 0 16 16"
        fill="none"
        stroke="currentColor"
        stroke-width="1.8"
        aria-hidden="true"
      >
        <path d="M5.5 3.5 L10.5 8 L5.5 12.5" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
    </button>

    <!-- 子菜单组: 展开态才显示 (折叠态下子菜单在 popover 内) -->
    <transition name="submenu">
      <div
        v-show="!collapsed && item.children?.length && openKeys.has(item.path)"
        class="sub-group"
      >
        <SidebarMenu
          v-for="child in item.children"
          :key="child.path"
          :item="child"
          :collapsed="collapsed"
          :is-active="isActive"
          :open-keys="openKeys"
          :toggle="toggle"
          :go="go"
          :active-popover="activePopover"
          :on-popover-open="onPopoverOpen"
        />
      </div>
    </transition>
  </div>
</template>

<script lang="ts">
// 显式声明 name 让递归组件能找到自身
export default { name: 'SidebarMenu' }
</script>

<style scoped>
/* =====================================================================
 * 侧边栏菜单样式 (重写)
 * <p>状态矩阵: 4 状态 × 3 类型 = 12 条规则
 * <p>状态: default / hover / active(当前路由) / open(已展开)
 * <p>类型: nav-top(顶级) / nav-sub.depth-1(二级) / nav-sub.depth-2(三级)
 *
 * 关键原则: 一级 (sidebar 深蓝上) 用白色高亮; 二三级 (子组黑底上) 用蓝色高亮
 * <p>active 优先级: active > open > hover > default
 * ===================================================================== */

/* ============ 整体布局 ============ */
.nav-block {
  display: flex;
  flex-direction: column;
  gap: 0;
}
.nav-block.depth-0 {
  gap: 2px;
}

/* ============ 子菜单组 (整块深色底, 4 个子项共享)
 * <p>左侧保持内缩 (margin-left 8px), 让它视觉上"挂在"一级菜单下
 * <p>右侧顶到 sidebar 边, 顶宽, 顶到头
 * <p>左边上/下圆角, 右边无圆角
 */
.sub-group {
  display: flex;
  flex-direction: column;
  margin: 4px 0 6px 8px;
  padding: 4px 0;
  background: rgba(0, 0, 0, 0.25);
  border-radius: 6px 0 0 6px;
  overflow: hidden;
}

/* ============ nav-item 通用基础 ============ */
.nav-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  border: none;
  background: transparent;
  text-align: left;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.85);
  font-size: 13px;
  transition: background-color 0.18s ease, color 0.18s ease, font-weight 0.18s ease;
  font-weight: 400;
}
.collapsed .nav-item {
  justify-content: center;
  padding-left: 0;
  padding-right: 0;
}
.collapsed .nav-icon {
  margin-right: 0;
}

/* ============ 图标固定宽占位 ============ */
.nav-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  margin-right: 8px;
  color: rgba(255, 255, 255, 0.75);
}
.nav-icon.is-empty {
  background: transparent;
}

.nav-label {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ============ caret (展开箭头) ============ */
.nav-caret {
  flex-shrink: 0;
  transition: transform 0.2s ease;
  opacity: 0.7;
}
.nav-caret.open {
  transform: rotate(90deg);
}

/* ============ 展开/折叠过渡动画 ============ */
/* <p>优化: 时长 0.35s, 丝滑缓动, 去掉渐隐 (仅 max-height), max-height 留够大
 * <p>不用 opacity: 你不要"渐隐"效果
 */
.submenu-enter-active,
.submenu-leave-active {
  transition: max-height 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}
.submenu-enter-from,
.submenu-leave-to {
  max-height: 0;
}
.submenu-enter-to,
.submenu-leave-from {
  max-height: 1000px;   /* 够大, 4~10 个子项都不会截断 */
}

/* =====================================================================
 * 一级菜单 (nav-top) — 在 sidebar 深蓝渐变上
 * <p>使用白色半透明底做"高亮", 跟深蓝底强对比
 * ===================================================================== */
.nav-top {
  font-size: 14px;
  padding: 10px 12px;
  border-radius: 0;  /* 顶到头, 无圆角 */
}

/* default: 透明 */
.nav-top { background-color: transparent; color: rgba(255, 255, 255, 0.85); }

/* hover: 极浅白 */
.nav-top:hover { background-color: rgba(255, 255, 255, 0.08); color: #fff; }

/* open: 透明 (caret 旋转已足够标识"展开了", 不再给背景, 避免切走时残留高亮) */
.nav-top.open,
.nav-top.open:hover {
  background-color: transparent;
  color: #fff;
}

/* active (含 active+open 组合): 蓝 800 (比 sidebar 深一档, 柔和) + 弱描边
 * <p>无圆角, 左右顶到头
 */
.nav-top.active,
.nav-top.active.open,
.nav-top.active:hover,
.nav-top.active.open:hover {
  background-color: #1e40af;
  color: #fff;
  font-weight: 600;
  box-shadow: inset 0 0 0 1px rgba(147, 197, 253, 0.2);
}

/* =====================================================================
 * 二三级菜单 (nav-sub) — 在子组黑底 0.25 上
 * <p>使用蓝色实底做"高亮", 跟黑底强对比
 * ===================================================================== */

/* 二级 (depth=1) - 与一级菜单行高一致 */
.nav-sub.nav-sub-d1 {
  font-size: 13px;
  padding: 9px 12px 9px 14px;
}
/* 三级 (depth=2) - 与一级菜单行高一致 */
.nav-sub.nav-sub-d2 {
  font-size: 12px;
  padding: 9px 12px 9px 28px;
  opacity: 0.9;
}

/* default: 透明 */
.nav-sub { background-color: transparent; color: rgba(255, 255, 255, 0.85); }

/* hover: 极浅白 */
.nav-sub:hover { background-color: rgba(255, 255, 255, 0.1); color: #fff; }

/* open 态: 子组容器已经展开, 子项本身无变化 */

/* active: 蓝 800 (比 sidebar 深一档, 柔和不跳) + 弱描边
 * <p>无圆角, 左右顶到子组容器边 (子组右侧已经顶到头)
 */
.nav-sub.active,
.nav-sub.active:hover {
  background-color: #1e40af;
  color: #fff;
  font-weight: 600;
  box-shadow: inset 0 0 0 1px rgba(147, 197, 253, 0.2);
}
/* ============ 折叠态子菜单浮层 (el-popover 内容) ============ */
.popover-menu {
  display: flex;
  flex-direction: column;
  padding: 6px 0;
}
.popover-title {
  padding: 6px 14px 8px;
  font-size: 12px;
  color: #64748b;
  font-weight: 600;
  border-bottom: 1px solid #e2e8f0;
  margin-bottom: 4px;
}
.popover-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 9px 14px;
  border: none;
  background: transparent;
  text-align: left;
  cursor: pointer;
  color: #334155;
  font-size: 13px;
  transition: background-color 0.15s, color 0.15s;
}
.popover-item:hover {
  background-color: rgba(30, 64, 175, 0.08);
  color: #1e40af;
}
.popover-item.active,
.popover-item.active:hover {
  background-color: #1e40af;
  color: #fff;
  font-weight: 600;
}
.popover-item-icon {
  flex-shrink: 0;
  color: inherit;
  opacity: 0.85;
}
.popover-item-label {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ============ 折叠态父菜单 icon 居中 ============ */
.is-collapsed-parent {
  justify-content: center;
  padding-left: 0;
  padding-right: 0;
}
.is-collapsed-parent .nav-icon {
  margin-right: 0;
}

<style>
/* 全局: popover 容器去掉默认 padding, 由内部 popover-menu 自管 */
.jc-sidebar-popover.el-popper {
  padding: 0 !important;
  border: 1px solid #e2e8f0;
  box-shadow: 0 6px 24px rgba(15, 23, 42, 0.12);
}
</style>
