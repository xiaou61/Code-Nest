<template>
  <CnPage class="home-revamp" surface="transparent" max-width="1440px" full-height>
    <section class="home-hero" aria-label="首页首屏">
      <div class="home-hero__copy">
        <CnPageHeader
          title="Code Nest"
          description="在一个站点完成刷题、知识体系建设、社区互动、计划打卡和 AI 模拟面试，让学习、练习和输出形成闭环。"
          eyebrow="开发者成长社区"
          compact
        >
          <template #meta>
            <CnStatusTag type="brand" size="sm">成长数据实时汇总</CnStatusTag>
            <CnStatusTag type="success" size="sm">刷新 {{ refreshAt || '--:--:--' }}</CnStatusTag>
            <CnStatusTag :type="moduleState.hero.available ? 'success' : 'warning'" size="sm">
              {{ moduleState.hero.available ? '数据已连接' : moduleState.hero.message }}
            </CnStatusTag>
          </template>

          <template #actions>
            <el-button plain :loading="refreshingRealtime" @click="handleManualRefresh">
              <el-icon><DataAnalysis /></el-icon>
              刷新数据
            </el-button>
          </template>
        </CnPageHeader>

        <div class="hero-actions">
          <el-button type="primary" size="large" @click="goTo('/interview')">
            <el-icon><Reading /></el-icon>
            立即刷题
          </el-button>
          <el-button size="large" @click="goTo('/learning-cockpit')">
            <el-icon><DataAnalysis /></el-icon>
            打开驾驶舱
          </el-button>
        </div>
      </div>

      <CnSection class="hero-metrics" surface="panel" compact>
        <div v-if="moduleState.hero.loading" class="home-state">成长数据加载中...</div>
        <CnEmptyState
          v-else-if="!moduleState.hero.available"
          title="实时数据暂不可用"
          :description="moduleState.hero.message"
          icon="CN"
          size="sm"
          surface="transparent"
        />
        <div v-else class="hero-metric-grid">
          <article v-for="metric in heroMetricCards" :key="metric.key" class="hero-metric-card">
            <span class="metric-label">{{ metric.label }}</span>
            <strong>{{ formatMetric(metric) }}</strong>
          </article>
        </div>
      </CnSection>
    </section>

    <CnSection
      v-if="recentWorkspaceItems.length"
      class="home-section section-reveal"
      data-section="recent"
      :class="{ 'is-visible': visibleSections.recent }"
      title="最近继续"
      description="保留最近使用过的模块，回到站点可以直接接着做。"
      surface="panel"
      divided
    >
      <div class="recent-grid">
        <button
          v-for="item in recentWorkspaceItems"
          :key="item.path"
          class="recent-card"
          type="button"
          @click="goTo(item.path)"
        >
          <span class="recent-icon">
            <el-icon><component :is="item.icon" /></el-icon>
          </span>
          <span class="recent-copy">
            <strong>{{ item.label }}</strong>
            <em>{{ item.desc }}</em>
          </span>
          <el-icon class="card-arrow"><ArrowRight /></el-icon>
        </button>
      </div>
    </CnSection>

    <section class="home-two-column">
      <CnSection
        class="home-section section-reveal"
        data-section="hot"
        :class="{ 'is-visible': visibleSections.hot }"
        title="今日热门"
        description="真实社区热帖与动态广场，优先展示今天值得看的讨论。"
        surface="panel"
        divided
      >
        <template #actions>
          <el-button text @click="goTo('/community')">更多社区</el-button>
        </template>

        <div v-if="moduleState.hot.loading" class="home-state">热门内容加载中...</div>
        <CnEmptyState
          v-else-if="!moduleState.hot.available"
          title="热门内容暂不可用"
          :description="moduleState.hot.message"
          icon="HOT"
          size="sm"
          surface="transparent"
        />
        <div v-else class="hot-stack">
          <div class="feed-group">
            <div class="feed-title">
              <el-icon><ChatDotRound /></el-icon>
              社区热帖
            </div>
            <CnEmptyState
              v-if="homeData.hotFeed.posts.length === 0"
              title="暂无热门帖子"
              icon="POST"
              size="sm"
              surface="transparent"
            />
            <button
              v-for="post in homeData.hotFeed.posts"
              v-else
              :key="`post-${post.id}`"
              class="feed-item"
              type="button"
              @click="goTo(post.routePath)"
            >
              <strong>{{ post.title }}</strong>
              <span>{{ post.authorName }} · {{ post.likeCount }} 赞 · {{ post.commentCount }} 评</span>
            </button>
          </div>

          <div class="feed-group">
            <div class="feed-title">
              <el-icon><Connection /></el-icon>
              热门动态
            </div>
            <CnEmptyState
              v-if="homeData.hotFeed.moments.length === 0"
              title="暂无热门动态"
              icon="MOM"
              size="sm"
              surface="transparent"
            />
            <button
              v-for="moment in homeData.hotFeed.moments"
              v-else
              :key="`moment-${moment.id}`"
              class="feed-item"
              type="button"
              @click="goTo(moment.routePath)"
            >
              <strong>{{ moment.title }}</strong>
              <span>{{ moment.authorName }} · {{ moment.likeCount }} 赞 · {{ moment.commentCount }} 评</span>
            </button>
          </div>
        </div>
      </CnSection>

      <CnSection
        class="home-section section-reveal"
        data-section="challenge"
        :class="{ 'is-visible': visibleSections.challenge }"
        title="每日挑战"
        description="一题算法 + 一轮 AI 模拟面试，把练习强度拉起来。"
        surface="panel"
        divided
      >
        <template #actions>
          <el-button text @click="goTo('/oj')">更多题目</el-button>
        </template>

        <div v-if="moduleState.challenge.loading" class="home-state">挑战数据加载中...</div>
        <CnEmptyState
          v-else-if="!moduleState.challenge.available"
          title="挑战数据暂不可用"
          :description="moduleState.challenge.message"
          icon="OJ"
          size="sm"
          surface="transparent"
        />
        <div v-else class="challenge-stack">
          <button class="challenge-card" type="button" @click="goTo(homeData.challenge.dailyProblem.routePath)">
            <span class="challenge-kicker">OJ 每日一题</span>
            <strong>{{ homeData.challenge.dailyProblem.title }}</strong>
            <span class="challenge-meta">
              <CnStatusTag :type="difficultyTone" size="sm" :dot="false">
                {{ homeData.challenge.dailyProblem.difficultyText }}
              </CnStatusTag>
              通过率 {{ homeData.challenge.dailyProblem.acceptanceRate }}%
            </span>
            <span class="tag-row">
              <CnStatusTag
                v-for="tag in homeData.challenge.dailyProblem.tags"
                :key="tag"
                type="neutral"
                size="sm"
                :dot="false"
                subtle
              >
                {{ tag }}
              </CnStatusTag>
            </span>
          </button>

          <button class="challenge-card is-secondary" type="button" @click="goTo('/mock-interview')">
            <span class="challenge-kicker">AI 模拟面试</span>
            <strong>最高分 {{ homeData.growth.mockInterview.highestScore }}</strong>
            <span class="challenge-meta">
              累计 {{ homeData.growth.mockInterview.totalInterviews }} 次 · 平均分 {{ homeData.growth.mockInterview.avgScore }}
            </span>
            <el-progress :show-text="false" :percentage="safeCompletionRate" :stroke-width="8" status="success" />
          </button>
        </div>
      </CnSection>
    </section>

    <CnSection
      class="home-section section-reveal"
      data-section="growth"
      :class="{ 'is-visible': visibleSections.growth }"
      title="成长驾驶舱"
      description="学习执行、面试练习、积分激励，三条成长曲线同屏可见。"
      surface="panel"
      divided
    >
      <template #actions>
        <el-button text @click="goTo('/learning-cockpit')">打开完整驾驶舱</el-button>
      </template>

      <div v-if="moduleState.growth.loading" class="home-state">成长数据加载中...</div>
      <CnEmptyState
        v-else-if="!moduleState.growth.available"
        title="成长数据暂不可用"
        :description="moduleState.growth.message"
        icon="AI"
        size="sm"
        surface="transparent"
      />
      <div v-else class="growth-grid">
        <article class="growth-card">
          <header>
            <span>计划打卡</span>
            <el-button text @click="goTo('/plan')">进入计划</el-button>
          </header>
          <strong>{{ homeData.growth.plan.todayCompletionRate }}%</strong>
          <p>
            今日完成 {{ homeData.growth.plan.todayCompleted }} / {{ todayTaskTotal }}，累计打卡
            {{ homeData.growth.plan.totalCheckins }} 次
          </p>
          <el-progress :show-text="false" :percentage="homeData.growth.plan.todayCompletionRate" :stroke-width="8" />
        </article>

        <article class="growth-card">
          <header>
            <span>积分资产</span>
            <el-button text @click="goTo('/points')">积分中心</el-button>
          </header>
          <strong>{{ homeData.growth.points.totalPoints }}</strong>
          <p>
            价值约 ¥{{ homeData.growth.points.balanceYuan }}，连续打卡
            {{ homeData.growth.points.continuousDays }} 天
          </p>
          <CnStatusTag :type="homeData.growth.points.todayCheckedIn ? 'success' : 'warning'" size="sm">
            {{ homeData.growth.points.todayCheckedIn ? '今日已打卡' : `今日可得 +${homeData.growth.points.todayPoints}` }}
          </CnStatusTag>
        </article>

        <article class="growth-card">
          <header>
            <span>模拟面试</span>
            <el-button text @click="goTo('/mock-interview')">进入面试</el-button>
          </header>
          <strong>{{ homeData.growth.mockInterview.totalInterviews }}</strong>
          <p>
            累计面试 {{ homeData.growth.mockInterview.totalInterviews }} 次，平均分
            {{ homeData.growth.mockInterview.avgScore }}
          </p>
          <el-progress :show-text="false" :percentage="safeCompletionRate" :stroke-width="8" status="success" />
        </article>
      </div>
    </CnSection>

    <CnSection
      class="home-section section-reveal"
      data-section="version"
      :class="{ 'is-visible': visibleSections.version }"
      title="版本播报"
      description="持续迭代记录，随时掌握平台最新能力。"
      surface="panel"
      divided
    >
      <template #actions>
        <el-button text @click="goTo('/version-history')">查看版本历史</el-button>
      </template>

      <div v-if="moduleState.version.loading" class="home-state">版本数据加载中...</div>
      <CnEmptyState
        v-else-if="!moduleState.version.available"
        title="版本数据暂不可用"
        :description="moduleState.version.message"
        icon="VER"
        size="sm"
        surface="transparent"
      />
      <CnEmptyState
        v-else-if="homeData.versions.length === 0"
        title="暂无版本记录"
        icon="VER"
        size="sm"
        surface="transparent"
      />
      <div v-else class="version-list">
        <button
          v-for="version in homeData.versions"
          :key="version.id || version.version"
          class="version-item"
          type="button"
          @click="goTo('/version-history')"
        >
          <span>
            <CnStatusTag type="brand" size="sm" :dot="false">{{ version.typeName }}</CnStatusTag>
            <strong>{{ version.title }}</strong>
            <em>{{ version.description || '点击查看完整更新说明' }}</em>
          </span>
          <span class="version-meta">
            <strong>{{ version.version }}</strong>
            <em>{{ version.dateText }}</em>
          </span>
        </button>
      </div>
    </CnSection>

    <section class="home-two-column">
      <CnSection
        class="home-section section-reveal"
        data-section="features"
        :class="{ 'is-visible': visibleSections.features }"
        title="核心功能"
        description="一站式覆盖程序员从学习到输出的关键场景。"
        surface="panel"
        divided
      >
        <div class="feature-grid">
          <button v-for="feature in features" :key="feature.title" class="feature-card" type="button" @click="goTo(feature.path)">
            <span class="feature-icon" :class="`is-${feature.tone}`">
              <el-icon><component :is="feature.icon" /></el-icon>
            </span>
            <strong>{{ feature.title }}</strong>
            <em>{{ feature.desc }}</em>
          </button>
        </div>
      </CnSection>

      <CnSection
        class="home-section section-reveal"
        data-section="quick"
        :class="{ 'is-visible': visibleSections.quick }"
        title="快速入口"
        description="常用能力一键直达。"
        surface="panel"
        divided
      >
        <div class="quick-grid">
          <button v-for="item in quickAccess" :key="item.title" class="quick-item" type="button" @click="goTo(item.path)">
            <span class="quick-icon" :class="`is-${item.tone}`">
              <el-icon><component :is="item.icon" /></el-icon>
            </span>
            <span>{{ item.title }}</span>
          </button>
        </div>
      </CnSection>
    </section>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
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
  Connection,
  Cpu,
  DataAnalysis,
  Document,
  Edit,
  Mic,
  Monitor,
  Reading,
  Share,
  Ticket,
  Trophy
} from '@element-plus/icons-vue'
import {
  CnEmptyState,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatusTag
} from '@/design-system'
import { commandSections, flattenCommandItems } from '@/config/navigation'
import { readCommandHistory } from '@/utils/command-history'
import { useHomeData } from '@/utils/home-data'
import { useHomeMotion } from '@/utils/home-motion'

