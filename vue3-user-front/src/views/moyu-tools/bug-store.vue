<template>
  <CnPage class="bug-store-page" max-width="1180px" full-height>
    <CnPageHeader
      title="Bug 商店"
      description="随机发现经典 Bug 案例，学习现象定位、原因分析和解决方案。"
      eyebrow="MOYU TOOL"
      :breadcrumbs="[{ label: '摸鱼工具箱', to: '/moyu-tools' }, { label: 'Bug 商店' }]"
    >
      <template #meta>
        <CnStatusTag :type="currentBug ? 'success' : 'info'" size="sm">
          {{ currentBug ? '已获取案例' : '等待获取' }}
        </CnStatusTag>
        <CnStatusTag v-if="currentBug?.difficultyName" :type="getDifficultyTone(currentBug.difficultyLevel)" size="sm" subtle>
          {{ currentBug.difficultyName }}
        </CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="ArrowLeft" @click="goBack">返回工具箱</el-button>
        <el-button type="primary" :icon="Refresh" :loading="loading" @click="getRandomBug">
          {{ currentBug ? '换一个' : '来一发 Bug' }}
        </el-button>
      </template>
    </CnPageHeader>

    <CnSection v-if="!currentBug || !showBugDetail" class="intro-section" surface="panel">
      <CnEmptyState
        title="发现经典 Bug"
        description="点击按钮随机获取一个经典 Bug 案例，学习问题分析和解决思路。"
        icon="BUG"
        size="lg"
        surface="transparent"
      >
        <template #actions>
          <el-button type="danger" size="large" :icon="Refresh" :loading="loading" @click="getRandomBug">
            来一发 Bug
          </el-button>
        </template>
      </CnEmptyState>
    </CnSection>

    <template v-else>
      <section class="bug-summary-grid" aria-label="Bug 案例概览">
        <CnStatCard
          title="难度等级"
          :value="currentBug.difficultyName || '未知'"
          description="按排查复杂度划分"
          :tone="getDifficultyTone(currentBug.difficultyLevel)"
          trend="flat"
          trend-text="难度"
        />
        <CnStatCard
          title="技术标签"
          :value="currentBug.techTags?.length || 0"
          unit="个"
          description="案例关联的技术方向"
          tone="info"
          trend="flat"
          trend-text="标签"
        />
        <CnStatCard
          title="内容结构"
          value="3"
          unit="段"
          description="现象、原因、解决方案"
          tone="brand"
          trend="flat"
          trend-text="分析"
        />
      </section>

      <CnSection class="bug-detail-section" :title="currentBug.title" description="按现象、原因和解决方案拆解问题。" divided>
        <template #actions>
          <el-button :icon="CopyDocument" @click="copyBugContent">复制内容</el-button>
          <el-button type="primary" :icon="Refresh" :loading="loading" @click="getRandomBug">再来一个</el-button>
        </template>

        <div class="bug-meta">
          <CnStatusTag :type="getDifficultyTone(currentBug.difficultyLevel)" size="sm">
            {{ currentBug.difficultyName || '未知难度' }}
          </CnStatusTag>
          <CnStatusTag v-for="tag in currentBug.techTags || []" :key="tag" type="neutral" size="sm" subtle>
            {{ tag }}
          </CnStatusTag>
        </div>

        <div class="bug-content-grid">
          <article class="content-block phenomenon">
            <header>
              <el-icon><Warning /></el-icon>
              <h3>现象描述</h3>
            </header>
            <p>{{ currentBug.phenomenon }}</p>
          </article>

          <article class="content-block analysis">
            <header>
              <el-icon><Search /></el-icon>
              <h3>原因分析</h3>
            </header>
            <div class="formatted-content" v-html="formatContent(currentBug.causeAnalysis)" />
          </article>

          <article class="content-block solution">
            <header>
              <el-icon><Tools /></el-icon>
              <h3>解决方案</h3>
            </header>
            <div class="formatted-content" v-html="formatContent(currentBug.solution)" />
          </article>
        </div>

        <div class="bug-footer">
          <p>遇到类似问题时，可以沿着“复现现象 -> 缩小范围 -> 验证假设 -> 固化方案”的路径排查。</p>
          <el-button type="primary" :icon="Refresh" :loading="loading" @click="getRandomBug">再来一个</el-button>
        </div>
      </CnSection>
    </template>
  </CnPage>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, CopyDocument, Refresh, Search, Tools, Warning } from '@element-plus/icons-vue'
