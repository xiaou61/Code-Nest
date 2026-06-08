<template>
  <CnPage class="codepen-editor" max-width="100%" full-height dense>
    <CnPageHeader
      :title="pageTitle"
      :description="pageDescription"
      eyebrow="CODEPEN EDITOR"
      :breadcrumbs="[{ label: '首页', to: '/' }, { label: '代码广场', to: '/codepen' }, { label: pageTitle }]"
      compact
    >
      <template #meta>
        <CnStatusTag :type="penData.status === 1 ? 'success' : 'warning'" size="sm" dot>
          {{ penData.status === 1 ? '已发布' : '草稿' }}
        </CnStatusTag>
        <CnStatusTag :type="penData.isPublic === 1 ? 'brand' : 'neutral'" size="sm" subtle>
          {{ penData.isPublic === 1 ? '公开可见' : '私密作品' }}
        </CnStatusTag>
        <CnStatusTag :type="penData.isFree === 1 ? 'info' : 'success'" size="sm" subtle>
          {{ penData.isFree === 1 ? '免费 Fork' : `${penData.forkPrice || 0} 积分 Fork` }}
        </CnStatusTag>
      </template>

      <template #actions>
        <el-button icon="Back" @click="goBack">返回</el-button>
        <el-button icon="Document" :loading="saving" @click="saveDraft">保存草稿</el-button>
        <el-button type="success" icon="Upload" :loading="publishing" @click="publish">发布作品</el-button>
        <el-button icon="Setting" @click="showSettings = true">设置</el-button>
      </template>
    </CnPageHeader>

    <section class="editor-stats" aria-label="代码编辑概览">
      <CnStatCard
        title="HTML"
        :value="codeMetrics.htmlLines"
        unit="行"
        description="结构代码"
        tone="brand"
        trend="flat"
        trend-text="Markup"
      />
      <CnStatCard
        title="CSS"
        :value="codeMetrics.cssLines"
        unit="行"
        description="样式代码"
        tone="info"
        trend="flat"
        trend-text="Style"
      />
      <CnStatCard
        title="JavaScript"
        :value="codeMetrics.jsLines"
        unit="行"
        description="交互代码"
        tone="warning"
        trend="flat"
        trend-text="Logic"
      />
      <CnStatCard
        title="标签"
        :value="penData.tags.length"
        unit="/ 5"
        description="发布和搜索标识"
        tone="neutral"
        trend="flat"
        trend-text="Meta"
      />
    </section>

    <CnSection
      class="editor-meta-section"
      title="作品信息"
      description="标题和描述会同步用于保存草稿、发布作品与广场展示。"
      compact
      divided
    >
      <div class="editor-meta-form">
        <el-input
          v-model="penData.title"
          placeholder="请输入作品标题"
          maxlength="100"
          show-word-limit
        >
          <template #prefix>
            <el-icon><Document /></el-icon>
          </template>
        </el-input>
        <el-input
          v-model="penData.description"
          type="textarea"
          :rows="2"
          placeholder="补充一句作品描述，便于别人理解这个示例的用途。"
          maxlength="500"
          show-word-limit
        />
      </div>
    </CnSection>

    <CnSection
      class="workbench-section"
      title="代码工作台"
      description="编辑 HTML、CSS、JavaScript 后自动刷新预览，也可以手动运行当前代码。"
      compact
      divided
    >
      <template #actions>
        <el-button text icon="Refresh" @click="runCode">运行</el-button>
        <el-button text icon="FullScreen" @click="fullscreenPreview = true">全屏预览</el-button>
      </template>

      <div class="editor-body">
        <div class="code-panels">
          <div class="code-panel code-panel--html">
            <div class="panel-header">
              <span class="panel-title">
                <el-icon><Document /></el-icon>
                HTML
              </span>
              <CnStatusTag type="brand" size="sm" subtle>{{ codeMetrics.htmlLines }} 行</CnStatusTag>
            </div>
            <textarea
              v-model="penData.htmlCode"
              class="code-textarea"
              placeholder="在这里编写HTML代码..."
              spellcheck="false"
              @input="autoRunCode"
            ></textarea>
          </div>

          <div class="code-panel code-panel--css">
            <div class="panel-header">
              <span class="panel-title">
                <el-icon><Brush /></el-icon>
                CSS
              </span>
              <CnStatusTag type="info" size="sm" subtle>{{ codeMetrics.cssLines }} 行</CnStatusTag>
            </div>
            <textarea
              v-model="penData.cssCode"
              class="code-textarea"
              placeholder="在这里编写CSS代码..."
              spellcheck="false"
              @input="autoRunCode"
            ></textarea>
          </div>

          <div class="code-panel code-panel--js">
            <div class="panel-header">
              <span class="panel-title">
                <el-icon><Lightning /></el-icon>
                JavaScript
              </span>
              <CnStatusTag type="warning" size="sm" subtle>{{ codeMetrics.jsLines }} 行</CnStatusTag>
            </div>
            <textarea
              v-model="penData.jsCode"
              class="code-textarea"
              placeholder="在这里编写JavaScript代码..."
              spellcheck="false"
              @input="autoRunCode"
            ></textarea>
          </div>
        </div>

        <div class="preview-panel">
          <div class="panel-header panel-header--preview">
            <span class="panel-title">
              <el-icon><View /></el-icon>
              预览
            </span>
            <el-button text size="small" icon="FullScreen" @click="fullscreenPreview = true">
              全屏
            </el-button>
          </div>
          <iframe
            ref="previewFrame"
            class="preview-iframe"
            sandbox="allow-scripts allow-same-origin"
          ></iframe>
        </div>
      </div>
    </CnSection>

    <!-- 设置对话框 -->
    <el-dialog
      v-model="showSettings"
      title="作品设置"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form :model="penData" label-width="100px">
        <el-form-item label="作品标题">
          <el-input
            v-model="penData.title"
            placeholder="请输入作品标题"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="作品描述">
          <el-input
            v-model="penData.description"
            type="textarea"
            :rows="4"
            placeholder="请输入作品描述"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="标签">
          <el-select
            v-model="penData.tags"
            multiple
            filterable
            allow-create
            placeholder="选择或输入标签（最多5个）"
            class="settings-select"
            :max-collapse-tags="3"
          >
            <el-option
              v-for="tag in allTags"
              :key="tag.tagName"
              :label="tag.tagName"
              :value="tag.tagName"
            />
          </el-select>
          <div class="form-tip">最多选择5个标签</div>
        </el-form-item>

        <el-form-item label="分类">
          <el-select
            v-model="penData.category"
            placeholder="选择分类"
            class="settings-select"
          >
            <el-option label="动画特效" value="动画" />
            <el-option label="组件库" value="组件" />
            <el-option label="游戏" value="游戏" />
            <el-option label="工具" value="工具" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>

        <el-form-item label="可见性">
          <el-radio-group v-model="penData.isPublic">
            <el-radio :label="1">公开</el-radio>
            <el-radio :label="0">私密</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="作品类型">
          <el-radio-group v-model="penData.isFree">
            <el-radio :label="1">免费（源码公开，可免费Fork）</el-radio>
            <el-radio :label="0">付费（源码隐藏，Fork需付费）</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="Fork价格" v-if="penData.isFree === 0">
          <el-input-number
            v-model="penData.forkPrice"
            :min="1"
            :max="1000"
            placeholder="设置Fork价格"
            class="fork-price-input"
          />
          <span class="form-tip fork-price-tip">积分（1-1000）</span>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showSettings = false">取消</el-button>
        <el-button type="primary" @click="showSettings = false">确定</el-button>
      </template>
    </el-dialog>

    <!-- 全屏预览 -->
    <el-dialog
      v-model="fullscreenPreview"
      title="全屏预览"
      width="90%"
      :close-on-click-modal="false"
      fullscreen
    >
      <iframe
        ref="fullscreenFrame"
        class="fullscreen-iframe"
        sandbox="allow-scripts allow-same-origin"
      ></iframe>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, ref, reactive, onMounted, watch, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { codepenApi } from '@/api/codepen'
