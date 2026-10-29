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


export interface HoverTarget {
  element: HTMLElement
  id: number
  level: number
  hasChildren: boolean
  expanded: boolean
}

export interface HoverRowMeta {
  id: number
  level: number
  hasChildren: boolean
  expanded: boolean
}

export function useHoverHit(getRowMeta: (id: number) => HoverRowMeta | null) {
  function findTargetRow(mouseX: number, mouseY: number, tbody: HTMLElement): HoverTarget | null {
    const doc = tbody.ownerDocument
    const elements = doc.elementsFromPoint(mouseX, mouseY)
    for (const el of elements) {
      if (el instanceof HTMLElement) {
        const tr = el.closest('tr.el-table__row') as HTMLElement | null
        if (tr && tbody.contains(tr)) {
          const rowData = extractRowData(tr)
          if (rowData) {
            return { element: tr, ...rowData }
          }
        }
      }
    }
    return findClosestRow(mouseY, tbody)
  }

  function extractDeptId(tr: HTMLElement): number | null {
    const match = tr.className.match(/dept-row-(\d+)/)
    return match ? Number(match[1]) : null
  }

  function extractRowData(tr: HTMLElement): HoverRowMeta | null {
    const id = extractDeptId(tr)
    if (id == null) return null
    return getRowMeta(id)
  }

  function findClosestRow(mouseY: number, tbody: HTMLElement): HoverTarget | null {
    const rows = Array.from(tbody.querySelectorAll('tr.el-table__row')) as HTMLElement[]
    let nearestRow: HTMLElement | null = null
    let nearestDistance = Number.POSITIVE_INFINITY
    for (const row of rows) {
      const rect = row.getBoundingClientRect()
      const distance = mouseY < rect.top ? rect.top - mouseY : mouseY > rect.bottom ? mouseY - rect.bottom : 0
      if (distance < nearestDistance) {
        nearestDistance = distance
        nearestRow = row
      }
    }
    if (!nearestRow) return null
    const rowData = extractRowData(nearestRow)
    return rowData ? { element: nearestRow, ...rowData } : null
  }

  return { findTargetRow, extractDeptId }
}
