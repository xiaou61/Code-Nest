<template>
  <CnPage class="create-post-container" surface="transparent" max-width="1400px">
    <CnPageHeader
      title="创作帖子"
      description="使用 Markdown 格式组织内容，发布后将在技术社区中展示。"
      eyebrow="Community Editor"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">{{ postForm.title.length }}/200 标题</CnStatusTag>
        <CnStatusTag type="info" size="sm">{{ postForm.content.length }}/50000 内容</CnStatusTag>
        <CnStatusTag v-if="draftSavedAt" type="success" size="sm">
          草稿已保存 {{ draftSavedAt }}
        </CnStatusTag>
      </template>

      <template #actions>
        <el-button plain :icon="Back" @click="goBack">社区首页</el-button>
        <el-button
          type="primary"
          :loading="publishLoading"
          @click="handlePublish"
        >
          发布帖子
        </el-button>
      </template>
    </CnPageHeader>

    <!-- 主内容区 -->
    <div class="create-main">
      <!-- 基本信息卡片 -->
      <div class="info-card">
        <div class="card-header">
          <h3>📝 基本信息</h3>
          <div class="draft-status">
            <CnStatusTag v-if="draftSavedAt" type="success" size="sm">
              <el-icon><CircleCheck /></el-icon>
              草稿已保存于 {{ draftSavedAt }}
            </CnStatusTag>
            <button 
              v-if="postForm.title || postForm.content"
              class="clear-draft-btn"
              @click="clearDraft"
            >
              <el-icon><Delete /></el-icon>
              清空草稿
            </button>
          </div>
        </div>
        
        <div class="form-row">
          <div class="form-group title-group">
            <label>帖子标题 <span class="required">*</span></label>
            <input 
              v-model="postForm.title"
              type="text"
              class="form-input title-input"
              placeholder="输入一个吸引人的标题..."
              maxlength="200"
              @input="saveDraft"
            />
            <span class="char-count">{{ postForm.title.length }}/200</span>
          </div>
          <div class="form-group category-group">
            <label>分类</label>
            <el-select 
              v-model="postForm.categoryId" 
              placeholder="选择分类"
              clearable
              class="category-select"
              @change="saveDraft"
            >
              <el-option
                v-for="category in categoryList"
                :key="category.id"
                :label="category.name"
                :value="category.id"
              />
            </el-select>
          </div>
        </div>

        <!-- 标签选择 -->
        <div class="tags-section">
          <label>标签（最多5个）</label>
          <div class="tags-wrapper">
            <CnStatusTag
              v-for="tagId in postForm.tagIds" 
              :key="tagId" 
              class="tag-item selected"
              type="brand"
              size="sm"
              :dot="false"
            >
              # {{ getTagName(tagId) }}
              <span class="tag-remove" @click="removeTag(tagId)">×</span>
            </CnStatusTag>
            <button 
              v-if="postForm.tagIds.length < 5"
              class="add-tag-btn"
              @click="showTagSelector = true"
            >
              <el-icon><Plus /></el-icon>
              添加标签
            </button>
          </div>
        </div>
      </div>

      <!-- 编辑器区域 -->
      <div class="editor-card">
        <div class="editor-layout">
          <!-- 左侧编辑器 -->
          <div class="editor-pane">
            <div class="pane-header">
              <h3>
                <span class="icon">💻</span>
                Markdown 编辑
              </h3>
              <div class="toolbar">
                <button class="tool-btn" @click="insertMarkdown('**', '**')" title="加粗">
                  <strong>B</strong>
                </button>
                <button class="tool-btn" @click="insertMarkdown('*', '*')" title="斜体">
                  <em>I</em>
                </button>
                <button class="tool-btn" @click="insertMarkdown('`', '`')" title="行内代码">
                  &lt;/&gt;
                </button>
                <button class="tool-btn" @click="insertMarkdown('### ', '')" title="标题">
                  H
                </button>
                <button class="tool-btn" @click="insertMarkdown('\n```\n', '\n```')" title="代码块">
                  { }
                </button>
                <button class="tool-btn" @click="insertMarkdown('> ', '')" title="引用">
                  ”
                </button>
                <button class="tool-btn" @click="insertMarkdown('- ', '')" title="列表">
                  •
                </button>
                <button class="tool-btn" @click="insertMarkdown('[', '](url)')" title="链接">
                  🔗
                </button>
              </div>
            </div>
            <textarea
              v-model="postForm.content"
              class="editor-textarea"
              placeholder="在这里使用 Markdown 格式编写内容...

