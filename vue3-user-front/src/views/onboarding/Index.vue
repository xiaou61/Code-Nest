<template>
  <CnPage class="onboarding-page" surface="transparent" max-width="1120px" full-height>
    <main class="onboarding-shell" aria-labelledby="onboarding-title">
      <section class="onboarding-intro">
        <CnStatusTag type="brand" size="sm">FIRST WEEK</CnStatusTag>
        <h1 id="onboarding-title">制定你的第一周</h1>
        <p>先确定方向和节奏，系统会把本周任务拆到每天。</p>

        <ol class="stage-preview" aria-label="首周计划重点">
          <li v-for="item in stageOptions" :key="item.value" :class="{ active: form.currentStage === item.value }">
            <span>{{ item.order }}</span>
            <div>
              <strong>{{ item.label }}</strong>
              <p>{{ item.description }}</p>
            </div>
          </li>
        </ol>
      </section>

      <CnSection class="onboarding-form-panel" surface="panel" compact>
        <div v-if="checkingProfile" class="onboarding-loading" aria-live="polite">正在检查你的学习设置...</div>

        <el-form
          v-else
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          class="onboarding-form"
          @submit.prevent="submitOnboarding"
        >
          <el-form-item label="目标岗位" prop="targetRole">
            <el-select
              v-model="form.targetRole"
              filterable
              allow-create
              default-first-option
              placeholder="选择或输入岗位"
              class="role-select"
            >
              <el-option v-for="role in roleOptions" :key="role" :label="role" :value="role" />
            </el-select>
          </el-form-item>

          <el-form-item label="当前阶段" prop="currentStage">
            <el-radio-group v-model="form.currentStage" class="stage-choice" aria-label="当前学习阶段">
              <el-radio-button v-for="item in stageOptions" :key="item.value" :label="item.value">
                {{ item.label }}
              </el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="每周投入时间" prop="weeklyHours">
            <div class="hours-control">
              <el-slider v-model="form.weeklyHours" :min="3" :max="40" :step="1" show-stops />
              <strong>{{ form.weeklyHours }} 小时</strong>
            </div>
          </el-form-item>

          <el-alert
            :title="selectedStage.description"
            type="info"
            :closable="false"
            show-icon
          />

          <el-button type="primary" native-type="submit" size="large" :loading="submitting" class="generate-button">
            生成第一周任务
          </el-button>
        </el-form>
      </CnSection>
    </main>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { CnPage, CnSection, CnStatusTag } from '@/design-system'
import { careerLoopApi } from '@/api/careerLoop'
import { growthAutopilotApi } from '@/api/growthAutopilot'

interface OnboardingForm {
  targetRole: string
  currentStage: string
  weeklyHours: number
}

interface CareerLoopProfile {
  session?: {
    targetRole?: string
    weeklyHours?: number
  }
}

const router = useRouter()
const formRef = ref<FormInstance>()
const checkingProfile = ref(true)
const submitting = ref(false)

const roleOptions = ['后端开发', '前端开发', '全栈开发', '算法工程师', '测试开发', '产品经理', '运维开发']
const stageOptions = [
  { value: 'foundation', order: '01', label: '基础补齐', description: '优先建立高频知识、错题和闪卡的稳定节奏。' },
  { value: 'practice', order: '02', label: '能力练习', description: '以题库、项目表达和周期打卡形成连续输出。' },
  { value: 'interview', order: '03', label: '面试冲刺', description: '提高高频题、模拟面试和复盘任务的比重。' }
]

const form = reactive<OnboardingForm>({
  targetRole: '',
  currentStage: 'practice',
  weeklyHours: 8
})

const rules: FormRules<OnboardingForm> = {
  targetRole: [
    { required: true, message: '请选择或输入目标岗位', trigger: 'change' },
    { min: 2, max: 50, message: '目标岗位长度为 2 到 50 个字符', trigger: 'blur' }
  ],
  currentStage: [{ required: true, message: '请选择当前阶段', trigger: 'change' }],
  weeklyHours: [{ type: 'number', min: 3, max: 40, message: '每周投入时间应为 3 到 40 小时', trigger: 'change' }]
}

const selectedStage = computed(() =>
  stageOptions.find((item) => item.value === form.currentStage) || stageOptions[1]
)

