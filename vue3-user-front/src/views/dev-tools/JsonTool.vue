<template>
  <CnPage class="json-tool-page" max-width="1440px" full-height>
    <CnPageHeader
      title="JSON 格式化工具"
      description="格式化、压缩、验证 JSON，并查看结构深度、键数量和本地历史记录。"
      eyebrow="DEV TOOL"
      :breadcrumbs="[{ label: '开发工具', to: '/dev-tools' }, { label: 'JSON 格式化' }]"
    >
      <template #meta>
        <CnStatusTag :type="inputError ? 'danger' : outputText ? 'success' : 'info'" size="sm">
          {{ inputError ? '语法错误' : outputText ? '已生成结果' : '待处理' }}
        </CnStatusTag>
        <CnStatusTag type="brand" size="sm" subtle>{{ inputText.length }} 字符输入</CnStatusTag>
      </template>
    </CnPageHeader>

    <CnSection title="操作栏" description="选择格式化、压缩或只校验语法。上传内容只在本地浏览器读取。" divided>
      <div class="toolbar">
        <div class="toolbar-left">
          <el-button-group>
            <el-button type="primary" :icon="Edit" :disabled="!inputText.trim()" @click="formatJson">
              格式化
            </el-button>
            <el-button :icon="Minus" :disabled="!inputText.trim()" @click="compressJson">
              压缩
            </el-button>
            <el-button :icon="CircleCheck" :disabled="!inputText.trim()" @click="validateJson">
              验证
            </el-button>
          </el-button-group>
        </div>

        <div class="toolbar-right">
          <el-button :icon="Delete" @click="clearAll">清空</el-button>
          <el-button :icon="CopyDocument" :disabled="!outputText" @click="copyResult">复制结果</el-button>
          <el-upload :show-file-list="false" :before-upload="handleFileUpload" accept=".json,.txt">
            <el-button :icon="Upload">上传文件</el-button>
          </el-upload>
        </div>
      </div>
    </CnSection>

    <div class="workbench-grid">
      <CnSection title="输入 JSON" description="粘贴或上传 JSON 文本，编辑后会自动清空旧结果。" divided>
        <template #actions>
          <el-button size="small" text :icon="Document" @click="loadExample">加载示例</el-button>
          <el-button size="small" text :icon="CopyDocument" @click="pasteFromClipboard">粘贴</el-button>
        </template>

        <div class="editor-container">
          <textarea
            v-model="inputText"
            class="json-editor input-editor"
            placeholder="请输入或粘贴 JSON 数据..."
            @input="onInputChange"
          />
          <div class="editor-info">
            <span>{{ inputText.length }} 字符</span>
            <span v-if="inputError" class="error-info">
              <el-icon><Warning /></el-icon>
              {{ inputError }}
            </span>
          </div>
        </div>
      </CnSection>

      <CnSection title="格式化结果" description="结果会进行基础语法高亮，可复制或下载为 JSON 文件。" divided>
        <template #actions>
          <el-button size="small" text :icon="CopyDocument" :disabled="!outputText" @click="copyResult">复制</el-button>
          <el-button size="small" text :icon="Download" :disabled="!outputText" @click="downloadResult">下载</el-button>
        </template>

        <div class="editor-container">
          <div v-if="outputText" class="json-editor output-editor" v-html="highlightedJson" />
          <div v-else class="json-editor output-editor placeholder">
            格式化后的 JSON 将显示在这里...
          </div>
          <div class="editor-info">
            <span>{{ outputText.length }} 字符</span>
            <span v-if="jsonStats" class="stats-info">
              <el-icon><DataAnalysis /></el-icon>
              {{ jsonStats }}
            </span>
          </div>
        </div>
      </CnSection>
    </div>

    <div v-if="jsonInfo" class="info-grid">
      <CnStatCard title="类型" :value="jsonInfo.type" description="根节点数据结构" tone="brand" />
      <CnStatCard title="深度" :value="jsonInfo.depth" unit="层" description="嵌套层级估算" tone="info" />
      <CnStatCard title="键数量" :value="jsonInfo.keyCount" description="对象键总数" tone="success" />
      <CnStatCard title="大小" :value="formatBytes(outputText.length)" description="按输出文本估算" tone="warning" />
    </div>

    <CnSection v-if="history.length > 0" title="历史记录" description="仅保存在当前浏览器 localStorage 中。" divided>
      <template #actions>
        <el-button size="small" text type="danger" @click="clearHistory">清空历史</el-button>
      </template>

      <div class="history-list">
        <button
          v-for="(item, index) in history.slice(0, 5)"
          :key="`${item.timestamp}-${index}`"
          type="button"
          class="history-item"
          @click="loadFromHistory(item)"
        >
          <span class="history-content">
            <span class="history-preview">{{ item.preview }}</span>
            <span class="history-time">{{ formatTime(item.timestamp) }}</span>
          </span>
          <el-button size="small" text :icon="Close" @click.stop="removeFromHistory(index)" />
        </button>
      </div>
    </CnSection>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadRawFile } from 'element-plus'
