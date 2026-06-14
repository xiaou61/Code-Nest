<template>
  <CnPage class="resume-template-page" max-width="1180px" full-height>
    <CnPageHeader
      title="简历模板中心"
      description="挑选适合的模板，快速开始创作你的专属简历。"
      eyebrow="RESUME TEMPLATES"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">共 {{ pagination.total }} 个模板</CnStatusTag>
        <CnStatusTag type="success" size="sm" subtle>已发布模板</CnStatusTag>
      </template>

      <template #actions>
        <el-button type="primary" :icon="Briefcase" @click="goMyResumes">我的简历</el-button>
      </template>
    </CnPageHeader>

    <div class="summary-grid">
      <CnStatCard title="模板总数" :value="pagination.total" description="当前条件下可用模板" tone="brand" :loading="loading" />
      <CnStatCard title="当前展示" :value="templates.length" description="本页加载模板数量" tone="success" :loading="loading" />
      <CnStatCard title="每页容量" :value="pagination.size" description="分页展示容量" tone="info" :loading="loading" />
    </div>

    <CnSection title="筛选模板" description="按关键字、方向分类和经验层级定位合适模板。" divided>
      <CnFilterForm
        v-model="filters"
        :fields="filterFields"
        :columns="3"
        :loading="loading"
        @search="handleSearch"
        @reset="handleReset"
      />
    </CnSection>

    <CnSection title="模板列表" description="查看模板信息，预览后可直接进入编辑器使用。" divided>
      <div v-loading="loading" class="template-shell">
        <div v-if="templates.length" class="template-grid">
          <article v-for="template in templates" :key="template.id" class="template-card">
            <button class="cover" type="button" @click="handleUseTemplate(template)">
              <img :src="template.coverUrl || defaultCover" :alt="`${template.name || '简历模板'}封面`" />
              <span class="cover-mask">
                <el-button type="primary" size="small">使用模板</el-button>
              </span>
            </button>

            <div class="card-body">
              <div class="title-row">
                <h3>{{ template.name }}</h3>
                <CnStatusTag type="success" size="sm">{{ formatExperience(template.experienceLevel) }}</CnStatusTag>
              </div>
              <p class="desc">{{ template.description || '暂无描述' }}</p>

              <div class="meta-row">
                <span class="meta-item">
                  <el-icon><CollectionTag /></el-icon>
                  {{ template.category || '通用' }}
                </span>
                <el-rate
                  :model-value="Number(template.rating || 0)"
                  disabled
                  show-score
                  text-color="var(--cn-color-warning)"
                  score-template="{value}"
                />
              </div>

              <div v-if="template.tags" class="tags">
                <CnStatusTag v-for="tag in parseTags(template.tags)" :key="tag" type="neutral" size="sm" subtle>
                  {{ tag }}
                </CnStatusTag>
              </div>

              <div class="card-actions">
                <el-button link type="primary" @click="previewTemplate(template)">查看详情</el-button>
                <el-button type="primary" :icon="EditPen" @click="handleUseTemplate(template)">立即使用</el-button>
              </div>
            </div>
          </article>
        </div>

        <CnEmptyState
          v-else-if="!loading"
          title="暂无模板"
          description="当前筛选条件下没有可用模板，稍后再来看看。"
          icon="RT"
        />
      </div>

      <div v-if="pagination.total > pagination.size" class="pagination-wrapper">
        <el-pagination
          :current-page="pagination.page"
          :page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[8, 12, 16, 24]"
          background
          layout="total, sizes, prev, pager, next"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </CnSection>

    <el-dialog v-model="previewVisible" title="模板说明" width="560px">
      <div v-if="currentTemplate" class="template-preview">
        <div class="preview-cover">
          <img :src="currentTemplate.previewUrl || currentTemplate.coverUrl || defaultCover" alt="模板预览" />
        </div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="模板名称">{{ currentTemplate.name }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ currentTemplate.category || '通用' }}</el-descriptions-item>
          <el-descriptions-item label="经验层级">{{ formatExperience(currentTemplate.experienceLevel) }}</el-descriptions-item>
          <el-descriptions-item label="适用技术栈">
            {{ currentTemplate.techStack || '未标注' }}
          </el-descriptions-item>
          <el-descriptions-item label="简介">
            {{ currentTemplate.description || '暂无介绍' }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="previewVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUseTemplate(currentTemplate)">立即使用</el-button>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Briefcase, CollectionTag, EditPen } from '@element-plus/icons-vue'
import {
  CnEmptyState,
  CnFilterForm,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag,
  type CnFilterField
} from '@/design-system'
import { resumeApi } from '@/api/resume'

interface ResumeTemplate extends Record<string, unknown> {
  id: number | string
  name?: string
  description?: string
  category?: string
  experienceLevel?: number | string
  techStack?: string
  tags?: string
  rating?: number
  coverUrl?: string
  previewUrl?: string
}

interface TemplateResponse {
  records?: ResumeTemplate[]
  total?: number
}

const router = useRouter()
const loading = ref(false)
const templates = ref<ResumeTemplate[]>([])
const currentTemplate = ref<ResumeTemplate | null>(null)
const previewVisible = ref(false)

const pagination = reactive({
  page: 1,
  size: 8,
  total: 0
})

const createFilters = () => ({
  keyword: '',
  category: '',
  experienceLevel: ''
})

const filters = ref<Record<string, unknown>>(createFilters())

