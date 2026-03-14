<template>
  <div class="home-container">
    <section class="hero-section" aria-label="首页首屏">
      <div class="hero-bg" aria-hidden="true"></div>

      <div class="hero-layout">
        <div class="hero-brand cn-stagger-enter" style="--stagger-delay: 40ms">
          <span class="brand-badge">开发者成长社区</span>
          <h1 class="hero-title">Code Nest</h1>
          <p class="hero-sub-title">更强内容、更快成长、更高效沉淀</p>
          <p class="hero-desc">
            在一个站点完成刷题、知识体系建设、社区互动、计划打卡、AI 模拟面试，让学习和输出形成闭环。
          </p>
          <div class="hero-actions">
            <el-button type="primary" size="large" class="hero-btn" @click="goTo('/interview')">
              <el-icon><Reading /></el-icon>
              立即刷题
            </el-button>
            <el-button size="large" class="hero-btn ghost" @click="goTo('/community')">
              <el-icon><ChatDotRound /></el-icon>
              进入社区
            </el-button>
          </div>
          <div class="hero-live">
            <span>实时刷新 {{ refreshAt || '--:--:--' }}</span>
            <el-button text :loading="refreshingRealtime" @click="handleManualRefresh">手动刷新</el-button>
          </div>
        </div>

        <div class="hero-panel cn-stagger-enter cn-glow-border cn-card-shimmer" style="--stagger-delay: 130ms">
          <div class="panel-head">
            <h3>成长实时驾驶舱</h3>
            <span class="panel-dot" :class="{ ready: moduleState.hero.available }"></span>
          </div>
          <div v-if="moduleState.hero.loading" class="panel-state">正在加载实时数据...</div>
          <div v-else-if="!moduleState.hero.available" class="panel-state error">{{ moduleState.hero.message }}</div>
          <div v-else class="metric-grid">
            <div v-for="metric in heroMetricCards" :key="metric.key" class="metric-card cn-tilt-card">
              <p class="metric-label">{{ metric.label }}</p>
              <p class="metric-value">{{ formatMetric(metric) }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="content-section section-reveal" data-section="hot" :class="{ 'is-visible': visibleSections.hot }">
      <div class="section-head">
        <h2>今日热门</h2>
        <p>真实社区热帖 + 动态广场，直接看今天最有价值的讨论</p>
      </div>
      <div class="two-col">
        <article class="glass-card">
          <header class="card-head">
            <h3><el-icon><DataAnalysis /></el-icon> 社区热帖</h3>
            <el-button text @click="goTo('/community')">更多</el-button>
          </header>
          <div v-if="moduleState.hot.loading" class="block-state">拉取热门帖子中...</div>
          <div v-else-if="!moduleState.hot.available" class="block-state error">{{ moduleState.hot.message }}</div>
          <div v-else-if="homeData.hotFeed.posts.length === 0" class="block-state">暂无热门帖子</div>
          <ul v-else class="simple-list">
            <li v-for="post in homeData.hotFeed.posts" :key="`post-${post.id}`" @click="goTo(post.routePath)">
              <p class="item-title">{{ post.title }}</p>
              <p class="item-meta">{{ post.authorName }} · {{ post.likeCount }}赞 · {{ post.commentCount }}评</p>
            </li>
          </ul>
        </article>
        <article class="glass-card">
          <header class="card-head">
            <h3><el-icon><Connection /></el-icon> 热门动态</h3>
            <el-button text @click="goTo('/moments')">更多</el-button>
          </header>
          <div v-if="moduleState.hot.loading" class="block-state">拉取热门动态中...</div>
          <div v-else-if="!moduleState.hot.available" class="block-state error">{{ moduleState.hot.message }}</div>
          <div v-else-if="homeData.hotFeed.moments.length === 0" class="block-state">暂无热门动态</div>
          <ul v-else class="simple-list">
            <li v-for="moment in homeData.hotFeed.moments" :key="`moment-${moment.id}`" @click="goTo(moment.routePath)">
              <p class="item-title">{{ moment.title }}</p>
              <p class="item-meta">{{ moment.authorName }} · {{ moment.likeCount }}赞 · {{ moment.commentCount }}评</p>
            </li>
          </ul>
        </article>
      </div>
    </section>

    <section class="content-section section-reveal" data-section="growth" :class="{ 'is-visible': visibleSections.growth }">
      <div class="section-head">
        <h2>成长驾驶舱</h2>
        <p>学习执行、面试练习、积分激励，三条成长曲线同屏可见</p>
      </div>
      <div v-if="moduleState.growth.loading" class="block-state">成长数据加载中...</div>
      <div v-else-if="!moduleState.growth.available" class="block-state error">{{ moduleState.growth.message }}</div>
      <div v-else class="three-col">
        <article class="growth-card cn-tilt-card">
          <div class="growth-head">
            <h3>计划打卡</h3>
            <el-button text @click="goTo('/plan')">进入计划</el-button>
          </div>
          <p class="growth-value">{{ homeData.growth.plan.todayCompletionRate }}%</p>
          <p class="growth-desc">
            今日完成 {{ homeData.growth.plan.todayCompleted }} / {{ todayTaskTotal }}，累计打卡 {{ homeData.growth.plan.totalCheckins }} 次
          </p>
          <el-progress :show-text="false" :percentage="homeData.growth.plan.todayCompletionRate" :stroke-width="8" />
        </article>
        <article class="growth-card cn-tilt-card">
          <div class="growth-head">
            <h3>积分资产</h3>
            <el-button text @click="goTo('/points')">积分中心</el-button>
          </div>
          <p class="growth-value">{{ homeData.growth.points.totalPoints }}</p>
          <p class="growth-desc">价值约 ¥{{ homeData.growth.points.balanceYuan }}，连续打卡 {{ homeData.growth.points.continuousDays }} 天</p>
          <span class="badge" :class="{ active: homeData.growth.points.todayCheckedIn }">
            {{ homeData.growth.points.todayCheckedIn ? '今日已打卡' : `今日可得 +${homeData.growth.points.todayPoints}` }}
          </span>
        </article>
        <article class="growth-card cn-tilt-card">
          <div class="growth-head">
            <h3>模拟面试</h3>
            <el-button text @click="goTo('/mock-interview')">进入面试</el-button>
          </div>
          <p class="growth-value">{{ homeData.growth.mockInterview.totalInterviews }}</p>
          <p class="growth-desc">
            累计面试 {{ homeData.growth.mockInterview.totalInterviews }} 次，平均分 {{ homeData.growth.mockInterview.avgScore }}
          </p>
          <el-progress :show-text="false" :percentage="safeCompletionRate" :stroke-width="8" status="success" />
        </article>
      </div>
    </section>

    <section class="content-section section-reveal" data-section="challenge" :class="{ 'is-visible': visibleSections.challenge }">
      <div class="section-head">
        <h2>每日挑战</h2>
        <p>一题算法 + 一轮 AI 模拟面试，把练习强度拉满</p>
      </div>
      <div class="two-col">
        <article class="challenge-card cn-card-shimmer">
          <header class="card-head">
            <h3><el-icon><Cpu /></el-icon> OJ 每日一题</h3>
            <el-button text @click="goTo('/oj')">更多题目</el-button>
          </header>
          <div v-if="moduleState.challenge.loading" class="block-state">正在获取每日一题...</div>
          <div v-else-if="!moduleState.challenge.available" class="block-state error">{{ moduleState.challenge.message }}</div>
          <template v-else>
            <h4 class="challenge-title">{{ homeData.challenge.dailyProblem.title }}</h4>
            <div class="challenge-meta">
              <span class="difficulty" :class="difficultyClass">{{ homeData.challenge.dailyProblem.difficultyText }}</span>
              <span>通过率 {{ homeData.challenge.dailyProblem.acceptanceRate }}%</span>
              <span>{{ homeData.challenge.dailyProblem.acceptedCount }}/{{ homeData.challenge.dailyProblem.submitCount }}</span>
            </div>
            <div class="tags">
              <span v-for="tag in homeData.challenge.dailyProblem.tags" :key="tag">{{ tag }}</span>
            </div>
            <el-button type="primary" class="challenge-btn" @click="goTo(homeData.challenge.dailyProblem.routePath)">
              开始挑战
              <el-icon class="el-icon--right"><ArrowRight /></el-icon>
            </el-button>
          </template>
        </article>
        <article class="challenge-card cn-card-shimmer">
          <header class="card-head">
            <h3><el-icon><Mic /></el-icon> AI 模拟面试</h3>
            <el-button text @click="goTo('/mock-interview/history')">历史记录</el-button>
          </header>
          <p class="challenge-value">最高分 {{ homeData.growth.mockInterview.highestScore }}</p>
          <p class="growth-desc">本周安排 3 轮结构化面试训练，覆盖项目追问、算法推演和系统设计表达。</p>
          <div class="hero-actions">
            <el-button type="primary" @click="goTo('/mock-interview')">马上开始</el-button>
            <el-button @click="goTo('/mock-interview/history')">查看报告</el-button>
          </div>
        </article>
      </div>
    </section>

    <section class="content-section section-reveal" data-section="version" :class="{ 'is-visible': visibleSections.version }">
      <div class="section-head">
        <h2>版本播报</h2>
        <p>持续迭代记录，随时掌握平台最新能力</p>
      </div>
      <article class="glass-card">
        <div v-if="moduleState.version.loading" class="block-state">版本数据加载中...</div>
        <div v-else-if="!moduleState.version.available" class="block-state error">{{ moduleState.version.message }}</div>
        <div v-else-if="homeData.versions.length === 0" class="block-state">暂无版本记录</div>
        <ul v-else class="version-list">
          <li v-for="version in homeData.versions" :key="version.id || version.version" @click="goTo('/version-history')">
            <div>
              <span class="badge">{{ version.typeName }}</span>
              <h4>{{ version.title }}</h4>
              <p>{{ version.description || '点击查看完整更新说明' }}</p>
            </div>
            <div class="version-right">
              <span>{{ version.version }}</span>
              <small>{{ version.dateText }}</small>
            </div>
          </li>
        </ul>
      </article>
    </section>

    <section class="content-section section-reveal" data-section="features" :class="{ 'is-visible': visibleSections.features }">
      <div class="section-head">
        <h2>核心功能</h2>
        <p>一站式覆盖程序员从学习到输出的关键场景</p>
      </div>
      <div class="feature-grid">
        <article v-for="feature in features" :key="feature.title" class="feature-card cn-hover-lift" @click="goTo(feature.path)">
          <div class="feature-icon" :style="{ background: feature.gradient }">
            <el-icon :size="20"><component :is="feature.icon" /></el-icon>
          </div>
          <h3>{{ feature.title }}</h3>
          <p>{{ feature.desc }}</p>
        </article>
      </div>
    </section>

    <section class="content-section section-reveal" data-section="quick" :class="{ 'is-visible': visibleSections.quick }">
      <div class="section-head">
        <h2>快速入口</h2>
        <p>常用能力一键直达</p>
      </div>
      <div class="quick-grid">
        <article v-for="item in quickAccess" :key="item.title" class="quick-item cn-hover-lift" @click="goTo(item.path)">
          <div class="quick-icon" :style="{ background: item.color }">
            <el-icon :size="18"><component :is="item.icon" /></el-icon>
          </div>
          <span>{{ item.title }}</span>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
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
import { useHomeData } from '@/utils/home-data'
import { useHomeMotion } from '@/utils/home-motion'

const router = useRouter()
const refreshingRealtime = ref(false)

const visibleSections = ref({
  hot: false,
  growth: false,
  challenge: false,
  version: false,
  features: false,
  quick: false
})

const features = [
  { title: '面试题库', desc: '海量真题与题单，覆盖前后端、算法与系统设计。', icon: Reading, path: '/interview', gradient: 'linear-gradient(135deg, #2f7bff 0%, #1f6feb 100%)' },
  { title: '知识图谱', desc: '可视化构建技术知识网络，形成长期可复用资产。', icon: Share, path: '/knowledge', gradient: 'linear-gradient(135deg, #26a6f4 0%, #1f6feb 100%)' },
  { title: '在线简历', desc: '模板化编辑 + 导出，快速完成高质量简历表达。', icon: Document, path: '/resume', gradient: 'linear-gradient(135deg, #3a8fff 0%, #58bbff 100%)' },
  { title: '代码工坊', desc: '在线编码与作品分享，沉淀你的代码表达能力。', icon: Monitor, path: '/codepen', gradient: 'linear-gradient(135deg, #2fb987 0%, #20a0a8 100%)' },
  { title: '技术博客', desc: 'Markdown 写作与持续输出，打造你的技术品牌。', icon: Edit, path: '/blog', gradient: 'linear-gradient(135deg, #ff9d4a 0%, #f07b34 100%)' },
  { title: '在线判题', desc: '多语言实时判题，提升代码正确性与速度。', icon: Cpu, path: '/oj', gradient: 'linear-gradient(135deg, #15a7a1 0%, #24c78e 100%)' }
]

const quickAccess = [
  { title: '朋友圈', icon: ChatLineSquare, path: '/moments', color: 'linear-gradient(135deg, #4f89ff, #2f65d9)' },
  { title: '聊天室', icon: ChatDotRound, path: '/chat', color: 'linear-gradient(135deg, #4b8ef8, #20a0f3)' },
  { title: '通知中心', icon: Bell, path: '/notification', color: 'linear-gradient(135deg, #20a0f3, #1f6feb)' },
  { title: '计划打卡', icon: Calendar, path: '/plan', color: 'linear-gradient(135deg, #f59e42, #ef7d2c)' },
  { title: '我的积分', icon: Trophy, path: '/points', color: 'linear-gradient(135deg, #20b486, #13a67c)' },
  { title: '幸运抽奖', icon: Ticket, path: '/lottery', color: 'linear-gradient(135deg, #f5b43b, #e0861c)' },
  { title: '摸鱼工具', icon: Coffee, path: '/moyu-tools', color: 'linear-gradient(135deg, #688eff, #446ddc)' },
  { title: '开发工具', icon: Compass, path: '/dev-tools', color: 'linear-gradient(135deg, #2ea8d0, #2f78cd)' },
  { title: '版本历史', icon: DataAnalysis, path: '/version-history', color: 'linear-gradient(135deg, #6aa8ff, #3b76de)' },
  { title: '成长驾驶舱 2.0', icon: DataAnalysis, path: '/learning-cockpit', color: 'linear-gradient(135deg, #1f6feb, #0fa4ef)' },
  { title: '自动驾驶', icon: Trophy, path: '/learning-cockpit?tab=autopilot', color: 'linear-gradient(135deg, #0f78e7, #33b2ff)' }
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
  { key: 'hot', label: '今日热门话题', value: heroDisplay.hotTopicCount, suffix: '' },
  { key: 'plan', label: '今日任务完成率', value: heroDisplay.todayTaskCompletionRate, suffix: '%' }
])

