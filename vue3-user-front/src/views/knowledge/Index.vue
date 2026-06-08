<template>
  <CnPage class="knowledge-page" max-width="1180px" full-height>
    <CnPageHeader
      title="知识图谱"
      description="可视化学习面试知识点，从整体到细节推进知识体系。"
      eyebrow="KNOWLEDGE MAP"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">共 {{ pagination.total }} 张</CnStatusTag>
        <CnStatusTag v-if="searchForm.keyword" type="info" size="sm" subtle>
          搜索：{{ searchForm.keyword }}
        </CnStatusTag>
      </template>

      <template #actions>
        <div class="search-actions">
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索知识图谱..."
            :prefix-icon="Search"
            clearable
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-button type="primary" :icon="Search" :loading="loading" @click="handleSearch">搜索</el-button>
        </div>
      </template>
    </CnPageHeader>

    <div class="summary-grid">
      <CnStatCard title="发布图谱" :value="pagination.total" description="可访问的知识图谱总数" tone="brand" :loading="loading" />
      <CnStatCard title="当前展示" :value="mapList.length" description="本页加载的图谱数量" tone="success" :loading="loading" />
      <CnStatCard title="每页容量" :value="pagination.size" description="分页列表展示容量" tone="info" :loading="loading" />
    </div>

    <CnSection title="图谱列表" description="点击卡片进入图谱学习视图。" divided>
      <div v-loading="loading" class="maps-shell">
        <div v-if="mapList.length" class="maps-grid">
          <article
            v-for="map in mapList"
            :key="map.id"
            class="map-card knowledge-reveal"
            tabindex="0"
            role="button"
            @click="handleViewMap(map)"
            @keyup.enter="handleViewMap(map)"
          >
            <div class="card-cover">
              <img v-if="map.coverImage" :src="map.coverImage" :alt="map.title || '知识图谱'" class="cover-image" />
              <div v-else class="cover-placeholder">
                <el-icon><DataAnalysis /></el-icon>
              </div>
            </div>

            <div class="card-content">
              <h3 class="map-title">{{ map.title }}</h3>
              <p class="map-description">{{ map.description || '暂无描述' }}</p>

              <div class="map-stats">
                <span>
                  <el-icon><Connection /></el-icon>
                  {{ map.nodeCount || 0 }} 个节点
                </span>
                <span>
                  <el-icon><View /></el-icon>
                  {{ map.viewCount || 0 }} 次查看
                </span>
              </div>

              <div class="card-footer">
                <span class="update-time">更新于 {{ formatDate(map.updateTime) }}</span>
                <el-button type="primary" size="small">
                  开始学习
                  <el-icon><ArrowRight /></el-icon>
                </el-button>
              </div>
            </div>
          </article>
        </div>

        <CnEmptyState
          v-else-if="!loading"
          title="暂无知识图谱"
          :description="searchForm.keyword ? '未找到相关图谱，请尝试其他关键词。' : '敬请期待更多内容。'"
          icon="KM"
        />
      </div>

      <div v-if="pagination.total > 0" class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[12, 24, 48]"
          background
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </CnSection>
  </CnPage>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Connection, DataAnalysis, Search, View } from '@element-plus/icons-vue'
import {
  CnEmptyState,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag
} from '@/design-system'
import { getPublishedKnowledgeMaps } from '@/api/knowledge'
import { useRevealMotion } from '@/utils/reveal-motion'

interface KnowledgeMap extends Record<string, unknown> {
  id: number | string
  title?: string
  description?: string
  coverImage?: string
  nodeCount?: number
  viewCount?: number
  updateTime?: string
}

interface KnowledgeMapResponse {
  records?: KnowledgeMap[]
  total?: number
  pageNum?: number
}

const router = useRouter()
useRevealMotion('.knowledge-page .knowledge-reveal')

const loading = ref(false)
const mapList = ref<KnowledgeMap[]>([])

const searchForm = reactive({
  keyword: ''
})

