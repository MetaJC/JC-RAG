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
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import type { MenuNode } from '../types/auth'
import SidebarMenu, { type SidebarMenuItem } from './SidebarMenu.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const COLLAPSED_KEY = 'sidebar-collapsed'

const collapsed = ref(localStorage.getItem(COLLAPSED_KEY) === 'true')

watch(collapsed, (val) => {
  localStorage.setItem(COLLAPSED_KEY, String(val))
})

function toggleSidebar() {
  collapsed.value = !collapsed.value
}

/** 把后端菜单树压成 {path,label,short,depth,children}，缺 short 用 label 取首 2 字 */
function toNav(nodes: MenuNode[], depth = 0): SidebarMenuItem[] {
  return nodes
    .filter((n) => n.visible !== 0 && n.status !== 0)
    .sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0))
    .map((n) => ({
      path: n.path || `__node_${n.id}__`,
      label: n.title,
      short: n.title.slice(0, 2),
      icon: n.icon || '',
      depth,
      children: n.children?.length ? toNav(n.children, depth + 1) : undefined,
    }))
    .filter((n) => n.path)
}

/** 仅渲染后端返回的菜单树，不做硬编码兜底 (空则空，避免掩盖后端问题) */
const navItems = computed<SidebarMenuItem[]>(() => {
  const remote = authStore.user?.menus
  return remote ? toNav(remote) : []
})

/** 当前展开的节点 key（path）。reactive 包裹 Set 才能追踪 add/delete */
const openKeys = reactive(new Set<string>())

/** 折叠态下, 当前打开 popover 浮层的一级菜单 path; null = 全部关闭
 * <p>实现折叠态子菜单的互斥: 切换一级菜单时, 自动关闭上一个浮层
 */
const activePopover = ref<string | null>(null)

function onPopoverOpen(path: string | null) {
  activePopover.value = path
}

/** 路由变化时，自动展开包含当前路由的所有上级, 同时清掉"非命中"级的 openKeys
 * <p>之前 openKeys 只增不减: 切到别的顶级后, 旧顶级仍挂着 .open class → 残留"高亮"
 * <p>现在: 只保留包含当前路由路径的那些顶级在 openKeys 里
 */
watch(
  () => route.path,
  (cur) => {
    // 先清空, 再重新展开 (只展开真正包含当前路由的)
    const need: string[] = []
    function findOwners(items: SidebarMenuItem[]): boolean {
      for (const item of items) {
        if (item.path === cur) return true
        if (item.children?.length && findOwners(item.children)) {
          need.push(item.path)
          return true
        }
      }
      return false
    }
    findOwners(navItems.value)
    // 清理不相关的 open 状态, 防止"残留"
    for (const k of [...openKeys]) {
      if (!need.includes(k)) openKeys.delete(k)
    }
    // 当前路由所在链上所有顶级全部展开
    for (const k of need) openKeys.add(k)
  },
  { immediate: true }
)

function isActive(path: string): boolean {
  return route.path === path
}

function go(path: string) {
  if (!path) return
  router.push(path)
}

function toggleTop(item: SidebarMenuItem) {
  // 折叠态 + 有子菜单 = 留给 SidebarMenu 的 popover 浮层处理 (这里不能 go, 否则直接跳转)
  if (collapsed.value && item.children?.length) {
    return
  }
  if (collapsed.value || !item.children?.length) {
    go(item.path)
    return
  }
  // 手风琴式互斥: 同一父级下, 同时只展开一个子菜单
  if (openKeys.has(item.path)) {
    openKeys.delete(item.path)
  } else {
    const parent = findParent(item.path, navItems.value)
    for (const k of [...openKeys]) {
      // 同父级 + 同深度 = 互斥
      if (findParent(k, navItems.value) === parent
          && findLevel(k, navItems.value, 0) === findLevel(item.path, navItems.value, 0)) {
        openKeys.delete(k)
      }
    }
    openKeys.add(item.path)
  }
}

