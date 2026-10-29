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


export interface IndicatorMetrics {
  lineLeft: number
  lineWidth: number
  boxLeft: number
  boxWidth: number
  boxHeight: number
}

export function useAutoIndentCalculator() {
  function calculate(rowEl: HTMLElement): IndicatorMetrics {
    const rowRect = rowEl.getBoundingClientRect()
    const firstCell = rowEl.querySelector('.el-table__cell:first-child') as HTMLElement | null
    const contentEl = rowEl.querySelector('.dept-name-cell') as HTMLElement | null
    const cellRect = firstCell?.getBoundingClientRect() ?? rowRect
    const contentRect = contentEl?.getBoundingClientRect() ?? cellRect
    const contentStyle = contentEl ? window.getComputedStyle(contentEl) : null
    const cellStyle = firstCell ? window.getComputedStyle(firstCell) : null
    const contentPaddingLeft = contentStyle ? parseFloat(contentStyle.paddingLeft || '0') : 0
    const cellPaddingRight = cellStyle ? parseFloat(cellStyle.paddingRight || '0') : 0
    const lineLeft = Math.round(contentRect.left + contentPaddingLeft)
    const lineRight = Math.round(cellRect.right - cellPaddingRight - 8)
    const boxLeft = Math.round(Math.max(contentRect.left + Math.max(contentPaddingLeft - 8, 0), cellRect.left + 4))
    const boxRight = Math.round(cellRect.right - cellPaddingRight - 4)

    return {
      lineLeft,
      lineWidth: Math.max(lineRight - lineLeft, 48),
      boxLeft,
      boxWidth: Math.max(boxRight - boxLeft, 48),
      boxHeight: Math.max(Math.round(rowRect.height - 8), 28),
    }
  }

  return { calculate }
}