import {
  CircleCheck,
  Close,
  CopyDocument,
  DataAnalysis,
  Delete,
  Document,
  Download,
  Edit,
  Minus,
  Upload,
  Warning
} from '@element-plus/icons-vue'
import { CnPage, CnPageHeader, CnSection, CnStatCard, CnStatusTag } from '@/design-system'

interface JsonInfo {
  type: string
  depth: number
  keyCount: number
}

interface HistoryItem {
  input: string
  output: string
  type: string
  preview: string
  timestamp: number
}

const inputText = ref('')
const outputText = ref('')
const inputError = ref('')
const jsonInfo = ref<JsonInfo | null>(null)
const history = ref<HistoryItem[]>([])

const highlightedJson = computed(() => {
  if (!outputText.value) return ''
  return highlightJson(outputText.value)
})

const jsonStats = computed(() => {
  if (!jsonInfo.value) return ''
  return `${jsonInfo.value.type} • ${jsonInfo.value.keyCount} 个键 • ${jsonInfo.value.depth} 层深度`
})

const formatJson = () => {
  try {
    const parsed = JSON.parse(inputText.value) as unknown
    outputText.value = JSON.stringify(parsed, null, 2)
    inputError.value = ''
    analyzeJson(parsed)
    addToHistory(inputText.value, outputText.value, 'format')
    ElMessage.success('JSON 格式化成功')
  } catch (error) {
    inputError.value = `JSON 语法错误: ${error instanceof Error ? error.message : '解析失败'}`
    outputText.value = ''
    jsonInfo.value = null
    ElMessage.error('JSON 格式化失败')
  }
}

const compressJson = () => {
  try {
    const parsed = JSON.parse(inputText.value) as unknown
    outputText.value = JSON.stringify(parsed)
    inputError.value = ''
    analyzeJson(parsed)
    addToHistory(inputText.value, outputText.value, 'compress')
    ElMessage.success('JSON 压缩成功')
  } catch (error) {
    inputError.value = `JSON 语法错误: ${error instanceof Error ? error.message : '解析失败'}`
    outputText.value = ''
    jsonInfo.value = null
    ElMessage.error('JSON 压缩失败')
  }
}

const validateJson = () => {
  try {
    const parsed = JSON.parse(inputText.value) as unknown
    inputError.value = ''
    analyzeJson(parsed)
    ElMessage.success('JSON 格式正确')
  } catch (error) {
    inputError.value = `JSON 语法错误: ${error instanceof Error ? error.message : '解析失败'}`
    jsonInfo.value = null
    ElMessage.error('JSON 格式错误')
  }
}

const clearAll = () => {
  inputText.value = ''
  outputText.value = ''
  inputError.value = ''
  jsonInfo.value = null
}

const copyResult = async () => {
  try {
    await navigator.clipboard.writeText(outputText.value)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败')
  }
}