const todayTaskTotal = computed(() => {
  return homeData.growth.plan.todayCompleted + homeData.growth.plan.todayPending
})

const safeCompletionRate = computed(() => {
  return Math.min(100, Math.max(0, Math.round(homeData.growth.mockInterview.completionRate)))
})

const difficultyClass = computed(() => `difficulty-${homeData.challenge.dailyProblem.difficulty || 'easy'}`)

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

const goTo = (path) => {
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
  bindHeroParallax('.hero-section')
  startAutoRefresh()
})

onBeforeUnmount(() => {
  stopAutoRefresh()
  cleanup()
})
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  color: #1c3659;
  background: linear-gradient(180deg, #f7fbff 0%, #eef5ff 100%);
  padding-bottom: 60px;
}

.hero-section {
  position: relative;
  padding: 58px 20px 48px;
  --hero-shift-x: 0px;
  --hero-shift-y: 0px;
}

.hero-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 12% 16%, rgba(67, 142, 255, 0.26) 0%, transparent 36%),
    radial-gradient(circle at 87% 20%, rgba(76, 182, 255, 0.18) 0%, transparent 32%),
    linear-gradient(165deg, #f9fcff 0%, #f1f7ff 54%, #eaf3ff 100%);
  transform: translate3d(calc(var(--hero-shift-x) * 0.22), calc(var(--hero-shift-y) * 0.22), 0);
}

