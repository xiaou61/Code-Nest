<template>
  <div class="mastery-selector" v-if="visible">
    <el-card shadow="hover" class="mastery-card">
      <div class="mastery-header">
        <span class="mastery-title">这道题你掌握得如何？</span>
        <el-button v-if="masteryInfo" text size="small" @click="showHistory = !showHistory">
          {{ showHistory ? '收起' : '历史记录' }}
        </el-button>
      </div>

      <!-- 历史记录 -->
      <div v-if="showHistory && masteryInfo" class="mastery-history">
        <CnStatusTag :type="getLevelType(masteryInfo.masteryLevel)" size="sm">
          上次标记：{{ masteryInfo.masteryLevelText }}
        </CnStatusTag>
        <span class="history-text">
          已复习 {{ masteryInfo.reviewCount }} 次
          <template v-if="masteryInfo.lastReviewTime">
            · {{ formatTime(masteryInfo.lastReviewTime) }}
          </template>
        </span>
      </div>

      <!-- 掌握度选项 -->
      <div class="mastery-options">
        <div 
          v-for="option in masteryOptions" 
          :key="option.level"
          class="mastery-option"
          :class="{ 
            'selected': selectedLevel === option.level,
            'was-selected': masteryInfo?.masteryLevel === option.level && selectedLevel !== option.level
          }"
          @click="selectLevel(option.level)"
        >
          <span class="option-code">{{ option.code }}</span>
          <span class="option-text">{{ option.text }}</span>
        </div>
      </div>

      <!-- 提交按钮 -->
      <div class="mastery-actions">
        <el-button 
          type="primary" 
          :loading="submitting"
          :disabled="selectedLevel === null"
          @click="submitMastery"
        >
          确认标记
        </el-button>
        <CnStatusTag v-if="lastResult" type="success" size="sm">
          下次复习：{{ lastResult.nextReviewDays }} 天后
        </CnStatusTag>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { CnStatusTag } from '@/design-system'
import { interviewApi } from '@/api/interview'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

interface MasteryInfo {
  questionId: number
  masteryLevel: number
  masteryLevelText?: string
  reviewCount?: number
  lastReviewTime?: string
  nextReviewDays?: number
}

type StatusTone = 'brand' | 'success' | 'warning' | 'danger' | 'info' | 'neutral'

const props = withDefaults(defineProps<{
  questionId: number
  questionSetId: number
  visible?: boolean
}>(), {
  visible: false
})

const emit = defineEmits<{
  (e: 'marked', result: MasteryInfo): void
}>()

// 掌握度选项
const masteryOptions = [
  { level: 1, code: 'L1', text: '不会' },
  { level: 2, code: 'L2', text: '模糊' },
  { level: 3, code: 'L3', text: '熟悉' },
  { level: 4, code: 'L4', text: '已掌握' }
]

// 状态
const selectedLevel = ref<number | null>(null)
const submitting = ref(false)
const masteryInfo = ref<MasteryInfo | null>(null)
const showHistory = ref(false)
const lastResult = ref<MasteryInfo | null>(null)

// 获取当前掌握度
const fetchMastery = async () => {
  try {
    const res = await interviewApi.getMastery(props.questionId)
    // request拦截器已经提取了data，res直接就是数据内容
    if (res && res.questionId) {
      masteryInfo.value = res as MasteryInfo
      showHistory.value = true
    } else {
      masteryInfo.value = null
      showHistory.value = false
    }
  } catch (error) {
    // 未标记过的题目会返回null，这是正常情况
    masteryInfo.value = null
    showHistory.value = false
  }
}

// 选择掌握度
const selectLevel = (level: number) => {
  selectedLevel.value = level
}

// 提交掌握度
const submitMastery = async () => {
  if (selectedLevel.value === null) {
    ElMessage.warning('请选择掌握程度')
    return
  }

  submitting.value = true
  try {
    const res = await interviewApi.markMastery({
      questionId: props.questionId,
      questionSetId: props.questionSetId,
      masteryLevel: selectedLevel.value,
      isReview: masteryInfo.value !== null
    })

    // request拦截器已经提取了data，res直接就是数据内容
    if (res && res.questionId) {
      lastResult.value = res as MasteryInfo
      masteryInfo.value = res as MasteryInfo
      
      const levelText = masteryOptions.find(o => o.level === selectedLevel.value)?.text
      ElMessage.success(`已标记为"${levelText}"，${res.nextReviewDays}天后复习`)
      
      emit('marked', res)
    }
  } catch (error) {
    ElMessage.error('标记失败，请重试')
    console.error('标记掌握度失败', error)
  } finally {
    submitting.value = false
  }
}

