import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

import { proExtensionRoutes } from './pro-extension'

// Pro 商业版构建注入 (inject-pro-frontend.ps1 生成 pro-extension.ts.built)
// community 独立运行无 .built 时, glob 返回空对象, 降级为空数组
const builtMod = import.meta.glob<{ proExtensionRoutesBuilt: RouteRecordRaw[] }>(
  './pro-extension.ts.built',
  { eager: true }
)
const proExtensionRoutesBuilt: RouteRecordRaw[] =
  builtMod['./pro-extension.ts.built']?.proExtensionRoutesBuilt ?? []

import AuditLogView from '../views/AuditLogView.vue'
import DataPermissionView from '../views/DataPermissionView.vue'
import DemoConfigView from '../views/DemoConfigView.vue'
import KnowledgeBaseView from '../views/KnowledgeBaseView.vue'
import KnowledgeBaseAuthView from '../views/KnowledgeBaseAuthView.vue'
import LegalDocManageView from '../views/LegalDocManageView.vue'
import LoginView from '../views/LoginView.vue'
import MainLayout from '../components/MainLayout.vue'
import PromptManageView from '../views/PromptManageView.vue'
import RagAgentManageView from '../views/RagAgentManageView.vue'
import RagSkillManageView from '../views/RagSkillManageView.vue'
import RbacUserView from '../views/RbacUserView.vue'
import RbacRoleView from '../views/RbacRoleView.vue'
import RbacMenuView from '../views/RbacMenuView.vue'
import RbacPermissionView from '../views/RbacPermissionView.vue'
import RbacDepartmentView from '../views/RbacDepartmentView.vue'
import RbacPositionView from '../views/RbacPositionView.vue'
import { useAuthStore } from '../stores/auth'
import SystemSettingsView from '../views/SystemSettingsView.vue'
import ModelDashboardView from '../views/ModelDashboardView.vue'
import ModelProviderConfigView from '../views/ModelProviderConfigView.vue'
import ModelSettingsView from '../views/ModelSettingsView.vue'
import ModelParameterSettingsView from '../views/ModelParameterSettingsView.vue'
import DeveloperOptionsView from '../views/DeveloperOptionsView.vue'

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
      // ★ 加 name: 'admin-main-layout' 作为 Pro 端 addRoute 的 parent 引用点
      //   (vue-router 4 的 addRoute(parentName, route) 需要 parent 有 name)
      //   社区版独立运行无影响 (children 仍按 path 匹配, name 仅用于引用)
      path: '/',
      name: 'admin-main-layout',
      component: MainLayout,
      children: [
        {
          path: '/system/settings',
          redirect: '/models/dashboard',
        },
        {
          path: '/models/dashboard',
          name: 'model-dashboard',
          component: ModelDashboardView,
        },
        {
          path: '/models/providers',
          name: 'model-providers',
          component: ModelProviderConfigView,
        },
        {
          path: '/models/settings',
          name: 'model-settings',
          component: ModelSettingsView,
        },
        {
          path: '/models/parameters',
          name: 'model-parameters',
          component: ModelParameterSettingsView,
        },
        {
          path: '/system/developer-options',
          name: 'developer-options',
          component: DeveloperOptionsView,
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
          // 部门管理 (RBAC 组织维度)
          path: '/rbac/departments',
          name: 'rbac-departments',
          component: RbacDepartmentView,
        },
        {
          // 岗位管理 (RBAC 组织维度)
          path: '/rbac/positions',
          name: 'rbac-positions',
          component: RbacPositionView,
        },
        {
          path: '/prompts/manage',
          name: 'prompt-manage',
          component: PromptManageView,
        },
        {
          // 旧路径保留重定向, 避免旧链接失效
          path: '/agents/manage',
          redirect: '/rag/agent',
        },
        {
          path: '/rag/skill',
          name: 'rag-skill',
          component: RagSkillManageView,
        },
        {
          path: '/rag/agent',
          name: 'rag-agent',
          component: RagAgentManageView,
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