const router = useRouter()
const refreshingRealtime = ref(false)

const visibleSections = ref({
  recent: false,
  hot: false,
  growth: false,
  challenge: false,
  version: false,
  features: false,
  quick: false
})

const commandItems = flattenCommandItems(commandSections)

const recentWorkspaceItems = computed(() =>
  readCommandHistory()
    .map((path) => commandItems.find((item) => item.path === path))
    .filter(Boolean)
    .slice(0, 4)
)

const features = [
  { title: '面试题库', desc: '海量真题与题单，覆盖前后端、算法与系统设计。', icon: Reading, path: '/interview', tone: 'brand' },
  { title: '知识图谱', desc: '可视化构建技术知识网络，形成长期可复用资产。', icon: Share, path: '/knowledge', tone: 'info' },
  { title: '在线简历', desc: '模板化编辑与导出，快速完成高质量简历表达。', icon: Document, path: '/resume', tone: 'brand-soft' },
  { title: '代码工坊', desc: '在线编码与作品分享，沉淀你的代码表达能力。', icon: Monitor, path: '/codepen', tone: 'success' },
  { title: '技术博客', desc: 'Markdown 写作与持续输出，打造你的技术品牌。', icon: Edit, path: '/blog', tone: 'warning' },
  { title: '在线判题', desc: '多语言实时判题，提升代码正确性与速度。', icon: Cpu, path: '/oj', tone: 'success-soft' }
]

