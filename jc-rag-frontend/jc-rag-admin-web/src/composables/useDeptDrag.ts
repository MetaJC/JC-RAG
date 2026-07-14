import { nextTick, ref } from 'vue'
import Sortable from 'sortablejs'
import { useAutoIndentCalculator } from './useAutoIndentCalculator'
import { useAutoExpand } from './useAutoExpand'
import { useAutoScroll } from './useAutoScroll'
import { useDropIndicator } from './useDropIndicator'
import { useHoverHit } from './useHoverHit'
import { useDropPosition } from './useDropPosition'

export type DropPosition = 'before' | 'after' | 'inner' | null

export interface DragRowMeta {
  id: number
  level: number
  hasChildren: boolean
}

export interface DragState {
  dragging: boolean
  draggedId: number | null
  targetId: number | null
  position: DropPosition
  illegal: boolean
}

export interface DropResult {
  draggedId: number
  targetId: number
  position: 'before' | 'after' | 'inner'
}

export interface UseDeptDragOptions {
  getFlatList: () => DragRowMeta[]
  getExpandedIds: () => Set<number>
  toggleExpand: (id: number) => void
  isDescendantOrSelf: (rootId: number, searchId: number, nodes: any[]) => boolean
  deptTree: () => any[]
  onDrop: (result: DropResult) => void | Promise<void>
}

