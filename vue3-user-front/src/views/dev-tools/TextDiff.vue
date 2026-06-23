<template>
  <CnPage class="text-diff-page" max-width="1440px" full-height>
    <CnPageHeader
      title="文本差异比对工具"
      description="输入左右两段文本，按字符、单词或行级模式对比差异，并导出 HTML 结果。"
      eyebrow="DEV TOOL"
      :breadcrumbs="[{ label: '开发工具', to: '/dev-tools' }, { label: '文本差异比对' }]"
    >
      <template #meta>
        <CnStatusTag :type="hasDiff ? 'success' : 'info'" size="sm">
          {{ hasDiff ? '已生成差异' : '待比对' }}
        </CnStatusTag>
        <CnStatusTag type="brand" size="sm" subtle>{{ diffModeLabel }}</CnStatusTag>
      </template>
    </CnPageHeader>

    <CnSection title="比对设置" description="选择比对粒度和忽略规则，然后开始生成差异视图。" divided>
      <div class="toolbar">
        <div class="toolbar-left">
          <el-button-group>
            <el-button
              type="primary"
              :icon="Search"
              :disabled="!leftText.trim() || !rightText.trim()"
              @click="performDiff"
            >
              开始比对
            </el-button>
            <el-button :icon="Delete" @click="clearAll">清空</el-button>
          </el-button-group>

          <el-select v-model="diffMode" placeholder="比对模式" class="mode-select">
            <el-option label="字符级" value="chars" />
            <el-option label="单词级" value="words" />
            <el-option label="行级" value="lines" />
          </el-select>
        </div>

        <div class="toolbar-right">
          <el-checkbox v-model="ignoreWhitespace">忽略空白</el-checkbox>
          <el-checkbox v-model="ignoreCase">忽略大小写</el-checkbox>
          <el-button :icon="Download" :disabled="!hasDiff" @click="exportDiff">导出结果</el-button>
        </div>
      </div>
    </CnSection>

    <div class="input-panels">
      <CnSection title="原文本（左侧）" description="上传、粘贴或直接输入待比较的原始文本。" divided>
        <template #actions>
          <el-upload
            :show-file-list="false"
            :before-upload="(file: UploadRawFile) => handleFileUpload(file, 'left')"
            accept=".txt,.md,.js,.css,.html,.json,.xml"
          >
            <el-button size="small" text :icon="Upload">上传文件</el-button>
          </el-upload>
          <el-button size="small" text :icon="CopyDocument" @click="pasteFromClipboard('left')">粘贴</el-button>
        </template>

        <div class="editor-container">
          <textarea
            ref="leftEditor"
            v-model="leftText"
            class="text-editor"
            placeholder="请输入或粘贴原始文本..."
            @input="onTextChange"
            @scroll="syncScroll($event, 'left')"
          />
          <div class="editor-info">
            <span>{{ getLineCount(leftText) }} 行</span>
            <span>{{ leftText.length }} 字符</span>
          </div>
        </div>
      </CnSection>

      <CnSection title="新文本（右侧）" description="输入修改后的文本，滚动会与左侧编辑器同步。" divided>
        <template #actions>
          <el-upload
            :show-file-list="false"
            :before-upload="(file: UploadRawFile) => handleFileUpload(file, 'right')"
            accept=".txt,.md,.js,.css,.html,.json,.xml"
          >
            <el-button size="small" text :icon="Upload">上传文件</el-button>
          </el-upload>
          <el-button size="small" text :icon="CopyDocument" @click="pasteFromClipboard('right')">粘贴</el-button>
        </template>

        <div class="editor-container">
          <textarea
            ref="rightEditor"
            v-model="rightText"
            class="text-editor"
            placeholder="请输入或粘贴新文本..."
            @input="onTextChange"
            @scroll="syncScroll($event, 'right')"
          />
          <div class="editor-info">
            <span>{{ getLineCount(rightText) }} 行</span>
            <span>{{ rightText.length }} 字符</span>
          </div>
        </div>
      </CnSection>
    </div>

    <CnSection v-if="hasDiff" title="比对结果" description="可在并排视图和统一视图之间切换。" divided>
      <template #actions>
        <div class="result-stats">
          <CnStatusTag type="success" size="sm">+{{ diffStats.added }}</CnStatusTag>
          <CnStatusTag type="danger" size="sm">-{{ diffStats.removed }}</CnStatusTag>
          <CnStatusTag type="warning" size="sm">~{{ diffStats.modified }}</CnStatusTag>
        </div>
        <el-button-group>
          <el-button
            size="small"
            :type="viewMode === 'side-by-side' ? 'primary' : ''"
            @click="viewMode = 'side-by-side'"
          >
            并排视图
          </el-button>
          <el-button
            size="small"
            :type="viewMode === 'unified' ? 'primary' : ''"
            @click="viewMode = 'unified'"
          >
            统一视图
          </el-button>
        </el-button-group>
      </template>

      <div v-if="viewMode === 'side-by-side'" class="side-by-side-view">
        <div class="diff-panel">
          <div class="diff-content" v-html="formattedLeftDiff" />
        </div>
        <div class="diff-panel">
          <div class="diff-content" v-html="formattedRightDiff" />
        </div>
      </div>

      <div v-else class="unified-view">
        <div class="diff-content" v-html="formattedUnifiedDiff" />
      </div>

      <div v-if="diffPositions.length > 0" class="diff-navigation">
        <el-button size="small" :icon="ArrowUp" :disabled="currentDiffIndex <= 0" @click="previousDiff">
          上一个差异
        </el-button>
        <span class="nav-info">{{ currentDiffIndex + 1 }} / {{ diffPositions.length }}</span>
        <el-button
          size="small"
          :icon="ArrowDown"
          :disabled="currentDiffIndex >= diffPositions.length - 1"
          @click="nextDiff"
        >
          下一个差异
        </el-button>
      </div>
    </CnSection>

    <CnEmptyState
      v-else
      title="等待比对"
      description="请在左右两个文本框中输入内容，然后点击开始比对。"
      icon="DIFF"
      surface="panel"
    />
  </CnPage>