const pasteFromClipboard = async () => {
  try {
    inputText.value = await navigator.clipboard.readText()
    ElMessage.success('已从剪贴板粘贴')
  } catch {
    ElMessage.error('粘贴失败')
  }
}

const downloadResult = () => {
  const blob = new Blob([outputText.value], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = 'formatted.json'
  document.body.appendChild(anchor)
  anchor.click()
  document.body.removeChild(anchor)
  URL.revokeObjectURL(url)
  ElMessage.success('文件下载成功')
}

const handleFileUpload = (file: UploadRawFile) => {
  const reader = new FileReader()
  reader.onload = (event) => {
    inputText.value = String(event.target?.result || '')
    ElMessage.success('文件上传成功')
  }
  reader.onerror = () => {
    ElMessage.error('文件读取失败')
  }
  reader.readAsText(file)
  return false
}

const loadExample = () => {
  const example = {
    name: '张三',
    age: 30,
    city: '北京',
    skills: ['JavaScript', 'Vue.js', 'Node.js'],
    experience: {
      years: 5,
      companies: [
        { name: '科技公司A', position: '前端工程师', duration: '2年' },
        { name: '科技公司B', position: '高级前端工程师', duration: '3年' }
      ]
    },
    contact: {
      email: 'zhangsan@example.com',
      phone: '13800138000'
    },
    active: true
  }
  inputText.value = JSON.stringify(example)
  ElMessage.success('示例数据已加载')
}

const onInputChange = () => {
  inputError.value = ''
  jsonInfo.value = null
  outputText.value = ''
}

const analyzeJson = (value: unknown) => {
  const getType = (target: unknown) => {
    if (Array.isArray(target)) return 'Array'
    if (target === null) return 'null'
    return typeof target === 'object' ? 'Object' : typeof target
  }

  const getDepth = (target: unknown, depth = 1): number => {
    if (typeof target !== 'object' || target === null) return depth
    if (Array.isArray(target)) {
      return Math.max(depth, ...target.map((item) => getDepth(item, depth + 1)))
    }
    return Math.max(depth, ...Object.values(target as Record<string, unknown>).map((item) => getDepth(item, depth + 1)))
  }

  const countKeys = (target: unknown): number => {
    if (typeof target !== 'object' || target === null) return 0
    if (Array.isArray(target)) {
      return target.reduce((count, item) => count + countKeys(item), 0)
    }
    const record = target as Record<string, unknown>
    return Object.keys(record).length + Object.values(record).reduce((count, item) => count + countKeys(item), 0)
  }

  jsonInfo.value = {
    type: getType(value),
    depth: getDepth(value),
    keyCount: countKeys(value)
  }
}

const highlightJson = (json: string) => {
  return json.replace(
    /("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+-]?\d+)?)/g,
    (match) => {
      let className = 'json-number'
      if (/^"/.test(match)) {
        className = /:$/.test(match) ? 'json-key' : 'json-string'
      } else if (/true|false/.test(match)) {
        className = 'json-boolean'
      } else if (/null/.test(match)) {
        className = 'json-null'
      }
      return `<span class="${className}">${escapeHtml(match)}</span>`
    }
  )
}

const escapeHtml = (text: string) => {
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

const addToHistory = (input: string, output: string, type: string) => {
  const item: HistoryItem = {
    input,
    output,
    type,
    preview: input.substring(0, 50) + (input.length > 50 ? '...' : ''),
    timestamp: Date.now()
  }
  history.value.unshift(item)
  if (history.value.length > 10) {
    history.value = history.value.slice(0, 10)
  }
  saveHistory()
}

const loadFromHistory = (item: HistoryItem) => {
  inputText.value = item.input
  outputText.value = item.output
  try {
    analyzeJson(JSON.parse(item.input) as unknown)
  } catch {
    jsonInfo.value = null
  }
}

const removeFromHistory = (index: number) => {
  history.value.splice(index, 1)
  saveHistory()
}

const clearHistory = () => {
  history.value = []
  saveHistory()
  ElMessage.success('历史记录已清空')
}

const saveHistory = () => {
  localStorage.setItem('json-tool-history', JSON.stringify(history.value))
}

const loadHistory = () => {
  try {
    const saved = localStorage.getItem('json-tool-history')
    if (saved) {
      history.value = JSON.parse(saved) as HistoryItem[]
    }
  } catch (error) {
    console.error('Failed to load history:', error)
  }
}

const formatBytes = (bytes: number) => {
  if (bytes === 0) return '0 Bytes'
  const unit = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB']
  const index = Math.floor(Math.log(bytes) / Math.log(unit))
  return `${parseFloat((bytes / Math.pow(unit, index)).toFixed(2))} ${sizes[index]}`
}

const formatTime = (timestamp: number) => {
  return new Date(timestamp).toLocaleString('zh-CN')
}

onMounted(() => {
  loadHistory()
})
</script>

<style scoped>
.json-tool-page {
  min-height: calc(100vh - 68px);
}

.toolbar,
.toolbar-left,
.toolbar-right {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--cn-space-3);
}

.toolbar {
  justify-content: space-between;
}

.workbench-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cn-space-5);
}

