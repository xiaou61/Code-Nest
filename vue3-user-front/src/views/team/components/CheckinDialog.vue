<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="emit('update:modelValue', $event)"
    title="打卡"
    width="500px"
    :close-on-click-modal="false"
  >
    <div class="checkin-dialog">
      <CnSection v-if="displayTask" surface="plain" compact class="task-info">
        <div class="task-info__content">
          <CnStatusTag type="brand" size="sm" subtle>
            <el-icon><Calendar /></el-icon>
            打卡任务
          </CnStatusTag>
          <h3>{{ displayTask.taskName || '未命名任务' }}</h3>
          <p v-if="displayTask.taskDesc">{{ displayTask.taskDesc }}</p>
        </div>
      </CnSection>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item v-if="!task" label="选择任务" prop="taskId">
          <el-select v-model="form.taskId" placeholder="请选择要打卡的任务" filterable :loading="tasksLoading">
            <el-option v-for="item in availableTasks" :key="item.id" :label="item.taskName" :value="item.id" />
          </el-select>
          <div v-if="!tasksLoading && availableTasks.length === 0" class="task-empty-tip">
            当前没有待打卡任务
          </div>
        </el-form-item>

        <el-form-item label="打卡内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="4"
            placeholder="记录你今天的学习/完成情况..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="学习时长（分钟）" prop="duration">
          <div class="duration-row">
            <el-input-number v-model="form.duration" :min="1" :max="1440" placeholder="可选" />
            <span class="form-tip">选填，记录你本次学习的时长</span>
          </div>
        </el-form-item>

        <el-form-item label="图片（可选）">
          <div class="image-upload">
            <div v-for="(img, index) in imageList" :key="`${img}-${index}`" class="image-item">
              <img :src="img" alt="打卡图片" />
              <button class="image-delete" type="button" aria-label="删除图片" @click="removeImage(index)">
                <el-icon><Close /></el-icon>
              </button>
            </div>
            <button v-if="imageList.length < 3" class="image-add" type="button" aria-label="添加图片" @click="openImageInput">
              <el-icon><Plus /></el-icon>
            </button>
          </div>
          <input ref="imageInput" type="file" accept="image/*" class="hidden-input" @change="handleImageSelect" />
        </el-form-item>

        <el-form-item v-if="allowSupplement">
          <div class="supplement-row">
            <el-checkbox v-model="isSupplement">补卡（选择补卡日期）</el-checkbox>
            <el-date-picker
              v-if="isSupplement"
              v-model="form.checkinDate"
              type="date"
              placeholder="选择补卡日期"
              :disabled-date="disabledDate"
              value-format="YYYY-MM-DD"
            />
          </div>
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitting">完成打卡</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Calendar, Close, Plus } from '@element-plus/icons-vue'
import { CnSection, CnStatusTag } from '@/design-system'
import teamApi from '@/api/team'

interface TeamTask {
  id: number | string
  taskName?: string
  taskDesc?: string
  todayChecked?: boolean | number
}

interface CheckinForm {
  taskId: number | string | null
  content: string
  duration: number | null
  checkinDate: string | null
}

interface CheckinPayload {
  taskId: number | string | null
  content: string
  duration: number | null
  images: string[]
}

const props = withDefaults(
  defineProps<{
    modelValue?: boolean
    teamId: string | number
    task?: TeamTask | null
    allowSupplement?: boolean
  }>(),
  {
    modelValue: false,
    task: null,
    allowSupplement: true
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: []
}>()

const formRef = ref<FormInstance | null>(null)
const imageInput = ref<HTMLInputElement | null>(null)
const submitting = ref(false)
const imageList = ref<string[]>([])
const isSupplement = ref(false)
const tasksLoading = ref(false)
const availableTasks = ref<TeamTask[]>([])

const form = ref<CheckinForm>({
  taskId: null,
  content: '',
  duration: null,
  checkinDate: null
})

const rules: FormRules<CheckinForm> = {
  taskId: [{ required: true, message: '请选择任务', trigger: 'change' }],
  content: [{ required: true, message: '请输入打卡内容', trigger: 'blur' }]
}

const selectedTask = computed(() => {
  if (props.task) {
    return props.task
  }
  return availableTasks.value.find((item) => item.id === form.value.taskId) || null
})

const displayTask = computed(() => props.task || selectedTask.value)

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      resetForm()
      if (!props.task) {
        loadAvailableTasks()
      }
    }
  }
)

