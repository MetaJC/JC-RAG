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