</template>

<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadRawFile } from 'element-plus'
import {
  ArrowDown,
  ArrowUp,
  CopyDocument,
  Delete,
  Download,
  Search,
  Upload
} from '@element-plus/icons-vue'
import { CnEmptyState, CnPage, CnPageHeader, CnSection, CnStatusTag } from '@/design-system'

type DiffMode = 'chars' | 'words' | 'lines'
type ViewMode = 'side-by-side' | 'unified'
type DiffSide = 'left' | 'right'

interface DiffPart {
  value: string
  equal?: boolean
  added?: boolean
  removed?: boolean
  modified?: boolean
}

interface DiffPosition extends DiffPart {
  index: number
}

const leftText = ref('')
const rightText = ref('')
const diffMode = ref<DiffMode>('lines')
const ignoreWhitespace = ref(false)
const ignoreCase = ref(false)
const viewMode = ref<ViewMode>('side-by-side')
const diffResult = ref<DiffPart[] | null>(null)
const currentDiffIndex = ref(0)
const leftEditor = ref<HTMLTextAreaElement | null>(null)
const rightEditor = ref<HTMLTextAreaElement | null>(null)

const hasDiff = computed(() => diffResult.value !== null)

const diffModeLabel = computed(() => {
  const labels: Record<DiffMode, string> = {
    chars: '字符级',
    words: '单词级',
    lines: '行级'
  }
  return labels[diffMode.value]
})

const diffStats = computed(() => {
  if (!diffResult.value) return { added: 0, removed: 0, modified: 0 }

  let added = 0
  let removed = 0
  let modified = 0
  diffResult.value.forEach((part) => {
    if (part.added) added += 1
    else if (part.removed) removed += 1
    else if (part.modified) modified += 1
  })

  return { added, removed, modified }
})

const diffPositions = computed<DiffPosition[]>(() => {
  if (!diffResult.value) return []
  return diffResult.value
    .map((part, index) => ({ ...part, index }))
    .filter((part) => part.added || part.removed || part.modified)
})

const formattedLeftDiff = computed(() => {
  if (!diffResult.value) return ''
  return formatDiffForSideBySide(diffResult.value, 'left')
})

const formattedRightDiff = computed(() => {
  if (!diffResult.value) return ''
  return formatDiffForSideBySide(diffResult.value, 'right')
})

const formattedUnifiedDiff = computed(() => {
  if (!diffResult.value) return ''
  return formatDiffForUnified(diffResult.value)
})