/** 找到指定 path 所在的深度 */
function findLevel(target: string, items: SidebarMenuItem[], depth: number): number {
  for (const item of items) {
    if (item.path === target) return depth
    if (item.children?.length) {
      const r = findLevel(target, item.children, depth + 1)
      if (r !== -1) return r
    }
  }
  return -1
}

/** 找到指定 path 的父级 path (顶级时返回 undefined) */
function findParent(target: string, items: SidebarMenuItem[], parent?: string): string | undefined {
  for (const item of items) {
    if (item.path === target) return parent
    if (item.children?.length) {
      const r = findParent(target, item.children, item.path)
      if (r !== undefined || item.children.some((c) => c.path === target)) return r
    }
  }
  return undefined
}
</script>

<template>
  <div class="layout-shell" :class="{ collapsed: collapsed }">
    <aside class="layout-sidebar">
      <div class="brand-row">
        <span class="brand-title" v-show="!collapsed">RAG 系统</span>
        <span class="toggle-btn" :class="{ 'is-standalone': collapsed }" @click="toggleSidebar" :title="collapsed ? '展开' : '收起'">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <rect x="2" y="3" width="12" height="1.5" rx="0.75" :fill="collapsed ? 'rgba(255,255,255,0.6)' : 'rgba(255,255,255,0.5)'"/>
            <rect x="2" y="7" width="12" height="1.5" rx="0.75" :fill="collapsed ? 'rgba(255,255,255,0.6)' : 'rgba(255,255,255,0.5)'"/>
            <rect x="2" y="11" width="12" height="1.5" rx="0.75" :fill="collapsed ? 'rgba(255,255,255,0.6)' : 'rgba(255,255,255,0.5)'"/>
          </svg>
        </span>
      </div>
      <div class="brand-subtitle" v-show="!collapsed">离线部署管理控制台</div>
      <nav class="nav-group">
        <SidebarMenu
          v-for="top in navItems"
          :key="top.path"
          :item="top"
          :collapsed="collapsed"
          :is-active="isActive"
          :open-keys="openKeys"
          :toggle="toggleTop"
          :go="go"
          :active-popover="activePopover"
          :on-popover-open="onPopoverOpen"
        />
      </nav>
    </aside>
    <main class="layout-main">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.layout-shell {
  display: grid;
  grid-template-columns: 200px 1fr;
  min-height: calc(100vh - 56px);
  transition: grid-template-columns 0.2s ease;
}

.layout-shell.collapsed {
  grid-template-columns: 56px 1fr;
}

.layout-sidebar {
  padding: 16px 0;            /* 左右 padding 归 0: 让菜单 active 背景能顶到 sidebar 边 */
  padding-left: 0;
  background: linear-gradient(180deg, #0f2744 0%, #173d66 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  overflow-x: hidden;
  transition: padding 0.2s ease;
}

.collapsed .layout-sidebar {
  padding: 12px 0;
}

.brand-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px 14px 12px;   /* 顶替外层 padding, 让品牌区有左右间距 */
  margin-bottom: 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
}
.collapsed .brand-row {
  justify-content: center;
  padding: 0 0 12px 0;
  border-bottom-color: transparent;
}
.toggle-btn.is-standalone {
  margin: 0 auto;
}

.brand-title {
  font-size: 18px;
  font-weight: 700;
}

.brand-icon {
  text-align: center;
  width: 100%;
  font-size: 16px;
}

.toggle-btn {
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 4px;
  flex-shrink: 0;
  transition: background 0.15s;
}

.toggle-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

.toggle-btn:hover svg rect {
  fill: rgba(255, 255, 255, 0.85) !important;
}

.brand-subtitle {
  margin-top: -8px;
  margin-bottom: 16px;
  padding: 0 12px;            /* 顶替外层 padding */
  color: rgba(255, 255, 255, 0.45);
  font-size: 11px;
}

.nav-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
  overflow-y: auto;
}

.layout-main {
  padding: 24px;
  overflow-y: auto;
}
</style>
