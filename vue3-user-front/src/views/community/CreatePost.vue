<template>
  <div class="create-post-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <div class="breadcrumb-nav">
          <span class="back-link" @click="goBack">
            <el-icon><Back /></el-icon>
            社区首页
          </span>
          <span class="breadcrumb-sep">/</span>
          <span class="current-page">创作帖子</span>
        </div>
        <div class="page-title-row">
          <h1 class="page-title">✍️ 创作帖子</h1>
          <p class="page-subtitle">使用 Markdown 格式，让你的内容更加丰富多彩</p>
        </div>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="create-main">
      <!-- 基本信息卡片 -->
      <div class="info-card">
        <div class="card-header">
          <h3>📝 基本信息</h3>
          <div class="draft-status">
            <span v-if="draftSavedAt" class="saved-hint">
              <el-icon><CircleCheck /></el-icon>
              草稿已保存于 {{ draftSavedAt }}
            </span>
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
            <span 
              v-for="tagId in postForm.tagIds" 
              :key="tagId" 
              class="tag-item selected"
            >
              # {{ getTagName(tagId) }}
              <span class="tag-remove" @click="removeTag(tagId)">×</span>
            </span>
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
              <span class="preview-badge">{{ previewWordCount }} 字</span>
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
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Back, Edit, Plus, Check, CircleCheck, Delete, Loading
} from '@element-plus/icons-vue'
import { communityApi } from '@/api/community'
import { renderMarkdown } from '@/utils/markdown'

const router = useRouter()

// 响应式数据
const publishLoading = ref(false)
const showTagSelector = ref(false)
const draftSavedAt = ref('')

// 表单数据
const postForm = reactive({
  title: '',
  content: '',
  categoryId: null,
  tagIds: []
})

// 分类列表
const categoryList = ref([])
// 标签列表
const tagList = ref([])

// 草稿保存的key
const DRAFT_KEY = 'community_post_draft'
// 防抖定时器
let draftTimer = null

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
const getTagName = (tagId) => {
  const tag = tagList.value.find(t => t.id === tagId)
  return tag ? tag.name : ''
}

// 切换标签
const toggleTag = (tagId) => {
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
const removeTag = (tagId) => {
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
      const draft = JSON.parse(draftStr)
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
const insertMarkdown = (before, after) => {
  const textarea = document.querySelector('.markdown-editor textarea')
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
/* ========== 全局容器 ========== */
.create-post-container {
  min-height: 100vh;
  background: #f4f5f5;
}

/* ========== 页面头部 ========== */
.page-header {
  background: white;
  border-bottom: 1px solid #e5e5e5;
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px 24px;
}

.breadcrumb-nav {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  margin-bottom: 12px;
  color: #999;
}

.back-link {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #00b894;
  cursor: pointer;
  transition: color 0.3s;
}

.back-link:hover {
  color: #00a085;
}

.breadcrumb-sep {
  color: #ddd;
}

.current-page {
  color: #666;
}

.page-title-row {
  display: flex;
  align-items: baseline;
  gap: 16px;
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
}

.page-subtitle {
  margin: 0;
  font-size: 14px;
  color: #999;
}

/* ========== 主内容区 ========== */
.create-main {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}

/* ========== 信息卡片 ========== */
.info-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
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
  color: #10b981;
}

.clear-draft-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: none;
  border: 1px solid #f56565;
  border-radius: 6px;
  font-size: 12px;
  color: #f56565;
  cursor: pointer;
  transition: all 0.3s;
}

.clear-draft-btn:hover {
  background: #f56565;
  color: white;
}

/* 表单 */
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

.form-group label {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.required {
  color: #f56565;
}

.title-group {
  position: relative;
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  font-size: 15px;
  transition: all 0.3s;
  box-sizing: border-box;
}

.form-input:focus {
  outline: none;
  border-color: #00b894;
  box-shadow: 0 0 0 3px rgba(0, 184, 148, 0.1);
}

.title-input {
  padding-right: 70px;
}

.char-count {
  position: absolute;
  right: 12px;
  bottom: 12px;
  font-size: 12px;
  color: #999;
}

.category-select {
  width: 100%;
}

:deep(.category-select .el-input__wrapper) {
  border-radius: 8px;
  padding: 8px 12px;
}

/* 标签区域 */
.tags-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tags-section label {
  font-size: 14px;
  font-weight: 500;
  color: #333;
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
  background: linear-gradient(135deg, #00b894 0%, #00a085 100%);
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

.add-tag-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  background: #f5f5f5;
  border: 1px dashed #ccc;
  border-radius: 6px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s;
}

.add-tag-btn:hover {
  border-color: #00b894;
  color: #00b894;
}

/* ========== 编辑器卡片 ========== */
.editor-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
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
  color: #333;
}

.pane-header .icon {
  font-size: 18px;
}

/* 工具栏 */
.toolbar {
  display: flex;
  gap: 4px;
}

.tool-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s;
}

.tool-btn:hover {
  background: #00b894;
  color: white;
}

/* 编辑器 */
.editor-textarea {
  flex: 1;
  min-height: 400px;
  padding: 16px;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  font-family: 'Consolas', 'Monaco', 'SF Mono', monospace;
  font-size: 14px;
  line-height: 1.6;
  resize: none;
  transition: all 0.3s;
}

.editor-textarea:focus {
  outline: none;
  border-color: #00b894;
  box-shadow: 0 0 0 3px rgba(0, 184, 148, 0.1);
}

.editor-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

.word-count {
  font-size: 12px;
  color: #999;
}

/* 预览 */
.preview-badge {
  padding: 2px 10px;
  background: #e8f8f5;
  border-radius: 12px;
  font-size: 12px;
  color: #00b894;
}

.preview-body {
  flex: 1;
  min-height: 400px;
  padding: 16px;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  background: #fafafa;
  overflow-y: auto;
}

.preview-body::-webkit-scrollbar {
  width: 6px;
}

.preview-body::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 3px;
}

/* ========== 发布栏 ========== */
.publish-bar {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.cancel-btn {
  padding: 12px 32px;
  background: white;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  font-size: 15px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s;
}

.cancel-btn:hover {
  border-color: #00b894;
  color: #00b894;
}

.publish-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 32px;
  background: linear-gradient(135deg, #00b894 0%, #00a085 100%);
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 500;
  color: white;
  cursor: pointer;
  transition: all 0.3s;
}

.publish-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 184, 148, 0.4);
}

.publish-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

/* ========== 标签对话框 ========== */
.tag-selector-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  max-height: 300px;
  overflow-y: auto;
}

.tag-option {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: #f5f5f5;
  border: 1px solid transparent;
  border-radius: 6px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s;
}

.tag-option:hover {
  border-color: #00b894;
  color: #00b894;
}

.tag-option.selected {
  background: linear-gradient(135deg, #00b894 0%, #00a085 100%);
  color: white;
}

.check-icon {
  font-size: 14px;
}

.empty-tags {
  width: 100%;
  text-align: center;
  padding: 40px;
  color: #999;
}

.dialog-btn {
  padding: 10px 24px;
  background: linear-gradient(135deg, #00b894 0%, #00a085 100%);
  border: none;
  border-radius: 8px;
  font-size: 14px;
  color: white;
  cursor: pointer;
}

/* ========== 响应式 ========== */
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
  .header-content {
    padding: 16px;
  }
  
  .page-title-row {
    flex-direction: column;
    gap: 4px;
  }
  
  .page-title {
    font-size: 20px;
  }
  
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
