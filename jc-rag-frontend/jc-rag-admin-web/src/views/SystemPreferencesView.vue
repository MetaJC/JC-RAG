<script setup lang="ts">
/**
 * 系统设置 (系统管理 → 系统设置)
 * <p>布局: 左侧分类导航 (锚点定位) + 右侧分组卡片表单, 顶部统一保存.
 * <p>设置项落 sys_config 表; 用户端登录页经 /api/auth/site-config 读取生效.
 *
 * @author JC
 */
import { onMounted, reactive, ref } from 'vue'
import { Check, Lock, Setting } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

import { fetchSystemSettings, saveSystemSettings } from '../api/sys'

const loading = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const activeAnchor = ref('basic')

const form = reactive({
  systemName: '',
  logoUrl: '',
  copyright: '',
  registerOpen: true,
  loginAgreeRequired: true,
})

const rules: FormRules = {
  systemName: [
    { required: true, message: '请输入系统名称', trigger: 'blur' },
    { max: 64, message: '系统名称不能超过 64 个字符', trigger: 'blur' },
  ],
  logoUrl: [{ max: 512, message: 'Logo 地址不能超过 512 个字符', trigger: 'blur' }],
  copyright: [{ max: 256, message: '版权信息不能超过 256 个字符', trigger: 'blur' }],
}

async function loadSettings() {
  loading.value = true
  try {
    const data = await fetchSystemSettings()
    form.systemName = data.systemName ?? ''
    form.logoUrl = data.logoUrl ?? ''
    form.copyright = data.copyright ?? ''
    form.registerOpen = !!data.registerOpen
    form.loginAgreeRequired = !!data.loginAgreeRequired
  } catch (e) {
    console.error('[SystemPreferences] load failed:', e)
    ElMessage.error('系统设置加载失败')
  } finally {
    loading.value = false
  }
}

async function onSave() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await saveSystemSettings({ ...form })
    ElMessage.success('系统设置已保存, 用户端即时生效')
  } catch (e) {
    console.error('[SystemPreferences] save failed:', e)
    ElMessage.error('系统设置保存失败')
  } finally {
    saving.value = false
  }
}

/** 左侧导航点击: 滚动到对应分组卡片 */
function onNavSelect(index: string) {
  activeAnchor.value = index
  document.getElementById(`anchor-${index}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

onMounted(loadSettings)
</script>

<template>
  <div class="pref-page" v-loading="loading">
    <header class="page-header">
      <div>
        <h1>系统设置</h1>
        <span>维护系统基础信息与账号策略, 保存后用户端即时生效</span>
      </div>
      <el-button type="primary" :icon="Check" :loading="saving" @click="onSave">保存设置</el-button>
    </header>

    <div class="pref-body">
      <el-menu class="pref-nav" :default-active="activeAnchor" @select="onNavSelect">
        <el-menu-item index="basic">
          <el-icon><Setting /></el-icon>
          <span>基础信息</span>
        </el-menu-item>
        <el-menu-item index="account">
          <el-icon><Lock /></el-icon>
          <span>账号设置</span>
        </el-menu-item>
      </el-menu>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="pref-content">
        <el-card id="anchor-basic" class="pref-card" shadow="never">
          <template #header>
            <div class="pref-card-title">
              <span>基础信息</span>
              <span class="pref-card-desc">系统名称、Logo 与版权信息</span>
            </div>
          </template>
          <el-form-item label="系统名称" prop="systemName">
            <el-input v-model="form.systemName" placeholder="请输入系统名称" maxlength="64" show-word-limit />
          </el-form-item>
          <el-form-item label="系统 Logo 地址" prop="logoUrl">
            <el-input v-model="form.logoUrl" placeholder="Logo 图片 URL, 留空使用默认" clearable />
          </el-form-item>
          <el-form-item label="版权信息" prop="copyright">
            <el-input v-model="form.copyright" placeholder="如: Copyright © 2026 JC RAG" clearable />
          </el-form-item>
        </el-card>

        <el-card id="anchor-account" class="pref-card" shadow="never">
          <template #header>
            <div class="pref-card-title">
              <span>账号设置</span>
              <span class="pref-card-desc">用户注册与登录协议策略</span>
            </div>
          </template>
          <el-form-item>
            <div class="switch-row">
              <div class="switch-text">
                <div class="switch-label">开放用户注册</div>
                <div class="switch-desc">关闭后账号注册入口与短信验证码自动注册均不可用, 仅已有账号可登录</div>
              </div>
              <el-switch v-model="form.registerOpen" inline-prompt active-text="开放" inactive-text="关闭" />
            </div>
          </el-form-item>
          <el-form-item>
            <div class="switch-row">
              <div class="switch-text">
                <div class="switch-label">登录必须勾选用户协议</div>
                <div class="switch-desc">开启时登录页展示协议复选框且必须勾选才能登录; 关闭时隐藏复选框且不限制</div>
              </div>
              <el-switch v-model="form.loginAgreeRequired" inline-prompt active-text="开启" inactive-text="关闭" />
            </div>
          </el-form-item>
        </el-card>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.pref-page {
  padding: 4px;
}
.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 16px;
}
.page-header h1 {
  font-size: 20px;
  margin: 0 0 4px;
}
.page-header span {
  color: #6b7280;
  font-size: 13px;
}
.pref-body {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}
.pref-nav {
  width: 180px;
  flex-shrink: 0;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background: #fff;
  border-right: 1px solid #e5e7eb;
}
.pref-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.pref-card :deep(.el-card__header) {
  padding: 14px 20px;
}
.pref-card-title {
  display: flex;
  align-items: baseline;
  gap: 10px;
}
.pref-card-title > span:first-child {
  font-size: 15px;
  font-weight: 600;
}
.pref-card-desc {
  font-size: 12px;
  color: #9ca3af;
}
.switch-row {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.switch-label {
  font-size: 14px;
  color: #111827;
  margin-bottom: 4px;
}
.switch-desc {
  font-size: 12px;
  color: #9ca3af;
  line-height: 1.5;
}
</style>
