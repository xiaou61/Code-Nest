<template>
  <div class="home-container">
    <!-- Hero 区域 -->
    <section class="hero-section">
      <div class="hero-bg">
        <div class="floating-shapes">
          <div class="shape shape-1"></div>
          <div class="shape shape-2"></div>
          <div class="shape shape-3"></div>
          <div class="shape shape-4"></div>
          <div class="shape shape-5"></div>
        </div>
      </div>
      <div class="hero-content">
        <div class="hero-badge">开发者成长社区</div>
        <h1 class="hero-title">
          <span class="gradient-text">Code Nest</span>
          <br />
          <span class="sub-title">程序员的技术成长乐园</span>
        </h1>
        <p class="hero-desc">
          集面试刷题、知识图谱、在线简历、代码工坊、技术博客、社区互动于一体的一站式开发者平台
        </p>
        <div class="hero-actions">
          <el-button type="primary" size="large" class="action-btn primary-btn" @click="$router.push('/interview')">
            <el-icon><Reading /></el-icon>
            开始刷题
          </el-button>
          <el-button size="large" class="action-btn secondary-btn" @click="$router.push('/community')">
            <el-icon><ChatDotRound /></el-icon>
            进入社区
          </el-button>
        </div>
        <div class="hero-stats">
          <template v-for="(stat, index) in stats" :key="stat.label">
            <div class="stat-item">
              <span class="stat-number">{{ stat.display }}{{ stat.suffix }}</span>
              <span class="stat-label">{{ stat.label }}</span>
            </div>
            <div v-if="index < stats.length - 1" class="stat-divider"></div>
          </template>
        </div>
      </div>
    </section>

    <!-- 核心功能区域 -->
    <section
      class="features-section section-reveal"
      data-section="features"
      :class="{ 'is-visible': visibleSections.features }"
    >
      <div class="section-header">
        <h2 class="section-title">核心功能</h2>
        <p class="section-subtitle">全方位助力你的技术成长之路</p>
      </div>
      <div class="features-grid">
        <div 
          v-for="(feature, index) in features" 
          :key="feature.title" 
          class="feature-card cn-hover-lift"
          :style="{ '--delay': `${index * 70}ms` }"
          @click="$router.push(feature.path)"
        >
          <div class="feature-icon" :style="{ background: feature.gradient }">
            <el-icon :size="28"><component :is="feature.icon" /></el-icon>
          </div>
          <h3 class="feature-title">{{ feature.title }}</h3>
          <p class="feature-desc">{{ feature.desc }}</p>
          <div class="feature-arrow">
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>
      </div>
    </section>

    <!-- 快速入口区域 -->
    <section
      class="quick-access-section section-reveal"
      data-section="quick"
      :class="{ 'is-visible': visibleSections.quick }"
    >
      <div class="section-header">
        <h2 class="section-title">快速入口</h2>
        <p class="section-subtitle">一键直达常用功能</p>
      </div>
      <div class="quick-access-grid">
        <div 
          v-for="item in quickAccess" 
          :key="item.title" 
          class="quick-item cn-hover-lift"
          @click="$router.push(item.path)"
        >
          <div class="quick-icon" :style="{ background: item.color }">
            <el-icon :size="24"><component :is="item.icon" /></el-icon>
          </div>
          <span class="quick-title">{{ item.title }}</span>
        </div>
      </div>
    </section>

    <!-- 特色亮点区域 -->
    <section
      class="highlights-section section-reveal"
      data-section="highlights"
      :class="{ 'is-visible': visibleSections.highlights }"
    >
      <div class="highlights-content">
        <div class="highlight-item cn-hover-lift">
          <div class="highlight-icon">
            <el-icon :size="32"><Cpu /></el-icon>
          </div>
          <div class="highlight-info">
            <h3>智能学习系统</h3>
            <p>基于知识图谱的学习路径规划，助你高效提升技术能力</p>
          </div>
        </div>
        <div class="highlight-item cn-hover-lift">
          <div class="highlight-icon">
            <el-icon :size="32"><Trophy /></el-icon>
          </div>
          <div class="highlight-info">
            <h3>积分激励体系</h3>
            <p>完成任务获取积分，参与抽奖赢取丰厚奖品</p>
          </div>
        </div>
        <div class="highlight-item cn-hover-lift">
          <div class="highlight-icon">
            <el-icon :size="32"><Connection /></el-icon>
          </div>
          <div class="highlight-info">
            <h3>实时互动社区</h3>
            <p>技术讨论、在线聊天、动态分享，与志同道合的开发者交流</p>
          </div>
        </div>
      </div>
    </section>

    <!-- 底部 CTA -->
    <section
      class="cta-section section-reveal"
      data-section="cta"
      :class="{ 'is-visible': visibleSections.cta }"
    >
      <div class="cta-content">
        <h2>准备好开始你的技术成长之旅了吗？</h2>
        <p>加入 Code Nest，与万千开发者一起学习进步</p>
        <el-button type="primary" size="large" class="cta-btn" @click="$router.push('/interview')">
          立即开始
          <el-icon class="el-icon--right"><ArrowRight /></el-icon>
        </el-button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  Reading, ChatDotRound, ArrowRight, Document, Edit, 
  Monitor, Share, Bell, Trophy, Cpu, Connection,
  Compass, Coffee, Ticket, ChatLineSquare, DataAnalysis, Calendar
} from '@element-plus/icons-vue'
import { interviewApi } from '@/api/interview'
import { getPublishedKnowledgeMaps } from '@/api/knowledge'
import { getOnlineCount } from '@/api/chat'

