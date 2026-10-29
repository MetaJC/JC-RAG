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


export type DropPosition = 'before' | 'after' | 'inner' | null

export interface DropPositionResult {
  position: DropPosition
  targetId: number | null
  level: number
  illegal: boolean
}

export function useDropPosition(deadZone = 8) {
  let lastTargetId: number | null = null
  let lastPosition: DropPosition = null

  function calculatePosition(target: HTMLElement, mouseY: number, level: number, insideAllowed = true): DropPositionResult {
    const rect = target.getBoundingClientRect()
    const height = rect.height || 1
    const mouseOffset = mouseY - rect.top

    const topZoneEnd = height * 0.25
    const bottomZoneStart = height * 0.75
    const middleLine = height / 2

    let position: DropPosition
    if (mouseOffset <= topZoneEnd) {
      position = 'before'
    } else if (mouseOffset >= bottomZoneStart) {
      position = 'after'
    } else {
      position = insideAllowed ? 'inner' : mouseOffset < middleLine ? 'before' : 'after'
    }

    const match = target.className.match(/dept-row-(\d+)/)
    const targetId = match ? Number(match[1]) : null

    if (lastTargetId === targetId && lastPosition !== null) {
      if (lastPosition === 'before' && position === 'inner') {
        if (mouseOffset <= topZoneEnd + deadZone) {
          position = 'before'
        }
      } else if (lastPosition === 'inner' && position === 'before') {
        if (mouseOffset >= topZoneEnd - deadZone) {
          position = 'inner'
        }
      } else if (lastPosition === 'inner' && position === 'after') {
        if (mouseOffset <= bottomZoneStart + deadZone) {
          position = 'inner'
        }
      } else if (lastPosition === 'after' && position === 'inner') {
        if (mouseOffset >= bottomZoneStart - deadZone) {
          position = 'after'
        }
      }
    }

    lastTargetId = targetId
    lastPosition = position

    const finalLevel = position === 'inner' ? level + 1 : level

    return {
      position,
      targetId,
      level: finalLevel,
      illegal: false,
    }
  }

  function reset() {
    lastTargetId = null
    lastPosition = null
  }

  return { calculatePosition, reset }
}