import { CnEmptyState, CnPage, CnPageHeader, CnSection, CnStatCard, CnStatusTag } from '@/design-system'
import type { CnTone } from '@/design-system'
import { sanitizeHtml } from '@/utils/markdown'
import { getRandomBug as fetchRandomBug } from '@/api/moyu'

interface BugItem {
  title: string
  phenomenon?: string
  causeAnalysis?: string
  solution?: string
  techTags?: string[]
  difficultyLevel?: number
  difficultyName?: string
}

const router = useRouter()

const loading = ref(false)
const currentBug = ref<BugItem | null>(null)
const showBugDetail = ref(false)

const goBack = () => {
  router.push('/moyu-tools')
}

const getRandomBug = async () => {
  try {
    loading.value = true
    showBugDetail.value = false

    const response = (await fetchRandomBug()) as BugItem | null

    if (response) {
      currentBug.value = response
      showBugDetail.value = true
    } else {
      ElMessage.warning('暂无可用的Bug内容，请稍后再试')
    }
  } catch (error) {
    console.error('获取随机Bug失败:', error)
    ElMessage.error(error instanceof Error ? error.message : '获取Bug失败，请稍后再试')
  } finally {
    loading.value = false
  }
}

const getDifficultyTone = (level?: number): CnTone => {
  const tones: Record<number, CnTone> = {
    1: 'success',
    2: 'warning',
    3: 'danger',
    4: 'brand'
  }
  return level ? tones[level] || 'neutral' : 'neutral'
}

const formatContent = (content?: string) => {
  if (!content) return ''
  return sanitizeHtml(escapeHtml(content).replace(/\n/g, '<br/>'))
}

const escapeHtml = (text: string) => {
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

const copyBugContent = async () => {
  if (!currentBug.value) return

  const bug = currentBug.value
  const content = `
【Bug标题】${bug.title}

【现象描述】
${bug.phenomenon || ''}

【原因分析】
${bug.causeAnalysis || ''}

【解决方案】
${bug.solution || ''}

【技术标签】${bug.techTags ? bug.techTags.join(', ') : ''}
【难度等级】${bug.difficultyName || ''}
  `.trim()

  try {
    await navigator.clipboard.writeText(content)
    ElMessage.success('内容已复制到剪贴板')
  } catch (error) {
    console.error('复制失败:', error)
    ElMessage.error('复制失败，请手动选择复制')
  }
}
</script>

<style scoped>
.bug-store-page {
  min-width: 0;
}

.intro-section :deep(.cn-section__body) {
  padding: var(--cn-space-6);
}

.bug-summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.bug-detail-section {
  min-width: 0;
}

.bug-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
  margin-bottom: var(--cn-space-5);
}

.bug-content-grid {
  display: grid;
  gap: var(--cn-space-4);
}

.content-block {
  display: grid;
  gap: var(--cn-space-3);
  min-width: 0;
  padding: var(--cn-space-5);
  border: 1px solid var(--cn-card-border);
  border-radius: var(--cn-card-radius);
  background: var(--cn-card-bg);
  box-shadow: var(--cn-card-shadow);
}

.content-block header {
  display: flex;
  align-items: center;
  gap: var(--cn-space-2);
  color: var(--cn-color-text-primary);
}

.content-block header .el-icon {
  color: var(--cn-color-brand-primary);
  font-size: 18px;
}

.content-block h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 650;
}

.content-block p,
.formatted-content {
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 14px;
  line-height: 1.8;
}

.phenomenon {
  border-left: 3px solid var(--cn-color-warning);
}

.analysis {
  border-left: 3px solid var(--cn-color-info);
}

.solution {
  border-left: 3px solid var(--cn-color-success);
}

.bug-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-4);
  margin-top: var(--cn-space-5);
  padding: var(--cn-space-4);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
}

.bug-footer p {
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.7;
}

@media (max-width: 900px) {
  .bug-summary-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .bug-footer {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
