<template>
  <CnPage class="moyu-tools-page" max-width="1180px" full-height>
    <CnPageHeader
      title="摸鱼工具箱"
      description="工作间隙的轻松工具集合，集中查看热点、日历、每日内容和趣味调试素材。"
      eyebrow="MOYU TOOLS"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">{{ tools.length }} 个工具</CnStatusTag>
        <CnStatusTag type="warning" size="sm" subtle>轻量入口</CnStatusTag>
      </template>
    </CnPageHeader>

    <div class="summary-grid">
      <CnStatCard title="工具数量" :value="tools.length" description="当前可进入的摸鱼工具" tone="brand" />
      <CnStatCard title="内容类型" value="热点 / 日历 / 内容" description="信息浏览与轻学习场景" tone="warning" />
      <CnStatCard title="操作路径" value="点击进入" description="保留原工具路由入口" tone="success" />
    </div>

    <CnSection title="工具入口" description="选择一个工具进入对应页面。" divided>
      <div class="tools-grid">
        <article
          v-for="tool in tools"
          :key="tool.key"
          class="tool-card"
          :class="`tool-card--${tool.tone}`"
          tabindex="0"
          role="button"
          @click="navigateToTool(tool.key)"
          @keyup.enter="navigateToTool(tool.key)"
        >
          <div class="card-header">
            <div class="tool-icon">
              <el-icon>
                <component :is="tool.icon" />
              </el-icon>
            </div>
            <CnStatusTag :type="tool.tone" size="sm" subtle>{{ tool.badge }}</CnStatusTag>
          </div>

          <div class="card-content">
            <h3 class="tool-title">{{ tool.title }}</h3>
            <p class="tool-description">{{ tool.description }}</p>
            <div class="tool-features">
              <CnStatusTag
                v-for="feature in tool.features"
                :key="feature"
                type="neutral"
                size="sm"
                subtle
              >
                {{ feature }}
              </CnStatusTag>
            </div>
          </div>

          <el-button type="primary" class="tool-button">
            <el-icon><Right /></el-icon>
            {{ tool.actionText }}
          </el-button>
        </article>
      </div>
    </CnSection>
  </CnPage>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import type { Component } from 'vue'
import { Calendar, Money, Notification, Reading, Right, TrendCharts } from '@element-plus/icons-vue'
import {
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag,
  type CnTone
} from '@/design-system'

interface ToolEntry {
  key: string
  title: string
  description: string
  badge: string
  tone: CnTone
  icon: Component
  actionText: string
  features: string[]
}

const router = useRouter()

const tools: ToolEntry[] = [
  {
    key: 'hot-topics',
    title: '今日热榜',
    description: '汇聚微博、知乎、抖音等平台热门话题，一站式浏览实时热榜。',
    badge: '热门',
    tone: 'danger',
    icon: TrendCharts,
    actionText: '立即使用',
    features: ['实时更新', '多平台', '分类浏览', '一键跳转']
  },
  {
    key: 'salary-calculator',
    title: '时薪计算器',
    description: '实时计算工作收入，追踪每日工作时长，让每分钟的努力都有可见反馈。',
    badge: '激励',
    tone: 'success',
    icon: Money,
    actionText: '开始计算',
    features: ['实时计算', '收入统计', '工作跟踪', '激励展示']
  },
  {
    key: 'calendar',
    title: '程序员日历',
    description: '查看技术纪念日、开源节日和程序员相关日期，发现日常里的技术节点。',
    badge: '精选',
    tone: 'brand',
    icon: Calendar,
    actionText: '查看日历',
    features: ['月历视图', '今日推荐', '事件详情', '类型筛选']
  },
  {
    key: 'daily-content',
    title: '每日内容',
    description: '每天一点小知识，包含编程格言、技术小贴士、代码片段和历史回顾。',
    badge: '学习',
    tone: 'warning',
    icon: Reading,
    actionText: '开始学习',
    features: ['今日推荐', '分类浏览', '点赞收藏', '知识积累']
  },
  {
    key: 'bug-store',
    title: 'Bug 商店',
    description: '随机发现经典问题，学习分析思路和解决方案，提升调试经验。',
    badge: '发现',
    tone: 'info',
    icon: Notification,
    actionText: '来一发 Bug',
    features: ['随机推荐', '经典案例', '解决方案', '技能提升']
  }
]

const navigateToTool = (toolName: string) => {
  router.push(`/moyu-tools/${toolName}`)
}
</script>

<style scoped>
.moyu-tools-page {
  min-height: calc(100vh - 68px);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.tools-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: var(--cn-space-4);
}

.tool-card {
  --tool-accent: var(--cn-color-brand-primary);
  position: relative;
  display: grid;
  grid-template-rows: auto 1fr auto;
  gap: var(--cn-space-5);
  min-width: 0;
  min-height: 330px;
  padding: var(--cn-space-5);
  overflow: hidden;
  border: 1px solid var(--cn-card-border);
  border-radius: var(--cn-radius-panel);
  background: var(--cn-card-bg);
  box-shadow: var(--cn-card-shadow);
  cursor: pointer;
  transition:
    transform var(--cn-motion-fast) var(--cn-ease-out),
    border-color var(--cn-motion-base) var(--cn-ease-out),
    box-shadow var(--cn-motion-base) var(--cn-ease-out);
}

.tool-card::before {
  content: '';
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  background: var(--tool-accent);
}

.tool-card:hover,
.tool-card:focus-visible {
  transform: translateY(-3px);
  border-color: color-mix(in srgb, var(--tool-accent) 34%, var(--cn-color-border));
  box-shadow: var(--cn-shadow-popover);
  outline: none;
}

.tool-card--brand {
  --tool-accent: var(--cn-color-brand-primary);
}

.tool-card--success {
  --tool-accent: var(--cn-color-success);
}

.tool-card--warning {
  --tool-accent: var(--cn-color-warning);
}

.tool-card--danger {
  --tool-accent: var(--cn-color-danger);
}

.tool-card--info {
  --tool-accent: var(--cn-color-info);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
}

.tool-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 58px;
  height: 58px;
  border-radius: var(--cn-radius-panel);
  background: color-mix(in srgb, var(--tool-accent) 14%, var(--cn-color-bg-surface));
  color: var(--tool-accent);
  font-size: 28px;
}

.card-content {
  min-width: 0;
}

.tool-title {
  margin: 0 0 var(--cn-space-3);
  color: var(--cn-color-text-primary);
  font-size: 20px;
  font-weight: 750;
  line-height: 1.35;
}

.tool-description {
  min-height: 72px;
  margin: 0 0 var(--cn-space-4);
  color: var(--cn-color-text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.tool-features {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.tool-button {
  width: 100%;
  min-height: 44px;
}

@media (max-width: 820px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .tools-grid {
    grid-template-columns: 1fr;
  }
}
</style>