import { CnPage, CnPageHeader, CnSection, CnStatCard, CnStatusTag } from '@/design-system'
import { Document, Brush, Lightning, View } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

interface CodePenTag {
  tagName: string
  useCount?: number
}

interface PenData {
  id: number | string | null
  title: string
  description: string
  htmlCode: string
  cssCode: string
  jsCode: string
  tags: string[]
  category: string
  isPublic: number
  isFree: number
  forkPrice: number
  status: number
}

// 作品数据
const penData = reactive<PenData>({
  id: null,
  title: '',
  description: '',
  htmlCode: '',
  cssCode: '',
  jsCode: '',
  tags: [],
  category: '',
  isPublic: 1,
  isFree: 1,
  forkPrice: 0,
  status: 0 // 0-草稿 1-已发布
})

// 页面状态
const saving = ref(false)
const publishing = ref(false)
const showSettings = ref(false)
const fullscreenPreview = ref(false)
const allTags = ref<CodePenTag[]>([])
const previewFrame = ref<HTMLIFrameElement | null>(null)
const fullscreenFrame = ref<HTMLIFrameElement | null>(null)
const autoRunTimer = ref<ReturnType<typeof setTimeout> | null>(null)

const routePenId = computed(() => {
  const id = route.params.id
  return Array.isArray(id) ? id[0] : id
})