const submitOnboarding = async () => {
  await formRef.value?.validate()
  submitting.value = true
  try {
    const targetRole = form.targetRole.trim()
    await careerLoopApi.updateProfile({
      targetRole,
      weeklyHours: form.weeklyHours
    })
    await growthAutopilotApi.generate({
      targetRole,
      currentStage: form.currentStage,
      weeklyHours: form.weeklyHours
    })
    ElMessage.success('第一周任务已生成')
    router.replace({ path: '/learning-cockpit', query: { tab: 'autopilot' } })
  } catch (error) {
    console.error('生成第一周任务失败', error)
    ElMessage.error('生成第一周任务失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  try {
    const profile = (await careerLoopApi.getCurrent()) as CareerLoopProfile
    const targetRole = profile?.session?.targetRole?.trim()
    if (targetRole) {
      router.replace({ path: '/learning-cockpit', query: { tab: 'autopilot' } })
      return
    }
    form.weeklyHours = Math.max(3, Math.min(40, Number(profile?.session?.weeklyHours) || 8))
  } catch (error) {
    console.warn('读取首次学习设置失败', error)
  } finally {
    checkingProfile.value = false
  }
})
</script>

<style scoped>
.onboarding-page {
  min-height: 100vh;
  display: grid;
  align-items: center;
  padding-block: var(--cn-space-7);
}

.onboarding-shell {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(360px, 0.8fr);
  gap: clamp(32px, 5vw, 72px);
  align-items: center;
}

.onboarding-intro h1 {
  margin: var(--cn-space-4) 0 var(--cn-space-3);
  color: var(--cn-color-text-primary);
  font-family: var(--cn-font-heading);
  font-size: 36px;
  font-weight: 720;
  line-height: 1.18;
}

.onboarding-intro > p {
  max-width: 480px;
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 16px;
  line-height: 1.75;
}

.stage-preview {
  display: grid;
  gap: var(--cn-space-3);
  margin: var(--cn-space-7) 0 0;
  padding: 0;
  list-style: none;
}

.stage-preview li {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: var(--cn-space-3);
  align-items: start;
  padding-left: var(--cn-space-3);
  border-left: 2px solid var(--cn-color-border-subtle);
}

.stage-preview li.active {
  border-color: var(--cn-color-brand-primary);
}

.stage-preview span {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  font-weight: 700;
}

.stage-preview li.active span,
.stage-preview li.active strong {
  color: var(--cn-color-brand-primary);
}

.stage-preview strong,
.stage-preview p {
  display: block;
}

.stage-preview strong {
  color: var(--cn-color-text-primary);
  font-size: 14px;
}

.stage-preview p {
  margin: var(--cn-space-1) 0 0;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.onboarding-form-panel {
  min-width: 0;
}

.onboarding-form {
  display: grid;
  gap: var(--cn-space-2);
}

.role-select,
.stage-choice {
  width: 100%;
}

.stage-choice :deep(.el-radio-button) {
  flex: 1;
}

.stage-choice :deep(.el-radio-button__inner) {
  width: 100%;
}

.hours-control {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: var(--cn-space-4);
  align-items: center;
}

.hours-control strong {
  min-width: 72px;
  color: var(--cn-color-text-primary);
  font-size: 14px;
  text-align: right;
}

.generate-button {
  width: 100%;
  margin-top: var(--cn-space-4);
}

.onboarding-loading {
  min-height: 260px;
  display: grid;
  place-items: center;
  color: var(--cn-color-text-secondary);
  font-size: 14px;
}

@media (max-width: 840px) {
  .onboarding-page {
    display: block;
    padding-block: var(--cn-space-5);
  }

  .onboarding-shell {
    grid-template-columns: 1fr;
    gap: var(--cn-space-6);
  }

  .onboarding-intro h1 {
    font-size: 30px;
  }

  .stage-preview {
    margin-top: var(--cn-space-5);
  }
}

@media (max-width: 480px) {
  .stage-choice {
    display: grid;
    grid-template-columns: 1fr;
  }

  .hours-control {
    grid-template-columns: 1fr;
    gap: var(--cn-space-2);
  }

  .hours-control strong {
    text-align: left;
  }
}
</style>
