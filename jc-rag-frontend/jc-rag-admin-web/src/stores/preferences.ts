import { defineStore } from 'pinia'

import {
  DEFAULT_MEMORY_FILE_PATH,
  fetchUserPreference,
  updateUserPreference,
  type UserPreferenceUpdateParam,
  type UserPreferenceVO,
} from '../api/preferences'

/**
 * 当前用户偏好 Store。
 * <p>集中维护偏好加载/保存, 组件内通过 useUserPreferenceStore() 获取。
 * <p>第一阶段仅包含: memoryFilePath
 *
 * @author JC
 */

interface UserPreferenceState {
  loaded: boolean
  memoryFilePath: string
}

export const useUserPreferenceStore = defineStore('user-preference', {
  state: (): UserPreferenceState => ({
    loaded: false,
    memoryFilePath: DEFAULT_MEMORY_FILE_PATH,
  }),

  actions: {
    /** 拉取当前用户偏好 (静默失败 → 保留默认值) */
    async load(force = false) {
      if (this.loaded && !force) return
      try {
        const vo: UserPreferenceVO = await fetchUserPreference()
        if (vo?.memoryFilePath) this.memoryFilePath = vo.memoryFilePath
      } catch {
        // 静默: 偏好是辅助能力, 不阻塞主流程
      } finally {
        this.loaded = true
      }
    },

    /** 更新偏好 (按字段可选, 只提交非空字段) */
    async save(payload: UserPreferenceUpdateParam) {
      const vo: UserPreferenceVO = await updateUserPreference(payload)
      if (vo?.memoryFilePath) this.memoryFilePath = vo.memoryFilePath
      this.loaded = true
      return vo
    },

    /** 重置为默认 (本地重置 + 写库清空) */
    async resetMemoryFilePath() {
      return this.save({ memoryFilePath: DEFAULT_MEMORY_FILE_PATH })
    },
  },
})