// 核心功能数据
const features = ref([
  {
    title: '面试题库',
    desc: '海量面试真题，覆盖前端、后端、算法等热门方向，助你斩获心仪 Offer',
    icon: Reading,
    path: '/interview',
    gradient: 'linear-gradient(135deg, #2f7bff 0%, #1f6feb 100%)'
  },
  {
    title: '知识图谱',
    desc: '可视化知识体系，理清技术脉络，构建完整的技术知识网络',
    icon: Share,
    path: '/knowledge',
    gradient: 'linear-gradient(135deg, #20a0f3 0%, #1f6feb 100%)'
  },
  {
    title: '在线简历',
    desc: '专业简历模板，在线编辑导出，让你的简历脱颖而出',
    icon: Document,
    path: '/resume',
    gradient: 'linear-gradient(135deg, #2d8df7 0%, #48b6ff 100%)'
  },
  {
    title: '代码工坊',
    desc: '在线代码编辑器，支持多种语言，实时预览与分享你的创意',
    icon: Monitor,
    path: '/codepen',
    gradient: 'linear-gradient(135deg, #2fb987 0%, #20a0a8 100%)'
  },
  {
    title: '技术博客',
    desc: 'Markdown 写作，记录学习心得，分享技术经验',
    icon: Edit,
    path: '/blog',
    gradient: 'linear-gradient(135deg, #ff9d4a 0%, #f07b34 100%)'
  },
  {
    title: '技术社区',
    desc: '问答交流、技术讨论，与全国开发者一起探讨技术难题',
    icon: ChatDotRound,
    path: '/community',
    gradient: 'linear-gradient(135deg, #5f8bff 0%, #3a6be0 100%)'
  },
  {
    title: '在线判题',
    desc: '支持多语言在线编程，实时判题反馈，提升算法与编码能力',
    icon: Cpu,
    path: '/oj',
    gradient: 'linear-gradient(135deg, #15a7a1 0%, #24c78e 100%)'
  }
])

// 快速入口数据
const quickAccess = ref([
  { title: '朋友圈', icon: ChatLineSquare, path: '/moments', color: 'linear-gradient(135deg, #4f89ff, #2f65d9)' },
  { title: '聊天室', icon: ChatDotRound, path: '/chat', color: 'linear-gradient(135deg, #4b8ef8, #20a0f3)' },
  { title: '通知中心', icon: Bell, path: '/notification', color: 'linear-gradient(135deg, #20a0f3, #1f6feb)' },
  { title: '计划打卡', icon: Calendar, path: '/plan', color: 'linear-gradient(135deg, #f59e42, #ef7d2c)' },
  { title: '我的积分', icon: Trophy, path: '/points', color: 'linear-gradient(135deg, #20b486, #13a67c)' },
  { title: '幸运抽奖', icon: Ticket, path: '/lottery', color: 'linear-gradient(135deg, #f5b43b, #e0861c)' },
  { title: '摸鱼工具', icon: Coffee, path: '/moyu-tools', color: 'linear-gradient(135deg, #688eff, #446ddc)' },
  { title: '开发工具', icon: Compass, path: '/dev-tools', color: 'linear-gradient(135deg, #2ea8d0, #2f78cd)' },
  { title: '版本历史', icon: DataAnalysis, path: '/version-history', color: 'linear-gradient(135deg, #6aa8ff, #3b76de)' }
])

