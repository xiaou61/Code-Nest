<template>
  <CnPage class="submission-detail" surface="transparent" max-width="1080px">
    <CnPageHeader
      class="cn-learn-reveal"
      title="提交详情"
      :description="headerDescription"
      eyebrow="Submission Detail"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag :type="getStatusTone(submission.status)" size="sm">
          {{ getStatusLabel(submission.status) }}
        </CnStatusTag>
        <CnStatusTag type="neutral" size="sm">
          {{ getLanguageLabel(submission.language) }}
        </CnStatusTag>
        <CnStatusTag v-if="submission.id" type="info" size="sm">
          #{{ submission.id }}
        </CnStatusTag>
      </template>

      <template #actions>
        <el-button plain @click="router.back()">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <el-button
          v-if="submission.problemId"
          type="primary"
          @click="router.push(`/oj/problem/${submission.problemId}`)"
        >
          查看题目
        </el-button>
      </template>
    </CnPageHeader>

    <div class="detail-content" v-loading="loading">
      <section v-if="submission.id" class="summary-grid cn-learn-reveal" aria-label="提交运行指标">
        <CnStatCard
          title="判题状态"
          :value="getStatusLabel(submission.status)"
          description="本次提交的最终状态"
          :tone="getStatusTone(submission.status)"
          trend="flat"
          :loading="loading"
          trend-text="状态"
        />
        <CnStatCard
          title="运行耗时"
          :value="formatTime(submission.timeUsed)"
          description="判题环境记录的执行耗时"
          tone="info"
          trend="flat"
          :loading="loading"
          trend-text="Time"
        />
        <CnStatCard
          title="内存占用"
          :value="formatMemory(submission.memoryUsed)"
          description="判题环境记录的内存占用"
          tone="warning"
          trend="flat"
          :loading="loading"
          trend-text="Memory"
        />
        <CnStatCard
          title="通过用例"
          :value="casePassText"
          description="通过测试用例 / 总测试用例"
          tone="success"
          trend="flat"
          :loading="loading"
          trend-text="Cases"
        />
      </section>

      <CnSection
        v-if="submission.id"
        class="cn-learn-reveal"
        title="基本信息"
        description="提交关联题目、语言和时间。"
        surface="panel"
        divided
      >
        <dl class="info-grid">
          <div class="info-item">
            <dt>题目</dt>
            <dd>
              <button class="info-link" type="button" @click="router.push(`/oj/problem/${submission.problemId}`)">
                {{ submission.problemTitle || `#${submission.problemId}` }}
              </button>
            </dd>
          </div>
          <div class="info-item">
            <dt>语言</dt>
            <dd>{{ getLanguageLabel(submission.language) }}</dd>
          </div>
          <div class="info-item">
            <dt>提交时间</dt>
            <dd>{{ submission.createTime || '-' }}</dd>
          </div>
          <div class="info-item">
            <dt>耗时</dt>
            <dd>{{ formatTime(submission.timeUsed) }}</dd>
          </div>
          <div class="info-item">
            <dt>内存</dt>
            <dd>{{ formatMemory(submission.memoryUsed) }}</dd>
          </div>
          <div class="info-item">
            <dt>通过用例</dt>
            <dd>{{ casePassText }}</dd>
          </div>
        </dl>
      </CnSection>

      <CnSection
        v-if="submission.errorMessage"
        class="cn-learn-reveal"
        title="错误信息"
        description="编译、运行或判题过程返回的错误内容。"
        surface="panel"
        divided
      >
        <pre class="error-message">{{ submission.errorMessage }}</pre>
      </CnSection>

      <CnSection
        v-if="submission.code"
        class="cn-learn-reveal"
        title="提交代码"
        :description="`语言：${getLanguageLabel(submission.language)}`"
        surface="panel"
        divided
      >
        <pre class="code-block"><code>{{ submission.code }}</code></pre>
      </CnSection>

      <CnEmptyState
        v-if="!loading && !submission.id"
        class="cn-learn-reveal"
        title="提交记录不存在"
        description="这条提交可能已被删除，或当前账号无权查看。"
        icon="OJ"
        surface="panel"
      >
        <template #actions>
          <el-button type="primary" @click="router.push('/oj/my-submissions')">返回我的提交</el-button>
        </template>
      </CnEmptyState>
    </div>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import {
  CnEmptyState,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag
} from '@/design-system'
import type { CnTone } from '@/design-system'
import { ojApi } from '@/api/oj'
import { useRevealMotion } from '@/utils/reveal-motion'