.editor-container {
  display: flex;
  flex-direction: column;
  height: 520px;
  overflow: hidden;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
}

.json-editor {
  flex: 1;
  min-height: 0;
  padding: var(--cn-space-4);
  border: 0;
  outline: 0;
  resize: none;
  background: transparent;
  color: var(--cn-color-text-primary);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  line-height: 1.7;
}

.output-editor {
  overflow: auto;
  white-space: pre;
}

.output-editor.placeholder {
  display: grid;
  place-items: center;
  color: var(--cn-color-text-tertiary);
  font-style: italic;
  text-align: center;
}

.editor-info {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: var(--cn-space-2);
  padding: var(--cn-space-3) var(--cn-space-4);
  border-top: 1px solid var(--cn-color-border-subtle);
  background: var(--cn-color-bg-surface);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.error-info,
.stats-info {
  display: inline-flex;
  align-items: center;
  gap: var(--cn-space-1);
}

.error-info {
  color: var(--cn-color-danger);
}

.stats-info {
  color: var(--cn-color-success);
}

:deep(.json-key) {
  color: var(--cn-color-info);
  font-weight: 700;
}

:deep(.json-string) {
  color: var(--cn-color-success);
}

:deep(.json-number) {
  color: var(--cn-color-danger);
}

:deep(.json-boolean) {
  color: var(--cn-color-warning);
  font-weight: 700;
}

:deep(.json-null) {
  color: var(--cn-color-text-tertiary);
  font-style: italic;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.history-list {
  display: grid;
  gap: var(--cn-space-2);
}

.history-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
  width: 100%;
  padding: var(--cn-space-3);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
  color: inherit;
  cursor: pointer;
  text-align: left;
  transition:
    border-color var(--cn-motion-fast) var(--cn-ease-out),
    background-color var(--cn-motion-fast) var(--cn-ease-out);
}

.history-item:hover {
  border-color: var(--cn-color-brand-primary);
  background: var(--cn-color-brand-soft);
}

.history-content {
  display: grid;
  gap: var(--cn-space-1);
  min-width: 0;
}

.history-preview {
  overflow: hidden;
  color: var(--cn-color-text-primary);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-time {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

@media (max-width: 1080px) {
  .workbench-grid,
  .info-grid {
    grid-template-columns: 1fr;
  }

  .editor-container {
    height: 420px;
  }
}

@media (max-width: 640px) {
  .toolbar,
  .toolbar-left,
  .toolbar-right,
  .toolbar :deep(.el-button-group),
  .toolbar :deep(.el-button) {
    width: 100%;
  }

  .toolbar :deep(.el-button-group) {
    display: grid;
  }
}
</style>
