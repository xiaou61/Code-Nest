<template>
  <div class="mastery-selector" v-if="visible">
    <el-card shadow="hover" class="mastery-card">
      <div class="mastery-header">
        <span class="mastery-title">📊 这道题你掌握得如何？</span>
        <el-button v-if="masteryInfo" text size="small" @click="showHistory = !showHistory">
          {{ showHistory ? '收起' : '历史记录' }}
        </el-button>
      </div>

      <!-- 历史记录 -->
      <div v-if="showHistory && masteryInfo" class="mastery-history">
        <el-tag :type="getLevelType(masteryInfo.masteryLevel)" size="small">
          上次标记：{{ masteryInfo.masteryLevelText }}
        </el-tag>
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
          <span class="option-icon">{{ option.icon }}</span>
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
        <span v-if="lastResult" class="result-tip">
          ✅ 下次复习：{{ lastResult.nextReviewDays }} 天后
        </span>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { interviewApi } from '@/api/interview'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const props = defineProps({
  questionId: {
    type: Number,
    required: true
  },
  questionSetId: {
    type: Number,
    required: true
  },
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['marked'])

// 掌握度选项
const masteryOptions = [
  { level: 1, icon: '❌', text: '不会', color: '#F56C6C' },
  { level: 2, icon: '🤔', text: '模糊', color: '#E6A23C' },
  { level: 3, icon: '😊', text: '熟悉', color: '#409EFF' },
  { level: 4, icon: '✅', text: '已掌握', color: '#67C23A' }
]

// 状态
const selectedLevel = ref(null)
const submitting = ref(false)
const masteryInfo = ref(null)
const showHistory = ref(false)
const lastResult = ref(null)

// 获取当前掌握度
const fetchMastery = async () => {
  try {
    const res = await interviewApi.getMastery(props.questionId)
    // request拦截器已经提取了data，res直接就是数据内容
    if (res && res.questionId) {
      masteryInfo.value = res
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
const selectLevel = (level) => {
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
      lastResult.value = res
      masteryInfo.value = res
      
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
const formatTime = (time) => {
  return dayjs(time).fromNow()
}

// 获取等级对应的tag类型
const getLevelType = (level) => {
  const types = ['', 'danger', 'warning', 'primary', 'success']
  return types[level] || ''
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
  margin-top: 20px;
  animation: fadeInUp 0.3s ease;
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
  border-radius: 14px;
  background: linear-gradient(135deg, #f8f7ff 0%, #f0eeff 100%);
}

:deep(.el-card__body) {
  padding: 16px 20px;
}

.mastery-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.mastery-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.mastery-history {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #fff;
  border-radius: 8px;
}

.history-text {
  font-size: 13px;
  color: #909399;
}

.mastery-options {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.mastery-option {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 8px;
  background: #fff;
  border: 2px solid transparent;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.mastery-option:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.mastery-option.selected {
  border-color: #6c63ff;
  background: #f5f3ff;
}

.mastery-option.was-selected {
  border-color: #dcdfe6;
  border-style: dashed;
}

.mastery-option:nth-child(1).selected {
  border-color: #F56C6C;
  background: #fef0f0;
}

.mastery-option:nth-child(2).selected {
  border-color: #E6A23C;
  background: #fdf6ec;
}

.mastery-option:nth-child(3).selected {
  border-color: #6c63ff;
  background: #f5f3ff;
}

.mastery-option:nth-child(4).selected {
  border-color: #67C23A;
  background: #f0f9eb;
}

.option-icon {
  font-size: 24px;
  margin-bottom: 4px;
}

.option-text {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}

.mastery-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.result-tip {
  font-size: 13px;
  color: #67C23A;
  font-weight: 500;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .mastery-options {
    gap: 8px;
  }

  .mastery-option {
    padding: 10px 4px;
  }

  .option-icon {
    font-size: 20px;
  }

  .option-text {
    font-size: 12px;
  }
}
</style>
