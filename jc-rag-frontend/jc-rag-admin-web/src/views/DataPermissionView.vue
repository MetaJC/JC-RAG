<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '../api/admin'

const permissions = ref<any[]>([])
const dialogVisible = ref(false)
const form = ref({
  roleId: null as number | null,
  resourceType: 'knowledge_base',
  resourceId: null as number | null,
  permission: 'read',
})

async function load() {
  const rows = await adminApi.listDataPermissions()
  permissions.value = (rows.records ?? (rows || [])).map((item: any) => ({
    id: item.id,
    roleId: item.role_id ?? item.roleId,
    resourceType: item.resource_type ?? item.resourceType,
    resourceId: item.resource_id ?? item.resourceId,
    resourceName: item.resource_name ?? item.resourceName ?? '-',
    permission: item.permission,
  }))
}

async function save() {
  try {
    await adminApi.saveDataPermission(form.value)
    dialogVisible.value = false
    await load()
    ElMessage.success('保存成功')
  } catch {
    ElMessage.error('保存失败')
  }
}

async function removeItem(id: number) {
  try {
    await ElMessageBox.confirm('确认删除该数据权限规则？', '删除确认', { type: 'warning' })
    await adminApi.deleteDataPermission(id)
    await load()
    ElMessage.success('删除成功')
  } catch {}
}

onMounted(load)
</script>

<template>
  <div class="page-wrap">
    <div class="page-head">
      <div>
        <h1>数据权限</h1>
        <p>控制角色对知识库等资源的可见范围。</p>
      </div>
      <el-button type="primary" @click="dialogVisible = true">新增规则</el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="permissions" stripe>
        <el-table-column prop="roleId" label="角色 ID" width="100" />
        <el-table-column prop="resourceType" label="资源类型" min-width="140" />
        <el-table-column prop="resourceId" label="资源 ID" width="100" />
        <el-table-column prop="resourceName" label="资源名称" min-width="200" />
        <el-table-column prop="permission" label="权限" width="100" />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-tooltip content="删除" placement="top">
              <el-button link type="danger" :icon="Delete" @click="removeItem(row.id)" />
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="新增数据权限规则" width="520px">
      <el-form label-position="top">
        <el-form-item label="角色 ID">
          <el-input-number v-model="form.roleId" :min="1" />
        </el-form-item>
        <el-form-item label="资源类型">
          <el-select v-model="form.resourceType" style="width: 100%">
            <el-option label="知识库" value="knowledge_base" />
            <el-option label="文档" value="document" />
            <el-option label="字段" value="field" />
          </el-select>
        </el-form-item>
        <el-form-item label="资源 ID">
          <el-input-number v-model="form.resourceId" :min="1" />
        </el-form-item>
        <el-form-item label="权限">
          <el-select v-model="form.permission" style="width: 100%">
            <el-option label="只读" value="read" />
            <el-option label="拒绝" value="deny" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-wrap { padding: 24px; }
.page-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.page-head h1 { margin: 0 0 6px; font-size: 24px; }
.page-head p { margin: 0; color: #6b7280; }
</style>