interface SubmissionRecord extends Record<string, unknown> {
  id?: number | string
  problemId?: number | string
  problemTitle?: string
  language?: string
  status?: string
  timeUsed?: number | null
  memoryUsed?: number | null
  passCount?: number | null
  totalCount?: number | null
  createTime?: string
  errorMessage?: string
  code?: string
}

const route = useRoute()
const router = useRouter()
useRevealMotion('.submission-detail .cn-learn-reveal')

const loading = ref(false)
const submission = ref<SubmissionRecord>({})

const breadcrumbs = [
  { label: '首页', to: '/' },
  { label: '在线判题', to: '/oj' },
  { label: '我的提交', to: '/oj/my-submissions' },
  { label: '提交详情' }
]

const statusMap: Record<string, { label: string; tone: CnTone }> = {
  pending: { label: '等待判题', tone: 'info' },
  judging: { label: '判题中', tone: 'info' },
  accepted: { label: '通过', tone: 'success' },
  wrong_answer: { label: '答案错误', tone: 'danger' },
  time_limit_exceeded: { label: '超时', tone: 'warning' },
  memory_limit_exceeded: { label: '超内存', tone: 'warning' },
  runtime_error: { label: '运行错误', tone: 'danger' },
  compile_error: { label: '编译错误', tone: 'danger' },
  system_error: { label: '系统错误', tone: 'neutral' }
}

const languageMap: Record<string, string> = {
  java: 'Java',
  cpp: 'C++',
  c: 'C',
  python: 'Python3',
  go: 'Go',
  javascript: 'JavaScript'
}

const headerDescription = computed(() => {
  if (!submission.value.id) return '查看判题状态、运行指标、错误信息和提交代码。'
  return `${submission.value.problemTitle || `#${submission.value.problemId}`} · ${submission.value.createTime || '-'}`
})

const casePassText = computed(() => {
  return `${submission.value.passCount ?? '-'} / ${submission.value.totalCount ?? '-'}`
})

const getStatusLabel = (status?: string) => {
  if (!status) return '未知状态'
  return statusMap[status]?.label || status
}

const getStatusTone = (status?: string): CnTone => {
  if (!status) return 'neutral'
  return statusMap[status]?.tone || 'info'
}

const getLanguageLabel = (lang?: string) => {
  if (!lang) return '-'
  return languageMap[lang] || lang
}

const formatTime = (time?: number | null) => {
  return time != null ? `${time}ms` : '-'
}

const formatMemory = (kb?: number | null) => {
  if (!kb) return '-'
  return kb > 1024 ? `${(kb / 1024).toFixed(1)}MB` : `${kb}KB`
}

onMounted(async () => {
  loading.value = true
  try {
    submission.value = await ojApi.getSubmissionDetail(route.params.id) || {}
  } catch {
    submission.value = {}
    ElMessage.error('提交记录不存在')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.submission-detail {
  min-height: calc(100vh - 68px);
}

.detail-content {
  display: grid;
  gap: var(--cn-space-5);
  min-height: 260px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--cn-space-4);
  margin: 0;
}

.info-item {
  min-width: 0;
}

.info-item dt {
  margin-bottom: var(--cn-space-1);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  font-weight: 700;
}

.info-item dd {
  margin: 0;
  min-width: 0;
  color: var(--cn-color-text-primary);
  font-size: 14px;
  font-weight: 650;
  overflow-wrap: anywhere;
}

.info-link {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--cn-color-brand-primary);
  cursor: pointer;
  font: inherit;
  text-align: left;
}

.info-link:hover {
  color: var(--cn-color-brand-hover);
  text-decoration: underline;
}

.error-message {
  margin: 0;
  padding: var(--cn-space-4);
  overflow-x: auto;
  border: 1px solid color-mix(in srgb, var(--cn-color-danger) 24%, var(--cn-color-border-subtle));
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-danger-soft);
  color: var(--cn-color-danger);
  font-family: var(--cn-font-mono);
  font-size: 13px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}

.code-block {
  margin: 0;
  padding: var(--cn-space-5);
  overflow-x: auto;
  border-radius: var(--cn-radius-card);
  background: color-mix(in srgb, black 88%, var(--cn-color-brand-primary));
  color: color-mix(in srgb, white 84%, var(--cn-color-brand-soft));
  font-family: var(--cn-font-mono);
  font-size: 13px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 1024px) {
  .summary-grid,
  .info-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .summary-grid,
  .info-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
