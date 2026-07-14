<template>
  <section
    class="cn-section"
    :class="[
      `cn-section--${surface}`,
      {
        'cn-section--compact': compact,
        'cn-section--divided': divided
      }
    ]"
  >
    <header v-if="hasHeader" class="cn-section__header">
      <div class="cn-section__copy">
        <h2 v-if="title" class="cn-section__title">{{ title }}</h2>
        <p v-if="description" class="cn-section__description">{{ description }}</p>
      </div>
      <div v-if="hasActions" class="cn-section__actions">
        <slot name="actions" />
      </div>
    </header>

    <div class="cn-section__body">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, useSlots } from 'vue'
import type { CnSectionProps } from '../../types/components'

const props = withDefaults(defineProps<CnSectionProps>(), {
  title: '',
  description: '',
  compact: false,
  surface: 'panel',
  divided: false
})

const slots = useSlots()

const hasActions = computed(() => Boolean(slots.actions))
const hasHeader = computed(() => Boolean(props.title || props.description || slots.actions))
</script>

<style scoped>
.cn-section {
  min-width: 0;
  border-radius: var(--cn-radius-panel);
  transition:
    background-color var(--cn-motion-base) var(--cn-ease-out),
    border-color var(--cn-motion-base) var(--cn-ease-out),
    box-shadow var(--cn-motion-base) var(--cn-ease-out);
}

.cn-section--transparent {
  background: transparent;
}

.cn-section--plain,
.cn-section--panel {
  border: 1px solid var(--cn-panel-border);
  background: var(--cn-panel-bg);
}

.cn-section--panel {
  box-shadow: var(--cn-shadow-card);
}

.cn-section__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--cn-space-4);
  padding: var(--cn-space-5) var(--cn-space-5) 0;
}

.cn-section--compact .cn-section__header {
  padding: var(--cn-space-4) var(--cn-space-4) 0;
}

.cn-section--transparent .cn-section__header,
.cn-section--transparent .cn-section__body {
  padding-right: 0;
  padding-left: 0;
}

.cn-section--divided .cn-section__header {
  padding-bottom: var(--cn-space-4);
  border-bottom: 1px solid var(--cn-color-border-subtle);
}

.cn-section__copy {
  min-width: 0;
}

.cn-section__title {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-size: 17px;
  font-weight: 650;
  line-height: 1.35;
}

.cn-section__description {
  margin: var(--cn-space-1) 0 0;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.cn-section__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--cn-space-2);
  flex-shrink: 0;
}

.cn-section__body {
  min-width: 0;
  padding: var(--cn-space-5);
}

.cn-section--compact .cn-section__body {
  padding: var(--cn-space-4);
}

.cn-section__header + .cn-section__body {
  padding-top: var(--cn-space-4);
}

@media (max-width: 768px) {
  .cn-section__header {
    display: grid;
  }

  .cn-section__actions {
    justify-content: flex-start;
  }
}
</style>