const pageTitle = computed(() => (routePenId.value ? '编辑作品' : '创建作品'))

const pageDescription = computed(() => {
  if (routePenId.value) {
    return '调整已有作品的源码、可见性与 Fork 设置，保存草稿或重新发布。'
  }
  if (route.query.template) {
    return '从系统模板开始创作，保留预览反馈并逐步完善作品信息。'
  }
  return '创建一个可运行的 HTML、CSS、JavaScript 示例，并发布到代码广场。'
})

const countLines = (code: string) => {
  if (!code.trim()) {
    return 0
  }
  return code.split(/\r?\n/).length
}

const codeMetrics = computed(() => ({
  htmlLines: countLines(penData.htmlCode),
  cssLines: countLines(penData.cssCode),
  jsLines: countLines(penData.jsCode)
}))

// 返回
const goBack = () => {
  ElMessageBox.confirm(
    '确定要离开吗？未保存的更改将丢失。',
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  )
    .then(() => {
      router.back()
    })
    .catch(() => {})
}

// 保存草稿
const saveDraft = async () => {
  if (!validatePen()) return

  try {
    saving.value = true
    penData.status = 0 // 草稿状态
    
    const response = penData.id 
      ? await codepenApi.updatePen(penData)
      : await codepenApi.savePen(penData)
    
    if (!penData.id && response.penId) {
      penData.id = response.penId
    }
    
    ElMessage.success('草稿保存成功')
  } catch (error) {
    console.error('保存草稿失败:', error)
  } finally {
    saving.value = false
  }
}

// 发布作品
const publish = async () => {
  if (!validatePen()) return

  try {
    publishing.value = true
    penData.status = 1 // 发布状态
    
    const response = penData.id 
      ? await codepenApi.updatePen(penData)
      : await codepenApi.createPen(penData)
    
    if (response.pointsAdded) {
      ElMessage.success(`作品发布成功！奖励 ${response.pointsAdded} 积分`)
    } else {
      ElMessage.success('作品发布成功')
    }
    
    // 跳转到代码广场
    router.push('/codepen')
  } catch (error) {
    console.error('发布作品失败:', error)
    penData.status = 0 // 恢复草稿状态
  } finally {
    publishing.value = false
  }
}

// 验证作品
const validatePen = () => {
  if (!penData.title || penData.title.trim() === '') {
    ElMessage.warning('请输入作品标题')
    return false
  }

  if (!penData.htmlCode && !penData.cssCode && !penData.jsCode) {
    ElMessage.warning('至少需要编写一种代码（HTML/CSS/JS）')
    return false
  }

  if (penData.tags.length > 5) {
    ElMessage.warning('最多只能选择5个标签')
    return false
  }

  if (penData.isFree === 0 && (!penData.forkPrice || penData.forkPrice < 1 || penData.forkPrice > 1000)) {
    ElMessage.warning('Fork价格范围为1-1000积分')
    return false
  }

  return true
}

// 运行代码
const runCode = () => {
  const html = penData.htmlCode || ''
  const css = penData.cssCode || ''
  const js = penData.jsCode || ''
  const scriptCloseTag = '</scr' + 'ipt>'

  const content = `
    <!DOCTYPE html>
    <html>
      <head>
        <style>${css}</style>
      </head>
      <body>
        ${html}
        <script>${js}${scriptCloseTag}
      </body>
    </html>
  `

  if (previewFrame.value) {
    previewFrame.value.srcdoc = content
  }

  if (fullscreenPreview.value && fullscreenFrame.value) {
    fullscreenFrame.value.srcdoc = content
  }
}

// 自动运行代码（防抖）
const autoRunCode = () => {
  if (autoRunTimer.value) {
    clearTimeout(autoRunTimer.value)
  }
  autoRunTimer.value = setTimeout(() => {
    runCode()
  }, 500)
}

// 加载标签
const loadTags = async () => {
  try {
    allTags.value = await codepenApi.getHotTags()
  } catch (error) {
    console.error('加载标签失败:', error)
  }
}

// 加载作品数据（编辑模式）
const loadPenData = async () => {
  const penId = routePenId.value
  if (!penId) return

  try {
    const data = await codepenApi.getPenDetail(penId)
    Object.assign(penData, {
      id: data.id,
      title: data.title,
      description: data.description,
      htmlCode: data.htmlCode || '',
      cssCode: data.cssCode || '',
      jsCode: data.jsCode || '',
      tags: data.tags || [],
      category: data.category,
      isPublic: data.isPublic,
      isFree: data.isFree ? 1 : 0,
      forkPrice: data.forkPrice || 0,
      status: data.status
    })

    runCode()
  } catch (error) {
    console.error('加载作品失败:', error)
    ElMessage.error('加载作品失败')
  }
}