const pagination = reactive({
  page: 1,
  size: 12,
  total: 0
})

const fetchMapList = async () => {
  try {
    loading.value = true
    const params = {
      pageNum: pagination.page,
      pageSize: pagination.size,
      ...searchForm
    }

    const data = (await getPublishedKnowledgeMaps(params)) as KnowledgeMapResponse
    mapList.value = data.records || []
    pagination.total = data.total || 0
    pagination.page = data.pageNum || 1
  } catch (error) {
    console.error('获取知识图谱失败', error)
    ElMessage.error('获取知识图谱失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  fetchMapList()
}

const handleSizeChange = (size: number) => {
  pagination.size = size
  pagination.page = 1
  fetchMapList()
}

const handleCurrentChange = (page: number) => {
  pagination.page = page
  fetchMapList()
}

const handleViewMap = (map: KnowledgeMap) => {
  router.push(`/knowledge/maps/${map.id}`)
}

const formatDate = (dateString?: string) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  if (Number.isNaN(date.getTime())) return '-'

  const now = new Date()
  const diffTime = now.getTime() - date.getTime()
  const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24))

  if (diffDays === 0) return '今天'
  if (diffDays === 1) return '昨天'
  if (diffDays < 7) return `${diffDays}天前`
  if (diffDays < 30) return `${Math.floor(diffDays / 7)}周前`
  if (diffDays < 365) return `${Math.floor(diffDays / 30)}个月前`

  return date.toLocaleDateString('zh-CN')
}

onMounted(() => {
  fetchMapList()
})
</script>

<style scoped>
.knowledge-page {
  min-height: calc(100vh - 68px);
}

.search-actions {
  display: flex;
  align-items: center;
  gap: var(--cn-space-2);
  width: min(460px, 100%);
}

.search-actions .el-input {
  min-width: 240px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.maps-shell {
  min-height: 320px;
}

.maps-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: var(--cn-space-4);
}

.map-card {
  display: grid;
  grid-template-rows: 180px 1fr;
  min-width: 0;
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

.map-card:hover,
.map-card:focus-visible {
  transform: translateY(-3px);
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 30%, var(--cn-color-border));
  box-shadow: var(--cn-shadow-popover);
  outline: none;
}

.card-cover {
  position: relative;
  min-width: 0;
  overflow: hidden;
  background: var(--cn-color-bg-surface-muted);
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform var(--cn-motion-base) var(--cn-ease-out);
}

.map-card:hover .cover-image,
.map-card:focus-visible .cover-image {
  transform: scale(1.04);
}

.cover-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--cn-color-brand-primary);
  font-size: 48px;
  background: color-mix(in srgb, var(--cn-color-bg-surface-muted) 82%, var(--cn-color-brand-soft));
}

.card-content {
  display: grid;
  gap: var(--cn-space-4);
  padding: var(--cn-space-5);
}

.map-title {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  color: var(--cn-color-text-primary);
  font-size: 18px;
  font-weight: 750;
  line-height: 1.4;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.map-description {
  display: -webkit-box;
  min-height: 64px;
  margin: 0;
  overflow: hidden;
  color: var(--cn-color-text-secondary);
  font-size: 14px;
  line-height: 1.6;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.map-stats {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-3);
  padding-bottom: var(--cn-space-4);
  border-bottom: 1px solid var(--cn-color-border-subtle);
}

.map-stats span {
  display: inline-flex;
  align-items: center;
  gap: var(--cn-space-1);
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
  font-weight: 600;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
}

.update-time {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  font-weight: 600;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--cn-space-5);
  overflow-x: auto;
}

@media (max-width: 820px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .search-actions {
    width: 100%;
  }

  .search-actions .el-input {
    min-width: 0;
  }

  .pagination-container {
    justify-content: flex-start;
  }
}

@media (max-width: 560px) {
  .search-actions {
    display: grid;
  }

  .search-actions .el-button {
    width: 100%;
  }

  .maps-grid {
    grid-template-columns: 1fr;
  }

  .card-footer {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