const performDiff = () => {
  if (!leftText.value.trim() || !rightText.value.trim()) {
    ElMessage.warning('请输入要比对的文本')
    return
  }

  try {
    const leftContent = processText(leftText.value)
    const rightContent = processText(rightText.value)
    diffResult.value = calculateDiff(leftContent, rightContent)
    currentDiffIndex.value = 0
    ElMessage.success('文本比对完成')
  } catch (error) {
    console.error(error)
    ElMessage.error('文本比对失败')
  }
}

const processText = (text: string) => {
  let processed = text
  if (ignoreWhitespace.value) {
    processed = processed.replace(/\s+/g, ' ').trim()
  }
  if (ignoreCase.value) {
    processed = processed.toLowerCase()
  }
  return processed
}

const calculateDiff = (left: string, right: string) => {
  const leftItems = splitText(left)
  const rightItems = splitText(right)

  if (diffMode.value === 'chars') {
    return calculateCharacterDiff(leftItems, rightItems)
  }
  return calculateLineDiff(leftItems, rightItems)
}

const calculateCharacterDiff = (leftChars: string[], rightChars: string[]) => {
  const lcs = computeLCS(leftChars, rightChars)
  const diff: DiffPart[] = []

  let i = 0
  let j = 0
  let k = 0

  while (i < leftChars.length || j < rightChars.length) {
    if (k < lcs.length && i < leftChars.length && leftChars[i] === lcs[k]) {
      diff.push({ value: leftChars[i], equal: true })
      i += 1
      j += 1
      k += 1
    } else if (k < lcs.length && j < rightChars.length && rightChars[j] === lcs[k]) {
      diff.push({ value: rightChars[j], added: true })
      j += 1
    } else if (i < leftChars.length) {
      diff.push({ value: leftChars[i], removed: true })
      i += 1
    } else if (j < rightChars.length) {
      diff.push({ value: rightChars[j], added: true })
      j += 1
    }
  }

  return mergeAdjacentDiffs(diff)
}

const mergeAdjacentDiffs = (diff: DiffPart[]) => {
  if (diff.length === 0) return diff

  const merged: DiffPart[] = []
  let current = { ...diff[0] }

  for (let index = 1; index < diff.length; index += 1) {
    const item = diff[index]
    if ((current.equal && item.equal) || (current.added && item.added) || (current.removed && item.removed)) {
      current.value += item.value
    } else {
      merged.push(current)
      current = { ...item }
    }
  }

  merged.push(current)
  return merged
}

const calculateLineDiff = (leftItems: string[], rightItems: string[]) => {
  const diff: DiffPart[] = []
  const maxLength = Math.max(leftItems.length, rightItems.length)

  for (let index = 0; index < maxLength; index += 1) {
    const leftItem = leftItems[index] || ''
    const rightItem = rightItems[index] || ''

    if (leftItem === rightItem) {
      if (leftItem || rightItem) {
        diff.push({ value: leftItem, equal: true })
      }
    } else if (!leftItem) {
      diff.push({ value: rightItem, added: true })
    } else if (!rightItem) {
      diff.push({ value: leftItem, removed: true })
    } else {
      diff.push({ value: leftItem, removed: true })
      diff.push({ value: rightItem, added: true })
    }
  }

  return diff
}

const computeLCS = (arr1: string[], arr2: string[]) => {
  const rowCount = arr1.length
  const columnCount = arr2.length
  const dp = Array.from({ length: rowCount + 1 }, () => Array(columnCount + 1).fill(0) as number[])

  for (let i = 1; i <= rowCount; i += 1) {
    for (let j = 1; j <= columnCount; j += 1) {
      if (arr1[i - 1] === arr2[j - 1]) {
        dp[i][j] = dp[i - 1][j - 1] + 1
      } else {
        dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1])
      }
    }
  }

  const lcs: string[] = []
  let i = rowCount
  let j = columnCount

  while (i > 0 && j > 0) {
    if (arr1[i - 1] === arr2[j - 1]) {
      lcs.unshift(arr1[i - 1])
      i -= 1
      j -= 1
    } else if (dp[i - 1][j] > dp[i][j - 1]) {
      i -= 1
    } else {
      j -= 1
    }
  }

  return lcs
}

const splitText = (text: string) => {
  switch (diffMode.value) {
    case 'chars':
      return text.split('')
    case 'words':
      return text.split(/\s+/)
    case 'lines':
    default:
      return text.split('\n')
  }
}