const stats = ref([
  { label: '已学习题目', value: 0, suffix: '', display: 0 },
  { label: '已发布图谱', value: 0, suffix: '', display: 0 },
  { label: '实时在线', value: 0, suffix: '', display: 0 }
])

const visibleSections = ref({
  features: false,
  quick: false,
  highlights: false,
  cta: false
})

let sectionObserver = null

const animateStats = () => {
  const duration = 1300
  const startTime = performance.now()

  const update = (now) => {
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
    stats.value[1].value = Number(knowledgeMaps?.total) || 0
    stats.value[2].value = Number(onlineCount) || 0
  } catch (error) {
    ElMessage.warning('首页统计加载失败，已展示默认值')
    stats.value.forEach((item) => {
      item.value = 0
    })
  }
}

const observeSections = () => {
  if (!('IntersectionObserver' in window)) {
    Object.keys(visibleSections.value).forEach((key) => {
      visibleSections.value[key] = true
    })
    return
  }

  sectionObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting) return
        const section = entry.target.dataset.section
        if (section && Object.prototype.hasOwnProperty.call(visibleSections.value, section)) {
          visibleSections.value[section] = true
        }
        sectionObserver.unobserve(entry.target)
      })
    },
    { threshold: 0.15, rootMargin: '0px 0px -10% 0px' }
  )

  document.querySelectorAll('.section-reveal').forEach((element) => {
    sectionObserver.observe(element)
  })
}

onMounted(async () => {
  await loadHeroStats()
  animateStats()
  observeSections()
})

onBeforeUnmount(() => {
  if (sectionObserver) {
    sectionObserver.disconnect()
    sectionObserver = null
  }
})
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  overflow-x: hidden;
  background: transparent;
  color: var(--cn-text-primary);
}

.hero-section {
  position: relative;
  min-height: 88vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 72px 20px 56px;
  overflow: hidden;
}

.hero-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 12% 18%, rgba(56, 131, 255, 0.24) 0%, rgba(56, 131, 255, 0) 34%),
    radial-gradient(circle at 92% 20%, rgba(61, 174, 255, 0.2) 0%, rgba(61, 174, 255, 0) 38%),
    linear-gradient(165deg, #f7fbff 0%, #eef4ff 56%, #e8f0fc 100%);
}

.floating-shapes {
  position: absolute;
  inset: 0;
}

.shape {
  position: absolute;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(66, 131, 255, 0.2), rgba(48, 109, 224, 0.08));
  border: 1px solid rgba(255, 255, 255, 0.38);
  animation: float 18s infinite ease-in-out;
}

.shape-1 {
  width: 320px;
  height: 320px;
  top: -80px;
  left: -80px;
  animation-delay: 0s;
}

.shape-2 {
  width: 260px;
  height: 260px;
  top: 46%;
  right: -40px;
  animation-delay: -5s;
}

.shape-3 {
  width: 180px;
  height: 180px;
  bottom: 8%;
  left: 12%;
  animation-delay: -10s;
}

.shape-4 {
  width: 120px;
  height: 120px;
  top: 14%;
  right: 22%;
  animation-delay: -7s;
}

.shape-5 {
  width: 220px;
  height: 220px;
  bottom: -50px;
  right: 34%;
  animation-delay: -13s;
}

.hero-content {
  position: relative;
  z-index: 1;
  text-align: center;
  max-width: 860px;
  animation: fadeInUp 0.85s ease-out;
}

.hero-badge {
  display: inline-block;
  margin-bottom: 22px;
  padding: 8px 18px;
  border-radius: 999px;
  border: 1px solid #cde0fb;
  background: rgba(255, 255, 255, 0.82);
  color: #235bb8;
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.03em;
}

.hero-title {
  margin: 0 0 20px;
  line-height: 1.18;
}