const quickAccess = [
  { title: '朋友圈', icon: ChatLineSquare, path: '/moments', tone: 'brand' },
  { title: '聊天室', icon: ChatDotRound, path: '/chat', tone: 'info' },
  { title: '通知中心', icon: Bell, path: '/notification', tone: 'brand-soft' },
  { title: '计划打卡', icon: Calendar, path: '/plan', tone: 'warning' },
  { title: '我的积分', icon: Trophy, path: '/points', tone: 'success' },
  { title: '幸运抽奖', icon: Ticket, path: '/lottery', tone: 'warning-soft' },
  { title: '摸鱼工具', icon: Coffee, path: '/moyu-tools', tone: 'neutral' },
  { title: '开发工具', icon: Compass, path: '/dev-tools', tone: 'info' },
  { title: '版本历史', icon: DataAnalysis, path: '/version-history', tone: 'brand-soft' },
  { title: '成长驾驶舱', icon: DataAnalysis, path: '/learning-cockpit', tone: 'brand' },
  { title: '自动驾驶', icon: Trophy, path: '/learning-cockpit?tab=autopilot', tone: 'info' }
]

const heroDisplay = reactive({
  learnedCount: 0,
  knowledgeCount: 0,
  onlineCount: 0,
  hotTopicCount: 0,
  todayTaskCompletionRate: 0
})