支持的格式：
# 标题  **加粗**  *斜体*  `代码`
- 列表项  > 引用  [链接](url)
```代码块```"
              @input="handleContentInput"
            ></textarea>
            <div class="editor-footer">
              <span class="word-count">{{ postForm.content.length }} / 50000 字符</span>
            </div>
          </div>

          <!-- 右侧预览 -->
          <div class="preview-pane">
            <div class="pane-header">
              <h3>
                <span class="icon">👁️</span>
                实时预览
              </h3>
              <CnStatusTag type="info" size="sm" :dot="false" subtle>
                {{ previewWordCount }} 字
              </CnStatusTag>
            </div>
            <div class="preview-body markdown-content" v-html="previewHtml"></div>
          </div>
        </div>
      </div>

      <!-- 发布按钮 -->
      <div class="publish-bar">
        <button class="cancel-btn" @click="goBack">
          取消
        </button>
        <button 
          class="publish-btn"
          @click="handlePublish"
          :disabled="publishLoading"
        >
          <el-icon v-if="publishLoading" class="is-loading"><Loading /></el-icon>
          <el-icon v-else><Edit /></el-icon>
          {{ publishLoading ? '发布中...' : '发布帖子' }}
        </button>
      </div>
    </div>

    <!-- 标签选择对话框 -->
    <el-dialog
      v-model="showTagSelector"
      title="选择标签"
      width="500px"
      :close-on-click-modal="false"
      class="tag-dialog"
    >
      <div class="tag-selector-grid">
        <span
          v-for="tag in tagList"
          :key="tag.id"
          class="tag-option"
          :class="{ selected: postForm.tagIds.includes(tag.id) }"
          @click="toggleTag(tag.id)"
        >
          # {{ tag.name }}
          <el-icon v-if="postForm.tagIds.includes(tag.id)" class="check-icon"><Check /></el-icon>
        </span>
        <div v-if="tagList.length === 0" class="empty-tags">
          暂无可用标签
        </div>
      </div>
      <template #footer>
        <button class="dialog-btn" @click="showTagSelector = false">完成</button>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Back, Edit, Plus, Check, CircleCheck, Delete, Loading
} from '@element-plus/icons-vue'
import { CnPage, CnPageHeader, CnStatusTag } from '@/design-system'
import { communityApi } from '@/api/community'
import { renderMarkdown } from '@/utils/markdown'

const router = useRouter()

interface CommunityCategory {
  id: number | string
  name: string
}

interface CommunityTag {
  id: number | string
  name: string
}

interface PostDraft {
  title?: string
  content?: string
  categoryId?: number | string | null
  tagIds?: Array<number | string>
  savedAt?: number
}

// 响应式数据
const publishLoading = ref(false)
const showTagSelector = ref(false)
const draftSavedAt = ref('')

// 表单数据
const postForm = reactive({
  title: '',
  content: '',
  categoryId: null as number | string | null,
  tagIds: [] as Array<number | string>
})

// 分类列表
const categoryList = ref<CommunityCategory[]>([])
// 标签列表
const tagList = ref<CommunityTag[]>([])

// 草稿保存的key
const DRAFT_KEY = 'community_post_draft'
// 防抖定时器
let draftTimer: ReturnType<typeof setTimeout> | null = null

const breadcrumbs = [
  { label: '首页', to: '/' },
  { label: '技术社区', to: '/community' },
  { label: '创作帖子' }
]

// 计算属性 - 预览内容
const previewHtml = computed(() => {
  if (!postForm.content) {
    return '<div class="empty-preview">在左侧编辑器中输入内容，右侧将显示实时预览...</div>'
  }
  return renderMarkdown(postForm.content)
})

// 计算属性 - 预览字符数
const previewWordCount = computed(() => {
  return postForm.content.length
})

// 获取标签名称
const getTagName = (tagId: number | string) => {
  const tag = tagList.value.find((item) => item.id === tagId)
  return tag ? tag.name : ''
}

// 切换标签
const toggleTag = (tagId: number | string) => {
  const index = postForm.tagIds.indexOf(tagId)
  if (index > -1) {
    postForm.tagIds.splice(index, 1)
  } else {
    if (postForm.tagIds.length < 5) {
      postForm.tagIds.push(tagId)
    } else {
      ElMessage.warning('最多只能选择5个标签')
    }
  }
  saveDraft()
}