.gradient-text {
  font-size: clamp(46px, 8vw, 72px);
  font-weight: 700;
  letter-spacing: -0.02em;
  background: linear-gradient(132deg, #1f6feb 0%, #2f82ff 46%, #44a1ff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.sub-title {
  font-size: clamp(20px, 3.8vw, 30px);
  color: #2f4e7a;
  font-weight: 500;
  letter-spacing: 0.04em;
}

.hero-desc {
  margin: 0 auto 34px;
  max-width: 650px;
  font-size: 17px;
  line-height: 1.75;
  color: var(--cn-text-secondary);
}

.hero-actions {
  display: flex;
  justify-content: center;
  gap: 14px;
  margin-bottom: 42px;
}

.action-btn {
  min-width: 148px;
  height: 48px;
  padding: 0 24px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
}

.primary-btn {
  border: 1px solid #1f6feb;
  background: linear-gradient(135deg, #2b7cf6 0%, #1f6feb 100%);
  box-shadow: 0 10px 24px rgba(31, 111, 235, 0.22);
}

.secondary-btn {
  border: 1px solid #c7d8f2;
  color: #275090;
  background: rgba(255, 255, 255, 0.8);
}

.secondary-btn:hover {
  color: #1f6feb;
  border-color: #b7ccf1;
  background: #f6faff;
}

.hero-stats {
  width: min(620px, 100%);
  margin: 0 auto;
  padding: 20px 18px;
  border-radius: 16px;
  border: 1px solid #d8e5f8;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 10px 28px rgba(18, 38, 63, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 28px;
}

.stat-item {
  min-width: 110px;
  text-align: center;
}

.stat-number {
  display: block;
  font-size: 34px;
  font-weight: 700;
  line-height: 1.1;
  color: #1f6feb;
}

.stat-label {
  font-size: 13px;
  color: var(--cn-text-secondary);
}

.stat-divider {
  width: 1px;
  height: 36px;
  background: #d7e3f6;
}

.features-section {
  padding: 94px 20px 80px;
  max-width: 1240px;
  margin: 0 auto;
}

.features-section.section-reveal {
  transition-delay: 30ms;
}

.section-header {
  text-align: center;
  margin-bottom: 52px;
}

.section-title {
  margin: 0 0 12px;
  font-size: clamp(28px, 4.2vw, 38px);
  font-weight: 700;
  color: var(--cn-text-primary);
}

.section-subtitle {
  margin: 0;
  font-size: 16px;
  color: var(--cn-text-secondary);
}

.section-reveal {
  opacity: 0;
  transform: translateY(28px);
  transition:
    opacity var(--cn-motion-slow) var(--cn-ease-out),
    transform var(--cn-motion-slow) var(--cn-ease-out);
}

.section-reveal.is-visible {
  opacity: 1;
  transform: translateY(0);
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 22px;
}

.feature-card {
  position: relative;
  overflow: hidden;
  background: #fff;
  border: 1px solid #dbe7f8;
  border-radius: 18px;
  padding: 28px 26px 56px;
  box-shadow: 0 10px 30px rgba(18, 38, 63, 0.06);
  cursor: pointer;
  opacity: 0;
  transform: translateY(18px);
  transition:
    transform var(--cn-motion-base) var(--cn-ease-out),
    box-shadow var(--cn-motion-base) var(--cn-ease-out),
    border-color var(--cn-motion-base) var(--cn-ease-out),
    opacity var(--cn-motion-slow) var(--cn-ease-out);
  transition-delay: var(--delay, 0ms);
}

.feature-card::before {
  content: '';
  position: absolute;
  inset: 0 auto auto 0;
  width: 100%;
  height: 4px;
  background: linear-gradient(90deg, #3e86ff 0%, #1f6feb 100%);
  opacity: 0;
  transition: opacity 0.24s ease;
}

.feature-card::after {
  content: '';
  position: absolute;
  inset: -38% auto auto -24%;
  width: 160px;
  height: 160px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.55) 0%, rgba(255, 255, 255, 0) 72%);
  opacity: 0;
  transition: opacity var(--cn-motion-base) var(--cn-ease-out);
}

.features-section.is-visible .feature-card {
  opacity: 1;
  transform: translateY(0);
}

.feature-card:hover {
  transform: translateY(-6px);
  border-color: #c7daf6;
  box-shadow: 0 18px 38px rgba(18, 38, 63, 0.12);
}

.feature-card:hover::before {
  opacity: 1;
}

.feature-card:hover::after {
  opacity: 1;
}

.feature-icon {
  width: 58px;
  height: 58px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  margin-bottom: 18px;
}

.feature-title {
  margin: 0 0 10px;
  font-size: 19px;
  font-weight: 600;
  color: var(--cn-text-primary);
}

.feature-desc {
  margin: 0;
  color: var(--cn-text-secondary);
  font-size: 14px;
  line-height: 1.65;
}

.feature-arrow {
  position: absolute;
  right: 22px;
  bottom: 20px;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 1px solid #d2e1f8;
  background: #f5f9ff;
  color: #4f6ea2;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.24s ease;
}

.feature-card:hover .feature-arrow {
  color: #fff;
  border-color: transparent;
  background: linear-gradient(135deg, #3480ff 0%, #1f6feb 100%);
  transform: translateX(4px);
}

.quick-access-section {
  padding: 78px 20px;
  border-top: 1px solid #e2ebf8;
  border-bottom: 1px solid #e2ebf8;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.86) 0%, #f7fbff 100%);
}

.quick-access-section.section-reveal {
  transition-delay: 70ms;
}

.quick-access-grid {
  max-width: 1240px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(8, minmax(0, 1fr));
  gap: 16px;
}

.quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 20px 14px;
  border-radius: 14px;
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.24s ease;
}

.quick-item:hover {
  transform: translateY(-3px);
  border-color: #d8e4f8;
  background: #f5faff;
}

.quick-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.22s ease;
}

