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


import { IS_PRO_BUILD, BUILD_EDITION, BUILD_EDITION_LABEL, type Edition } from './edition'
import { ref } from 'vue'

/**
 * 运行时版本检测 composable.
 * <p>
 * 启动时调用后端 {@code /api/admin/edition} 校验构建期版本与运行期后端版本是否一致.
 * 校验失败时给出 warning (防止用 oss 前端访问 pro 后端 / 反之).
 *
 * @author JC
 */
export function useEdition() {
  /** 运行时版本 (由后端返回, 默认为构建期版本). */
  const runtimeEdition = ref<Edition>(BUILD_EDITION)
  /** 运行时版本标签. */
  const runtimeLabel = ref<string>(BUILD_EDITION_LABEL)
  /** 是否完成首次校验. */
  const checked = ref(false)

  /** 启动时调用一次, 拉取后端版本信息. */
  async function fetchEdition() {
    try {
      const res = await fetch('/api/admin/edition')
      if (!res.ok) return
      const data = (await res.json()) as { code?: number; data?: { edition: Edition; label: string } }
      if (data.code === 0 && data.data) {
        runtimeEdition.value = data.data.edition
        runtimeLabel.value = data.data.label
        if (data.data.edition !== BUILD_EDITION) {
          console.warn(
            `[JC RAG] 前端构建版本(${BUILD_EDITION}) 与后端运行版本(${data.data.edition}) 不一致, 可能出现功能异常`,
          )
        }
      }
    } catch (e) {
      console.warn('[JC RAG] 获取后端版本信息失败, 跳过校验', e)
    } finally {
      checked.value = true
    }
  }

  /** 当前生效版本 (优先运行时, 否则构建期). */
  const currentEdition = (): Edition => checked.value ? runtimeEdition.value : BUILD_EDITION

  /** 是否为商业版 (运行时). */
  const isPro = (): boolean => currentEdition() === 'pro'

  return {
    BUILD_EDITION,
    BUILD_EDITION_LABEL,
    IS_PRO_BUILD,
    runtimeEdition,
    runtimeLabel,
    checked,
    fetchEdition,
    currentEdition,
    isPro,
  }
}
