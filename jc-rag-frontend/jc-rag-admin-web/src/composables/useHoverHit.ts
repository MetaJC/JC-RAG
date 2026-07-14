export interface HoverTarget {
  element: HTMLElement
  id: number
  level: number
  hasChildren: boolean
  expanded: boolean
}

export function useHoverHit() {
  function findTargetRow(mouseX: number, mouseY: number, tableWrapper: HTMLElement): HoverTarget | null {
    const doc = tableWrapper.ownerDocument
    const elements = doc.elementsFromPoint(mouseX, mouseY)
    for (const el of elements) {
      if (el instanceof HTMLElement) {
        const tr = el.closest('tr')
        if (tr && tableWrapper.contains(tr)) {
          const rowData = extractRowData(tr)
          if (rowData) {
            return { element: tr, ...rowData }
          }
        }
      }
    }
    return null
  }

  function extractRowData(tr: HTMLElement): { id: number; level: number; hasChildren: boolean; expanded: boolean } | null {
    const idAttr = tr.getAttribute('data-row-key')
    const id = idAttr ? parseInt(idAttr, 10) : null
    if (!id || isNaN(id)) return null

    const nameCell = tr.querySelector('.dept-name-cell')
    if (!nameCell) return null

    const paddingLeft = (nameCell as HTMLElement).style.paddingLeft || '0px'
    const level = Math.round(parseInt(paddingLeft, 10) / 24)

    const hasChildren = !!tr.querySelector('.dept-expand-btn')
    const expanded = !!tr.querySelector('.dept-expand-btn .el-icon--arrow-down')

    return { id, level, hasChildren, expanded }
  }

  return { findTargetRow }
}