// 格式化时间
const formatTime = (time: string) => {
  return dayjs(time).fromNow()
}

// 获取等级对应的tag类型
const getLevelType = (level: number): StatusTone => {
  const types: StatusTone[] = ['neutral', 'danger', 'warning', 'info', 'success']
  return types[level] || 'neutral'
}

// 监听questionId变化
watch(() => props.questionId, () => {
  selectedLevel.value = null
  lastResult.value = null
  if (props.visible) {
    fetchMastery()
  }
}, { immediate: true })

watch(() => props.visible, (val) => {
  if (val) {
    fetchMastery()
  }
})
</script>

<style scoped>
.mastery-selector {
  margin-top: var(--cn-space-5);
  animation: fadeInUp var(--cn-motion-base) var(--cn-ease-out);
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.mastery-card {
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-panel);
  background: var(--cn-color-bg-surface);
  box-shadow: var(--cn-shadow-card);
}

:deep(.el-card__body) {
  padding: var(--cn-space-4) var(--cn-space-5);
}

.mastery-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--cn-space-3);
  margin-bottom: var(--cn-space-3);
}

.mastery-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--cn-color-text-primary);
}

.mastery-history {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
  margin-bottom: var(--cn-space-3);
  padding: var(--cn-space-2) var(--cn-space-3);
  background: var(--cn-color-bg-surface-muted);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
}

.history-text {
  font-size: 13px;
  color: var(--cn-color-text-tertiary);
}

.mastery-options {
  display: flex;
  gap: var(--cn-space-3);
  margin-bottom: var(--cn-space-4);
}

.mastery-option {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--cn-space-3) var(--cn-space-2);
  background: var(--cn-color-bg-surface);
  border: 2px solid transparent;
  border-radius: var(--cn-radius-card);
  cursor: pointer;
  transition:
    transform var(--cn-motion-fast) var(--cn-ease-out),
    border-color var(--cn-motion-fast) var(--cn-ease-out),
    background-color var(--cn-motion-fast) var(--cn-ease-out),
    box-shadow var(--cn-motion-fast) var(--cn-ease-out);
}

.mastery-option:hover {
  transform: translateY(-2px);
  box-shadow: var(--cn-shadow-sm);
}

.mastery-option.selected {
  border-color: var(--cn-color-brand-primary);
  background: var(--cn-color-brand-soft);
}

.mastery-option.was-selected {
  border-color: var(--cn-color-border);
  border-style: dashed;
}

.mastery-option:nth-child(1).selected {
  border-color: var(--cn-color-danger);
  background: var(--cn-color-danger-soft);
}

.mastery-option:nth-child(2).selected {
  border-color: var(--cn-color-warning);
  background: var(--cn-color-warning-soft);
}

.mastery-option:nth-child(3).selected {
  border-color: var(--cn-color-info);
  background: var(--cn-color-info-soft);
}

.mastery-option:nth-child(4).selected {
  border-color: var(--cn-color-success);
  background: var(--cn-color-success-soft);
}

.option-code {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 34px;
  height: 28px;
  margin-bottom: var(--cn-space-1);
  border-radius: var(--cn-radius-pill);
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  font-weight: 800;
}

.option-text {
  font-size: 13px;
  color: var(--cn-color-text-secondary);
  font-weight: 500;
}

.mastery-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--cn-space-3);
}

/* 移动端适配 */
@media (max-width: 768px) {
  .mastery-options {
    gap: var(--cn-space-2);
  }

  .mastery-option {
    padding: var(--cn-space-3) var(--cn-space-1);
  }

  .option-code {
    min-width: 30px;
  }

  .option-text {
    font-size: 12px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .mastery-selector,
  .mastery-option {
    animation: none;
    transition: none;
  }
}
</style>
