<template>
  <CnPage max-width="1040px" full-height>
    <CnPageHeader
      title="版本历史"
      description="跟踪 Code Nest 的产品更新、功能迭代和修复记录。"
      eyebrow="CHANGELOG"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">已加载 {{ versionList.length }} 条</CnStatusTag>
        <CnStatusTag type="info" size="sm" subtle>共 {{ pagination.total }} 条</CnStatusTag>
      </template>
    </CnPageHeader>

    <CnSection title="更新时间轴" description="按发布时间倒序展示版本记录。" class="timeline-section">
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="5" animated />
      </div>

      <CnEmptyState
        v-else-if="versionList.length === 0"
        title="暂无版本历史记录"
        description="有公开版本更新后，这里会展示更新内容。"
        icon="VER"
      />

      <div v-else class="timeline-container">
        <article
          v-for="(version, index) in versionList"
          :key="version.id"
          class="timeline-item"
          :class="{ 'is-featured': version.isFeatured === 1 }"
        >
          <div class="timeline-marker" :class="`timeline-marker--${getUpdateTypeClass(version.updateType)}`">
            {{ getUpdateTypeShort(version.updateType) }}
          </div>

          <div class="timeline-content">
            <div class="version-card">
              <div class="version-header">
                <div class="version-info">
                  <div class="version-tags">
                    <CnStatusTag type="brand" size="lg" :dot="false">
                      {{ version.versionNumber }}
                    </CnStatusTag>
                    <CnStatusTag v-if="version.isFeatured === 1" type="warning" size="sm">
                      重点推荐
                    </CnStatusTag>
                    <CnStatusTag :type="getUpdateTypeTone(version.updateType)" size="sm">
                      {{ version.updateTypeName || getUpdateTypeText(version.updateType) }}
                    </CnStatusTag>
                  </div>

                  <h2 class="version-title">{{ version.title }}</h2>

                  <div class="version-meta">
                    <span>
                      <el-icon><Calendar /></el-icon>
                      {{ formatReleaseTime(version.releaseTime) }}
                    </span>
                    <span>
                      <el-icon><View /></el-icon>
                      {{ version.viewCount || 0 }} 次查看
                    </span>
                  </div>
                </div>

                <span class="version-index">
                  #{{ pagination.total - (pagination.pageNum - 1) * pagination.pageSize - index }}
                </span>
              </div>

              <p v-if="version.description" class="version-description">
                {{ version.description }}
              </p>

              <div class="version-actions">
                <el-button v-if="version.prdUrl" type="primary" :icon="Document" @click="handleViewPrd(version)">
                  查看 PRD 详情
                </el-button>
                <el-button type="info" :icon="Share" @click="handleShare(version)">分享</el-button>
              </div>
            </div>
          </div>
        </article>
      </div>

      <div v-if="hasMore" class="load-more-container">
        <el-button type="primary" size="large" :loading="loadingMore" @click="loadMore">
          加载更多版本
        </el-button>
      </div>
    </CnSection>

    <el-backtop :right="40" :bottom="40" />
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Calendar, Document, Share, View } from '@element-plus/icons-vue'
import {
  CnEmptyState,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatusTag,
  type CnTone
} from '@/design-system'
import { versionApi } from '@/api/version'

interface VersionItem {
  id: number | string
  versionNumber?: string
  title?: string
  updateType?: number
  updateTypeName?: string
  releaseTime?: string
  viewCount?: number
  description?: string
  prdUrl?: string
  isFeatured?: number
}

interface VersionResponse {
  records?: VersionItem[]
  total?: number
}

const loading = ref(false)
const loadingMore = ref(false)
const versionList = ref<VersionItem[]>([])

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const hasMore = computed(() => {
  const totalPages = Math.ceil(pagination.total / pagination.pageSize)
  return pagination.pageNum < totalPages
})

onMounted(() => {
  loadVersionList()
})

