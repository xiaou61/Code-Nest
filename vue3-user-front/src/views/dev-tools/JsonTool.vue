<template>
  <div class="json-tool">
    <!-- 头部导航 -->
    <div class="tool-header">
      <div class="header-content">
        <h1 class="tool-title">
          <el-icon><Document /></el-icon>
          JSON 格式化工具
        </h1>
        <p class="tool-description">JSON格式化、验证、压缩和转换的专业工具</p>
      </div>
    </div>

    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-button-group>
          <el-button 
            type="primary" 
            @click="formatJson"
            :disabled="!inputText.trim()"
          >
            <el-icon><Edit /></el-icon>
            格式化
          </el-button>
          <el-button 
            @click="compressJson"
            :disabled="!inputText.trim()"
          >
            <el-icon><Minus /></el-icon>
            压缩
          </el-button>
          <el-button 
            @click="validateJson"
            :disabled="!inputText.trim()"
          >
            <el-icon><CircleCheck /></el-icon>
            验证
          </el-button>
        </el-button-group>
      </div>
      <div class="toolbar-right">
        <el-button @click="clearAll">
          <el-icon><Delete /></el-icon>
          清空
        </el-button>
        <el-button @click="copyResult" :disabled="!outputText">
          <el-icon><CopyDocument /></el-icon>
          复制结果
        </el-button>
        <el-upload
          ref="uploadRef"
          :show-file-list="false"
          :before-upload="handleFileUpload"
          accept=".json,.txt"
        >
          <el-button>
            <el-icon><Upload /></el-icon>
            上传文件
          </el-button>
        </el-upload>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="content-area">
      <!-- 输入区域 -->
      <div class="input-section">
        <div class="section-header">
          <h3>输入 JSON</h3>
          <div class="section-actions">
            <el-button size="small" text @click="loadExample">
              <el-icon><Document /></el-icon>
              加载示例
            </el-button>
            <el-button size="small" text @click="pasteFromClipboard">
              <el-icon><CopyDocument /></el-icon>
              粘贴
            </el-button>
          </div>
        </div>
        <div class="editor-container">
          <textarea
            v-model="inputText"
            class="json-editor input-editor"
            placeholder="请输入或粘贴 JSON 数据..."
            @input="onInputChange"
          ></textarea>
          <div class="editor-info">
            <span class="char-count">{{ inputText.length }} 字符</span>
            <span v-if="inputError" class="error-info">
              <el-icon><Warning /></el-icon>
              {{ inputError }}
            </span>
          </div>
        </div>
      </div>

      <!-- 输出区域 -->
      <div class="output-section">
        <div class="section-header">
          <h3>格式化结果</h3>
          <div class="section-actions">
            <el-button size="small" text @click="copyResult" :disabled="!outputText">
              <el-icon><CopyDocument /></el-icon>
              复制
            </el-button>
            <el-button size="small" text @click="downloadResult" :disabled="!outputText">
              <el-icon><Download /></el-icon>
              下载
            </el-button>
          </div>
        </div>
        <div class="editor-container">
          <div
            v-if="outputText"
            class="json-editor output-editor"
            v-html="highlightedJson"
          ></div>
          <div v-else class="json-editor output-editor placeholder">
            格式化后的 JSON 将显示在这里...
          </div>
          <div class="editor-info">
            <span class="char-count">{{ outputText.length }} 字符</span>
            <span v-if="jsonStats" class="stats-info">
              <el-icon><DataAnalysis /></el-icon>
              {{ jsonStats }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- JSON 统计信息 -->
    <div v-if="jsonInfo" class="json-info">
      <el-card>
        <template #header>
          <div class="card-header">
            <el-icon><DataAnalysis /></el-icon>
            JSON 分析
          </div>
        </template>
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">类型</span>
            <span class="info-value">{{ jsonInfo.type }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">深度</span>
            <span class="info-value">{{ jsonInfo.depth }} 层</span>
          </div>
          <div class="info-item">
            <span class="info-label">键数量</span>
            <span class="info-value">{{ jsonInfo.keyCount }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">大小</span>
            <span class="info-value">{{ formatBytes(outputText.length) }}</span>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 历史记录 -->
    <div v-if="history.length > 0" class="history-section">
      <el-card>
        <template #header>
          <div class="card-header">
            <el-icon><Clock /></el-icon>
            历史记录
            <el-button size="small" text type="danger" @click="clearHistory">
              清空历史
            </el-button>
          </div>
        </template>
        <div class="history-list">
          <div
            v-for="(item, index) in history.slice(0, 5)"
            :key="index"
            class="history-item"
            @click="loadFromHistory(item)"
          >
            <div class="history-content">
              <span class="history-preview">{{ item.preview }}</span>
              <span class="history-time">{{ formatTime(item.timestamp) }}</span>
            </div>
            <el-button size="small" text @click.stop="removeFromHistory(index)">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Document, 
  Edit, 
  Minus, 
  CircleCheck, 
  Delete, 
  CopyDocument,
  Upload, 
  Warning, 
  Download, 
  DataAnalysis, 
  Clock, 
  Close
} from '@element-plus/icons-vue'

// 响应式数据
const inputText = ref('')
const outputText = ref('')
const inputError = ref('')
const jsonInfo = ref(null)
const history = ref([])
const uploadRef = ref()

// 计算属性
const highlightedJson = computed(() => {
  if (!outputText.value) return ''
  return highlightJson(outputText.value)
})

const jsonStats = computed(() => {
  if (!jsonInfo.value) return ''
  return `${jsonInfo.value.type} • ${jsonInfo.value.keyCount} 个键 • ${jsonInfo.value.depth} 层深度`
})

// 生命周期
onMounted(() => {
  loadHistory()
})

// JSON 格式化
const formatJson = () => {
  try {
    const parsed = JSON.parse(inputText.value)
    outputText.value = JSON.stringify(parsed, null, 2)
    inputError.value = ''
    analyzeJson(parsed)
    addToHistory(inputText.value, outputText.value, 'format')
    ElMessage.success('JSON 格式化成功')
  } catch (error) {
    inputError.value = `JSON 语法错误: ${error.message}`
    outputText.value = ''
    jsonInfo.value = null
    ElMessage.error('JSON 格式化失败')
  }
}

// JSON 压缩
const compressJson = () => {
  try {
    const parsed = JSON.parse(inputText.value)
    outputText.value = JSON.stringify(parsed)
    inputError.value = ''
    analyzeJson(parsed)
    addToHistory(inputText.value, outputText.value, 'compress')
    ElMessage.success('JSON 压缩成功')
  } catch (error) {
    inputError.value = `JSON 语法错误: ${error.message}`
    outputText.value = ''
    jsonInfo.value = null
    ElMessage.error('JSON 压缩失败')
  }
}

// JSON 验证
const validateJson = () => {
  try {
    const parsed = JSON.parse(inputText.value)
    inputError.value = ''
    analyzeJson(parsed)
    ElMessage.success('JSON 格式正确')
  } catch (error) {
    inputError.value = `JSON 语法错误: ${error.message}`
    jsonInfo.value = null
    ElMessage.error('JSON 格式错误')
  }
}

// 清空所有内容
const clearAll = () => {
  inputText.value = ''
  outputText.value = ''
  inputError.value = ''
  jsonInfo.value = null
}

// 复制结果
const copyResult = async () => {
  try {
    await navigator.clipboard.writeText(outputText.value)
    ElMessage.success('已复制到剪贴板')
  } catch (error) {
    ElMessage.error('复制失败')
  }
}

// 从剪贴板粘贴
const pasteFromClipboard = async () => {
  try {
    const text = await navigator.clipboard.readText()
    inputText.value = text
    ElMessage.success('已从剪贴板粘贴')
  } catch (error) {
    ElMessage.error('粘贴失败')
  }
}

// 下载结果
const downloadResult = () => {
  const blob = new Blob([outputText.value], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'formatted.json'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
  ElMessage.success('文件下载成功')
}

// 文件上传处理
const handleFileUpload = (file) => {
  const reader = new FileReader()
  reader.onload = (e) => {
    inputText.value = e.target.result
    ElMessage.success('文件上传成功')
  }
  reader.onerror = () => {
    ElMessage.error('文件读取失败')
  }
  reader.readAsText(file)
  return false // 阻止自动上传
}

// 加载示例数据
const loadExample = () => {
  const example = {
    "name": "张三",
    "age": 30,
    "city": "北京",
    "skills": ["JavaScript", "Vue.js", "Node.js"],
    "experience": {
      "years": 5,
      "companies": [
        {
          "name": "科技公司A",
          "position": "前端工程师",
          "duration": "2年"
        },
        {
          "name": "科技公司B",
          "position": "高级前端工程师",
          "duration": "3年"
        }
      ]
    },
    "contact": {
      "email": "zhangsan@example.com",
      "phone": "13800138000"
    },
    "active": true
  }
  inputText.value = JSON.stringify(example)
  ElMessage.success('示例数据已加载')
}

// 输入变化处理
const onInputChange = () => {
  inputError.value = ''
  jsonInfo.value = null
  outputText.value = ''
}

// 分析 JSON 结构
const analyzeJson = (obj) => {
  const getType = (value) => {
    if (Array.isArray(value)) return 'Array'
    if (value === null) return 'null'
    return typeof value === 'object' ? 'Object' : typeof value
  }

  const getDepth = (obj, depth = 1) => {
    if (typeof obj !== 'object' || obj === null) return depth
    if (Array.isArray(obj)) {
      return Math.max(depth, ...obj.map(item => getDepth(item, depth + 1)))
    }
    return Math.max(depth, ...Object.values(obj).map(value => getDepth(value, depth + 1)))
  }

  const countKeys = (obj) => {
    if (typeof obj !== 'object' || obj === null) return 0
    if (Array.isArray(obj)) {
      return obj.reduce((count, item) => count + countKeys(item), 0)
    }
    return Object.keys(obj).length + Object.values(obj).reduce((count, value) => count + countKeys(value), 0)
  }

  jsonInfo.value = {
    type: getType(obj),
    depth: getDepth(obj),
    keyCount: countKeys(obj)
  }
}

// JSON 语法高亮
const highlightJson = (json) => {
  return json
    .replace(/("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+-]?\d+)?)/g, (match) => {
      let cls = 'json-number'
      if (/^"/.test(match)) {
        if (/:$/.test(match)) {
          cls = 'json-key'
        } else {
          cls = 'json-string'
        }
      } else if (/true|false/.test(match)) {
        cls = 'json-boolean'
      } else if (/null/.test(match)) {
        cls = 'json-null'
      }
      return `<span class="${cls}">${match}</span>`
    })
}

// 历史记录管理
const addToHistory = (input, output, type) => {
  const item = {
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

const loadFromHistory = (item) => {
  inputText.value = item.input
  outputText.value = item.output
  try {
    const parsed = JSON.parse(item.input)
    analyzeJson(parsed)
  } catch (error) {
    // 忽略历史记录中的错误
  }
}

const removeFromHistory = (index) => {
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
      history.value = JSON.parse(saved)
    }
  } catch (error) {
    console.error('Failed to load history:', error)
  }
}

// 工具函数
const formatBytes = (bytes) => {
  if (bytes === 0) return '0 Bytes'
  const k = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const formatTime = (timestamp) => {
  return new Date(timestamp).toLocaleString('zh-CN')
}
</script>

<style scoped>
.json-tool {
  min-height: 100vh;
  background: #f8fafc;
}

/* 头部区域 */
.tool-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 30px 20px;
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
}


.tool-title {
  font-size: 2rem;
  font-weight: 700;
  margin: 16px 0 8px 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.tool-description {
  opacity: 0.9;
  font-size: 1.1rem;
  margin: 0;
}

/* 工具栏 */
.toolbar {
  background: white;
  padding: 20px;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: 1400px;
  margin: 0 auto;
  gap: 20px;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  gap: 12px;
  align-items: center;
}

/* 内容区域 */
.content-area {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  min-height: 600px;
}

.input-section,
.output-section {
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.section-header {
  padding: 20px;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-header h3 {
  margin: 0;
  font-size: 1.125rem;
  font-weight: 600;
  color: #374151;
}

.section-actions {
  display: flex;
  gap: 8px;
}

.editor-container {
  position: relative;
  height: 500px;
  display: flex;
  flex-direction: column;
}

.json-editor {
  flex: 1;
  border: none;
  outline: none;
  padding: 20px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 14px;
  line-height: 1.6;
  resize: none;
  background: transparent;
}

.input-editor {
  color: #374151;
  background: #f9fafb;
}

.output-editor {
  color: #1f2937;
  overflow: auto;
  white-space: pre;
}

.output-editor.placeholder {
  color: #9ca3af;
  font-style: italic;
  display: flex;
  align-items: center;
  justify-content: center;
}

.editor-info {
  padding: 12px 20px;
  background: #f3f4f6;
  border-top: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.875rem;
}

.char-count {
  color: #6b7280;
}

.error-info {
  color: #ef4444;
  display: flex;
  align-items: center;
  gap: 4px;
}

.stats-info {
  color: #059669;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* JSON 语法高亮 */
:deep(.json-key) {
  color: #0369a1;
  font-weight: 600;
}

:deep(.json-string) {
  color: #059669;
}

:deep(.json-number) {
  color: #dc2626;
}

:deep(.json-boolean) {
  color: #7c3aed;
  font-weight: 600;
}

:deep(.json-null) {
  color: #6b7280;
  font-style: italic;
}

/* JSON 信息卡片 */
.json-info {
  max-width: 1400px;
  margin: 20px auto;
  padding: 0 20px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 20px;
}

.info-item {
  text-align: center;
  padding: 16px;
  background: #f8fafc;
  border-radius: 8px;
}

.info-label {
  display: block;
  font-size: 0.875rem;
  color: #6b7280;
  margin-bottom: 4px;
}

.info-value {
  display: block;
  font-size: 1.25rem;
  font-weight: 600;
  color: #1f2937;
}

/* 历史记录 */
.history-section {
  max-width: 1400px;
  margin: 20px auto;
  padding: 0 20px;
}

.history-list {
  max-height: 300px;
  overflow-y: auto;
}

.history-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-bottom: 1px solid #f3f4f6;
  cursor: pointer;
  transition: background-color 0.2s;
}

.history-item:hover {
  background: #f9fafb;
}

.history-item:last-child {
  border-bottom: none;
}

.history-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.history-preview {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 0.875rem;
  color: #374151;
}

.history-time {
  font-size: 0.75rem;
  color: #9ca3af;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .content-area {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  
  .toolbar-left,
  .toolbar-right {
    justify-content: center;
  }
}

@media (max-width: 768px) {
  .tool-header {
    padding: 20px 15px;
  }
  
  .tool-title {
    font-size: 1.5rem;
  }
  
  .toolbar {
    padding: 15px;
  }
  
  .content-area {
    padding: 15px;
  }
  
  .editor-container {
    height: 400px;
  }
  
  .section-header {
    padding: 15px;
    flex-direction: column;
    gap: 10px;
    align-items: stretch;
  }
  
  .section-actions {
    justify-content: center;
  }
  
  .json-editor {
    padding: 15px;
    font-size: 13px;
  }
  
  .info-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 15px;
  }
}

@media (max-width: 480px) {
  .toolbar-left,
  .toolbar-right {
    flex-wrap: wrap;
    gap: 8px;
  }
  
  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