const resetForm = () => {
  form.value = {
    taskId: props.task?.id || null,
    content: '',
    duration: null,
    checkinDate: null
  }
  imageList.value = []
  isSupplement.value = false
  availableTasks.value = []
  formRef.value?.clearValidate()
}

const loadAvailableTasks = async () => {
  tasksLoading.value = true
  try {
    const tasks = (await teamApi.getTodayTasks(props.teamId)) as TeamTask[]
    availableTasks.value = (tasks || []).filter((item) => !item.todayChecked)
    if (!form.value.taskId && availableTasks.value.length > 0) {
      form.value.taskId = availableTasks.value[0].id
    }
  } catch (error) {
    console.error('加载可打卡任务失败:', error)
    availableTasks.value = []
  } finally {
    tasksLoading.value = false
  }
}

const disabledDate = (time: Date) => {
  const now = new Date()
  now.setHours(23, 59, 59, 999)
  const weekAgo = new Date()
  weekAgo.setDate(weekAgo.getDate() - 7)
  weekAgo.setHours(0, 0, 0, 0)
  return time.getTime() > now.getTime() || time.getTime() < weekAgo.getTime()
}

const openImageInput = () => {
  imageInput.value?.click()
}

const handleImageSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  const reader = new FileReader()
  reader.onload = (loadEvent) => {
    const result = loadEvent.target?.result
    if (typeof result === 'string') {
      imageList.value.push(result)
    }
  }
  reader.readAsDataURL(file)
  target.value = ''
}

const removeImage = (index: number) => {
  imageList.value.splice(index, 1)
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const data: CheckinPayload = {
      taskId: form.value.taskId || props.task?.id || null,
      content: form.value.content,
      duration: form.value.duration,
      images: [...imageList.value]
    }

    if (!data.taskId) {
      ElMessage.warning('请先选择一个任务再打卡')
      return
    }

    if (isSupplement.value) {
      if (!form.value.checkinDate) {
        ElMessage.warning('请选择补卡日期')
        return
      }
      await teamApi.supplementCheckin(props.teamId, data, form.value.checkinDate)
      ElMessage.success('补卡成功')
    } else {
      await teamApi.checkin(props.teamId, data)
      ElMessage.success('打卡成功')
    }

    emit('update:modelValue', false)
    emit('success')
  } catch (error) {
    console.error('打卡失败:', error)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.checkin-dialog {
  display: grid;
  gap: var(--cn-space-5);
}

.task-info {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 22%, var(--cn-color-border));
}

.task-info__content {
  display: grid;
  gap: var(--cn-space-2);
}

.task-info__content h3 {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-size: 16px;
  font-weight: 750;
  line-height: 1.4;
  overflow-wrap: anywhere;
}

.task-info__content p {
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.6;
  overflow-wrap: anywhere;
}

.task-empty-tip,
.form-tip {
  margin-top: var(--cn-space-2);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  line-height: 1.5;
}

.duration-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--cn-space-3);
  width: 100%;
}

.duration-row :deep(.el-input-number) {
  width: 160px;
}

.image-upload {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-3);
}

.image-item,
.image-add {
  position: relative;
  width: 84px;
  height: 84px;
  overflow: hidden;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
}

.image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-delete,
.image-add {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin: 0;
  padding: 0;
  cursor: pointer;
}

.image-delete {
  position: absolute;
  top: var(--cn-space-1);
  right: var(--cn-space-1);
  width: 24px;
  height: 24px;
  border: 1px solid color-mix(in srgb, var(--cn-color-danger) 25%, var(--cn-color-border));
  border-radius: var(--cn-radius-pill);
  background: color-mix(in srgb, var(--cn-color-bg-elevated) 88%, transparent);
  color: var(--cn-color-danger);
  box-shadow: var(--cn-shadow-popover);
}

.image-add {
  border-style: dashed;
  color: var(--cn-color-brand-primary);
  font-size: 20px;
  transition:
    border-color var(--cn-motion-fast) var(--cn-ease-out),
    background-color var(--cn-motion-fast) var(--cn-ease-out);
}

.image-add:hover {
  border-color: var(--cn-color-brand-primary);
  background: var(--cn-color-brand-soft);
}

.hidden-input {
  display: none;
}

.supplement-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--cn-space-3);
  width: 100%;
}

@media (max-width: 560px) {
  .duration-row,
  .supplement-row {
    display: grid;
  }

  .duration-row :deep(.el-input-number),
  .supplement-row :deep(.el-date-editor) {
    width: 100%;
  }
}
</style>