const {
  refreshAt,
  moduleState,
  homeData,
  loadAllData,
  refreshRealtimeData,
  startAutoRefresh,
  stopAutoRefresh
} = useHomeData()

const { observeSections, bindHeroParallax, animateNumberMap, cleanup } = useHomeMotion(visibleSections)

const heroMetricCards = computed(() => [
  { key: 'learned', label: '已学习题目', value: heroDisplay.learnedCount, suffix: '' },
  { key: 'knowledge', label: '已发布图谱', value: heroDisplay.knowledgeCount, suffix: '' },
  { key: 'online', label: '实时在线', value: heroDisplay.onlineCount, suffix: '' },
  { key: 'hot', label: '热门话题', value: heroDisplay.hotTopicCount, suffix: '' },
  { key: 'plan', label: '今日完成率', value: heroDisplay.todayTaskCompletionRate, suffix: '%' }
])

const todayTaskTotal = computed(() => {
  return homeData.growth.plan.todayCompleted + homeData.growth.plan.todayPending
})

const safeCompletionRate = computed(() => {
  return Math.min(100, Math.max(0, Math.round(homeData.growth.mockInterview.completionRate)))
})

const difficultyTone = computed(() => {
  const difficulty = homeData.challenge.dailyProblem.difficulty
  if (difficulty === 'hard') return 'danger'
  if (difficulty === 'medium') return 'warning'
  return 'success'
})

