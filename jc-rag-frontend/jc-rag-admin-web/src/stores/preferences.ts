/*
 * Copyright 2024-2026 OpenJC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 本文件来源于 JC RAG Community Edition
 * 开源地址: https://gitee.com/Open-JC/jc-rag | https://github.com/MetaJC/JC-RAG
 * 二次开发请保留此出处声明。
 */


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