const formatDiffForSideBySide = (diff: DiffPart[], side: DiffSide) => {
  if (diffMode.value === 'chars') {
    const content = diff
      .map((part, index) => {
        let className = ''

        if (part.added && side === 'right') {
          className = 'diff-added'
        } else if (part.removed && side === 'left') {
          className = 'diff-removed'
        } else if (part.equal) {
          className = 'diff-equal'
        } else {
          return ''
        }

        return `<span class="${className}" data-index="${index}">${escapeHtml(part.value)}</span>`
      })
      .join('')

    return `<div class="diff-line">${content}</div>`
  }

  return diff
    .map((part, index) => {
      let className = ''

      if (part.added && side === 'right') {
        className = 'diff-added'
      } else if (part.removed && side === 'left') {
        className = 'diff-removed'
      } else if (part.equal) {
        className = 'diff-equal'
      } else {
        return ''
      }

      return `<div class="diff-line ${className}" data-index="${index}">${escapeHtml(part.value)}</div>`
    })
    .join('')
}

const formatDiffForUnified = (diff: DiffPart[]) => {
  if (diffMode.value === 'chars') {
    const content = diff
      .map((part, index) => {
        const className = part.added ? 'diff-added' : part.removed ? 'diff-removed' : 'diff-equal'
        return `<span class="${className}" data-index="${index}">${escapeHtml(part.value)}</span>`
      })
      .join('')

    return `<div class="diff-line">${content}</div>`
  }

  return diff
    .map((part, index) => {
      let className = 'diff-equal'
      let prefix = ' '

      if (part.added) {
        className = 'diff-added'
        prefix = '+'
      } else if (part.removed) {
        className = 'diff-removed'
        prefix = '-'
      }

      return `<div class="diff-line ${className}" data-index="${index}"><span class="diff-prefix">${prefix}</span>${escapeHtml(part.value)}</div>`
    })
    .join('')
}

const escapeHtml = (text: string) => {
  const div = document.createElement('div')
  div.textContent = text
  return div.innerHTML
}

const clearAll = () => {
  leftText.value = ''
  rightText.value = ''
  diffResult.value = null
  currentDiffIndex.value = 0
}

const pasteFromClipboard = async (side: DiffSide) => {
  try {
    const text = await navigator.clipboard.readText()
    if (side === 'left') {
      leftText.value = text
    } else {
      rightText.value = text
    }
    ElMessage.success('已从剪贴板粘贴')
  } catch {
    ElMessage.error('粘贴失败')
  }
}

const handleFileUpload = (file: UploadRawFile, side: DiffSide) => {
  const reader = new FileReader()
  reader.onload = (event) => {
    if (side === 'left') {
      leftText.value = String(event.target?.result || '')
    } else {
      rightText.value = String(event.target?.result || '')
    }
    ElMessage.success('文件上传成功')
  }
  reader.onerror = () => {
    ElMessage.error('文件读取失败')
  }
  reader.readAsText(file)
  return false
}

const exportDiff = () => {
  if (!diffResult.value) return

  const content = generateExportContent()
  const blob = new Blob([content], { type: 'text/html' })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = 'text-diff-result.html'
  document.body.appendChild(anchor)
  anchor.click()
  document.body.removeChild(anchor)
  URL.revokeObjectURL(url)
  ElMessage.success('结果导出成功')
}

const generateExportContent = () => {
  const stats = diffStats.value
  const timestamp = new Date().toLocaleString('zh-CN')

  return `<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>文本比对结果</title>
  <style>
    body { font-family: monospace; margin: 20px; }
    .header { margin-bottom: 20px; }
    .stats { margin: 10px 0; }
    .diff-added { background: honeydew; color: seagreen; }
    .diff-removed { background: mistyrose; color: firebrick; }
    .diff-equal { background: whitesmoke; }
    .diff-line { padding: 2px 5px; margin: 1px 0; }
    .diff-prefix { margin-right: 5px; font-weight: bold; }
  </style>
</head>
<body>
  <div class="header">
    <h1>文本比对结果</h1>
    <p>生成时间: ${timestamp}</p>
    <div class="stats">
      <span>新增: ${stats.added} | 删除: ${stats.removed} | 修改: ${stats.modified}</span>
    </div>
  </div>
  <div class="content">
    ${formattedUnifiedDiff.value}
  </div>
</body>
</html>`
}

