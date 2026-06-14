<template>
  <section class="cn-empty-state" :class="[`cn-empty-state--${surface}`, `cn-empty-state--${size}`]">
    <div class="cn-empty-state__icon" aria-hidden="true">
      <slot name="icon">{{ icon }}</slot>
    </div>

    <div class="cn-empty-state__copy">
      <h2 class="cn-empty-state__title">{{ title }}</h2>
      <p v-if="description" class="cn-empty-state__description">{{ description }}</p>
    </div>

    <div v-if="hasActions" class="cn-empty-state__actions">
      <slot name="actions" />
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, useSlots } from 'vue'
import type { CnEmptyStateProps } from '../../types/components'

withDefaults(defineProps<CnEmptyStateProps>(), {
  title: '暂无数据',
  description: '',
  icon: 'CN',
  size: 'md',
  surface: 'plain'
})

const slots = useSlots()

const hasActions = computed(() => Boolean(slots.actions))
</script>

<style scoped>
.cn-empty-state {
  display: grid;
  justify-items: center;
  gap: var(--cn-space-4);
  min-width: 0;
  padding: var(--cn-space-8) var(--cn-space-5);
  text-align: center;
  color: var(--cn-color-text-secondary);
}

.cn-empty-state--plain,
.cn-empty-state--panel {
  border: 1px dashed var(--cn-color-border);
  border-radius: var(--cn-radius-panel);
  background: var(--cn-color-bg-surface);
}

.cn-empty-state--panel {
  box-shadow: var(--cn-shadow-card);
}

.cn-empty-state--transparent {
  background: transparent;
}

.cn-empty-state--sm {
  padding: var(--cn-space-5);
}

.cn-empty-state--lg {
  padding: var(--cn-space-10) var(--cn-space-6);
}

.cn-empty-state__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 54px;
  height: 54px;
  border-radius: var(--cn-radius-panel);
  color: var(--cn-color-brand-primary);
  background: var(--cn-color-brand-soft);
  font-family: var(--cn-font-heading);
  font-size: 14px;
  font-weight: 800;
  letter-spacing: 0;
}

.cn-empty-state--sm .cn-empty-state__icon {
  width: 44px;
  height: 44px;
  border-radius: var(--cn-radius-card);
}

.cn-empty-state--lg .cn-empty-state__icon {
  width: 68px;
  height: 68px;
  font-size: 16px;
}

.cn-empty-state__copy {
  display: grid;
  gap: var(--cn-space-2);
  max-width: 520px;
}

.cn-empty-state__title {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-size: 17px;
  font-weight: 650;
  line-height: 1.35;
}

.cn-empty-state__description {
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.cn-empty-state__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: var(--cn-space-2);
}
</style>
