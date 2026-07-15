<template>
  <section class="cn-data-table" :class="{ 'cn-data-table--border': border }">
    <div v-if="hasToolbar" class="cn-data-table__toolbar">
      <slot name="toolbar" />
    </div>

    <el-table
      v-loading="loading"
      :data="data"
      :row-key="rowKey"
      :stripe="stripe"
      :border="border"
      class="cn-data-table__table"
      @selection-change="(selection: unknown[]) => emit('selection-change', selection)"
      @sort-change="(payload: unknown) => emit('sort-change', payload)"
      @row-click="(row: unknown, column: unknown, event: Event) => emit('row-click', row, column, event)"
    >
      <el-table-column
        v-for="column in columns"
        :key="columnKey(column)"
        :type="column.type"
        :prop="column.prop as string"
        :label="column.label"
        :width="column.width"
        :min-width="column.minWidth"
        :align="column.align"
        :fixed="column.fixed"
        :formatter="column.formatter as any"
        :show-overflow-tooltip="column.showOverflowTooltip"
        :sortable="column.sortable"
      >
        <template v-if="column.slot" #default="scope">
          <slot :name="column.slot" v-bind="scope" />
        </template>
      </el-table-column>

      <template #empty>
        <slot name="empty">
          <CnEmptyState
            :title="emptyTitle"
            :description="emptyDescription"
            :icon="emptyIcon"
            surface="transparent"
            size="sm"
          />
        </slot>
      </template>
    </el-table>

    <div v-if="pagination" class="cn-data-table__pagination">
      <el-pagination
        :current-page="pagination.page"
        :page-size="pagination.pageSize"
        :page-sizes="pagination.pageSizes || [10, 20, 50, 100]"
        :total="pagination.total"
        :layout="pagination.layout || 'total, sizes, prev, pager, next, jumper'"
        :background="pagination.background"
        :disabled="pagination.disabled || loading"
        @update:current-page="(page: number) => emit('page-change', page)"
        @update:page-size="(size: number) => emit('page-size-change', size)"
      />
    </div>
  </section>
</template>

<script setup lang="ts" generic="T extends Record<string, unknown> = Record<string, unknown>">
import { computed, useSlots } from 'vue'
import CnEmptyState from './CnEmptyState.vue'
import type { CnDataTableProps, CnTableColumn } from '../../types/components'

withDefaults(defineProps<CnDataTableProps<T>>(), {
  loading: false,
  pagination: null,
  rowKey: 'id',
  stripe: true,
  border: false,
  emptyTitle: '暂无数据',
  emptyDescription: '当前条件下没有可展示的数据。',
  emptyIcon: 'DT'
})

const emit = defineEmits<{
  'selection-change': [selection: unknown[]]
  'sort-change': [payload: unknown]
  'row-click': [row: unknown, column: unknown, event: Event]
  'page-change': [page: number]
  'page-size-change': [size: number]
}>()

const slots = useSlots()
const hasToolbar = computed(() => Boolean(slots.toolbar))

const columnKey = (column: CnTableColumn<T>) => {
  return `${column.type || 'data'}-${String(column.prop || column.label || column.slot)}`
}
</script>

<style scoped>
.cn-data-table {
  min-width: 0;
  overflow: hidden;
  border-radius: var(--cn-radius-panel);
}

.cn-data-table__toolbar {
  padding: var(--cn-space-4);
  border-bottom: 1px solid var(--cn-table-border);
  background: var(--cn-color-bg-surface);
}

.cn-data-table__table {
  width: 100%;
}

.cn-data-table :deep(.el-table__header th) {
  background: var(--cn-table-header-bg);
  color: var(--cn-color-text-secondary);
  font-weight: 650;
}

.cn-data-table :deep(.el-table__row:hover > td.el-table__cell) {
  background: var(--cn-color-bg-surface-muted);
}

.cn-data-table__pagination {
  display: flex;
  justify-content: flex-end;
  padding: var(--cn-space-4);
  overflow-x: auto;
  border-top: 1px solid var(--cn-table-border);
  background: var(--cn-color-bg-surface);
}

@media (max-width: 768px) {
  .cn-data-table__toolbar,
  .cn-data-table__pagination {
    padding: var(--cn-space-3);
  }

  .cn-data-table__pagination {
    justify-content: flex-start;
  }
}
</style>
