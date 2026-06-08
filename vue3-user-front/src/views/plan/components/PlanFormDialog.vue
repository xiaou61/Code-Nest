<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="emit('update:modelValue', $event)"
    :title="isEdit ? '编辑计划' : '创建计划'"
    width="600px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="form-dialog-shell">
      <CnSection surface="plain" compact class="form-summary">
        <div class="form-summary__content">
          <div>
            <CnStatusTag :type="isEdit ? 'info' : 'brand'" size="sm" subtle>
              {{ isEdit ? '编辑模式' : '新计划' }}
            </CnStatusTag>
            <h3>{{ form.planName || '准备创建一个稳定执行的计划' }}</h3>
          </div>
          <div class="form-summary__meta">
            <CnStatusTag type="success" size="sm">{{ targetTypeLabel }}</CnStatusTag>
            <CnStatusTag type="neutral" size="sm" subtle>{{ repeatTypeLabel }}</CnStatusTag>
          </div>
        </div>
      </CnSection>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" label-position="top">
        <el-form-item label="计划名称" prop="planName">
          <el-input v-model="form.planName" placeholder="给你的计划起个名字吧" maxlength="50" show-word-limit />
        </el-form-item>

        <el-form-item label="计划描述" prop="planDesc">
          <el-input
            v-model="form.planDesc"
            type="textarea"
            placeholder="简单描述一下你的计划（选填）"
            :rows="3"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="计划类型" prop="planType">
              <el-select v-model="form.planType" placeholder="选择类型" class="full-width-control">
                <el-option v-for="option in planTypeOptions" :key="option.value" :label="option.label" :value="option.value">
                  <span>{{ option.label }}</span>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目标类型" prop="targetType">
              <el-select v-model="form.targetType" placeholder="选择目标类型" class="full-width-control">
                <el-option label="每日目标" :value="1" />
                <el-option label="累计目标" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="目标数值" prop="targetValue">
              <el-input-number v-model="form.targetValue" :min="1" :max="9999" class="full-width-control" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目标单位" prop="targetUnit">
              <el-input v-model="form.targetUnit" placeholder="如：道题、分钟、页" maxlength="10" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="开始日期" prop="startDate">
              <el-date-picker
                v-model="form.startDate"
                type="date"
                placeholder="选择开始日期"
                class="full-width-control"
                value-format="YYYY-MM-DD"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束日期">
              <el-date-picker
                v-model="form.endDate"
                type="date"
                placeholder="不选则为长期计划"
                class="full-width-control"
                value-format="YYYY-MM-DD"
                :disabled-date="disabledEndDate"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="每日开始时间">
              <el-time-picker
                v-model="form.dailyStartTime"
                placeholder="选择时间（选填）"
                class="full-width-control"
                format="HH:mm"
                value-format="HH:mm"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="每日截止时间">
              <el-time-picker
                v-model="form.dailyEndTime"
                placeholder="选择时间（选填）"
                class="full-width-control"
                format="HH:mm"
                value-format="HH:mm"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="重复设置">
          <el-radio-group v-model="form.repeatType">
            <el-radio :label="1">每天</el-radio>
            <el-radio :label="2">工作日</el-radio>
            <el-radio :label="3">周末</el-radio>
            <el-radio :label="4">自定义</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="form.repeatType === 4" label="选择重复日">
          <el-checkbox-group v-model="repeatDaysArray" class="repeat-days">
            <el-checkbox label="1">周一</el-checkbox>
            <el-checkbox label="2">周二</el-checkbox>
            <el-checkbox label="3">周三</el-checkbox>
            <el-checkbox label="4">周四</el-checkbox>
            <el-checkbox label="5">周五</el-checkbox>
            <el-checkbox label="6">周六</el-checkbox>
            <el-checkbox label="7">周日</el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <div class="setting-divider">
          <span>提醒设置</span>
        </div>

        <el-form-item>
          <el-switch v-model="form.remindEnabled" active-text="开启提醒" />
        </el-form-item>

        <template v-if="form.remindEnabled">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="开始前提醒">
                <el-select v-model="form.remindBefore" placeholder="选择提醒时间" class="full-width-control">
                  <el-option v-for="option in reminderOptions" :key="option.value" :label="option.label" :value="option.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="截止前提醒">
                <el-select v-model="form.remindDeadline" placeholder="选择提醒时间" class="full-width-control">
                  <el-option v-for="option in reminderOptions" :key="option.value" :label="option.label" :value="option.value" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </template>
      </el-form>
    </div>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitting">
        {{ isEdit ? '保存修改' : '创建计划' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { CnSection, CnStatusTag } from '@/design-system'
import planApi from '@/api/plan'

interface PlanForm {
  id?: number | string
  planName: string
  planDesc: string
  planType: number
  targetType: number
  targetValue: number
  targetUnit: string
  startDate: string
  endDate: string | null
  dailyStartTime: string | null
  dailyEndTime: string | null
  repeatType: number
  repeatDays: string | null
  remindEnabled: boolean | number
  remindBefore: number
  remindDeadline: number
}

type PlanSubmitPayload = Omit<PlanForm, 'remindEnabled'> & {
  remindEnabled: number
}

const props = defineProps<{
  modelValue: boolean
  planData?: Partial<PlanForm> | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: []
}>()

const planTypeOptions = [
  { label: '刷题计划', value: 1 },
  { label: '学习计划', value: 2 },
  { label: '阅读计划', value: 3 },
  { label: '运动计划', value: 4 },
  { label: '其他计划', value: 5 }
]

const reminderOptions = [
  { label: '不提醒', value: 0 },
  { label: '5分钟前', value: 5 },
  { label: '10分钟前', value: 10 },
  { label: '15分钟前', value: 15 },
  { label: '30分钟前', value: 30 },
  { label: '1小时前', value: 60 }
]

const createDefaultForm = (): PlanForm => ({
  planName: '',
  planDesc: '',
  planType: 1,
  targetType: 1,
  targetValue: 1,
  targetUnit: '道题',
  startDate: new Date().toISOString().split('T')[0],
  endDate: null,
  dailyStartTime: null,
  dailyEndTime: null,
  repeatType: 1,
  repeatDays: null,
  remindEnabled: true,
  remindBefore: 10,
  remindDeadline: 30
})

const isEdit = computed(() => Boolean(props.planData?.id))

const formRef = ref<FormInstance | null>(null)
const submitting = ref(false)
const repeatDaysArray = ref<string[]>([])
const form = ref<PlanForm>(createDefaultForm())

const targetTypeLabel = computed(() => (Number(form.value.targetType) === 2 ? '累计目标' : '每日目标'))
const repeatTypeLabel = computed(() => {
  const map: Record<number, string> = {
    1: '每天',
    2: '工作日',
    3: '周末',
    4: '自定义'
  }
  return map[Number(form.value.repeatType)] || '每天'
})

const rules: FormRules<PlanForm> = {
  planName: [
    { required: true, message: '请输入计划名称', trigger: 'blur' },
    { min: 2, max: 50, message: '名称长度在2到50个字符', trigger: 'blur' }
  ],
  planType: [{ required: true, message: '请选择计划类型', trigger: 'change' }],
  targetType: [{ required: true, message: '请选择目标类型', trigger: 'change' }],
  targetValue: [{ required: true, message: '请输入目标数值', trigger: 'blur' }],
  targetUnit: [{ required: true, message: '请输入目标单位', trigger: 'blur' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }]
}

watch(
  () => props.modelValue,
  (val) => {
    if (!val) return

    if (props.planData) {
      form.value = {
        ...createDefaultForm(),
        ...props.planData,
        remindEnabled: props.planData.remindEnabled === 1 || props.planData.remindEnabled === true
      }
      repeatDaysArray.value = props.planData.repeatDays ? String(props.planData.repeatDays).split(',') : []
    } else {
      resetForm()
    }
  }
)

watch(repeatDaysArray, (val) => {
  form.value.repeatDays = val.length > 0 ? val.join(',') : null
})

const disabledEndDate = (date: Date) => {
  if (!form.value.startDate) return false
  return date < new Date(form.value.startDate)
}

const resetForm = () => {
  form.value = createDefaultForm()
  repeatDaysArray.value = []
  formRef.value?.resetFields()
}

const handleClose = () => {
  emit('update:modelValue', false)
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()

    submitting.value = true

    const submitData: PlanSubmitPayload = {
      ...form.value,
      remindEnabled: form.value.remindEnabled ? 1 : 0
    }

    if (isEdit.value && props.planData?.id) {
      await planApi.updatePlan(props.planData.id, submitData)
      ElMessage.success('计划修改成功')
    } else {
      await planApi.createPlan(submitData)
      ElMessage.success('计划创建成功')
    }

    emit('success')
    handleClose()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('提交失败:', error)
      ElMessage.error(error instanceof Error ? error.message : '操作失败')
    }
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
:deep(.el-dialog__body) {
  max-height: 65vh;
  overflow-y: auto;
  padding-top: var(--cn-space-3);
}

:deep(.el-form-item__label) {
  color: var(--cn-color-text-primary);
  font-weight: 650;
}

.form-dialog-shell {
  display: grid;
  gap: var(--cn-space-5);
}

.full-width-control {
  width: 100%;
}

.form-summary {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 22%, var(--cn-color-border));
}

.form-summary__content {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--cn-space-4);
  min-width: 0;
}

.form-summary__content h3 {
  margin: var(--cn-space-2) 0 0;
  color: var(--cn-color-text-primary);
  font-size: 17px;
  font-weight: 700;
  line-height: 1.4;
  overflow-wrap: anywhere;
}

.form-summary__meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--cn-space-2);
  flex-shrink: 0;
}

.repeat-days {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-1) var(--cn-space-3);
}

.setting-divider {
  display: flex;
  align-items: center;
  gap: var(--cn-space-3);
  margin: var(--cn-space-2) 0 var(--cn-space-4);
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  font-weight: 700;
}

.setting-divider::before,
.setting-divider::after {
  content: '';
  height: 1px;
  background: var(--cn-color-border-subtle);
  flex: 1;
}

@media (max-width: 640px) {
  .form-summary__content {
    display: grid;
  }

  .form-summary__meta {
    justify-content: flex-start;
  }
}
</style>