.hero-layout {
  position: relative;
  z-index: 1;
  max-width: 1240px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 18px;
}

.hero-brand,
.hero-panel,
.glass-card,
.growth-card,
.challenge-card {
  border: 1px solid rgba(205, 224, 249, 0.95);
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 16px 32px rgba(24, 54, 98, 0.08);
  border-radius: 18px;
}

.hero-brand,
.hero-panel {
  padding: 28px;
}

.brand-badge {
  display: inline-flex;
  align-items: center;
  padding: 7px 14px;
  border-radius: 999px;
  color: #1d63c0;
  background: rgba(69, 140, 246, 0.1);
  font-size: 12px;
  border: 1px solid rgba(69, 140, 246, 0.22);
}

.hero-title {
  margin: 14px 0 6px;
  font-size: clamp(42px, 7vw, 64px);
  line-height: 1;
  color: #246fdc;
  letter-spacing: -1px;
}

.hero-sub-title {
  margin: 0 0 12px;
  font-size: clamp(18px, 2.6vw, 28px);
  color: #1f4069;
  font-weight: 700;
}

.hero-desc {
  margin: 0;
  color: #4c698e;
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
  flex-wrap: wrap;
}

.hero-btn {
  min-width: 126px;
  border-radius: 12px;
}

.hero-btn.ghost {
  border: 1px solid rgba(54, 122, 229, 0.28);
  color: #2d63b1;
}

