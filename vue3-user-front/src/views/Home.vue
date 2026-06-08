<template>
  <CnPage class="legacy-home" surface="transparent">
    <CnPageHeader
      eyebrow="Code Nest"
      title="开发者成长社区"
      description="集面试刷题、知识图谱、在线简历、代码工坊、技术博客、社区互动于一体的一站式开发者平台。"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm" subtle>Legacy compatible page</CnStatusTag>
        <CnStatusTag type="success" size="sm" subtle>Design System</CnStatusTag>
      </template>
      <template #actions>
        <el-button type="primary" size="large" @click="go('/interview')">
          <el-icon><Reading /></el-icon>
          开始刷题
        </el-button>
        <el-button size="large" @click="go('/community')">
          <el-icon><ChatDotRound /></el-icon>
          进入社区
        </el-button>
      </template>
    </CnPageHeader>

    <section class="hero-panel">
      <div class="hero-copy">
        <CnStatusTag type="brand" size="lg">Code Nest</CnStatusTag>
        <h2>程序员的技术成长工作台</h2>
        <p>刷题、面试、知识沉淀、作品展示和社区交流可以在这里连成一条更顺手的成长路径。</p>
      </div>
      <div class="hero-actions">
        <button
          v-for="item in quickAccess.slice(0, 4)"
          :key="item.title"
          class="quick-pill"
          type="button"
          @click="go(item.path)"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </button>
      </div>
    </section>

    <div class="stats-grid">
      <CnStatCard
        v-for="stat in stats"
        :key="stat.label"
        :title="stat.label"
        :value="stat.display"
        :unit="stat.suffix"
        :tone="stat.tone"
        description="实时平台数据"
      />
    </div>

    <CnSection title="核心功能" description="保留旧首页入口，同时统一到用户端主题 token。">
      <div class="feature-grid">
        <article v-for="feature in features" :key="feature.title" class="feature-card" @click="go(feature.path)">
          <div class="feature-card__icon" :class="`feature-card__icon--${feature.tone}`">
            <el-icon><component :is="feature.icon" /></el-icon>
          </div>
          <div class="feature-card__body">
            <h3>{{ feature.title }}</h3>
            <p>{{ feature.desc }}</p>
          </div>
          <el-icon class="feature-card__arrow"><ArrowRight /></el-icon>
        </article>
      </div>
    </CnSection>

    <CnSection title="快速入口" description="常用模块一键直达。">
      <div class="quick-grid">
        <button v-for="item in quickAccess" :key="item.title" class="quick-card" type="button" @click="go(item.path)">
          <span class="quick-card__icon" :class="`quick-card__icon--${item.tone}`">
            <el-icon><component :is="item.icon" /></el-icon>
          </span>
          <span>{{ item.title }}</span>
        </button>
      </div>
    </CnSection>

    <CnSection surface="plain" class="cta-section">
      <div class="cta-copy">
        <CnStatusTag type="success" size="sm" subtle>Growth Loop</CnStatusTag>
        <h2>准备好开始你的技术成长之旅了吗？</h2>
        <p>从一个小任务开始，慢慢把刷题、项目、表达和复盘变成稳定节奏。</p>
      </div>
      <el-button type="primary" size="large" @click="go('/interview')">
        立即开始
        <el-icon class="el-icon--right"><ArrowRight /></el-icon>
      </el-button>
    </CnSection>
  </CnPage>
</template>

<script setup lang="ts">
import { onMounted, ref, type Component } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowRight,
  Bell,
  Calendar,
  ChatDotRound,
  ChatLineSquare,
  Coffee,
  Compass,
  Cpu,
  DataAnalysis,
  Document,
  Edit,
  Monitor,
  Reading,
  Share,
  Ticket,
  Trophy
} from '@element-plus/icons-vue'
import { CnPage, CnPageHeader, CnSection, CnStatCard, CnStatusTag, type CnTone } from '@/design-system'
import { interviewApi } from '@/api/interview'
import { getPublishedKnowledgeMaps } from '@/api/knowledge'
import { getOnlineCount } from '@/api/chat'

interface HomeFeature {
  title: string
  desc: string
  icon: Component
  path: string
  tone: CnTone
}

interface QuickAccessItem {
  title: string
  icon: Component
  path: string
  tone: CnTone
}

interface HomeStat {
  label: string
  value: number
  suffix: string
  display: number
  tone: CnTone
}

const router = useRouter()

