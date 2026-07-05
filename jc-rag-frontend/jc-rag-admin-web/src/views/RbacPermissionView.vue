<script setup lang="ts">
/**
 * 权限管理 (按钮/接口权限, RBAC 拆 4 页面之一)
 * <p>菜单/目录权限在 RbacMenuView 管理, 这里只管 BUTTON/API
 *
 * @author JC
 */
import { computed, onMounted, ref } from 'vue'
import { Delete, Edit } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import RbacPermissionDialog from '../components/RbacPermissionDialog.vue'
import {
  createRbacPermission,
  fetchRbacPermissions,
  updateRbacPermission,
  deleteRbacPermission,
} from '../api/rbac'

const loading = ref(false)
const permissions = ref<any[]>([])

const dialogVisible = ref(false)
const dialogMode = ref<'BUTTON' | 'API'>('BUTTON')
const editingId = ref<number | null>(null)
const initial = ref<any>({})

const actionPermissions = computed(() =>
  permissions.value.filter((p) => p.permType === 'BUTTON' || p.permType === 'API')
)

async function loadPermissions() {
  const data = await fetchRbacPermissions({ page: 1, size: 9999 })
  permissions.value = data.records ?? []
}

function openCreatePermission(type: 'BUTTON' | 'API') {
  editingId.value = null
  dialogMode.value = type
  initial.value = {}
  dialogVisible.value = true
}

function openEditPermission(perm: any) {
  editingId.value = perm.id
  dialogMode.value = perm.permType
  initial.value = {
    permCode: perm.permCode,
    permName: perm.permName,
    permType: perm.permType,
    parentId: perm.parentId ?? null,
    path: perm.path || '',
    method: perm.method || '',
    icon: perm.icon || '',
    sort: perm.sort ?? 0,
    component: perm.component || '',
    query: perm.query || '',
    isExternal: perm.isExternal ?? 0,
    keepAlive: perm.keepAlive ?? 0,
    visible: perm.visible ?? 1,
    status: perm.status ?? 1,
  }
  dialogVisible.value = true
}

async function onDialogSubmit(form: any) {
  const payload = {
    permCode: form.permCode,
    permName: form.permName,
    permType: form.permType,
    parentId: form.parentId || null,
    path: form.path,
    method: form.method,
    icon: form.icon,
    sort: form.sort,
    component: form.component,
    query: form.query,
    isExternal: form.isExternal,
    keepAlive: form.keepAlive,
    visible: form.visible,
    status: form.status,
  }
  if (editingId.value) {
    await updateRbacPermission(editingId.value, payload)
    ElMessage.success('权限已更新')
  } else {
    await createRbacPermission(payload)
    ElMessage.success('权限已创建')
  }
  dialogVisible.value = false
  await loadPermissions()
}

async function deletePermission(perm: any) {
  try {
    await ElMessageBox.confirm(`确定删除权限 "${perm.permCode}" 吗?`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  await deleteRbacPermission(perm.id)
  ElMessage.success('权限已删除')
  await loadPermissions()
}

onMounted(async () => {
  loading.value = true
  try {
    await loadPermissions()
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <header class="page-header">
      <div>
        <h1>权限管理</h1>
        <p>维护按钮级与接口级权限 (菜单/目录级权限在「菜单管理」维护)</p>
      </div>
    </header>

    <el-card class="panel-card" shadow="never" v-loading="loading">
      <div class="toolbar">
        <el-button @click="openCreatePermission('BUTTON')">新建按钮权限</el-button>
        <el-button @click="openCreatePermission('API')">新建接口权限</el-button>
      </div>
      <el-table :data="actionPermissions" stripe>
        <el-table-column prop="permCode" label="权限编码" min-width="180" />
        <el-table-column prop="permName" label="权限名称" min-width="140" />
        <el-table-column prop="permType" label="类型" min-width="100" />
        <el-table-column prop="path" label="路径" min-width="200" />
        <el-table-column prop="method" label="方法" min-width="100" />
        <el-table-column label="操作" min-width="120" fixed="right">
          <template #default="{ row }">
            <el-tooltip content="编辑" placement="top">
              <el-button link type="primary" :icon="Edit" @click="openEditPermission(row)" />
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-button link type="danger" :icon="Delete" @click="deletePermission(row)" />
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <RbacPermissionDialog
      v-model:visible="dialogVisible"
      :mode="dialogMode"
      :editing-id="editingId"
      :flat-menu-options="[]"
      :initial-perm-code="initial.permCode"
      :initial-perm-name="initial.permName"
      :initial-perm-type="initial.permType"
      :initial-path="initial.path"
      :initial-method="initial.method"
      :initial-icon="initial.icon"
      :initial-sort="initial.sort"
      :initial-component="initial.component"
      :initial-query="initial.query"
      :initial-is-external="initial.isExternal"
      :initial-keep-alive="initial.keepAlive"
      :initial-visible="initial.visible"
      :initial-status="initial.status"
      @submit="onDialogSubmit"
    />
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 16px;
}

.page-header h1 {
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 4px;
  color: #1f2937;
}

.page-header p {
  color: #6b7280;
  font-size: 13px;
  margin: 0;
}

.panel-card {
  border-radius: 8px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
</style>