.hero-live {
  margin-top: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #5f7da3;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.panel-head h3 {
  margin: 0;
  color: #1e3b62;
}

.panel-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #cfdcf2;
}

.panel-dot.ready {
  background: #2fbc87;
  box-shadow: 0 0 0 5px rgba(47, 188, 135, 0.16);
}

.panel-state,
.block-state {
  padding: 14px;
  border-radius: 12px;
  text-align: center;
  color: #5a769a;
  background: #f1f7ff;
}

.panel-state.error,
.block-state.error {
  background: #fff0f0;
  color: #b44a4a;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.metric-card {
  border: 1px solid #dbe8fb;
  background: linear-gradient(170deg, #ffffff, #f6faff);
  border-radius: 13px;
  padding: 12px;
}

.metric-label {
  margin: 0 0 8px;
  font-size: 12px;
  color: #5c799c;
}

.metric-value {
  margin: 0;
  font-size: 24px;
  color: #1f5fb5;
  font-weight: 700;
}

.content-section {
  max-width: 1240px;
  margin: 0 auto;
  padding: 34px 20px 0;
}

.section-head {
  margin-bottom: 14px;
}

.section-head h2 {
  margin: 0 0 8px;
  font-size: clamp(24px, 3vw, 34px);
  color: #203d66;
}

.section-head p {
  margin: 0;
  color: #5a769c;
}

.section-reveal {
  opacity: 0;
  transform: translateY(24px);
  transition:
    opacity var(--cn-motion-slow) var(--cn-ease-out),
    transform var(--cn-motion-slow) var(--cn-ease-out);
}

.section-reveal.is-visible {
  opacity: 1;
  transform: translateY(0);
}

.two-col {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.three-col {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.glass-card,
.growth-card,
.challenge-card {
  padding: 18px;
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.card-head h3 {
  margin: 0;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #24466f;
}

.simple-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.simple-list li {
  cursor: pointer;
  border: 1px solid #deebfc;
  border-radius: 10px;
  background: #f9fcff;
  padding: 10px;
}

.item-title {
  margin: 0 0 6px;
  color: #1f3c64;
  line-height: 1.45;
}

.item-meta {
  margin: 0;
  color: #6381a5;
  font-size: 12px;
}

.growth-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.growth-head h3 {
  margin: 0;
}

.growth-value,
.challenge-value {
  margin: 0;
  font-size: 34px;
  font-weight: 800;
  color: #1f63c6;
}

.growth-desc {
  margin: 9px 0 12px;
  color: #5c799e;
  line-height: 1.65;
  min-height: 44px;
}

.badge {
  display: inline-flex;
  align-items: center;
  height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  color: #466a95;
  border: 1px solid #d7e6fc;
  background: #eef5ff;
}

.badge.active {
  color: #197a58;
  border-color: #b6ebd2;
  background: #e7f9f1;
}

.challenge-title {
  margin: 0 0 10px;
  color: #1f3d65;
  font-size: 21px;
}

.challenge-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  color: #5d789d;
  font-size: 13px;
}

.difficulty {
  height: 22px;
  padding: 0 9px;
  border-radius: 999px;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
}

.difficulty-easy {
  color: #1a8d4f;
  background: #ecf8ef;
  border: 1px solid #bfe8cf;
}

.difficulty-medium {
  color: #c47a17;
  background: #fff6e9;
  border: 1px solid #f0d2a0;
}

.difficulty-hard {
  color: #bf4d4d;
  background: #fff0f0;
  border: 1px solid #efc2c2;
}

.tags {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tags span {
  height: 24px;
  padding: 0 9px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  color: #496e99;
  border: 1px solid #d9e7fb;
  background: #f0f7ff;
  font-size: 12px;
}

.challenge-btn {
  margin-top: 14px;
  border-radius: 11px;
}

.version-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.version-list li {
  border: 1px solid #dce8fb;
  border-radius: 12px;
  background: #f9fcff;
  padding: 12px;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  cursor: pointer;
}

.version-list h4 {
  margin: 8px 0 6px;
  color: #1f3c64;
}

.version-list p {
  margin: 0;
  color: #5f7ca1;
  font-size: 13px;
}

.version-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  color: #285fb3;
}

.version-right small {
  color: #6f89aa;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.feature-card {
  border: 1px solid #dae7f9;
  border-radius: 14px;
  background: #fff;
  padding: 16px;
  box-shadow: 0 8px 20px rgba(20, 52, 96, 0.06);
  cursor: pointer;
}

.feature-icon {
  width: 42px;
  height: 42px;
  border-radius: 11px;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.feature-card h3 {
  margin: 12px 0 8px;
  color: #234366;
}

.feature-card p {
  margin: 0;
  color: #5c799f;
  line-height: 1.65;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(10, minmax(0, 1fr));
  gap: 10px;
}

.quick-item {
  border: 1px solid #dce9fb;
  border-radius: 13px;
  background: #f8fbff;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 7px;
  padding: 10px 6px;
  cursor: pointer;
}

.quick-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.quick-item span {
  font-size: 12px;
  color: #2f4f78;
}

@media (max-width: 1200px) {
  .hero-layout {
    grid-template-columns: 1fr;
  }

  .feature-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .quick-grid {
    grid-template-columns: repeat(6, minmax(0, 1fr));
  }
}

@media (max-width: 992px) {
  .two-col,
  .three-col,
  .feature-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .hero-section {
    padding-top: 36px;
  }

  .hero-brand,
  .hero-panel {
    padding: 20px;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }

  .quick-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .version-list li {
    grid-template-columns: 1fr;
  }

  .version-right {
    align-items: flex-start;
  }
}

@media (max-width: 480px) {
  .hero-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .quick-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