const filterFields: CnFilterField[] = [
  {
    prop: 'keyword',
    label: '关键字',
    type: 'input',
    placeholder: '输入模板名称或标签'
  },
  {
    prop: 'category',
    label: '分类',
    type: 'select',
    placeholder: '全部分类',
    options: [
      { label: '前端', value: '前端' },
      { label: '后端', value: '后端' },
      { label: '全栈', value: '全栈' },
      { label: '算法', value: '算法' },
      { label: '移动端', value: '移动端' },
      { label: '测试', value: '测试' }
    ]
  },
  {
    prop: 'experienceLevel',
    label: '经验',
    type: 'select',
    placeholder: '经验层级',
    options: [
      { label: '应届生/实习', value: 1 },
      { label: '初级工程师', value: 2 },
      { label: '中级工程师', value: 3 },
      { label: '高级工程师', value: 4 },
      { label: '专家/管理', value: 5 }
    ]
  }
]

const defaultCover = 'https://cdn.xiaou.tech/static/resume-placeholder.png'

const fetchTemplates = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
      keyword: (filters.value.keyword as string) || undefined,
      category: (filters.value.category as string) || undefined,
      experienceLevel: filters.value.experienceLevel || undefined,
      status: 1
    }
    const result = (await resumeApi.getTemplates(params)) as TemplateResponse
    templates.value = result?.records || []
    pagination.total = result?.total || templates.value.length
  } catch (error) {
    console.error('加载模板失败', error)
    ElMessage.error('加载模板失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  fetchTemplates()
}

const handleReset = () => {
  filters.value = createFilters()
  pagination.page = 1
  fetchTemplates()
}

const handlePageChange = (page: number) => {
  pagination.page = page
  fetchTemplates()
}

const handleSizeChange = (size: number) => {
  pagination.size = size
  pagination.page = 1
  fetchTemplates()
}

const parseTags = (tags?: string) => {
  if (!tags) return []
  return tags.split(',').map((tag) => tag.trim()).filter(Boolean)
}

const formatExperience = (level?: number | string) => {
  const map: Record<string, string> = {
    1: '应届/实习',
    2: '初级',
    3: '中级',
    4: '高级',
    5: '专家'
  }
  return map[String(level)] || '通用'
}

const handleUseTemplate = (template: ResumeTemplate | null) => {
  if (!template?.id) {
    ElMessage.warning('模板信息异常')
    return
  }
  router.push({
    path: '/resume/editor',
    query: {
      templateId: template.id
    }
  })
}

const previewTemplate = (template: ResumeTemplate) => {
  currentTemplate.value = template
  previewVisible.value = true
}

const goMyResumes = () => {
  router.push('/resume')
}

onMounted(() => {
  fetchTemplates()
})
</script>

<style scoped>
.resume-template-page {
  min-height: calc(100vh - 68px);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.template-shell {
  min-height: 280px;
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--cn-space-4);
}

.template-card {
  display: grid;
  grid-template-rows: 180px 1fr;
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--cn-card-border);
  border-radius: var(--cn-radius-panel);
  background: var(--cn-card-bg);
  box-shadow: var(--cn-card-shadow);
  transition:
    transform var(--cn-motion-fast) var(--cn-ease-out),
    border-color var(--cn-motion-base) var(--cn-ease-out),
    box-shadow var(--cn-motion-base) var(--cn-ease-out);
}

.template-card:hover {
  transform: translateY(-2px);
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 30%, var(--cn-color-border));
  box-shadow: var(--cn-shadow-popover);
}

.cover {
  position: relative;
  display: block;
  width: 100%;
  height: 100%;
  padding: 0;
  overflow: hidden;
  border: 0;
  background: var(--cn-color-bg-surface-muted);
  cursor: pointer;
}

.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform var(--cn-motion-base) var(--cn-ease-out);
}

.cover:hover img,
.cover:focus-visible img {
  transform: scale(1.04);
}

.cover-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: color-mix(in srgb, var(--cn-color-text-primary) 46%, transparent);
  opacity: 0;
  transition: opacity var(--cn-motion-fast) var(--cn-ease-out);
}

.cover:hover .cover-mask,
.cover:focus-visible .cover-mask {
  opacity: 1;
}

.card-body {
  display: grid;
  gap: var(--cn-space-3);
  padding: var(--cn-space-4);
}

.title-row,
.meta-row,
.card-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
}

.title-row h3 {
  margin: 0;
  overflow-wrap: anywhere;
  color: var(--cn-color-text-primary);
  font-size: 17px;
  font-weight: 750;
  line-height: 1.35;
}

.desc {
  display: -webkit-box;
  min-height: 44px;
  margin: 0;
  overflow: hidden;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.65;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.meta-row {
  flex-wrap: wrap;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: var(--cn-space-1);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  font-weight: 600;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.card-actions {
  align-self: end;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--cn-space-5);
  overflow-x: auto;
}

.template-preview {
  display: grid;
  gap: var(--cn-space-4);
}

.preview-cover {
  width: 100%;
  height: 260px;
  overflow: hidden;
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
}

.preview-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

@media (max-width: 820px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .pagination-wrapper {
    justify-content: flex-start;
  }
}

@media (max-width: 560px) {
  .template-grid {
    grid-template-columns: 1fr;
  }

  .title-row,
  .card-actions {
    align-items: flex-start;
    flex-direction: column;
  }

  .card-actions :deep(.el-button) {
    width: 100%;
  }
}
</style>
