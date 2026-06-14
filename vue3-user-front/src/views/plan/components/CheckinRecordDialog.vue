<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="emit('update:modelValue', $event)"
    :title="`${planName || '计划'} - 打卡记录`"
    width="650px"
    :close-on-click-modal="true"
  >
    <div class="record-content">
      <div class="stats-summary">
        <CnStatCard title="累计打卡" :value="totalRecords" description="当前计划记录数" tone="brand" />
        <CnStatCard title="达标率" :value="completionRate" unit="%" description="完成度达到 100% 的比例" tone="success" />
      </div>

      <div v-loading="loading" class="record-list">
        <CnEmptyState
          v-if="records.length === 0 && !loading"
          title="暂无打卡记录"
          description="完成一次打卡后，执行记录会显示在这里。"
          icon="CK"
          size="sm"
          surface="transparent"
        />

        <article v-for="record in records" :key="record.id" class="record-item">
          <div class="record-date">
            <div class="date-day">{{ formatDay(record.checkinDate) }}</div>
            <div class="date-weekday">{{ formatWeekday(record.checkinDate) }}</div>
          </div>

          <div class="record-main">
            <div class="record-value">
              <span class="value">{{ record.actualValue }}</span>
              <span class="target">/ {{ record.targetValue }} {{ record.targetUnit || targetUnit }}</span>
              <CnStatusTag :type="record.completionRate >= 100 ? 'success' : 'warning'" size="sm">
                {{ record.completionRate }}%
              </CnStatusTag>
            </div>
            <div v-if="record.remark" class="record-remark">
              {{ record.remark }}
            </div>
            <div class="record-time">
              {{ formatTime(record.checkinTime) }}
            </div>
          </div>

          <div class="record-status">
            <el-icon v-if="record.completionRate >= 100" class="success"><SuccessFilled /></el-icon>
            <el-icon v-else class="partial"><WarningFilled /></el-icon>
          </div>
        </article>
      </div>

      <div v-if="hasMore" class="load-more">
        <el-button @click="loadMore" :loading="loading" text>加载更多</el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { SuccessFilled, WarningFilled } from '@element-plus/icons-vue'
import { CnEmptyState, CnStatCard, CnStatusTag } from '@/design-system'
import planApi from '@/api/plan'

interface CheckinRecord {
  id: number | string
  checkinDate?: string
  checkinTime?: string
  actualValue: number | string
  targetValue: number | string
  targetUnit?: string
  completionRate: number
  remark?: string
}

const props = defineProps<{
  modelValue: boolean
  planId: number | null
  planName: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const loading = ref(false)
const records = ref<CheckinRecord[]>([])
const targetUnit = ref('')

const totalRecords = computed(() => records.value.length)
const hasMore = computed(() => false)
const completionRate = computed(() => {
  if (records.value.length === 0) return 0
  const completed = records.value.filter((record) => Number(record.completionRate || 0) >= 100).length
  return Math.round((completed / records.value.length) * 100)
})

watch(
  () => props.modelValue,
  (val) => {
    if (val && props.planId) {
      records.value = []
      targetUnit.value = ''
      loadRecords()
    }
  }
)

const loadRecords = async () => {
  if (!props.planId) return

  loading.value = true
  try {
    const response = (await planApi.getCheckinRecords(props.planId)) as CheckinRecord[]
    records.value = response || []

    if (records.value.length > 0) {
      targetUnit.value = records.value[0].targetUnit || ''
    }
  } catch (error) {
    console.error('加载打卡记录失败:', error)
  } finally {
    loading.value = false
  }
}

const loadMore = () => {
  // 后端当前返回全部记录，这里保留入口以兼容后续分页。
}

const formatDay = (dateStr?: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.getDate()
}

const formatWeekday = (dateStr?: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const weekdays = ['日', '一', '二', '三', '四', '五', '六']
  const month = date.getMonth() + 1
  return `${month}月 周${weekdays[date.getDay()]}`
}

const formatTime = (timeStr?: string) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped>
.record-content {
  display: grid;
  gap: var(--cn-space-5);
  min-height: 300px;
}

.stats-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.record-list {
  display: grid;
  gap: var(--cn-space-3);
  max-height: 400px;
  overflow-y: auto;
  padding-right: var(--cn-space-1);
}

.record-item {
  display: flex;
  align-items: center;
  gap: var(--cn-space-4);
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
  transition:
    background-color var(--cn-motion-base) var(--cn-ease-out),
    border-color var(--cn-motion-base) var(--cn-ease-out);
}

.record-item:hover {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 24%, var(--cn-color-border));
  background: var(--cn-color-bg-surface);
}

.record-date {
  display: grid;
  justify-items: center;
  width: 64px;
  flex-shrink: 0;
}

.date-day {
  color: var(--cn-color-brand-primary);
  font-family: var(--cn-font-heading);
  font-size: 25px;
  font-weight: 700;
  line-height: 1;
}

.date-weekday {
  margin-top: var(--cn-space-1);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  font-weight: 650;
}

.record-main {
  display: grid;
  gap: var(--cn-space-1);
  flex: 1;
  min-width: 0;
}

.record-value {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.value {
  color: var(--cn-color-text-primary);
  font-size: 18px;
  font-weight: 700;
}

.target,
.record-time {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.record-remark {
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.record-status .el-icon {
  font-size: 24px;
}

.record-status .success {
  color: var(--cn-color-success);
}

.record-status .partial {
  color: var(--cn-color-warning);
}

.load-more {
  display: flex;
  justify-content: center;
  padding: var(--cn-space-2) 0 0;
}

@media (max-width: 640px) {
  .stats-summary {
    grid-template-columns: 1fr;
  }

  .record-item {
    align-items: flex-start;
  }
}
</style>