.quick-item:hover .quick-icon {
  transform: scale(1.06);
}

.quick-title {
  font-size: 13px;
  font-weight: 500;
  color: #2f4e7a;
}

.highlights-section {
  padding: 82px 20px;
  background: linear-gradient(180deg, #f2f7ff 0%, #eef4ff 100%);
}

.highlights-section.section-reveal {
  transition-delay: 90ms;
}

.highlights-content {
  max-width: 1240px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 24px;
}

.highlight-item {
  display: flex;
  gap: 18px;
  padding: 26px 24px;
  border-radius: 16px;
  border: 1px solid #d7e5fa;
  background: #fff;
  box-shadow: 0 8px 24px rgba(18, 38, 63, 0.06);
  transition: all 0.24s ease;
}

.highlight-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 14px 32px rgba(18, 38, 63, 0.1);
}

.highlight-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: linear-gradient(135deg, #3a85ff 0%, #1f6feb 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.highlight-info h3 {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 600;
  color: var(--cn-text-primary);
}

.highlight-info p {
  margin: 0;
  color: var(--cn-text-secondary);
  font-size: 14px;
  line-height: 1.65;
}

.cta-section {
  padding: 88px 20px 98px;
  text-align: center;
  background: linear-gradient(135deg, #2f73d8 0%, #1f60bf 58%, #1e57a8 100%);
}

.cta-section.section-reveal {
  transition-delay: 110ms;
}

.cta-content h2 {
  margin: 0 0 14px;
  font-size: clamp(28px, 4.2vw, 36px);
  font-weight: 700;
  color: #fff;
}

.cta-content p {
  margin: 0 0 34px;
  font-size: 17px;
  color: rgba(255, 255, 255, 0.86);
}

.cta-btn {
  min-width: 160px;
  height: 50px;
  border-radius: 12px;
  border: 1px solid #fff;
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
  backdrop-filter: blur(6px);
  box-shadow: 0 12px 26px rgba(7, 38, 84, 0.28);
}

.cta-btn:hover {
  border-color: #fff;
  color: #fff;
  background: rgba(255, 255, 255, 0.24);
}

@keyframes float {
  0%,
  100% {
    transform: translate(0, 0) rotate(0deg);
  }

  25% {
    transform: translate(24px, -22px) rotate(4deg);
  }

  50% {
    transform: translate(-16px, 18px) rotate(-4deg);
  }

  75% {
    transform: translate(16px, 10px) rotate(2deg);
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(26px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 1100px) {
  .features-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .quick-access-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .highlights-content {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .hero-section {
    min-height: auto;
    padding-top: 42px;
  }

  .hero-actions {
    flex-direction: column;
    align-items: center;
  }

  .hero-stats {
    flex-wrap: wrap;
    gap: 20px;
    padding: 16px;
  }

  .stat-divider {
    display: none;
  }

  .features-grid {
    grid-template-columns: 1fr;
  }

  .quick-access-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 12px;
  }

  .quick-item {
    padding: 14px 8px;
  }
}

@media (max-width: 480px) {
  .quick-access-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .section-header {
    margin-bottom: 34px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .shape,
  .hero-content,
  .feature-card,
  .section-reveal {
    animation: none !important;
  }

  .section-reveal,
  .feature-card {
    opacity: 1 !important;
    transform: none !important;
  }
}
</style>
