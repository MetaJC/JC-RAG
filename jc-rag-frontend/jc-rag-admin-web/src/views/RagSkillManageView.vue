<script setup lang="ts">
/**
 * 技能管理 (拆自 AgentSkillManageView, 2026-07-25)
 * <p>仅管理 Skill 模板: 名称/标识/触发方式/描述/提示词模板.
 * <p>Agent 管理独立到 RagAgentManageView.
 *
 * @author JC
 */
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit } from '@element-plus/icons-vue'
import {
  fetchRagSkills, createRagSkill, updateRagSkill, deleteRagSkill,
} from '../api/rag'

const skills = ref<any[]>([])

async function loadSkills() {
  const skillRows = await fetchRagSkills()
  skills.value = (skillRows.records ?? (skillRows || [])).map((item: any) => ({
    id: item.id,
    skillName: item.skill_name ?? item.skillName,
    skillKey: item.skill_key ?? item.skillKey,
    description: item.description ?? '',
    promptTemplate: item.prompt_template ?? item.promptTemplate ?? '',
    triggerType: item.trigger_type ?? item.triggerType ?? 'auto',
    triggerRules: item.trigger_rules ?? item.triggerRules ?? '',
    icon: item.icon ?? '',
    sortOrder: item.sort_order ?? item.sortOrder ?? 0,
    status: item.status ?? 1,
  }))
}

const skillDialog = ref(false)
const editingSkillId = ref<number | null>(null)
const skillForm = ref({
  skillName: '', skillKey: '', description: '', promptTemplate: '',
  triggerType: 'auto', triggerRules: '', icon: '', sortOrder: 0, status: 1,
})

function openSkill(item?: any) {
  editingSkillId.value = item?.id ?? null
  skillForm.value = item
    ? { ...item }
    : { skillName: '', skillKey: '', description: '', promptTemplate: '', triggerType: 'auto', triggerRules: '', icon: '', sortOrder: 0, status: 1 }
  skillDialog.value = true
}

async function saveSkill() {
  try {
    if (editingSkillId.value) {
      await updateRagSkill(editingSkillId.value, skillForm.value)
    } else {
      await createRagSkill(skillForm.value)
    }
    skillDialog.value = false
    await loadSkills()
    ElMessage.success('保存成功')
  } catch { ElMessage.error('保存失败') }
}

async function removeSkill(id: number) {
  try {
    await ElMessageBox.confirm('确认删除该 Skill？', '删除确认', { type: 'warning' })
    await deleteRagSkill(id)
    await loadSkills()
    ElMessage.success('删除成功')
  } catch {}
}

onMounted(loadSkills)
</script>

<template>
  <div class="page-wrap">
    <div class="page-head">
      <div>
        <h1>技能管理</h1>
        <p>管理 Skill 模板, 可被 Agent 引用以扩展能力。Agent 管理请前往「智能体管理」页面。</p>
      </div>
    </div>

    <el-card shadow="never">
      <div class="toolbar">
        <el-button type="primary" @click="openSkill()">新建 Skill</el-button>
      </div>
      <el-table :data="skills" stripe>
        <el-table-column prop="skillName" label="名称" min-width="160" />
        <el-table-column prop="skillKey" label="标识" min-width="140" />
        <el-table-column prop="triggerType" label="触发方式" width="100" />
        <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-tooltip content="编辑" placement="top">
              <el-button link type="primary" :icon="Edit" @click="openSkill(row)" />
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-button link type="danger" :icon="Delete" @click="removeSkill(row.id)" />
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="skillDialog" :title="editingSkillId ? '编辑 Skill' : '新建 Skill'" width="680px">
      <el-form label-position="top">
        <el-form-item label="名称"><el-input v-model="skillForm.skillName" /></el-form-item>
        <el-form-item label="标识键"><el-input v-model="skillForm.skillKey" /></el-form-item>
        <el-form-item label="触发方式">
          <el-select v-model="skillForm.triggerType" style="width: 100%">
            <el-option label="自动" value="auto" />
            <el-option label="关键词" value="keyword" />
            <el-option label="手动" value="manual" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="skillForm.description" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="提示词模板"><el-input v-model="skillForm.promptTemplate" type="textarea" :rows="6" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="skillDialog = false">取消</el-button>
        <el-button type="primary" @click="saveSkill">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-wrap { padding: 24px; }
.page-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.page-head h1 { margin: 0 0 6px; font-size: 24px; }
.page-head p { margin: 0; color: #6b7280; }
.toolbar { display: flex; justify-content: flex-end; margin-bottom: 12px; gap: 8px; }
</style>