const onTextChange = () => {
  diffResult.value = null
  currentDiffIndex.value = 0
}

const syncScroll = (event: Event, side: DiffSide) => {
  if (!leftEditor.value || !rightEditor.value) return

  const source = event.target as HTMLTextAreaElement
  const target = side === 'left' ? rightEditor.value : leftEditor.value
  const maxSourceScroll = source.scrollHeight - source.clientHeight
  const maxTargetScroll = target.scrollHeight - target.clientHeight
  const scrollPercentage = maxSourceScroll > 0 ? source.scrollTop / maxSourceScroll : 0
  target.scrollTop = scrollPercentage * maxTargetScroll
}

const previousDiff = () => {
  if (currentDiffIndex.value > 0) {
    currentDiffIndex.value -= 1
    scrollToDiff(currentDiffIndex.value)
  }
}

const nextDiff = () => {
  if (currentDiffIndex.value < diffPositions.value.length - 1) {
    currentDiffIndex.value += 1
    scrollToDiff(currentDiffIndex.value)
  }
}

const scrollToDiff = (index: number) => {
  nextTick(() => {
    const target = diffPositions.value[index]
    if (!target) return
    const element = document.querySelector(`[data-index="${target.index}"]`)
    element?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  })
}

const getLineCount = (text: string) => {
  return text.split('\n').length
}
</script>

<style scoped>
.text-diff-page {
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

.mode-select {
  width: 132px;
}

.input-panels {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cn-space-5);
}

.editor-container {
  display: flex;
  flex-direction: column;
  height: 420px;
  overflow: hidden;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
}

.text-editor {
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

.editor-info {
  display: flex;
  justify-content: space-between;
  gap: var(--cn-space-2);
  padding: var(--cn-space-3) var(--cn-space-4);
  border-top: 1px solid var(--cn-color-border-subtle);
  background: var(--cn-color-bg-surface);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.result-stats {
  display: flex;
  align-items: center;
  gap: var(--cn-space-2);
}

.side-by-side-view {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  overflow: hidden;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
}

.diff-panel {
  max-height: 600px;
  overflow: auto;
  background: var(--cn-color-bg-surface-muted);
}

.diff-panel + .diff-panel {
  border-left: 1px solid var(--cn-color-border-subtle);
}

.unified-view {
  max-height: 600px;
  overflow: auto;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
}

.diff-content {
  min-height: 180px;
  padding: var(--cn-space-3);
  color: var(--cn-color-text-primary);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  line-height: 1.7;
}

:deep(.diff-line) {
  margin: 0;
  padding: 4px var(--cn-space-2);
  border-left: 3px solid transparent;
  white-space: pre-wrap;
  word-break: break-word;
}

:deep(.diff-added) {
  display: inline;
  margin: 0 1px;
  padding: 2px 4px;
  border-radius: 3px;
  border-left-color: var(--cn-color-success);
  background: var(--cn-color-success-soft);
  color: var(--cn-color-success);
}

:deep(.diff-removed) {
  display: inline;
  margin: 0 1px;
  padding: 2px 4px;
  border-radius: 3px;
  border-left-color: var(--cn-color-danger);
  background: var(--cn-color-danger-soft);
  color: var(--cn-color-danger);
  text-decoration: line-through;
}

:deep(.diff-equal) {
  display: inline;
  color: var(--cn-color-text-secondary);
}

:deep(.diff-prefix) {
  margin-right: var(--cn-space-2);
  font-weight: 800;
}

.diff-navigation {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: var(--cn-space-3);
  padding-top: var(--cn-space-4);
}

.nav-info {
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  font-weight: 700;
}

@media (max-width: 1080px) {
  .input-panels,
  .side-by-side-view {
    grid-template-columns: 1fr;
  }

  .diff-panel + .diff-panel {
    border-top: 1px solid var(--cn-color-border-subtle);
    border-left: 0;
  }
}

@media (max-width: 640px) {
  .toolbar,
  .toolbar-left,
  .toolbar-right,
  .toolbar :deep(.el-button-group),
  .toolbar :deep(.el-button),
  .mode-select {
    width: 100%;
  }

  .diff-navigation {
    flex-direction: column;
  }
}
</style>