// 移除标签
const removeTag = (tagId: number | string) => {
  const index = postForm.tagIds.indexOf(tagId)
  if (index > -1) {
    postForm.tagIds.splice(index, 1)
    saveDraft()
  }
}

// 保存草稿（防抖）
const saveDraft = () => {
  if (draftTimer) {
    clearTimeout(draftTimer)
  }
  draftTimer = setTimeout(() => {
    try {
      const draft = {
        title: postForm.title,
        content: postForm.content,
        categoryId: postForm.categoryId,
        tagIds: postForm.tagIds,
        savedAt: new Date().getTime()
      }
      localStorage.setItem(DRAFT_KEY, JSON.stringify(draft))
      draftSavedAt.value = new Date().toLocaleTimeString('zh-CN')
    } catch (error) {
      console.error('保存草稿失败:', error)
    }
  }, 1000)
}

// 清空草稿
const clearDraft = () => {
  ElMessageBox.confirm(
    '确定要清空当前草稿吗？此操作不可恢复。',
    '清空草稿',
    {
      confirmButtonText: '确认清空',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    postForm.title = ''
    postForm.content = ''
    postForm.categoryId = null
    postForm.tagIds = []
    localStorage.removeItem(DRAFT_KEY)
    draftSavedAt.value = ''
    ElMessage.success('草稿已清空')
  }).catch(() => {})
}

// 加载草稿
const loadDraft = () => {
  try {
    const draftStr = localStorage.getItem(DRAFT_KEY)
    if (draftStr) {
      const draft = JSON.parse(draftStr) as PostDraft
      postForm.title = draft.title || ''
      postForm.content = draft.content || ''
      postForm.categoryId = draft.categoryId || null
      postForm.tagIds = draft.tagIds || []
      if (draft.savedAt) {
        draftSavedAt.value = new Date(draft.savedAt).toLocaleTimeString('zh-CN')
      }
      if (postForm.title || postForm.content) {
        ElMessage.info('已恢复上次编辑的草稿')
      }
    }
  } catch (error) {
    console.error('加载草稿失败:', error)
  }
}

// 处理内容输入
const handleContentInput = () => {
  saveDraft()
}

// 插入Markdown标记
const insertMarkdown = (before: string, after: string) => {
  const textarea = document.querySelector<HTMLTextAreaElement>('.markdown-editor textarea')
  if (!textarea) return
  
  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const selectedText = postForm.content.substring(start, end)
  
  const replacement = before + selectedText + after
  postForm.content = postForm.content.substring(0, start) + replacement + postForm.content.substring(end)
  
  // 重新设置光标位置
  nextTick(() => {
    textarea.focus()
    textarea.setSelectionRange(start + before.length, start + before.length + selectedText.length)
  })
}