const formatMetric = (metric) => {
  const value = Number(metric.value) || 0
  if (metric.suffix === '%') {
    return `${value}${metric.suffix}`
  }
  if (value >= 10000) {
    return `${(value / 10000).toFixed(1)}w${metric.suffix || ''}`
  }
  return `${value}${metric.suffix || ''}`
}

const goTo = (path?: string) => {
  if (path) {
    router.push(path)
  }
}

const handleManualRefresh = async () => {
  refreshingRealtime.value = true
  try {
    await refreshRealtimeData()
  } catch (error) {
    ElMessage.warning('实时数据刷新失败，请稍后再试')
  } finally {
    refreshingRealtime.value = false
  }
}

watch(
  () => [
    homeData.heroMetrics.learnedCount,
    homeData.heroMetrics.knowledgeCount,
    homeData.heroMetrics.onlineCount,
    homeData.heroMetrics.hotTopicCount,
    homeData.heroMetrics.todayTaskCompletionRate
  ],
  ([learnedCount, knowledgeCount, onlineCount, hotTopicCount, todayTaskCompletionRate]) => {
    animateNumberMap(
      { learnedCount, knowledgeCount, onlineCount, hotTopicCount, todayTaskCompletionRate },
      (current) => Object.assign(heroDisplay, current)
    )
  },
  { immediate: true }
)

onMounted(async () => {
  await loadAllData()
  observeSections()
  bindHeroParallax('.home-hero')
  startAutoRefresh()
})

onBeforeUnmount(() => {
  stopAutoRefresh()
  cleanup()
})
</script>

<style scoped>
.home-revamp {
  min-height: auto;
  --home-on-accent: white;
}

