<template>
  <CnPage class="codepen-templates-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="CodePen 系统模板"
      description="维护可复用的 HTML、CSS、JavaScript 模板，帮助用户快速创建作品。"
      eyebrow="CodePen Templates"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">模板 {{ tableData.length }} 个</CnStatusTag>
        <CnStatusTag type="success">含 HTML {{ htmlTemplateCount }} 个</CnStatusTag>
        <CnStatusTag type="info">含 CSS {{ cssTemplateCount }} 个</CnStatusTag>
        <CnStatusTag type="warning">含 JS {{ jsTemplateCount }} 个</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="handleAdd">新建模板</el-button>
      </template>
    </CnPageHeader>

    <CnSection title="模板列表" description="查看、编辑或删除系统预置模板，代码片段会在用户创建作品时复用。" divided>
      <CnDataTable
        :columns="tableColumns"
        :data="tableData"
        :loading="loading"
        :pagination="null"
        row-key="id"
        empty-title="暂无模板"
        empty-description="还没有系统模板，可以创建一个常用布局或交互示例。"
        empty-icon="TP"
      >
        <template #description="{ row }">
          <span class="description-text">{{ row.description || '-' }}</span>
        </template>

        <template #codeBadges="{ row }">
          <div class="code-badges">
            <CnStatusTag v-if="row.htmlCode" type="brand" size="sm">HTML</CnStatusTag>
            <CnStatusTag v-if="row.cssCode" type="success" size="sm">CSS</CnStatusTag>
            <CnStatusTag v-if="row.jsCode" type="warning" size="sm">JS</CnStatusTag>
            <CnStatusTag v-if="!row.htmlCode && !row.cssCode && !row.jsCode" type="neutral" size="sm" subtle>
              空模板
            </CnStatusTag>
          </div>
        </template>

        <template #actions="{ row }">
          <div class="table-actions">
            <el-button link type="primary" size="small" :icon="View" @click="handleView(row)">查看</el-button>
            <el-button link type="primary" size="small" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </div>
        </template>
      </CnDataTable>
    </CnSection>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="80%" :close-on-click-modal="false" fullscreen>
      <el-form :model="formData" label-width="100px">
        <el-form-item label="模板名称" required>
          <el-input v-model="formData.title" placeholder="请输入模板名称" maxlength="100" show-word-limit />
        </el-form-item>

        <el-form-item label="模板描述">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入模板描述"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="HTML代码">
          <textarea
            v-model="formData.htmlCode"
            class="code-textarea"
            placeholder="请输入HTML代码..."
            spellcheck="false"
          ></textarea>
        </el-form-item>

        <el-form-item label="CSS代码">
          <textarea
            v-model="formData.cssCode"
            class="code-textarea"
            placeholder="请输入CSS代码..."
            spellcheck="false"
          ></textarea>
        </el-form-item>

        <el-form-item label="JavaScript">
          <textarea
            v-model="formData.jsCode"
            class="code-textarea"
            placeholder="请输入JavaScript代码..."
            spellcheck="false"
          ></textarea>
        </el-form-item>

        <el-form-item label="预览">
          <div class="preview-stack">
            <div class="preview-container">
              <iframe ref="previewFrame" class="preview-iframe" sandbox="allow-scripts allow-same-origin"></iframe>
            </div>
            <el-button :icon="Refresh" @click="runPreview">刷新预览</el-button>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button @click="runPreview">预览</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="viewDialogVisible" title="查看模板" width="80%">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="模板名称">{{ viewData.title }}</el-descriptions-item>
        <el-descriptions-item label="模板描述">{{ viewData.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ viewData.createTime }}</el-descriptions-item>
      </el-descriptions>

      <el-tabs v-model="activeTab" class="template-tabs">
        <el-tab-pane v-if="viewData.htmlCode" label="HTML" name="html">
          <pre class="code-block">{{ viewData.htmlCode }}</pre>
        </el-tab-pane>
        <el-tab-pane v-if="viewData.cssCode" label="CSS" name="css">
          <pre class="code-block">{{ viewData.cssCode }}</pre>
        </el-tab-pane>
        <el-tab-pane v-if="viewData.jsCode" label="JavaScript" name="js">
          <pre class="code-block">{{ viewData.jsCode }}</pre>
        </el-tab-pane>
        <el-tab-pane label="预览" name="preview">
          <div class="preview-container">
            <iframe ref="viewPreviewFrame" class="preview-iframe" sandbox="allow-scripts allow-same-origin"></iframe>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Refresh, View } from '@element-plus/icons-vue'
import { codepenApi } from '@/api/codepen'
import { CnDataTable, CnPage, CnPageHeader, CnSection, CnStatusTag } from '@/design-system'
import type { CnBreadcrumbItem, CnTableColumn } from '@/design-system'

interface CodePenTemplate extends Record<string, unknown> {
  id: number | null
  title: string
  description?: string
  htmlCode?: string
  cssCode?: string
  jsCode?: string
  createTime?: string
}

const emptyTemplate = (): CodePenTemplate => ({
  id: null,
  title: '',
  description: '',
  htmlCode: '',
  cssCode: '',
  jsCode: ''
})

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: 'CodePen 管理' }, { label: '模板管理' }]

