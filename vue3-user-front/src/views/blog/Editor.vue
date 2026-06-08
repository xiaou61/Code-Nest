<template>
  <CnPage class="editor-container" surface="transparent" max-width="1180px">
    <CnPageHeader
      :title="pageTitle"
      :description="pageDescription"
      eyebrow="Blog Editor"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag :type="articleId ? 'info' : 'brand'" size="sm">
          {{ articleId ? '编辑模式' : '新建文章' }}
        </CnStatusTag>
        <CnStatusTag type="neutral" size="sm">Markdown</CnStatusTag>
      </template>

      <template #actions>
        <el-button @click="handleCancel">取消</el-button>
        <el-button type="primary" plain @click="handleSaveDraft">保存草稿</el-button>
        <el-button type="success" @click="handlePublish">发布文章（消耗20积分）</el-button>
      </template>
    </CnPageHeader>

    <div class="editor-layout">
      <CnSection title="文章信息" description="填写文章基础信息、摘要和正文内容。" surface="panel" divided>
        <el-form :model="form" label-position="top" class="editor-form">
          <el-form-item label="文章标题">
            <el-input
              v-model="form.title"
              placeholder="请输入文章标题（1-200字符）"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>

          <div class="form-grid">
            <el-form-item label="文章分类">
              <el-select v-model="form.categoryId" placeholder="请选择分类" class="full-control">
                <el-option
                  v-for="category in categories"
                  :key="category.id"
                  :label="category.categoryName"
                  :value="category.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="是否原创">
              <el-radio-group v-model="form.isOriginal">
                <el-radio :label="1">原创</el-radio>
                <el-radio :label="0">转载</el-radio>
              </el-radio-group>
            </el-form-item>
          </div>

          <el-form-item label="文章标签">
            <el-select
              v-model="form.tags"
              multiple
              placeholder="请选择标签（最多5个）"
              class="full-control"
            >
              <el-option
                v-for="tag in tags"
                :key="tag.id"
                :label="tag.tagName"
                :value="tag.tagName"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="文章封面">
            <el-input v-model="form.coverImage" placeholder="请输入封面图片URL" />
          </el-form-item>

          <el-form-item label="文章摘要">
            <el-input
              v-model="form.summary"
              type="textarea"
              :rows="3"
              placeholder="请输入文章摘要（不填写将自动提取，最多200字）"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="文章内容">
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="18"
              placeholder="请输入文章内容（支持Markdown格式，至少50字符）"
            />
          </el-form-item>

          <div class="form-actions">
            <el-button @click="handleCancel">取消</el-button>
            <el-button type="primary" plain @click="handleSaveDraft">保存草稿</el-button>
            <el-button type="success" @click="handlePublish">发布文章（消耗20积分）</el-button>
          </div>
        </el-form>
      </CnSection>

      <aside class="editor-side">
        <CnSection title="发布检查" surface="panel" compact divided>
          <div class="check-list">
            <div class="check-item">
              <span>标题</span>
              <CnStatusTag :type="form.title ? 'success' : 'warning'" size="sm">
                {{ form.title ? '已填写' : '待填写' }}
              </CnStatusTag>
            </div>
            <div class="check-item">
              <span>分类</span>
              <CnStatusTag :type="form.categoryId ? 'success' : 'warning'" size="sm">
                {{ form.categoryId ? '已选择' : '待选择' }}
              </CnStatusTag>
            </div>
            <div class="check-item">
              <span>正文</span>
              <CnStatusTag :type="form.content.length >= 50 ? 'success' : 'warning'" size="sm">
                {{ form.content.length }}/50
              </CnStatusTag>
            </div>
            <div class="check-item">
              <span>标签</span>
              <CnStatusTag :type="form.tags.length <= 5 ? 'success' : 'danger'" size="sm">
                {{ form.tags.length }}/5
              </CnStatusTag>
            </div>
          </div>
        </CnSection>

        <CnSection title="内容预览" surface="panel" compact divided>
          <div v-if="form.content" class="markdown-content preview-content" v-html="previewHtml" />
          <CnEmptyState
            v-else
            title="暂无预览"
            description="输入 Markdown 正文后会在这里显示预览。"
            icon="MD"
            size="sm"
          />
        </CnSection>
      </aside>
    </div>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CnEmptyState,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatusTag
} from '@/design-system'
import {
  createArticle,
  getAllCategories,
  getAllTags,
  getArticleDetail,
  publishArticle,
  updateArticle
} from '@/api/blog'
import { renderMarkdown } from '@/utils/markdown'

interface BlogCategory {
  id: number | string
  categoryName?: string
}

interface BlogTag {
  id: number | string
  tagName?: string
}

interface BlogArticleForm {
  title: string
  categoryId: number | string | null
  tags: string[]
  coverImage: string
  summary: string
  content: string
  isOriginal: number
}

const router = useRouter()
const route = useRoute()