// 发表帖子
const handlePublish = async () => {
  // 验证表单
  if (!postForm.title.trim()) {
    ElMessage.error('请输入帖子标题')
    return
  }
  if (postForm.title.length < 5 || postForm.title.length > 200) {
    ElMessage.error('标题长度应为5-200个字符')
    return
  }

  if (!postForm.content.trim()) {
    ElMessage.error('请输入帖子内容')
    return
  }
  if (postForm.content.length < 10 || postForm.content.length > 50000) {
    ElMessage.error('内容长度应为10-50000个字符')
    return
  }

  try {
    await ElMessageBox.confirm(
      '确认发表这篇帖子吗？发表后将在社区中展示。',
      '确认发表',
      {
        confirmButtonText: '确认发表',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    publishLoading.value = true
    
    await communityApi.createPost(postForm)
    
    // 清除草稿
    localStorage.removeItem(DRAFT_KEY)
    
    ElMessage.success('发表成功！')
    
    // 跳转到社区首页
    router.push('/community')
    
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('发表失败')
    }
  } finally {
    publishLoading.value = false
  }
}

// 返回社区
const goBack = () => {
  if (postForm.title.trim() || postForm.content.trim() || postForm.categoryId) {
    ElMessageBox.confirm(
      '您有未保存的内容，确定要离开吗？',
      '确认离开',
      {
        confirmButtonText: '确认离开',
        cancelButtonText: '继续编辑',
        type: 'warning'
      }
    ).then(() => {
      router.push('/community')
    }).catch(() => {
      // 用户取消，什么都不做
    })
  } else {
    router.push('/community')
  }
}

// 加载分类列表
const loadCategories = async () => {
  try {
    const response = await communityApi.getEnabledCategories()
    categoryList.value = response || []
  } catch (error) {
    console.error('加载分类列表失败:', error)
  }
}

// 加载标签列表
const loadTags = async () => {
  try {
    const response = await communityApi.getTags()
    tagList.value = response || []
  } catch (error) {
    console.error('加载标签列表失败:', error)
  }
}

// 初始化
onMounted(() => {
  loadCategories()
  loadTags()
  loadDraft()
})

// 组件卸载前清理定时器
onBeforeUnmount(() => {
  if (draftTimer) {
    clearTimeout(draftTimer)
  }
})
</script>

<style scoped>
.create-post-container {
  --editor-surface: var(--cn-color-bg-surface);
  --editor-surface-muted: var(--cn-color-bg-surface-muted);
  --editor-border: var(--cn-color-border-subtle);
  --editor-border-strong: var(--cn-color-border);
  --editor-accent: var(--cn-color-brand-primary);
  --editor-accent-hover: var(--cn-color-brand-hover);
  --editor-accent-soft: var(--cn-color-brand-soft);
  --editor-text: var(--cn-color-text-primary);
  --editor-text-muted: var(--cn-color-text-secondary);
  --editor-text-subtle: var(--cn-color-text-tertiary);
  --editor-danger: var(--cn-color-danger);
  --editor-danger-soft: var(--cn-color-danger-soft);
  --editor-shadow: var(--cn-shadow-sm);
  --editor-radius: var(--cn-radius-card);
  --editor-radius-sm: var(--cn-radius-control);
  --editor-ring: var(--cn-color-focus-ring);

  min-height: 100vh;
  background: var(--cn-color-bg-page);
}

.breadcrumb-nav {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  margin-bottom: 12px;
  color: var(--editor-text-subtle);
}

.back-link {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--editor-accent);
  cursor: pointer;
  transition: color var(--cn-motion-fast) var(--cn-ease-out);
}

.back-link:hover {
  color: var(--editor-accent-hover);
}

.breadcrumb-sep {
  color: var(--editor-border-strong);
}

.current-page {
  color: var(--editor-text-muted);
}

.create-main {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}

.info-card,
.editor-card {
  background: var(--editor-surface);
  border: 1px solid var(--editor-border);
  border-radius: var(--editor-radius);
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: var(--editor-shadow);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--editor-border);
}

.card-header h3,
.form-group label,
.tags-section label,
.pane-header h3 {
  color: var(--editor-text);
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.draft-status {
  display: flex;
  align-items: center;
  gap: 12px;
}

.saved-hint {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--cn-color-success);
}

.clear-draft-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: transparent;
  border: 1px solid var(--editor-danger);
  border-radius: 6px;
  font-size: 12px;
  color: var(--editor-danger);
  cursor: pointer;
  transition: all var(--cn-motion-base) var(--cn-ease-out);
}

.clear-draft-btn:hover {
  background: var(--editor-danger);
  color: white;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 200px;
  gap: 16px;
  margin-bottom: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label,
.tags-section label {
  font-size: 14px;
  font-weight: 500;
}

.required {
  color: var(--editor-danger);
}

.title-group {
  position: relative;
}

.form-input,
.editor-textarea,
.preview-body {
  width: 100%;
  border: 1px solid var(--editor-border-strong);
  border-radius: var(--editor-radius-sm);
  color: var(--editor-text);
  transition: all var(--cn-motion-base) var(--cn-ease-out);
  box-sizing: border-box;
}

.form-input {
  padding: 12px 16px;
  background: var(--editor-surface);
  font-size: 15px;
}

.form-input:focus,
.editor-textarea:focus {
  outline: none;
  border-color: var(--editor-accent);
  box-shadow: 0 0 0 3px var(--editor-ring);
}

.title-input {
  padding-right: 70px;
}

.char-count,
.word-count,
.empty-tags {
  color: var(--editor-text-subtle);
}

.char-count {
  position: absolute;
  right: 12px;
  bottom: 12px;
  font-size: 12px;
}

.category-select {
  width: 100%;
}

:deep(.category-select .el-input__wrapper) {
  border-radius: var(--editor-radius-sm);
  padding: 8px 12px;
}

.tags-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tags-wrapper {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: var(--editor-accent);
  border-radius: 6px;
  font-size: 13px;
  color: white;
}

.tag-remove {
  cursor: pointer;
  opacity: 0.8;
  font-size: 14px;
}

.tag-remove:hover {
  opacity: 1;
}

.add-tag-btn,
.tool-btn,
.tag-option,
.cancel-btn {
  display: flex;
  align-items: center;
  border-radius: 6px;
  color: var(--editor-text-muted);
  cursor: pointer;
  transition: all var(--cn-motion-base) var(--cn-ease-out);
}

.add-tag-btn {
  gap: 4px;
  padding: 6px 12px;
  background: var(--editor-surface-muted);
  border: 1px dashed var(--editor-border-strong);
  font-size: 13px;
}

.add-tag-btn:hover,
.tag-option:hover,
.cancel-btn:hover {
  border-color: var(--editor-accent);
  color: var(--editor-accent);
}

.editor-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  min-height: 500px;
}

