<template>
  <el-form class="cn-filter-form" :model="localValue" label-position="top" @submit.prevent="emitSearch">
    <div class="cn-filter-form__grid" :style="gridStyle">
      <el-form-item v-for="field in fields" :key="field.prop" :label="field.label">
        <slot
          v-if="field.type === 'custom'"
          :name="field.slot || field.prop"
          :field="field"
          :value="localValue[field.prop]"
          :set-value="(value: unknown) => setFieldValue(field.prop, value)"
        />

        <el-input
          v-else-if="field.type === 'input'"
          :model-value="localValue[field.prop] as string"
          :placeholder="field.placeholder || `请输入${field.label}`"
          :clearable="field.clearable !== false"
          :disabled="field.disabled"
          @update:model-value="(value: string) => setFieldValue(field.prop, value)"
          @keyup.enter="emitSearch"
        />

        <el-select
          v-else-if="field.type === 'select'"
          :model-value="localValue[field.prop]"
          :placeholder="field.placeholder || `请选择${field.label}`"
          :clearable="field.clearable !== false"
          :multiple="field.multiple"
          :disabled="field.disabled"
          @update:model-value="(value: unknown) => setFieldValue(field.prop, value)"
        >
          <el-option
            v-for="option in field.options || []"
            :key="String(option.value)"
            :label="option.label"
            :value="option.value"
            :disabled="option.disabled"
          />
        </el-select>

        <el-date-picker
          v-else-if="field.type === 'date'"
          :model-value="localValue[field.prop]"
          type="date"
          :placeholder="field.placeholder || `请选择${field.label}`"
          :clearable="field.clearable !== false"
          :disabled="field.disabled"
          value-format="YYYY-MM-DD"
          @update:model-value="(value: unknown) => setFieldValue(field.prop, value)"
        />

        <el-date-picker
          v-else-if="field.type === 'daterange'"
          :model-value="localValue[field.prop]"
          type="daterange"
          :start-placeholder="field.placeholder || '开始日期'"
          end-placeholder="结束日期"
          :clearable="field.clearable !== false"
          :disabled="field.disabled"
          value-format="YYYY-MM-DD"
          @update:model-value="(value: unknown) => setFieldValue(field.prop, value)"
        />

        <el-switch
          v-else-if="field.type === 'switch'"
          :model-value="Boolean(localValue[field.prop])"
          :disabled="field.disabled"
          @update:model-value="(value: boolean) => setFieldValue(field.prop, value)"
        />
      </el-form-item>

      <el-form-item class="cn-filter-form__actions">
        <el-button type="primary" :loading="loading" @click="emitSearch">{{ searchText }}</el-button>
        <el-button :disabled="loading" @click="emitReset">{{ resetText }}</el-button>
        <slot name="actions" />
      </el-form-item>
    </div>
  </el-form>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { CSSProperties } from 'vue'
import type { CnFilterFormProps } from '../../types/components'

const props = withDefaults(defineProps<CnFilterFormProps>(), {
  columns: 4,
  loading: false,
  searchText: '搜索',
  resetText: '重置'
})

const emit = defineEmits<{
  search: []
  reset: []
  'update:modelValue': [value: Record<string, unknown>]
}>()

const localValue = computed(() => props.modelValue)

const gridStyle = computed<CSSProperties>(() => ({
  '--cn-filter-columns': String(props.columns)
}))

const setFieldValue = (prop: string, value: unknown) => {
  emit('update:modelValue', {
    ...props.modelValue,
    [prop]: value
  })
}

const emitSearch = () => {
  emit('search')
}

const emitReset = () => {
  emit('reset')
}
</script>

<style scoped>
.cn-filter-form {
  min-width: 0;
}

.cn-filter-form__grid {
  display: grid;
  grid-template-columns: repeat(var(--cn-filter-columns), minmax(160px, 1fr));
  gap: var(--cn-space-4);
  align-items: end;
}

.cn-filter-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.cn-filter-form :deep(.el-select),
.cn-filter-form :deep(.el-input),
.cn-filter-form :deep(.el-date-editor) {
  width: 100%;
}

.cn-filter-form__actions :deep(.el-form-item__content) {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

@media (max-width: 1180px) {
  .cn-filter-form__grid {
    grid-template-columns: repeat(2, minmax(160px, 1fr));
  }
}

@media (max-width: 680px) {
  .cn-filter-form__grid {
    grid-template-columns: 1fr;
  }
}
</style>
