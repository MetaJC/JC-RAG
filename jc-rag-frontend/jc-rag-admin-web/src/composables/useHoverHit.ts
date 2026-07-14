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
