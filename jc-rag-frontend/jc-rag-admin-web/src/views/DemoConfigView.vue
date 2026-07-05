<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Edit } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { fetchDemoConfig, updateDemoConfig, fetchRoleQuotas, saveRoleQuota } from '../api/sys'

const config = ref({
  demoModeEnabled: 0,
  defaultQuotaPerDay: 20,
  defaultTokensPerQ: 2048,
  defaultTokensPerDay: 10000,
  upgradeHint: '',
})
const roleQuotas = ref<any[]>([])
const quotaDialog = ref(false)
const quotaForm = ref({
  roleId: null as number | null,
  quotaPerDay: null as number | null,
  tokensPerQ: null as number | null,
  tokensPerDay: null as number | null,
  canConfigureApiKey: 1,
})

async function load() {
  const [cfg, quotas] = await Promise.all([fetchDemoConfig(), fetchRoleQuotas()])
  config.value = {
    demoModeEnabled: cfg.demo_mode_enabled ?? cfg.demoModeEnabled ?? 0,
    defaultQuotaPerDay: cfg.default_quota_per_day ?? cfg.defaultQuotaPerDay ?? 20,
    defaultTokensPerQ: cfg.default_tokens_per_q ?? cfg.defaultTokensPerQ ?? 2048,
    defaultTokensPerDay: cfg.default_tokens_per_day ?? cfg.defaultTokensPerDay ?? 10000,
    upgradeHint: cfg.upgrade_hint ?? cfg.upgradeHint ?? '',
  }
  roleQuotas.value = (quotas || []).map((item: any) => ({
    roleId: item.role_id ?? item.roleId,
    roleName: item.role_name ?? item.roleName,
    quotaPerDay: item.quota_per_day ?? item.quotaPerDay,
    tokensPerQ: item.tokens_per_q ?? item.tokensPerQ,
    tokensPerDay: item.tokens_per_day ?? item.tokensPerDay,
    canConfigureApiKey: item.can_configure_api_key ?? item.canConfigureApiKey,
  }))
}

async function saveConfig() {
  try {
    await updateDemoConfig(config.value)
    ElMessage.success('配置已保存')
    await load()
  } catch {
    ElMessage.error('保存失败')
  }
}

function openQuota(item?: any) {
  quotaForm.value = item ? { ...item } : {
    roleId: null,
    quotaPerDay: null,
    tokensPerQ: null,
    tokensPerDay: null,
    canConfigureApiKey: 1,
  }
  quotaDialog.value = true
}

async function saveQuota() {
  try {
    await saveRoleQuota(quotaForm.value)
    quotaDialog.value = false
    await load()
    ElMessage.success('角色配额已保存')
  } catch {
    ElMessage.error('保存失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="page-wrap">
    <div class="page-head">
      <div>
        <h1>演示模式配置</h1>
        <p>统一管理 C 端默认限额、演示模式和角色配额。</p>
      </div>
    </div>

    <div class="grid">
      <el-card shadow="never">
        <template #header>全局配置</template>
        <el-form label-position="top">
          <el-form-item label="演示模式">
            <el-switch v-model="config.demoModeEnabled" :active-value="1" :inactive-value="0" />
          </el-form-item>
          <el-form-item label="默认每日问答次数">
            <el-input-number v-model="config.defaultQuotaPerDay" :min="0" />
          </el-form-item>
          <el-form-item label="每次问答最大 Token">
            <el-input-number v-model="config.defaultTokensPerQ" :min="0" :step="256" />
          </el-form-item>
          <el-form-item label="每日最大 Token">
            <el-input-number v-model="config.defaultTokensPerDay" :min="0" :step="1000" />
          </el-form-item>
          <el-form-item label="超限提示">
            <el-input v-model="config.upgradeHint" type="textarea" :rows="4" />
          </el-form-item>
          <el-button type="primary" @click="saveConfig">保存全局配置</el-button>
        </el-form>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <div class="card-head">
            <span>角色配额</span>
            <el-button type="primary" @click="openQuota()">新增配额</el-button>
          </div>
        </template>
        <el-table :data="roleQuotas" stripe>
          <el-table-column prop="roleName" label="角色" min-width="120" />
          <el-table-column prop="quotaPerDay" label="每日问答次数" min-width="120" />
          <el-table-column prop="tokensPerQ" label="单次 Token" min-width="120" />
          <el-table-column prop="tokensPerDay" label="每日 Token" min-width="120" />
          <el-table-column label="允许自配 Key" min-width="120">
            <template #default="{ row }">{{ row.canConfigureApiKey === 1 ? '是' : '否' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{ row }">
              <el-tooltip content="编辑" placement="top">
                <el-button link type="primary" :icon="Edit" @click="openQuota(row)" />
              </el-tooltip>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <el-dialog v-model="quotaDialog" title="角色配额" width="520px">
      <el-form label-position="top">
        <el-form-item label="角色 ID">
          <el-input-number v-model="quotaForm.roleId" :min="1" />
        </el-form-item>
        <el-form-item label="每日问答次数">
          <el-input-number v-model="quotaForm.quotaPerDay" :min="0" />
        </el-form-item>
        <el-form-item label="每次 Token">
          <el-input-number v-model="quotaForm.tokensPerQ" :min="0" />
        </el-form-item>
        <el-form-item label="每日 Token">
          <el-input-number v-model="quotaForm.tokensPerDay" :min="0" />
        </el-form-item>
        <el-form-item label="允许自配 Key">
          <el-switch v-model="quotaForm.canConfigureApiKey" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="quotaDialog = false">取消</el-button>
        <el-button type="primary" @click="saveQuota">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-wrap { padding: 24px; }
.page-head { margin-bottom: 16px; }
.page-head h1 { margin: 0 0 6px; font-size: 24px; }
.page-head p { margin: 0; color: #6b7280; }
.grid { display: grid; grid-template-columns: 420px 1fr; gap: 16px; }
.card-head { display: flex; align-items: center; justify-content: space-between; }
</style>
