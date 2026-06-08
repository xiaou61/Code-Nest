<template>
  <div class="cn-toolbar" :class="[`cn-toolbar--${align}`, { 'cn-toolbar--dense': dense }]">
    <div v-if="title || description || hasMeta" class="cn-toolbar__copy">
      <p v-if="title" class="cn-toolbar__title">{{ title }}</p>
      <p v-if="description" class="cn-toolbar__description">{{ description }}</p>
      <div v-if="hasMeta" class="cn-toolbar__meta">
        <slot name="meta" />
      </div>
    </div>

    <div class="cn-toolbar__actions">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, useSlots } from 'vue'
import type { CnToolbarProps } from '../../types/components'

withDefaults(defineProps<CnToolbarProps>(), {
  title: '',
  description: '',
  dense: false,
  align: 'end'
})

const slots = useSlots()
const hasMeta = computed(() => Boolean(slots.meta))
</script>

<style scoped>
.cn-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--cn-space-4);
  min-width: 0;
}

.cn-toolbar--center {
  align-items: center;
}

.cn-toolbar--end {
  align-items: flex-end;
}

.cn-toolbar--dense {
  gap: var(--cn-space-3);
}

.cn-toolbar__copy {
  display: grid;
  gap: var(--cn-space-1);
  min-width: 0;
}

.cn-toolbar__title {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-size: 15px;
  font-weight: 650;
  line-height: 1.35;
}

.cn-toolbar__description {
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.55;
}

.cn-toolbar__meta,
.cn-toolbar__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
  min-width: 0;
}

.cn-toolbar__actions {
  justify-content: flex-end;
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .cn-toolbar {
    display: grid;
  }

  .cn-toolbar__actions {
    justify-content: flex-start;
  }
}
</style>