.home-hero {
  --hero-shift-x: 0px;
  --hero-shift-y: 0px;

  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(340px, 0.95fr);
  gap: var(--cn-space-5);
  align-items: stretch;
  min-width: 0;
  padding: var(--cn-space-6);
  overflow: hidden;
  border: 1px solid var(--cn-panel-border);
  border-radius: var(--cn-radius-panel);
  background: color-mix(in srgb, var(--cn-color-brand-soft) 42%, var(--cn-color-bg-elevated));
  box-shadow: var(--cn-shadow-card);
}

.home-hero::before {
  content: '';
  position: absolute;
  inset: 0;
  background: color-mix(in srgb, var(--cn-color-brand-primary) 7%, transparent);
  opacity: 0.18;
  pointer-events: none;
  transform: translate3d(calc(var(--hero-shift-x) * 0.14), calc(var(--hero-shift-y) * 0.14), 0);
}

.home-hero__copy,
.hero-metrics {
  position: relative;
  z-index: 1;
  min-width: 0;
}

.home-hero__copy {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: var(--cn-space-5);
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-3);
}

.hero-metrics :deep(.cn-section__body) {
  height: 100%;
}

.home-state {
  display: grid;
  min-height: 156px;
  place-items: center;
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-text-secondary);
  font-size: 13px;
}

.hero-metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cn-space-3);
}

.hero-metric-card {
  min-width: 0;
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
}

.metric-label {
  display: block;
  color: var(--cn-color-text-secondary);
  font-size: 12px;
}

.hero-metric-card strong {
  display: block;
  margin-top: var(--cn-space-2);
  color: var(--cn-color-text-primary);
  font-family: var(--cn-font-heading);
  font-size: 28px;
  line-height: 1.1;
}

.home-section {
  min-width: 0;
}

.home-two-column {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cn-space-5);
}

.section-reveal {
  opacity: 0;
  transform: translateY(20px);
  transition:
    opacity var(--cn-motion-slow) var(--cn-ease-out),
    transform var(--cn-motion-slow) var(--cn-ease-out);
}

.section-reveal.is-visible {
  opacity: 1;
  transform: translateY(0);
}

.recent-grid,
.growth-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.recent-card,
.feed-item,
.challenge-card,
.version-item,
.feature-card,
.quick-item {
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface);
  color: inherit;
  cursor: pointer;
  font: inherit;
  text-align: left;
  transition:
    transform var(--cn-motion-fast) var(--cn-ease-out),
    border-color var(--cn-motion-fast) var(--cn-ease-out),
    box-shadow var(--cn-motion-fast) var(--cn-ease-out);
}

.recent-card:hover,
.feed-item:hover,
.challenge-card:hover,
.version-item:hover,
.feature-card:hover,
.quick-item:hover {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 30%, var(--cn-color-border-subtle));
  box-shadow: var(--cn-shadow-sm);
  transform: translateY(-2px);
}

.recent-card {
  display: flex;
  align-items: center;
  gap: var(--cn-space-3);
  min-width: 0;
  padding: var(--cn-space-4);
}

.recent-icon,
.feature-icon,
.quick-icon {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  color: var(--home-on-accent);
}

.recent-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: var(--cn-color-brand-primary);
}

.recent-copy {
  display: grid;
  min-width: 0;
  gap: 4px;
  flex: 1;
}

.recent-copy strong,
.feed-item strong,
.challenge-card strong,
.version-item strong,
.feature-card strong {
  min-width: 0;
  color: var(--cn-color-text-primary);
  font-weight: 650;
}

.recent-copy em,
.feed-item span,
.challenge-meta,
.version-item em,
.feature-card em {
  min-width: 0;
  overflow: hidden;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  font-style: normal;
  line-height: 1.6;
  text-overflow: ellipsis;
}

.card-arrow {
  color: var(--cn-color-text-tertiary);
  flex-shrink: 0;
}

.hot-stack,
.challenge-stack,
.version-list {
  display: grid;
  gap: var(--cn-space-4);
}

.feed-group {
  display: grid;
  gap: var(--cn-space-2);
}