export function useDeptDrag(options: UseDeptDragOptions) {
  const { findTargetRow, extractDeptId } = useHoverHit(getRowMeta)
  const { calculatePosition, reset: resetPosition } = useDropPosition(8)
  const { scheduleExpand, clear: clearExpand } = useAutoExpand(700)
  const { start: startScroll, update: updateScroll, stop: stopScroll } = useAutoScroll(50, 20)
  const { init: initIndicator, update: updateIndicator, hide: hideIndicator, cleanup: cleanupIndicator } = useDropIndicator()
  const { calculate: calculateIndent } = useAutoIndentCalculator()

  let sortableInstance: Sortable | null = null
  let tbodyEl: HTMLElement | null = null
  let bodyWrapper: HTMLElement | null = null
  let draggedRow: HTMLElement | null = null
  let hoveredRow: HTMLElement | null = null
  let mouseMoveHandler: ((e: MouseEvent) => void) | null = null
  let lastPointer: { x: number; y: number } | null = null
  let resolveFrameId: number | null = null
  let pendingDrop: Omit<DropResult, 'draggedId'> | null = null

  const dragState = ref<DragState>({
    dragging: false,
    draggedId: null,
    targetId: null,
    position: null,
    illegal: false,
  })

  function getRowMeta(id: number) {
    const row = options.getFlatList().find((item) => item.id === id)
    if (!row) return null
    return {
      id: row.id,
      level: row.level,
      hasChildren: row.hasChildren,
      expanded: options.getExpandedIds().has(id),
    }
  }

  function isLegalDrop(draggedId: number, targetId: number): boolean {
    if (draggedId === targetId) return false
    if (options.isDescendantOrSelf(draggedId, targetId, options.deptTree())) {
      return false
    }
    return true
  }

  function setHoveredRow(nextRow: HTMLElement | null) {
    if (hoveredRow === nextRow) return
    hoveredRow?.classList.remove('dept-hover-target')
    hoveredRow = nextRow
    hoveredRow?.classList.add('dept-hover-target')
  }

  function clearHoveredRow() {
    setHoveredRow(null)
  }

  function clearDragSource() {
    if (draggedRow) {
      draggedRow.classList.remove('dept-drop-source')
      draggedRow = null
    }
  }

  function updateCursor(illegal: boolean) {
    document.body.style.cursor = illegal ? 'not-allowed' : 'grabbing'
  }

  async function reprocessPointerAfterExpand() {
    await nextTick()
    if (!lastPointer) return
    resolvePointer(lastPointer.x, lastPointer.y)
  }

  function resolvePointer(mouseX: number, mouseY: number) {
    if (!tbodyEl || !bodyWrapper || !dragState.value.dragging) return

    const target = findTargetRow(mouseX, mouseY, tbodyEl)
    if (!target) {
      clearHoveredRow()
      hideIndicator()
      dragState.value = { ...dragState.value, targetId: null, position: null, illegal: true }
      pendingDrop = null
      updateCursor(true)
      return
    }

    const posResult = calculatePosition(target.element, mouseY, target.level, true)
    const targetId = posResult.targetId
    const illegal = !dragState.value.draggedId || !targetId || !posResult.position || !isLegalDrop(dragState.value.draggedId, targetId)

    setHoveredRow(target.element)

    scheduleExpand(
      target.id,
      posResult.position === 'inner' && target.hasChildren && !target.expanded,
      () => {
        options.toggleExpand(target.id)
        void reprocessPointerAfterExpand()
      }
    )

    dragState.value = {
      ...dragState.value,
      targetId,
      position: posResult.position,
      illegal,
    }

    if (illegal || !posResult.position) {
      hideIndicator()
      pendingDrop = null
      updateCursor(true)
      return
    }

    const metrics = calculateIndent(target.element)
    updateIndicator({
      position: posResult.position,
      rowRect: target.element.getBoundingClientRect(),
      lineLeft: metrics.lineLeft,
      lineWidth: metrics.lineWidth,
      boxLeft: metrics.boxLeft,
      boxWidth: metrics.boxWidth,
      boxHeight: metrics.boxHeight,
    })
    pendingDrop = { targetId, position: posResult.position }
    updateCursor(false)
  }

  function handleDragStart(evt: Sortable.SortableEvent) {
    const item = evt.item as HTMLElement
    if (!item) return

    draggedRow = item
    const draggedId = extractDeptId(item)

    if (!draggedId) return

    draggedRow.classList.add('dept-drop-source')

    dragState.value = {
      dragging: true,
      draggedId,
      targetId: null,
      position: null,
      illegal: false,
    }
    pendingDrop = null
    lastPointer = null

    bodyWrapper = document.querySelector('.dept-table-wrap .el-table__body-wrapper') as HTMLElement | null
    tbodyEl = document.querySelector('.dept-table-wrap .el-table__body-wrapper tbody') as HTMLElement | null

    if (bodyWrapper && tbodyEl) {
      initIndicator(bodyWrapper)
      startScroll(bodyWrapper, () => {
        if (!lastPointer) return
        resolvePointer(lastPointer.x, lastPointer.y)
      })
    }

    mouseMoveHandler = (e: MouseEvent) => {
      lastPointer = { x: e.clientX, y: e.clientY }
      updateScroll(e.clientY)
      if (resolveFrameId != null) return
      resolveFrameId = requestAnimationFrame(() => {
        resolveFrameId = null
        if (!lastPointer) return
        resolvePointer(lastPointer.x, lastPointer.y)
      })
    }
    document.addEventListener('mousemove', mouseMoveHandler)
  }

  async function handleDragEnd() {
    const dropResult =
      dragState.value.draggedId && pendingDrop && !dragState.value.illegal
        ? {
            draggedId: dragState.value.draggedId,
            targetId: pendingDrop.targetId,
            position: pendingDrop.position,
          }
        : null

    if (mouseMoveHandler) {
      document.removeEventListener('mousemove', mouseMoveHandler)
      mouseMoveHandler = null
    }
    if (resolveFrameId != null) {
      cancelAnimationFrame(resolveFrameId)
      resolveFrameId = null
    }

    stopScroll()
    hideIndicator()
    clearHoveredRow()
    clearExpand()
    resetPosition()
    document.body.style.cursor = ''
    pendingDrop = null
    lastPointer = null
    tbodyEl = null
    bodyWrapper = null

    clearDragSource()

    dragState.value = {
      dragging: false,
      draggedId: null,
      targetId: null,
      position: null,
      illegal: false,
    }

    if (dropResult) {
      await options.onDrop(dropResult)
    }
  }

  function initSortable() {
    destroySortable()

    const tbody = document.querySelector('.dept-table-wrap .el-table__body-wrapper tbody') as HTMLElement | null
    if (!tbody) return

    sortableInstance = Sortable.create(tbody, {
      animation: 180,
      handle: '.dept-drag-handle',
      ghostClass: 'dept-sortable-ghost',
      chosenClass: 'dept-sortable-chosen',
      dragClass: 'dept-sortable-drag',
      forceFallback: true,
      fallbackOnBody: true,
      fallbackTolerance: 3,
      onStart: handleDragStart,
      onEnd: handleDragEnd,
      onMove: () => {
        return false
      },
    })
  }

  function destroySortable() {
    if (mouseMoveHandler) {
      document.removeEventListener('mousemove', mouseMoveHandler)
      mouseMoveHandler = null
    }
    if (resolveFrameId != null) {
      cancelAnimationFrame(resolveFrameId)
      resolveFrameId = null
    }
    stopScroll()
    clearExpand()
    clearHoveredRow()
    clearDragSource()
    document.body.style.cursor = ''
    pendingDrop = null
    lastPointer = null
    if (sortableInstance) {
      sortableInstance.destroy()
      sortableInstance = null
    }
    cleanupIndicator()
  }

  return {
    dragState,
    initSortable,
    destroySortable,
  }
}