const tableData = ref<CodePenTemplate[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新建模板')
const submitting = ref(false)
const previewFrame = ref<HTMLIFrameElement | null>(null)
const formData = ref<CodePenTemplate>(emptyTemplate())
const viewDialogVisible = ref(false)
const viewData = ref<CodePenTemplate>(emptyTemplate())
const activeTab = ref('html')
const viewPreviewFrame = ref<HTMLIFrameElement | null>(null)

const tableColumns: CnTableColumn<CodePenTemplate>[] = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'title', label: '模板名称', minWidth: 240, showOverflowTooltip: true },
  { prop: 'description', label: '模板描述', minWidth: 280, slot: 'description', showOverflowTooltip: true },
  { label: '代码', width: 170, slot: 'codeBadges' },
  { prop: 'createTime', label: '创建时间', width: 170, showOverflowTooltip: true },
  { label: '操作', width: 180, fixed: 'right', slot: 'actions' }
]

const htmlTemplateCount = computed(() => tableData.value.filter((item) => Boolean(item.htmlCode)).length)
const cssTemplateCount = computed(() => tableData.value.filter((item) => Boolean(item.cssCode)).length)
const jsTemplateCount = computed(() => tableData.value.filter((item) => Boolean(item.jsCode)).length)

const loadData = async () => {
  try {
    loading.value = true
    const result = await codepenApi.getTemplateList()
    tableData.value = Array.isArray(result) ? result : []
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  dialogTitle.value = '新建模板'
  formData.value = emptyTemplate()
  dialogVisible.value = true
  window.setTimeout(() => runPreview(), 100)
}

const handleEdit = (row: CodePenTemplate) => {
  dialogTitle.value = '编辑模板'
  formData.value = {
    id: row.id,
    title: row.title,
    description: row.description || '',
    htmlCode: row.htmlCode || '',
    cssCode: row.cssCode || '',
    jsCode: row.jsCode || ''
  }
  dialogVisible.value = true
  window.setTimeout(() => runPreview(), 100)
}

const handleView = (row: CodePenTemplate) => {
  viewData.value = { ...row }
  viewDialogVisible.value = true
  activeTab.value = row.htmlCode ? 'html' : row.cssCode ? 'css' : 'js'
  window.setTimeout(() => runViewPreview(), 100)
}

const handleDelete = async (row: CodePenTemplate) => {
  try {
    await ElMessageBox.confirm(`确定删除模板"${row.title}"吗？`, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'error'
    })

    if (!row.id) return
    await codepenApi.deleteTemplate(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const handleSubmit = async () => {
  if (!formData.value.title) {
    ElMessage.warning('请输入模板名称')
    return
  }

  if (!formData.value.htmlCode && !formData.value.cssCode && !formData.value.jsCode) {
    ElMessage.warning('请至少输入一种代码')
    return
  }

  try {
    submitting.value = true

    if (formData.value.id) {
      await codepenApi.updateTemplate(formData.value)
      ElMessage.success('更新成功')
    } else {
      await codepenApi.createTemplate(formData.value)
      ElMessage.success('创建成功')
    }

    dialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    submitting.value = false
  }
}

const buildPreviewContent = (template: CodePenTemplate) => {
  const html = template.htmlCode || ''
  const css = template.cssCode || ''
  const js = template.jsCode || ''
  const scriptCloseTag = '</scr' + 'ipt>'

  return `
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
}

const runPreview = () => {
  if (previewFrame.value) {
    previewFrame.value.srcdoc = buildPreviewContent(formData.value)
  }
}

const runViewPreview = () => {
  if (viewPreviewFrame.value) {
    viewPreviewFrame.value.srcdoc = buildPreviewContent(viewData.value)
  }
}

watch(activeTab, (val) => {
  if (val === 'preview') {
    window.setTimeout(() => runViewPreview(), 100)
  }
})

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.codepen-templates-page {
  min-height: 100%;
}

.description-text {
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.55;
}

.code-badges,
.table-actions,
.dialog-footer,
.preview-stack {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.dialog-footer {
  justify-content: flex-end;
}

.preview-stack {
  width: 100%;
}

.code-textarea {
  width: 100%;
  min-height: 200px;
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-color-border);
  border-radius: var(--cn-radius-control);
  background: color-mix(in srgb, var(--cn-color-bg-surface-muted) 72%, var(--cn-color-text-primary) 28%);
  color: var(--cn-color-bg-surface);
  resize: vertical;
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.6;
  transition:
    border-color var(--cn-motion-fast) var(--cn-ease-out),
    box-shadow var(--cn-motion-fast) var(--cn-ease-out);
}

.code-textarea:focus {
  border-color: var(--cn-color-brand-primary);
  box-shadow: 0 0 0 3px var(--cn-color-focus-ring);
  outline: none;
}

.code-textarea::placeholder {
  color: color-mix(in srgb, var(--cn-color-bg-surface) 58%, transparent);
}

.preview-container {
  width: 100%;
  height: 400px;
  overflow: hidden;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface);
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
}

.template-tabs {
  margin-top: var(--cn-space-5);
}

.code-block {
  margin: 0;
  overflow-x: auto;
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-text-primary);
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.6;
}

@media (max-width: 680px) {
  .dialog-footer {
    justify-content: flex-start;
  }

  .preview-container {
    height: 320px;
  }
}
</style>
