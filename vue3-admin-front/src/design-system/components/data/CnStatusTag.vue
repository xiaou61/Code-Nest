<template>
  <span
    class="cn-status-tag"
    :class="[
      `cn-status-tag--${type}`,
      `cn-status-tag--${size}`,
      {
        'cn-status-tag--subtle': subtle,
        'cn-status-tag--dotless': !dot
      }
    ]"
  >
    <span v-if="dot" class="cn-status-tag__dot" aria-hidden="true" />
    <span class="cn-status-tag__label">
      <slot>{{ label }}</slot>
    </span>
  </span>
</template>

<script setup lang="ts">
import type { CnStatusTagProps } from '../../types/components'

withDefaults(defineProps<CnStatusTagProps>(), {
  label: '',
  type: 'neutral',
  size: 'md',
  dot: true,
  subtle: false
})
</script>

<style scoped>
.cn-status-tag {
  --cn-status-color: var(--cn-color-text-secondary);
  --cn-status-bg: var(--cn-color-bg-surface-muted);
  --cn-status-border: var(--cn-color-border-subtle);

  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  max-width: 100%;
  min-width: 0;
  border: 1px solid var(--cn-status-border);
  border-radius: var(--cn-radius-pill);
  background: var(--cn-status-bg);
  color: var(--cn-status-color);
  font-weight: 700;
  line-height: 1;
  vertical-align: middle;
  white-space: nowrap;
}

.cn-status-tag--sm {
  min-height: 22px;
  padding: 0 8px;
  font-size: 12px;
}

.cn-status-tag--md {
  min-height: 26px;
  padding: 0 10px;
  font-size: 12px;
}

.cn-status-tag--lg {
  min-height: 32px;
  padding: 0 12px;
  font-size: 13px;
}

.cn-status-tag__dot {
  width: 7px;
  height: 7px;
  border-radius: var(--cn-radius-pill);
  background: currentColor;
  box-shadow: 0 0 0 3px color-mix(in srgb, currentColor 14%, transparent);
  flex-shrink: 0;
}

.cn-status-tag__label {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cn-status-tag--brand {
  --cn-status-color: var(--cn-color-brand-primary);
  --cn-status-bg: var(--cn-color-brand-soft);
  --cn-status-border: color-mix(in srgb, var(--cn-color-brand-primary) 24%, var(--cn-color-border-subtle));
}

.cn-status-tag--success {
  --cn-status-color: var(--cn-color-success);
  --cn-status-bg: var(--cn-color-success-soft);
  --cn-status-border: color-mix(in srgb, var(--cn-color-success) 24%, var(--cn-color-border-subtle));
}

.cn-status-tag--warning {
  --cn-status-color: var(--cn-color-warning);
  --cn-status-bg: var(--cn-color-warning-soft);
  --cn-status-border: color-mix(in srgb, var(--cn-color-warning) 24%, var(--cn-color-border-subtle));
}

.cn-status-tag--danger {
  --cn-status-color: var(--cn-color-danger);
  --cn-status-bg: var(--cn-color-danger-soft);
  --cn-status-border: color-mix(in srgb, var(--cn-color-danger) 24%, var(--cn-color-border-subtle));
}

.cn-status-tag--info {
  --cn-status-color: var(--cn-color-info);
  --cn-status-bg: var(--cn-color-info-soft);
  --cn-status-border: color-mix(in srgb, var(--cn-color-info) 24%, var(--cn-color-border-subtle));
}

.cn-status-tag--neutral {
  --cn-status-color: var(--cn-color-text-secondary);
  --cn-status-bg: var(--cn-color-bg-surface-muted);
  --cn-status-border: var(--cn-color-border-subtle);
}

.cn-status-tag--subtle {
  background: transparent;
}
</style>
