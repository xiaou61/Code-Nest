<template>
  <CnPage class="dev-tools-page" max-width="1180px" full-height>
    <CnPageHeader
      title="程序员工具箱"
      description="集中放置 JSON、文本比对和聚合翻译等高频开发工具。"
      eyebrow="DEV TOOLS"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">3 个工具</CnStatusTag>
        <CnStatusTag type="success" size="sm" subtle>快速入口</CnStatusTag>
      </template>
    </CnPageHeader>

    <div class="summary-grid">
      <CnStatCard title="工具数量" :value="tools.length" description="当前已上线开发工具" tone="brand" />
      <CnStatCard title="覆盖场景" value="格式化 / 比对 / 翻译" description="日常开发和文本处理" tone="info" />
      <CnStatCard title="使用方式" value="即开即用" description="点击卡片进入具体工具" tone="success" />
    </div>

    <CnSection title="工具入口" description="选择一个工具进入对应工作台。" divided>
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
            立即使用
          </el-button>
        </article>
      </div>
    </CnSection>
  </CnPage>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import type { Component } from 'vue'
import { ChatLineSquare, Document, Operation, Right } from '@element-plus/icons-vue'
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
  features: string[]
}

const router = useRouter()

const tools: ToolEntry[] = [
  {
    key: 'json',
    title: 'JSON 工具',
    description: 'JSON 格式化、验证、压缩和转换工具，支持语法高亮和错误定位。',
    badge: '热门',
    tone: 'brand',
    icon: Document,
    features: ['格式化', '验证', '压缩', '转换']
  },
  {
    key: 'text-diff',
    title: '文本比对',
    description: '智能文本差异对比工具，支持字符级、单词级、行级对比模式。',
    badge: '实用',
    tone: 'success',
    icon: Operation,
    features: ['实时对比', '差异高亮', '多模式', '导出']
  },
  {
    key: 'translation',
    title: '聚合翻译',
    description: '集成多个翻译平台，同时获取多种翻译结果，辅助选择最佳译文。',
    badge: '强大',
    tone: 'warning',
    icon: ChatLineSquare,
    features: ['多平台', '对比', '批量', '专业']
  }
]

const navigateToTool = (toolName: string) => {
  router.push(`/dev-tools/${toolName}`)
}
</script>

<style scoped>
.dev-tools-page {
  min-height: calc(100vh - 68px);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.tools-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.tool-card {
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

@media (max-width: 1080px) {
  .tools-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 820px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
