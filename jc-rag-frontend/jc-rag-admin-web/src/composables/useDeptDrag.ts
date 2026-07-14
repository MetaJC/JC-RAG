import { ref } from 'vue'
import Sortable from 'sortablejs'
import { useHoverHit } from './useHoverHit'
import { useDropPosition } from './useDropPosition'
import { useAutoExpand } from './useAutoExpand'
import { useAutoScroll } from './useAutoScroll'
import { useDropIndicator } from './useDropIndicator'

export type DropPosition = 'before' | 'after' | 'inner' | null

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

export function useDeptDrag(
  getExpandedIds: () => Set<number>,
  toggleExpand: (id: number) => void,
  isDescendantOrSelf: (rootId: number, searchId: number, nodes: any[]) => boolean,
  deptTree: () => any[],
  onDrop: (result: DropResult) => void
) {
  const { findTargetRow } = useHoverHit()
  const { calculatePosition, reset: resetPosition } = useDropPosition(8)
  const { scheduleExpand, clear: clearExpand } = useAutoExpand(600)
  const { start: startScroll, update: updateScroll, stop: stopScroll } = useAutoScroll(50, 20)
  const { init: initIndicator, update: updateIndicator, hide: hideIndicator, cleanup: cleanupIndicator } = useDropIndicator()

  let sortableInstance: Sortable | null = null
  let tableWrapper: HTMLElement | null = null
  let bodyWrapper: HTMLElement | null = null
  let draggedRow: HTMLElement | null = null

  const dragState = ref<DragState>({
    dragging: false,
    draggedId: null,
    targetId: null,
    position: null,
    illegal: false,
  })

  let mouseMoveHandler: ((e: MouseEvent) => void) | null = null

  function isLegalDrop(draggedId: number, targetId: number, position: DropPosition): boolean {
    if (draggedId === targetId) return false
    if (position === 'inner' && isDescendantOrSelf(draggedId, targetId, deptTree())) {
      return false
    }
    return true
  }

  function handleMouseMove(e: MouseEvent) {
    if (!tableWrapper || !bodyWrapper) return

    updateScroll(e.clientY)

    const target = findTargetRow(e.clientX, e.clientY, tableWrapper)
    if (!target) {
      hideIndicator()
      dragState.value = { ...dragState.value, targetId: null, position: null, illegal: true }
      return
    }

    const expandedIds = getExpandedIds()
    const isExpanded = expandedIds.has(target.id)

    scheduleExpand(target.id, target.hasChildren, isExpanded, toggleExpand)

    const insideAllowed = target.hasChildren || target.level > 0

    const posResult = calculatePosition(target.element, e.clientY, target.level, insideAllowed)

    const illegal = !isLegalDrop(dragState.value.draggedId!, posResult.targetId!, posResult.position)

    dragState.value = {
      ...dragState.value,
      targetId: posResult.targetId,
      position: posResult.position,
      illegal,
    }

    updateIndicator({
      position: illegal ? null : posResult.position,
      targetRect: target.element.getBoundingClientRect(),
      indentPx: posResult.indentPx,
      illegal,
    })

    if (illegal) {
      document.body.style.cursor = 'not-allowed'
    } else {
      document.body.style.cursor = 'grabbing'
    }
  }

  function handleDragStart(evt: Sortable.SortableEvent) {
    const item = evt.item as HTMLElement
    if (!item) return

    draggedRow = item
    const draggedId = parseInt(item.getAttribute('data-row-key') || '0', 10) || null

    if (!draggedId) return

    dragState.value = {
      dragging: true,
      draggedId,
      targetId: null,
      position: null,
      illegal: false,
    }

    bodyWrapper = document.querySelector('.dept-table-wrap .el-table__body-wrapper') as HTMLElement
    tableWrapper = document.querySelector('.dept-table-wrap .el-table__body') as HTMLElement

    if (!bodyWrapper) {
      bodyWrapper = document.querySelector('.dept-table-wrap .el-table__body-wrapper') as HTMLElement
    }

    if (bodyWrapper) {
      initIndicator(bodyWrapper)
      startScroll(bodyWrapper)
    }

    mouseMoveHandler = (e: MouseEvent) => handleMouseMove(e)
    document.addEventListener('mousemove', mouseMoveHandler)
  }

  function handleDragEnd() {
    if (mouseMoveHandler) {
      document.removeEventListener('mousemove', mouseMoveHandler)
      mouseMoveHandler = null
    }

    stopScroll()
    hideIndicator()
    clearExpand()
    resetPosition()

    document.body.style.cursor = ''

    if (draggedRow) {
      draggedRow = null
    }

    const { draggedId, targetId, position, illegal } = dragState.value
    if (draggedId && targetId && position && !illegal) {
      onDrop({ draggedId, targetId, position })
    }

    dragState.value = {
      dragging: false,
      draggedId: null,
      targetId: null,
      position: null,
      illegal: false,
    }
  }

  function initSortable() {
    destroySortable()

    const tbody = document.querySelector('.dept-table-wrap .el-table__body-wrapper tbody') as HTMLElement | null
    if (!tbody) return

    sortableInstance = Sortable.create(tbody, {
      animation: 150,
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