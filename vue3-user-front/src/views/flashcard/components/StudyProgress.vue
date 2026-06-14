<template>
  <div class="study-progress">
    <div class="progress-bar">
      <div class="progress-fill" :style="{ '--progress-size': progressPercent + '%' }"></div>
    </div>
    <div class="progress-info">
      <div class="progress-left">
        <span class="current">{{ current }}</span>
        <span class="separator">/</span>
        <span class="total">{{ total }}</span>
      </div>
      <div class="progress-right">
        <CnStatusTag v-if="newCount > 0" type="success" size="sm">新卡 {{ newCount }}</CnStatusTag>
        <CnStatusTag v-if="dueCount > 0" type="warning" size="sm">复习 {{ dueCount }}</CnStatusTag>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { CnStatusTag } from '@/design-system'

const props = withDefaults(defineProps<{
  current?: number
  total?: number
  newCount?: number
  dueCount?: number
}>(), {
  current: 0,
  total: 0,
  newCount: 0,
  dueCount: 0
})

const progressPercent = computed(() => {
  if (props.total === 0) return 0
  return Math.round(props.current / props.total * 100)
})
</script>

<style lang="scss" scoped>
.study-progress {
  background: var(--cn-color-bg-surface);
  border-radius: var(--cn-radius-panel);
  padding: var(--cn-space-4) var(--cn-space-5);
  border: 1px solid var(--cn-color-border-subtle);
  box-shadow: var(--cn-shadow-card);
}

.progress-bar {
  height: 8px;
  background: var(--cn-color-bg-surface-muted);
  border-radius: var(--cn-radius-pill);
  overflow: hidden;
  margin-bottom: var(--cn-space-3);
  
  .progress-fill {
    width: var(--progress-size);
    height: 100%;
    background: color-mix(in srgb, var(--cn-color-brand-primary) 64%, var(--cn-color-success));
    border-radius: var(--cn-radius-pill);
    transition: width var(--cn-motion-base) var(--cn-ease-out);
  }
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.progress-left {
  font-size: 16px;
  color: var(--cn-color-text-primary);
  
  .current {
    font-weight: 700;
    color: var(--cn-color-brand-primary);
  }
  
  .separator {
    margin: 0 4px;
    color: var(--cn-color-text-secondary);
  }
  
  .total {
    color: var(--cn-color-text-secondary);
  }
}

.progress-right {
  display: flex;
  gap: var(--cn-space-4);
}

@media (prefers-reduced-motion: reduce) {
  .progress-fill {
    transition: none;
  }
}
</style>
