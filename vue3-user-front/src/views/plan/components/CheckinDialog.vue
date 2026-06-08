<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="emit('update:modelValue', $event)"
    title="任务打卡"
    width="450px"
    :close-on-click-modal="false"
  >
    <div v-if="task" class="checkin-content">
      <CnSection class="task-info" surface="plain" compact>
        <div class="task-info__header">
          <div class="task-info__copy">
            <CnStatusTag type="brand" size="sm" subtle>今日目标</CnStatusTag>
            <h3>{{ task.planName || '未命名任务' }}</h3>
          </div>
          <div class="task-info__target">
            <strong>{{ task.targetValue || 0 }}</strong>
            <span>{{ task.targetUnit || '' }}</span>
          </div>
        </div>
      </CnSection>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="今日完成量" prop="actualValue">
          <div class="value-input">
            <el-input-number v-model="form.actualValue" :min="0" :max="9999" size="large" />
            <span class="unit">{{ task.targetUnit || '' }}</span>
          </div>
          <div class="quick-buttons">
            <el-button v-for="percent in quickPercents" :key="percent" size="small" @click="setQuickValue(percent)">
              {{ percent }}%
            </el-button>
          </div>
        </el-form-item>

        <el-form-item label="打卡心得（选填）">
          <el-input
            v-model="form.remark"
            type="textarea"
            placeholder="记录今天的感受或收获..."
            :rows="3"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <div class="progress-section">
        <div class="progress-label">
          <span>完成进度</span>
          <CnStatusTag :type="completionTone" size="sm">{{ completionPercent }}%</CnStatusTag>
        </div>
        <el-progress :percentage="completionPercent" :color="progressColor" :stroke-width="12" />
      </div>

      <div v-if="Number(task.currentStreak || 0) > 0" class="streak-tip">
        <CnStatusTag type="warning" size="sm">连续 {{ task.currentStreak }} 天</CnStatusTag>
        <span>稳定执行中，今天补上这一格。</span>
      </div>
    </div>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button
        type="primary"
        @click="handleCheckin"
        :loading="submitting"
        :disabled="!form.actualValue || form.actualValue <= 0"
      >
        <el-icon v-if="!submitting"><Check /></el-icon>
        确认打卡
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Check } from '@element-plus/icons-vue'
import { CnSection, CnStatusTag, type CnTone } from '@/design-system'
import planApi from '@/api/plan'

interface CheckinTask {
  planId: number | string
  planName?: string
  targetValue?: number | string
  targetUnit?: string
  currentStreak?: number
}

interface CheckinResponse {
  currentStreak?: number
}

interface CheckinForm {
  actualValue: number
  remark: string
}

const props = defineProps<{
  modelValue: boolean
  task: CheckinTask | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: []
}>()

const quickPercents = [50, 75, 100, 120]

const formRef = ref<FormInstance | null>(null)
const submitting = ref(false)

const form = ref<CheckinForm>({
  actualValue: 0,
  remark: ''
})

const rules: FormRules<CheckinForm> = {
  actualValue: [{ required: true, message: '请输入完成量', trigger: 'blur' }]
}

watch(
  () => props.modelValue,
  (val) => {
    if (val && props.task) {
      form.value.actualValue = Number(props.task.targetValue || 0)
      form.value.remark = ''
    }
  }
)

const completionPercent = computed(() => {
  const targetValue = Number(props.task?.targetValue || 0)
  if (!targetValue) return 0
  const percent = Math.round((form.value.actualValue / targetValue) * 100)
  return Math.max(0, Math.min(percent, 100))
})

const completionTone = computed<CnTone>(() => {
  const percent = completionPercent.value
  if (percent >= 100) return 'success'
  if (percent >= 80) return 'brand'
  if (percent >= 50) return 'warning'
  return 'danger'
})

const progressColor = computed(() => `var(--cn-color-${completionTone.value === 'brand' ? 'brand-primary' : completionTone.value})`)

const setQuickValue = (percent: number) => {
  const targetValue = Number(props.task?.targetValue || 0)
  if (targetValue) {
    form.value.actualValue = Math.round((targetValue * percent) / 100)
  }
}

const handleCheckin = async () => {
  try {
    await formRef.value?.validate()

    if (form.value.actualValue <= 0) {
      ElMessage.warning('完成量必须大于0')
      return
    }

    if (!props.task) {
      ElMessage.warning('任务信息不存在')
      return
    }

    submitting.value = true

    const response = (await planApi.checkin({
      planId: props.task.planId,
      actualValue: form.value.actualValue,
      remark: form.value.remark
    })) as CheckinResponse

    let message = '打卡成功'
    if (Number(response?.currentStreak || 0) > 1) {
      message += `，已连续打卡 ${response.currentStreak} 天`
    }

    ElMessage.success(message)
    emit('success')
    emit('update:modelValue', false)
  } catch (error) {
    console.error('打卡失败:', error)
    ElMessage.error(error instanceof Error ? error.message : '打卡失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.checkin-content {
  display: grid;
  gap: var(--cn-space-5);
  padding: var(--cn-space-1) 0;
}

.task-info {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 22%, var(--cn-color-border));
}

.task-info__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-4);
  min-width: 0;
}

.task-info__copy {
  display: grid;
  gap: var(--cn-space-2);
  min-width: 0;
}

.task-info__copy h3 {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-size: 18px;
  font-weight: 700;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.task-info__target {
  display: grid;
  justify-items: end;
  flex-shrink: 0;
  color: var(--cn-color-text-secondary);
}

.task-info__target strong {
  color: var(--cn-color-brand-primary);
  font-family: var(--cn-font-heading);
  font-size: 28px;
  line-height: 1;
}

.task-info__target span {
  margin-top: var(--cn-space-1);
  font-size: 13px;
  font-weight: 650;
}

.value-input {
  display: flex;
  align-items: center;
  gap: var(--cn-space-3);
}

.value-input .el-input-number {
  flex: 1;
}

.unit {
  color: var(--cn-color-text-secondary);
  font-size: 15px;
  font-weight: 650;
}

.quick-buttons {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-2);
  margin-top: var(--cn-space-3);
}

.progress-section {
  display: grid;
  gap: var(--cn-space-2);
}

.progress-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  font-weight: 650;
}

.streak-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--cn-space-3);
  padding: var(--cn-space-3);
  border: 1px solid color-mix(in srgb, var(--cn-color-warning) 22%, var(--cn-color-border));
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-warning-soft);
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.5;
}

@media (max-width: 520px) {
  .task-info__header,
  .value-input,
  .streak-tip {
    display: grid;
    justify-items: stretch;
  }

  .task-info__target {
    justify-items: start;
  }
}
</style>