const loadVersionList = async (isLoadMore = false) => {
  try {
    if (isLoadMore) {
      loadingMore.value = true
    } else {
      loading.value = true
      pagination.pageNum = 1
      versionList.value = []
    }

    const data = (await versionApi.getVersionTimeline({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })) as VersionResponse

    if (isLoadMore) {
      versionList.value.push(...(data.records || []))
    } else {
      versionList.value = data.records || []
    }

    pagination.total = data.total || 0
  } catch (error) {
    console.error('加载版本列表失败:', error)
    ElMessage.error('加载版本列表失败')
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const loadMore = () => {
  pagination.pageNum += 1
  loadVersionList(true)
}

const handleViewPrd = (version: VersionItem) => {
  if (!version.prdUrl) return

  versionApi.recordViewCount(version.id).catch(console.error)
  window.open(version.prdUrl, '_blank')
}

const handleShare = async (version: VersionItem) => {
  const shareData = {
    title: `${version.versionNumber} - ${version.title}`,
    text: version.description || '查看最新版本更新',
    url: window.location.href
  }

  try {
    if (navigator.share) {
      await navigator.share(shareData)
      return
    }

    await navigator.clipboard.writeText(window.location.href)
    ElMessage.success('链接已复制到剪贴板')
  } catch (error) {
    console.error('分享失败:', error)
    try {
      await navigator.clipboard.writeText(window.location.href)
      ElMessage.success('链接已复制到剪贴板')
    } catch (clipboardError) {
      console.error('复制链接失败:', clipboardError)
      ElMessage.warning('分享功能暂时不可用')
    }
  }
}

const formatReleaseTime = (time?: string) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  if (diff < 24 * 60 * 60 * 1000) {
    const hours = Math.floor(diff / (60 * 60 * 1000))
    if (hours < 1) {
      const minutes = Math.floor(diff / (60 * 1000))
      return `${minutes} 分钟前`
    }
    return `${hours} 小时前`
  }

  if (diff < 7 * 24 * 60 * 60 * 1000) {
    const days = Math.floor(diff / (24 * 60 * 60 * 1000))
    return `${days} 天前`
  }

  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

const getUpdateTypeShort = (type?: number) => {
  const map: Record<number, string> = {
    1: 'MAJ',
    2: 'FEA',
    3: 'FIX',
    4: 'OTH'
  }
  return map[type || 4] || 'OTH'
}

const getUpdateTypeText = (type?: number) => {
  const map: Record<number, string> = {
    1: '重大更新',
    2: '功能更新',
    3: '修复更新',
    4: '其他'
  }
  return map[type || 4] || '其他'
}

const getUpdateTypeClass = (type?: number) => {
  const classMap: Record<number, string> = {
    1: 'major',
    2: 'feature',
    3: 'fix',
    4: 'other'
  }
  return classMap[type || 4] || 'other'
}

const getUpdateTypeTone = (type?: number): CnTone => {
  if (type === 1) return 'danger'
  if (type === 2) return 'brand'
  if (type === 3) return 'warning'
  return 'info'
}
</script>

<style scoped>
.timeline-section {
  overflow: hidden;
}

.loading-container {
  padding: var(--cn-space-6);
}

.timeline-container {
  position: relative;
  display: grid;
  gap: var(--cn-space-5);
  padding-left: 54px;
}

.timeline-container::before {
  content: '';
  position: absolute;
  top: var(--cn-space-2);
  bottom: var(--cn-space-2);
  left: 19px;
  width: 2px;
  border-radius: var(--cn-radius-pill);
  background: var(--cn-color-border);
}

.timeline-item {
  position: relative;
  min-width: 0;
}

.timeline-marker {
  position: absolute;
  top: var(--cn-space-4);
  left: -54px;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border: 2px solid var(--cn-color-bg-surface);
  border-radius: var(--cn-radius-panel);
  color: var(--cn-color-bg-surface);
  font-size: 11px;
  font-weight: 800;
  box-shadow: var(--cn-shadow-sm);
}

.timeline-marker--major {
  background: var(--cn-color-danger);
}

.timeline-marker--feature {
  background: var(--cn-color-brand-primary);
}

.timeline-marker--fix {
  background: var(--cn-color-warning);
}

.timeline-marker--other {
  background: var(--cn-color-info);
}

.version-card {
  position: relative;
  display: grid;
  gap: var(--cn-space-4);
  min-width: 0;
  padding: var(--cn-space-5);
  border: 1px solid var(--cn-card-border);
  border-radius: var(--cn-radius-panel);
  background: var(--cn-card-bg);
  box-shadow: var(--cn-card-shadow);
  transition:
    transform var(--cn-motion-fast) var(--cn-ease-out),
    border-color var(--cn-motion-fast) var(--cn-ease-out),
    box-shadow var(--cn-motion-fast) var(--cn-ease-out);
}

.version-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--cn-shadow-sm);
}

.timeline-item.is-featured .version-card {
  border-color: color-mix(in srgb, var(--cn-color-warning) 44%, var(--cn-card-border));
  background: color-mix(in srgb, var(--cn-card-bg) 84%, var(--cn-color-warning-soft));
}

.version-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--cn-space-4);
}

.version-info {
  min-width: 0;
}

.version-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.version-title {
  margin: var(--cn-space-3) 0 0;
  color: var(--cn-color-text-primary);
  font-size: 22px;
  font-weight: 750;
  line-height: 1.35;
}

.version-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-4);
  margin-top: var(--cn-space-3);
  color: var(--cn-color-text-secondary);
  font-size: 13px;
}

.version-meta span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.version-index {
  flex-shrink: 0;
  padding: 4px 8px;
  border-radius: var(--cn-radius-pill);
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  font-weight: 700;
}

.version-description {
  margin: 0;
  padding: var(--cn-space-4);
  border-left: 3px solid var(--cn-color-brand-primary);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.version-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.load-more-container {
  display: flex;
  justify-content: center;
  margin-top: var(--cn-space-6);
}

@media (max-width: 640px) {
  .timeline-container {
    padding-left: 42px;
  }

  .timeline-container::before {
    left: 15px;
  }

  .timeline-marker {
    left: -42px;
    width: 30px;
    height: 30px;
    font-size: 10px;
  }

  .version-header {
    display: grid;
  }

  .version-index {
    width: fit-content;
  }
}
</style>