const features = ref<HomeFeature[]>([
  {
    title: '面试题库',
    desc: '覆盖前端、后端、算法等热门方向，帮助你系统准备面试。',
    icon: Reading,
    path: '/interview',
    tone: 'brand'
  },
  {
    title: '知识图谱',
    desc: '可视化知识体系，理清技术脉络，构建完整知识网络。',
    icon: Share,
    path: '/knowledge',
    tone: 'info'
  },
  {
    title: '在线简历',
    desc: '专业简历模板，在线编辑导出，让履历表达更清楚。',
    icon: Document,
    path: '/resume',
    tone: 'success'
  },
  {
    title: '代码工坊',
    desc: '在线编辑、预览和分享代码作品，沉淀可展示项目。',
    icon: Monitor,
    path: '/codepen',
    tone: 'neutral'
  },
  {
    title: '技术博客',
    desc: '用 Markdown 记录学习心得，形成持续输出习惯。',
    icon: Edit,
    path: '/blog',
    tone: 'warning'
  },
  {
    title: '技术社区',
    desc: '围绕问题、动态和讨论，与其他开发者保持连接。',
    icon: ChatDotRound,
    path: '/community',
    tone: 'brand'
  },
  {
    title: '在线判题',
    desc: '多语言在线编程与实时判题，训练算法和编码能力。',
    icon: Cpu,
    path: '/oj',
    tone: 'success'
  }
])

const quickAccess = ref<QuickAccessItem[]>([
  { title: '朋友圈', icon: ChatLineSquare, path: '/moments', tone: 'brand' },
  { title: '聊天室', icon: ChatDotRound, path: '/chat', tone: 'info' },
  { title: '通知中心', icon: Bell, path: '/notification', tone: 'neutral' },
  { title: '计划打卡', icon: Calendar, path: '/plan', tone: 'warning' },
  { title: '我的积分', icon: Trophy, path: '/points', tone: 'success' },
  { title: '幸运抽奖', icon: Ticket, path: '/lottery', tone: 'warning' },
  { title: '摸鱼工具', icon: Coffee, path: '/moyu-tools', tone: 'neutral' },
  { title: '开发工具', icon: Compass, path: '/dev-tools', tone: 'info' },
  { title: '版本历史', icon: DataAnalysis, path: '/version-history', tone: 'brand' }
])

const stats = ref<HomeStat[]>([
  { label: '已学习题目', value: 0, suffix: '', display: 0, tone: 'brand' },
  { label: '已发布图谱', value: 0, suffix: '', display: 0, tone: 'success' },
  { label: '实时在线', value: 0, suffix: '', display: 0, tone: 'info' }
])

const go = (path: string) => {
  router.push(path)
}

const animateStats = () => {
  const duration = 900
  const startTime = performance.now()

  const update = (now: number) => {
    const progress = Math.min((now - startTime) / duration, 1)
    const easedProgress = 1 - Math.pow(1 - progress, 3)

    stats.value.forEach((item) => {
      item.display = Math.round(item.value * easedProgress)
    })

    if (progress < 1) {
      requestAnimationFrame(update)
    }
  }

  requestAnimationFrame(update)
}

const loadHeroStats = async () => {
  try {
    const [learnedCount, knowledgeMaps, onlineCount] = await Promise.all([
      interviewApi.getTotalLearned(),
      getPublishedKnowledgeMaps({ pageNum: 1, pageSize: 1 }),
      getOnlineCount()
    ])

    stats.value[0].value = Number(learnedCount) || 0
    stats.value[1].value = Number((knowledgeMaps as { total?: number })?.total) || 0
    stats.value[2].value = Number(onlineCount) || 0
  } catch (error) {
    ElMessage.warning('首页统计加载失败，已展示默认值')
    stats.value.forEach((item) => {
      item.value = 0
    })
  }
}

onMounted(async () => {
  await loadHeroStats()
  animateStats()
})
</script>

<style scoped>
.legacy-home {
  --legacy-home-accent: var(--cn-color-brand-primary);
}

.hero-panel {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(260px, 0.6fr);
  gap: var(--cn-space-6);
  align-items: center;
  min-height: 320px;
  padding: clamp(var(--cn-space-6), 5vw, var(--cn-space-10));
  overflow: hidden;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-panel);
  background: color-mix(in srgb, var(--cn-color-bg-surface) 88%, var(--cn-color-brand-soft));
  box-shadow: var(--cn-shadow-card);
}

