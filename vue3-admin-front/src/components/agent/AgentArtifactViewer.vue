<template>
  <section class="agent-artifact">
    <header class="agent-artifact__header">
      <div>
        <h4>{{ artifact?.title || artifact?.type || '结构化结果' }}</h4>
        <span v-if="artifact?.type">{{ artifact.type }}</span>
      </div>
      <el-tag v-if="tableRows.length" size="small" type="info">
        {{ tableRows.length }} 条
      </el-tag>
    </header>

    <el-table
      v-if="tableRows.length"
      :data="tableRows"
      size="small"
      max-height="300"
      table-layout="fixed"
      stripe
    >
      <el-table-column
        v-for="column in tableColumns"
        :key="column"
        :prop="column"
        :label="column"
        min-width="132"
        show-overflow-tooltip
      >
        <template #default="scope">
          {{ compactValue(scope.row[column]) }}
        </template>
      </el-table-column>
    </el-table>

    <dl v-else-if="summaryEntries.length" class="agent-artifact__summary">
      <div v-for="entry in summaryEntries" :key="entry.key">
        <dt>{{ entry.key }}</dt>
        <dd>{{ compactValue(entry.value) }}</dd>
      </div>
    </dl>

    <pre v-else class="agent-artifact__json">{{ prettyValue(artifact?.data) }}</pre>

    <details v-if="hasNestedData" class="agent-artifact__raw">
      <summary>原始数据</summary>
      <pre>{{ prettyValue(artifact?.data) }}</pre>
    </details>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  artifact: {
    type: Object,
    default: () => ({}),
  },
})

const tableRows = computed(() => {
  const rows = Array.isArray(props.artifact?.data)
    ? props.artifact.data
    : findTabularArray(props.artifact?.data)
  if (!Array.isArray(rows) || !rows.length || !rows.every(isPlainObject)) return []
  return rows.slice(0, 50)
})

const tableColumns = computed(() => {
  const keys = []
  tableRows.value.slice(0, 12).forEach((row) => {
    Object.keys(row).forEach((key) => {
      if (!keys.includes(key) && keys.length < 8) keys.push(key)
    })
  })
  return keys
})

const summaryEntries = computed(() => {
  if (!isPlainObject(props.artifact?.data)) return []
  return Object.entries(props.artifact.data)
    .filter(([, value]) => isCompactValue(value))
    .slice(0, 16)
    .map(([key, value]) => ({ key, value }))
})

const hasNestedData = computed(() => {
  const data = props.artifact?.data
  if (Array.isArray(data)) return Boolean(data.length)
  return isPlainObject(data) && Object.values(data).some((value) => !isCompactValue(value))
})

function findTabularArray(data) {
  if (!isPlainObject(data)) return []
  return Object.values(data).find((value) => Array.isArray(value) && value.length && value.every(isPlainObject)) || []
}

function compactValue(value) {
  if (value === null || value === undefined || value === '') return '-'
  if (typeof value === 'boolean') return value ? '是' : '否'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

function prettyValue(value) {
  if (value === null || value === undefined || value === '') return '-'
  if (typeof value === 'string') return value
  try {
    return JSON.stringify(value, null, 2)
  } catch {
    return String(value)
  }
}

function isCompactValue(value) {
  return value === null || ['string', 'number', 'boolean', 'undefined'].includes(typeof value)
}

function isPlainObject(value) {
  return Object.prototype.toString.call(value) === '[object Object]'
}
</script>

<style scoped>
.agent-artifact {
  display: grid;
  gap: var(--cn-space-3);
  padding: var(--cn-space-3) 0 var(--cn-space-4);
  border-bottom: 1px solid var(--cn-color-border-subtle);
}

.agent-artifact:last-child {
  border-bottom: 0;
}

.agent-artifact__header,
.agent-artifact__header > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-2);
  min-width: 0;
}

.agent-artifact__header > div {
  justify-content: flex-start;
}

.agent-artifact__header h4,
.agent-artifact__header span {
  margin: 0;
}

.agent-artifact__header h4 {
  overflow: hidden;
  color: var(--cn-color-text-primary);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.agent-artifact__header span {
  color: var(--cn-color-text-tertiary);
  font-size: 11px;
}

.agent-artifact__summary {
  display: grid;
  gap: var(--cn-space-2);
  margin: 0;
}

.agent-artifact__summary > div {
  display: grid;
  grid-template-columns: minmax(88px, 0.42fr) minmax(0, 1fr);
  gap: var(--cn-space-3);
  padding-bottom: var(--cn-space-2);
  border-bottom: 1px solid var(--cn-color-border-subtle);
}

.agent-artifact__summary > div:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.agent-artifact__summary dt,
.agent-artifact__summary dd {
  min-width: 0;
  margin: 0;
  font-size: 12px;
  line-height: 1.55;
  overflow-wrap: anywhere;
}

.agent-artifact__summary dt {
  color: var(--cn-color-text-tertiary);
}

.agent-artifact__summary dd {
  color: var(--cn-color-text-secondary);
}

.agent-artifact__json,
.agent-artifact__raw pre {
  max-height: 300px;
  overflow: auto;
  margin: 0;
  padding: var(--cn-space-3);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-control);
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-text-secondary);
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 11px;
  line-height: 1.6;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.agent-artifact__raw summary {
  cursor: pointer;
  color: var(--cn-color-brand-primary);
  font-size: 12px;
  font-weight: 700;
}

.agent-artifact__raw pre {
  margin-top: var(--cn-space-2);
}
</style>
