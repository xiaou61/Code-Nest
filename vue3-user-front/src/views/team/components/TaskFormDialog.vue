<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :title="isEdit ? '编辑任务' : '创建任务'"
    width="500px"
    :close-on-click-modal="false"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="80px"
    >
      <el-form-item label="任务名称" prop="taskName">
        <el-input
          v-model="form.taskName"
          placeholder="请输入任务名称"
          maxlength="50"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="任务描述" prop="taskDesc">
        <el-input
          v-model="form.taskDesc"
          type="textarea"
          :rows="3"
          placeholder="请输入任务描述（选填）"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="任务类型" prop="taskType">
        <el-radio-group v-model="form.taskType">
          <el-radio :value="1">刷题</el-radio>
          <el-radio :value="2">学习时长</el-radio>
          <el-radio :value="3">阅读</el-radio>
          <el-radio :value="4">自定义</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="任务周期" prop="repeatType">
        <el-radio-group v-model="form.repeatType">
          <el-radio :value="1">每日任务</el-radio>
          <el-radio :value="2">工作日任务</el-radio>
          <el-radio :value="3">自定义重复</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item v-if="form.repeatType === 3" label="重复日期">
        <el-checkbox-group v-model="repeatDaysArray">
          <el-checkbox label="1">周一</el-checkbox>
          <el-checkbox label="2">周二</el-checkbox>
          <el-checkbox label="3">周三</el-checkbox>
          <el-checkbox label="4">周四</el-checkbox>
          <el-checkbox label="5">周五</el-checkbox>
          <el-checkbox label="6">周六</el-checkbox>
          <el-checkbox label="7">周日</el-checkbox>
        </el-checkbox-group>
      </el-form-item>

      <el-form-item label="生效时间">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期（选填）"
          end-placeholder="结束日期（选填）"
          value-format="YYYY-MM-DD"
        />
      </el-form-item>

      <el-form-item label="任务目标" prop="targetValue">
        <div class="target-inputs">
          <el-input-number
            v-model="form.targetValue"
            :min="1"
            :max="9999"
          />
          <el-input
            v-model="form.targetUnit"
            placeholder="单位，如 次/分钟/页"
            maxlength="20"
          />
        </div>
      </el-form-item>

      <el-form-item label="完成要求">
        <div class="requirement-switches">
          <el-switch
            v-model="form.requireContent"
            :active-value="1"
            :inactive-value="0"
            active-text="必须填写内容"
          />
          <el-switch
            v-model="form.requireImage"
            :active-value="1"
            :inactive-value="0"
            active-text="必须上传图片"
          />
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitting">
        {{ isEdit ? '保存' : '创建' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import teamApi from '@/api/team'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  teamId: { type: [String, Number], required: true },
  taskData: { type: Object, default: null }
})

const emit = defineEmits(['update:modelValue', 'success'])

const isEdit = computed(() => !!props.taskData)

const formRef = ref()
const submitting = ref(false)
const dateRange = ref([])
const repeatDaysArray = ref([])

const form = ref({
  taskName: '',
  taskDesc: '',
  taskType: 1,
  targetValue: 1,
  targetUnit: '次',
  repeatType: 1,
  repeatDays: null,
  requireContent: 0,
  requireImage: 0,
  startDate: null,
  endDate: null
})

const rules = {
  taskName: [
    { required: true, message: '请输入任务名称', trigger: 'blur' }
  ],
  targetValue: [
    { required: true, message: '请输入任务目标', trigger: 'change' }
  ],
  taskType: [
    { required: true, message: '请选择任务类型', trigger: 'change' }
  ],
  repeatType: [
    { required: true, message: '请选择任务周期', trigger: 'change' }
  ]
}

// 监听弹窗打开
watch(() => props.modelValue, (val) => {
  if (val) {
    if (props.taskData) {
      // 编辑模式
      form.value = {
        taskName: props.taskData.taskName || '',
        taskDesc: props.taskData.taskDesc || '',
        taskType: props.taskData.taskType || 1,
        targetValue: props.taskData.targetValue || 1,
        targetUnit: props.taskData.targetUnit || '次',
        repeatType: props.taskData.repeatType || 1,
        repeatDays: props.taskData.repeatDays || null,
        requireContent: props.taskData.requireContent ?? 0,
        requireImage: props.taskData.requireImage ?? 0,
        startDate: props.taskData.startDate || null,
        endDate: props.taskData.endDate || null
      }
      repeatDaysArray.value = props.taskData.repeatDays ? props.taskData.repeatDays.split(',') : []
      if (props.taskData.startDate && props.taskData.endDate) {
        dateRange.value = [props.taskData.startDate, props.taskData.endDate]
      } else {
        dateRange.value = []
      }
    } else {
      // 创建模式
      resetForm()
    }
  }
})

// 监听日期范围
watch(dateRange, (val) => {
  if (val && val.length === 2) {
    form.value.startDate = val[0]
    form.value.endDate = val[1]
  } else {
    form.value.startDate = null
    form.value.endDate = null
  }
})

watch(repeatDaysArray, (val) => {
  form.value.repeatDays = val.length > 0 ? val.join(',') : null
})

watch(() => form.value.repeatType, (val) => {
  if (val !== 3) {
    repeatDaysArray.value = []
  }
})

const resetForm = () => {
  form.value = {
    taskName: '',
    taskDesc: '',
    taskType: 1,
    targetValue: 1,
    targetUnit: '次',
    repeatType: 1,
    repeatDays: null,
    requireContent: 0,
    requireImage: 0,
    startDate: null,
    endDate: null
  }
  dateRange.value = []
  repeatDaysArray.value = []
  formRef.value?.clearValidate()
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (form.value.repeatType === 3 && repeatDaysArray.value.length === 0) {
    ElMessage.warning('请选择自定义重复日期')
    return
  }

  submitting.value = true
  try {
    const data = {
      taskName: form.value.taskName,
      taskDesc: form.value.taskDesc,
      taskType: form.value.taskType,
      targetValue: form.value.targetValue || 1,
      targetUnit: form.value.targetUnit || '次',
      repeatType: form.value.repeatType || 1,
      repeatDays: form.value.repeatType === 3 ? form.value.repeatDays : null,
      requireContent: form.value.requireContent ?? 0,
      requireImage: form.value.requireImage ?? 0,
      startDate: form.value.startDate,
      endDate: form.value.endDate
    }

    if (isEdit.value) {
      await teamApi.updateTask(props.taskData.id, data)
      ElMessage.success('更新成功')
    } else {
      await teamApi.createTask(props.teamId, data)
      ElMessage.success('创建成功')
    }

    emit('update:modelValue', false)
    emit('success')
  } catch (error) {
    console.error('保存失败:', error)
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.target-inputs {
  display: flex;
  gap: 12px;
  width: 100%;

  :deep(.el-input-number) {
    width: 140px;
  }

  :deep(.el-input) {
    flex: 1;
  }
}

.requirement-switches {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}
</style>