.hero-copy {
  display: grid;
  gap: var(--cn-space-4);
  min-width: 0;
}

.hero-copy h2,
.cta-copy h2 {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-family: var(--cn-font-heading);
  font-size: clamp(28px, 5vw, 48px);
  font-weight: 750;
  line-height: 1.12;
  overflow-wrap: anywhere;
}

.hero-copy p,
.cta-copy p {
  max-width: 680px;
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 15px;
  line-height: 1.8;
}

.hero-actions {
  display: grid;
  gap: var(--cn-space-3);
}

.quick-pill,
.quick-card {
  display: flex;
  align-items: center;
  gap: var(--cn-space-2);
  margin: 0;
  border: 1px solid var(--cn-color-border-subtle);
  background: var(--cn-color-bg-elevated);
  color: var(--cn-color-text-primary);
  cursor: pointer;
  transition:
    transform var(--cn-motion-fast) var(--cn-ease-out),
    border-color var(--cn-motion-fast) var(--cn-ease-out),
    background-color var(--cn-motion-fast) var(--cn-ease-out);
}

.quick-pill {
  justify-content: flex-start;
  min-height: 44px;
  padding: 0 var(--cn-space-4);
  border-radius: var(--cn-radius-pill);
  font-weight: 700;
}

.quick-pill:hover,
.quick-card:hover {
  transform: translateY(-2px);
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 36%, var(--cn-color-border-subtle));
  background: var(--cn-color-brand-soft);
}

.stats-grid,
.feature-grid,
.quick-grid {
  display: grid;
  gap: var(--cn-space-4);
}

.stats-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.feature-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.feature-card {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: var(--cn-space-4);
  align-items: flex-start;
  min-width: 0;
  padding: var(--cn-space-5);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface);
  cursor: pointer;
  transition:
    transform var(--cn-motion-fast) var(--cn-ease-out),
    box-shadow var(--cn-motion-fast) var(--cn-ease-out),
    border-color var(--cn-motion-fast) var(--cn-ease-out);
}

.feature-card:hover {
  transform: translateY(-2px);
  border-color: color-mix(in srgb, var(--legacy-home-accent) 28%, var(--cn-color-border-subtle));
  box-shadow: var(--cn-shadow-card);
}

.feature-card__icon,
.quick-card__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-brand-primary);
  flex-shrink: 0;
}

.feature-card__icon {
  width: 46px;
  height: 46px;
  font-size: 22px;
}

.feature-card__icon--success,
.quick-card__icon--success {
  background: var(--cn-color-success-soft);
  color: var(--cn-color-success);
}

.feature-card__icon--warning,
.quick-card__icon--warning {
  background: var(--cn-color-warning-soft);
  color: var(--cn-color-warning);
}

.feature-card__icon--info,
.quick-card__icon--info {
  background: var(--cn-color-info-soft);
  color: var(--cn-color-info);
}

.feature-card__icon--neutral,
.quick-card__icon--neutral {
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-text-secondary);
}

.feature-card__body {
  min-width: 0;
}

.feature-card__body h3 {
  margin: 0 0 var(--cn-space-2);
  color: var(--cn-color-text-primary);
  font-size: 16px;
  font-weight: 750;
}

.feature-card__body p {
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.7;
}

.feature-card__arrow {
  margin-top: var(--cn-space-1);
  color: var(--cn-color-text-tertiary);
}

.quick-grid {
  grid-template-columns: repeat(6, minmax(0, 1fr));
}

.quick-card {
  justify-content: center;
  flex-direction: column;
  min-height: 112px;
  padding: var(--cn-space-4);
  border-radius: var(--cn-radius-card);
  font-size: 13px;
  font-weight: 700;
}

.quick-card__icon {
  width: 40px;
  height: 40px;
  font-size: 20px;
}

.cta-section :deep(.cn-section__body) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-5);
}

.cta-copy {
  display: grid;
  gap: var(--cn-space-3);
}

.cta-copy h2 {
  font-size: clamp(22px, 3vw, 30px);
}

@media (max-width: 1080px) {
  .feature-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .quick-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 820px) {
  .hero-panel,
  .stats-grid,
  .feature-grid {
    grid-template-columns: 1fr;
  }

  .cta-section :deep(.cn-section__body) {
    display: grid;
  }
}

@media (max-width: 560px) {
  .quick-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .feature-card {
    grid-template-columns: 1fr;
  }
}
</style>
