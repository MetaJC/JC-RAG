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


export function useAutoExpand(delay = 700) {
  let timer: ReturnType<typeof setTimeout> | null = null
  let pendingTargetId: number | null = null

  function scheduleExpand(targetId: number, enabled: boolean, onExpand: () => void) {
    if (!enabled) {
      clear()
      return
    }

    if (pendingTargetId === targetId) return

    clear()
    pendingTargetId = targetId

    timer = setTimeout(() => {
      if (pendingTargetId === targetId) {
        onExpand()
      }
      clear()
    }, delay)
  }

  function clear() {
    if (timer) {
      clearTimeout(timer)
      timer = null
    }
    pendingTargetId = null
  }

  return { scheduleExpand, clear }
}
