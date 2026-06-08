<template>
  <el-dialog
    :model-value="modelValue"
    title="转为学习资产"
    width="min(520px, calc(100vw - var(--cn-space-8)))"
    @close="handleClose"
  >
    <div class="transform-dialog">
      <CnSection surface="plain" compact>
        <div class="source-card">
          <div class="source-copy">
            <div class="source-label">来源内容</div>
            <div class="source-title">{{ sourceTitle || '未命名内容' }}</div>
          </div>
          <CnStatusTag type="brand" size="sm" subtle>{{ sourceTypeLabel }}</CnStatusTag>
        </div>
      </CnSection>

      <el-form label-position="top">
        <el-form-item label="转化风格">
          <el-radio-group v-model="form.transformMode">
            <el-radio-button label="quick">快速提炼</el-radio-button>
            <el-radio-button label="study">学习复习</el-radio-button>
            <el-radio-button label="interview">面试训练</el-radio-button>
            <el-radio-button label="practice">实战拆解</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="目标资产">
          <el-checkbox-group v-model="form.targetTypes" class="target-grid">
            <el-checkbox label="flashcard">闪卡卡组</el-checkbox>
            <el-checkbox label="knowledge_node">知识节点候选</el-checkbox>
            <el-checkbox label="practice_plan">练习清单</el-checkbox>
            <el-checkbox label="interview_question">面试题草稿</el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <el-form-item label="补充标签">
          <el-select
            v-model="form.extraTags"
            multiple
            filterable
            allow-create
            default-first-option
            class="tag-select"
            placeholder="输入标签后回车"
          >
            <el-option
              v-for="tag in mergedTagOptions"
              :key="tag"
              :label="tag"
              :value="tag"
            />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-switch v-model="form.useExistingSummary" />
          <span class="summary-switch-text">优先复用已有 AI 摘要</span>
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          开始转化
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { CnSection, CnStatusTag } from '@/design-system'
import learningAssetApi from '@/api/learningAssets'

type SourceType = 'blog' | 'community' | 'codepen' | 'mock_interview' | string

interface TransformForm {
  transformMode: string
  targetTypes: string[]
  extraTags: string[]
  useExistingSummary: boolean
}

const props = withDefaults(defineProps<{
  modelValue?: boolean
  sourceType: SourceType
  sourceId: number | string
  sourceTitle?: string
  defaultTags?: string[]
}>(), {
  modelValue: false,
  sourceTitle: '',
  defaultTags: () => []
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: [result: unknown]
}>()

const form = reactive<TransformForm>({
  transformMode: 'study',
  targetTypes: ['flashcard', 'practice_plan'],
  extraTags: [],
  useExistingSummary: true
})

const submitting = computed(() => state.submitting)

const state = reactive({
  submitting: false
})

const sourceTypeLabel = computed(() => {
  const map: Record<string, string> = {
    blog: '博客文章',
    community: '社区帖子',
    codepen: 'CodePen 作品',
    mock_interview: '模拟面试报告'
  }
  return map[props.sourceType] || props.sourceType
})

const mergedTagOptions = computed(() => {
  return Array.from(new Set([...(props.defaultTags || []), ...(form.extraTags || [])]))
})

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      form.extraTags = [...(props.defaultTags || [])]
    }
  }
)

const handleClose = () => {
  emit('update:modelValue', false)
}

const handleSubmit = async () => {
  if (!form.targetTypes.length) {
    ElMessage.warning('请至少选择一种目标资产')
    return
  }

  state.submitting = true
  try {
    const result = await learningAssetApi.convert({
      sourceType: props.sourceType,
      sourceId: Number(props.sourceId),
      transformMode: form.transformMode,
      targetTypes: form.targetTypes,
      useExistingSummary: form.useExistingSummary,
      extraTags: form.extraTags
    })
    ElMessage.success('转化成功，已生成候选学习资产')
    emit('success', result)
    handleClose()
  } catch (error) {
    console.error('学习资产转化失败', error)
  } finally {
    state.submitting = false
  }
}
</script>

<style scoped>
.transform-dialog {
  display: flex;
  flex-direction: column;
  gap: var(--cn-space-5);
}

.source-card {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--cn-space-4);
  min-width: 0;
}

.source-copy {
  min-width: 0;
}

.source-label {
  font-size: 12px;
  color: var(--cn-color-text-tertiary);
  margin-bottom: var(--cn-space-1);
}

.source-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--cn-color-text-primary);
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.target-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cn-space-3);
}

.tag-select {
  width: 100%;
}

.summary-switch-text {
  margin-left: var(--cn-space-3);
  color: var(--cn-color-text-secondary);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--cn-space-3);
}

@media (max-width: 640px) {
  .source-card {
    display: grid;
  }

  .target-grid {
    grid-template-columns: 1fr;
  }
}
</style>