const categories = ref<BlogCategory[]>([])
const tags = ref<BlogTag[]>([])
const articleId = ref<number | string | null>(null)

const form = reactive<BlogArticleForm>({
  title: '',
  categoryId: null,
  tags: [],
  coverImage: '',
  summary: '',
  content: '',
  isOriginal: 1
})

const pageTitle = computed(() => articleId.value ? '编辑文章' : '写文章')
const pageDescription = computed(() => {
  return articleId.value
    ? '更新文章内容，保存草稿或重新发布。'
    : '撰写博客文章，保存为草稿或发布到个人博客主页。'
})

const breadcrumbs = [
  { label: '博客管理', to: '/blog' },
  { label: '编辑器' }
]

const previewHtml = computed(() => renderMarkdown(form.content))

const getErrorMessage = (error: unknown) => {
  if (error instanceof Error) {
    return error.message
  }

  if (typeof error === 'object' && error && 'message' in error) {
    return String((error as { message?: unknown }).message || '')
  }

  return String(error || '')
}

const getFirstRouteParam = (param: unknown) => {
  return Array.isArray(param) ? String(param[0] || '') : String(param || '')
}

const loadCategories = async () => {
  try {
    const res = await getAllCategories()
    categories.value = Array.isArray(res) ? res : []
  } catch (error) {
    console.error('加载分类失败', error)
  }
}

const loadTags = async () => {
  try {
    const res = await getAllTags()
    tags.value = Array.isArray(res) ? res : []
  } catch (error) {
    console.error('加载标签失败', error)
  }
}

const loadArticle = async (id: number | string) => {
  try {
    const res = await getArticleDetail(id)
    Object.assign(form, {
      title: res.title || '',
      categoryId: res.categoryId ?? null,
      tags: res.tags || [],
      coverImage: res.coverImage || '',
      summary: res.summary || '',
      content: res.content || '',
      isOriginal: res.isOriginal ?? 1
    })
    articleId.value = id
  } catch (error) {
    ElMessage.error(getErrorMessage(error) || '加载文章失败')
  }
}

const validateForm = () => {
  if (!form.title) {
    ElMessage.warning('请输入文章标题')
    return false
  }
  if (!form.categoryId) {
    ElMessage.warning('请选择文章分类')
    return false
  }
  if (!form.content || form.content.length < 50) {
    ElMessage.warning('文章内容不能少于50个字符')
    return false
  }
  if (form.tags.length > 5) {
    ElMessage.warning('文章标签最多5个')
    return false
  }
  return true
}

const handleSaveDraft = async () => {
  if (!form.title) {
    ElMessage.warning('请至少填写文章标题')
    return
  }

  try {
    if (articleId.value) {
      await updateArticle(articleId.value, form)
      ElMessage.success('草稿保存成功')
    } else {
      const id = await createArticle(form)
      articleId.value = id
      ElMessage.success('草稿保存成功')
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error) || '保存失败')
  }
}

const handlePublish = async () => {
  if (!validateForm()) {
    return
  }

  try {
    await ElMessageBox.confirm('发布文章需要消耗20积分，确定要发布吗？', '提示', {
      type: 'warning'
    })

    const data = { ...form, id: articleId.value || undefined }
    await publishArticle(data)
    ElMessage.success('发布成功')
    router.push('/blog')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(getErrorMessage(error) || '发布失败')
    }
  }
}

const handleCancel = () => {
  router.back()
}

onMounted(() => {
  loadCategories()
  loadTags()

  const id = getFirstRouteParam(route.params.id)
  if (id) {
    loadArticle(id)
  }
})
</script>

<style scoped>
.editor-container {
  min-height: calc(100vh - 68px);
}

.editor-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  align-items: start;
  gap: var(--cn-space-5);
}

.editor-form {
  min-width: 0;
}

.form-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(180px, 240px);
  gap: var(--cn-space-4);
}

.full-control {
  width: 100%;
}

.form-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--cn-space-2);
  padding-top: var(--cn-space-3);
}

.editor-side {
  display: grid;
  gap: var(--cn-space-4);
  min-width: 0;
  position: sticky;
  top: 84px;
}

.check-list {
  display: grid;
  gap: var(--cn-space-3);
}

.check-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
  border-bottom: 1px solid var(--cn-color-border-subtle);
  color: var(--cn-color-text-secondary);
  padding-bottom: var(--cn-space-3);
}

.check-item:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.preview-content {
  max-height: 520px;
  overflow: auto;
  border: 0;
  border-radius: 0;
  box-shadow: none;
  padding: 0;
  background: transparent;
  color: var(--cn-color-text-primary);
}

@media (max-width: 1060px) {
  .editor-layout {
    grid-template-columns: 1fr;
  }

  .editor-side {
    position: static;
  }
}

@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .form-actions {
    justify-content: stretch;
  }

  .form-actions .el-button {
    flex: 1;
  }
}
</style>
