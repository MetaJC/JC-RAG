import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

import { proExtensionRoutes } from './pro-extension'

// Pro 商业版构建注入 (inject-pro-frontend.ps1 生成 pro-extension.ts.built)
// community 独立运行无 .built 时, glob 返回空对象, 降级为空数组
const builtMod = import.meta.glob<{ proExtensionRoutesBuilt: RouteRecordRaw[] }>(
  './pro-extension.built.ts',
  { eager: true }
)
const proExtensionRoutesBuilt: RouteRecordRaw[] =
  builtMod['./pro-extension.built.ts']?.proExtensionRoutesBuilt ?? []

import AuditLogView from '../views/AuditLogView.vue'
import AgentSkillManageView from '../views/AgentSkillManageView.vue'
import ChatCenterView from '../views/ChatCenterView.vue'
import DataPermissionView from '../views/DataPermissionView.vue'
import DemoConfigView from '../views/DemoConfigView.vue'
import KnowledgeBaseView from '../views/KnowledgeBaseView.vue'
import KnowledgeBaseAuthView from '../views/KnowledgeBaseAuthView.vue'
import LegalDocManageView from '../views/LegalDocManageView.vue'
import LoginView from '../views/LoginView.vue'
import MainLayout from '../components/MainLayout.vue'
import PromptManageView from '../views/PromptManageView.vue'
import RbacUserView from '../views/RbacUserView.vue'
import RbacRoleView from '../views/RbacRoleView.vue'
import RbacMenuView from '../views/RbacMenuView.vue'
import RbacPermissionView from '../views/RbacPermissionView.vue'
import { pinia } from '../stores'
import { useAuthStore } from '../stores/auth'
import SystemSettingsView from '../views/SystemSettingsView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/system/settings',
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { public: true },
    },
    {
      path: '/',
      component: MainLayout,
      children: [
        {
          path: '/system/settings',
          name: 'system-settings',
          component: SystemSettingsView,
        },
        {
          path: '/knowledge/base',
          name: 'knowledge-base',
          component: KnowledgeBaseView,
        },
        {
          path: '/knowledge/auth',
          name: 'knowledge-auth',
          component: KnowledgeBaseAuthView,
        },
        {
          path: '/qa/center',
          name: 'qa-center',
          component: ChatCenterView,
        },
        {
          path: '/audit/logs',
          name: 'audit-logs',
          component: AuditLogView,
        },
        {
          // === 旧 RBAC 整合页 → 拆 4 页面, 保留旧路径做重定向 ===
          path: '/rbac/manage',
          redirect: '/rbac/users',
        },
        {
          // === RBAC 拆 4: 用户/角色/菜单/权限 独立 URL + 独立 perm_code ===
          path: '/rbac/users',
          name: 'rbac-users',
          component: RbacUserView,
        },
        {
          path: '/rbac/roles',
          name: 'rbac-roles',
          component: RbacRoleView,
        },
        {
          path: '/rbac/menus',
          name: 'rbac-menus',
          component: RbacMenuView,
        },
        {
          path: '/rbac/permissions',
          name: 'rbac-permissions',
          component: RbacPermissionView,
        },
        {
          path: '/prompts/manage',
          name: 'prompt-manage',
          component: PromptManageView,
        },
        {
          path: '/agents/manage',
          name: 'agent-manage',
          component: AgentSkillManageView,
        },
        {
          path: '/demo/config',
          name: 'demo-config',
          component: DemoConfigView,
        },
        {
          path: '/data/permissions',
          name: 'data-permissions',
          component: DataPermissionView,
        },
        {
          path: '/sys/legal-doc',
          name: 'legal-doc-manage',
          component: LegalDocManageView,
        },
        // === 别名路由: 兼容 DB 现有 menu ===
        // DB 实际 path 与 router 历史路由不统一, 此处用别名指向最近 View
        // 标记 todo: 后续如需独立页面, 拆出独立 component 并替换 component
        {
          path: '/dashboard',
          name: 'dashboard',
          component: SystemSettingsView,
        },
        {
          path: '/chat',
          name: 'chat',
          component: ChatCenterView,
        },
        {
          path: '/sys/test',
          name: 'sys-test',
          component: SystemSettingsView,
        },
        ...proExtensionRoutes,
        ...proExtensionRoutesBuilt,
      ],
    },
  ],
})

router.beforeEach(async (to) => {
  // ★ 不传 pinia 参数: 让 Pinia 从 active pinia 取, 避免 Vite alias 路径差异导致
  //   Pro 端 (alias 解析) 与 community 端 (相对路径) 加载到不同的 stores 模块实例,
  //   进而产生两个独立 pinia, user 数据写入"幽灵" pinia, 侧边栏读不到.
  const authStore = useAuthStore()
  await authStore.init()

  if (to.meta.public) {
    if (authStore.user && to.path === '/login') {
      return '/system/settings'
    }
    return true
  }

  if (!authStore.user) {
    return '/login'
  }

  return true
})

export default router