.feed-title {
  display: flex;
  align-items: center;
  gap: var(--cn-space-2);
  color: var(--cn-color-text-primary);
  font-weight: 650;
}

.feed-item {
  display: grid;
  width: 100%;
  gap: 4px;
  padding: var(--cn-space-3);
}

.challenge-card {
  display: grid;
  gap: var(--cn-space-3);
  width: 100%;
  padding: var(--cn-space-4);
}

.challenge-card.is-secondary {
  background: var(--cn-color-bg-surface-muted);
}

.challenge-kicker {
  color: var(--cn-color-brand-primary);
  font-size: 12px;
  font-weight: 700;
}

.challenge-meta,
.tag-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--cn-space-2);
}

.growth-card {
  display: grid;
  min-width: 0;
  gap: var(--cn-space-3);
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface);
}

.growth-card header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-2);
  color: var(--cn-color-text-primary);
  font-weight: 650;
}

.growth-card strong {
  color: var(--cn-color-brand-primary);
  font-family: var(--cn-font-heading);
  font-size: 34px;
  line-height: 1;
}

.growth-card p {
  min-height: 42px;
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.65;
}

.version-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: var(--cn-space-4);
  width: 100%;
  padding: var(--cn-space-4);
}

.version-item > span:first-child,
.version-meta {
  display: grid;
  gap: var(--cn-space-2);
}

.version-meta {
  justify-items: end;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.feature-card {
  display: grid;
  min-width: 0;
  gap: var(--cn-space-3);
  padding: var(--cn-space-4);
}

.feature-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--cn-space-3);
}

.quick-item {
  display: grid;
  justify-items: center;
  gap: var(--cn-space-2);
  min-width: 0;
  padding: var(--cn-space-3) var(--cn-space-2);
  text-align: center;
}

.quick-icon {
  width: 38px;
  height: 38px;
  border-radius: 11px;
}

.feature-icon.is-brand,
.quick-icon.is-brand {
  background: var(--cn-color-brand-primary);
}

.feature-icon.is-brand-soft,
.quick-icon.is-brand-soft {
  background: color-mix(in srgb, var(--cn-color-brand-primary) 82%, var(--cn-color-info));
}

.feature-icon.is-info,
.quick-icon.is-info {
  background: var(--cn-color-info);
}

.feature-icon.is-success,
.quick-icon.is-success {
  background: var(--cn-color-success);
}

.feature-icon.is-success-soft,
.quick-icon.is-success-soft {
  background: color-mix(in srgb, var(--cn-color-success) 82%, var(--cn-color-info));
}

.feature-icon.is-warning,
.quick-icon.is-warning {
  background: var(--cn-color-warning);
}

.feature-icon.is-warning-soft,
.quick-icon.is-warning-soft {
  background: color-mix(in srgb, var(--cn-color-warning) 84%, var(--cn-color-brand-primary));
}

.feature-icon.is-neutral,
.quick-icon.is-neutral {
  background: var(--cn-color-text-tertiary);
}

.quick-item span:last-child {
  max-width: 100%;
  overflow: hidden;
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1180px) {
  .home-hero,
  .home-two-column {
    grid-template-columns: minmax(0, 1fr);
  }

  .recent-grid,
  .growth-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .home-hero {
    padding: var(--cn-space-4);
  }

  .hero-actions {
    display: grid;
  }

  .hero-metric-grid,
  .recent-grid,
  .growth-grid,
  .feature-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .version-item {
    grid-template-columns: minmax(0, 1fr);
  }

  .version-meta {
    justify-items: start;
  }
}

@media (max-width: 520px) {
  .quick-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (prefers-reduced-motion: reduce) {
  .section-reveal,
  .recent-card,
  .feed-item,
  .challenge-card,
  .version-item,
  .feature-card,
  .quick-item {
    transition: none !important;
    transform: none !important;
  }

  .section-reveal {
    opacity: 1 !important;
  }
}
</style>