.editor-pane,
.preview-pane {
  display: flex;
  flex-direction: column;
}

.pane-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.pane-header h3 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.pane-header .icon {
  font-size: 18px;
}

.toolbar {
  display: flex;
  gap: 4px;
}

.tool-btn {
  width: 32px;
  height: 32px;
  justify-content: center;
  background: var(--editor-surface-muted);
  border: none;
  font-size: 14px;
}

.tool-btn:hover {
  background: var(--editor-accent);
  color: white;
}

.editor-textarea {
  flex: 1;
  min-height: 400px;
  padding: 16px;
  background: var(--editor-surface);
  font-family: 'Consolas', 'Monaco', 'SF Mono', monospace;
  font-size: 14px;
  line-height: 1.6;
  resize: none;
}

.editor-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

.word-count {
  font-size: 12px;
}

.preview-badge {
  padding: 2px 10px;
  background: var(--editor-accent-soft);
  border-radius: var(--cn-radius-pill);
  font-size: 12px;
  color: var(--editor-accent);
}

.preview-body {
  flex: 1;
  min-height: 400px;
  padding: 16px;
  background: var(--editor-surface-muted);
  overflow-y: auto;
}

.preview-body::-webkit-scrollbar {
  width: 6px;
}

.preview-body::-webkit-scrollbar-thumb {
  background: var(--editor-border-strong);
  border-radius: 3px;
}

.publish-bar {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.cancel-btn {
  padding: 12px 32px;
  background: var(--editor-surface);
  border: 1px solid var(--editor-border-strong);
  font-size: 15px;
}

.publish-btn,
.dialog-btn,
.tag-option.selected {
  background: var(--editor-accent);
  color: white;
}

.publish-btn,
.dialog-btn {
  border: none;
  border-radius: var(--editor-radius-sm);
  cursor: pointer;
}

.publish-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 32px;
  font-size: 15px;
  font-weight: 500;
  transition: all var(--cn-motion-base) var(--cn-ease-out);
}

.publish-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px color-mix(in srgb, var(--editor-accent) 36%, transparent);
}

.publish-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.tag-selector-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  max-height: 300px;
  overflow-y: auto;
}

.tag-option {
  display: inline-flex;
  gap: 6px;
  padding: 8px 14px;
  background: var(--editor-surface-muted);
  border: 1px solid transparent;
  font-size: 14px;
}

.check-icon {
  font-size: 14px;
}

.empty-tags {
  width: 100%;
  text-align: center;
  padding: 40px;
}

.dialog-btn {
  padding: 10px 24px;
  font-size: 14px;
}

@media (max-width: 900px) {
  .editor-layout {
    grid-template-columns: 1fr;
  }

  .preview-pane {
    order: -1;
  }

  .editor-textarea,
  .preview-body {
    min-height: 300px;
  }
}

@media (max-width: 768px) {
  .create-main {
    padding: 16px;
  }

  .info-card,
  .editor-card {
    padding: 16px;
  }

  .form-row {
    grid-template-columns: 1fr;
  }

  .toolbar {
    flex-wrap: wrap;
  }

  .publish-bar {
    flex-direction: column;
  }

  .cancel-btn,
  .publish-btn {
    width: 100%;
    justify-content: center;
  }
}
</style>