// 从模板创建
const loadTemplate = async () => {
  const templateId = route.query.template
  if (!templateId) return

  try {
    const data = await codepenApi.getTemplateDetail(templateId)
    penData.htmlCode = data.htmlCode || ''
    penData.cssCode = data.cssCode || ''
    penData.jsCode = data.jsCode || ''
    penData.title = `基于模板：${data.title}`
    
    runCode()
  } catch (error) {
    console.error('加载模板失败:', error)
  }
}

// 监听全屏预览打开
watch(fullscreenPreview, (val) => {
  if (val) {
    setTimeout(() => {
      runCode()
    }, 100)
  }
})

// 初始化
onMounted(() => {
  loadTags()
  
  // 如果是编辑模式
  if (route.params.id) {
    loadPenData()
  }
  // 如果是从模板创建
  else if (route.query.template) {
    loadTemplate()
  }
  // 新建空白作品
  else {
    runCode()
  }
})

// 清理定时器
onBeforeUnmount(() => {
  if (autoRunTimer.value) {
    clearTimeout(autoRunTimer.value)
  }
})
</script>

<style scoped lang="scss">
.codepen-editor {
  min-height: calc(100vh - 68px);

  .form-tip {
    font-size: 12px;
    color: var(--cn-color-text-tertiary);
    margin-top: var(--cn-space-1);
  }

  .fullscreen-iframe {
    width: 100%;
    height: calc(100vh - 120px);
    border: none;
  }
}

.editor-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.editor-meta-form {
  display: grid;
  grid-template-columns: minmax(280px, 0.8fr) minmax(320px, 1.2fr);
  gap: var(--cn-space-4);
  align-items: start;
}

.workbench-section {
  min-height: 0;
}

.editor-body {
  display: grid;
  grid-template-columns: minmax(360px, 0.95fr) minmax(420px, 1.05fr);
  min-height: 680px;
  overflow: hidden;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface);
}

.code-panels {
  display: grid;
  grid-template-rows: repeat(3, minmax(180px, 1fr));
  min-width: 0;
  overflow: hidden;
  border-right: 1px solid var(--cn-color-border-subtle);
  background: color-mix(in srgb, var(--cn-slate-900) 94%, var(--cn-color-bg-surface));
}

.code-panel {
  display: flex;
  min-height: 0;
  flex-direction: column;
  border-bottom: 1px solid color-mix(in srgb, var(--cn-slate-700) 72%, transparent);

  &:last-child {
    border-bottom: 0;
  }
}

.panel-header {
  display: flex;
  min-height: 44px;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
  padding: 0 var(--cn-space-4);
  border-bottom: 1px solid color-mix(in srgb, var(--cn-slate-700) 72%, transparent);
  background: color-mix(in srgb, var(--cn-slate-800) 84%, var(--cn-color-bg-surface));
}

.panel-header--preview {
  border-color: var(--cn-color-border-subtle);
  background: var(--cn-color-bg-surface-muted);
}

.panel-title {
  display: flex;
  align-items: center;
  gap: var(--cn-space-2);
  color: var(--cn-slate-100);
  font-size: 14px;
  font-weight: 650;
}

.panel-header--preview .panel-title {
  color: var(--cn-color-text-primary);
}

.code-textarea {
  width: 100%;
  flex: 1;
  min-height: 0;
  padding: var(--cn-space-4);
  border: none;
  background: color-mix(in srgb, var(--cn-slate-900) 96%, var(--cn-color-bg-surface));
  color: var(--cn-slate-100);
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.6;
  resize: none;
  tab-size: 2;

  &:focus {
    outline: none;
    box-shadow: inset 0 0 0 2px var(--cn-color-focus-ring);
  }

  &::placeholder {
    color: var(--cn-slate-500);
  }
}

.preview-panel {
  display: flex;
  min-width: 0;
  min-height: 0;
  flex-direction: column;
  background: var(--cn-color-bg-surface);
}

.preview-iframe {
  width: 100%;
  flex: 1;
  min-height: 0;
  border: none;
  background: var(--cn-color-bg-surface);
}

:deep(.el-dialog) {
  .el-dialog__body {
    padding: var(--cn-space-5);
  }
}

.settings-select {
  width: 100%;
}

.fork-price-input {
  width: 200px;
}

.fork-price-tip {
  margin-left: var(--cn-space-3);
}

@media (max-width: 1180px) {
  .editor-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .editor-body {
    grid-template-columns: 1fr;
    min-height: 980px;
  }

  .code-panels {
    border-right: 0;
    border-bottom: 1px solid var(--cn-color-border-subtle);
  }
}

@media (max-width: 720px) {
  .editor-stats,
  .editor-meta-form {
    grid-template-columns: 1fr;
  }

  .editor-body {
    min-height: 1120px;
  }
}
</style>

