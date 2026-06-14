<template>
  <CnPage class="card-editor-page" max-width="1180px" full-height>
    <CnPageHeader
      title="管理闪卡"
      description="为当前卡组添加单张卡片、批量导入内容，或从面试题库转化为闪卡。"
      eyebrow="CARD EDITOR"
      :breadcrumbs="[
        { label: '闪卡记忆', to: '/flashcard' },
        { label: '卡组详情', to: `/flashcard/deck/${deckId}` },
        { label: '管理闪卡' }
      ]"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm" subtle>卡组 #{{ deckId }}</CnStatusTag>
        <CnStatusTag type="info" size="sm">{{ activeModeLabel }}</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="ArrowLeft" @click="goBack">返回卡组</el-button>
      </template>
    </CnPageHeader>

    <CnSection title="添加方式" description="按当前材料来源选择录入方式，卡片会直接写入当前卡组。" divided>
      <div class="editor-tabs">
        <el-radio-group v-model="activeTab">
          <el-radio-button value="single">单张添加</el-radio-button>
          <el-radio-button value="batch">批量添加</el-radio-button>
          <el-radio-button value="import">从题库导入</el-radio-button>
        </el-radio-group>
      </div>
    </CnSection>

    <div class="editor-grid">
      <CnSection
        v-if="activeTab === 'single'"
        title="单张添加"
        description="适合手动整理一条问题和答案，可选择纯文本、Markdown 或代码格式。"
        divided
      >
        <el-form ref="singleFormRef" :model="singleForm" :rules="singleRules" label-position="top">
          <div class="two-column-form">
            <el-form-item label="正面内容" prop="frontContent">
              <el-input
                v-model="singleForm.frontContent"
                type="textarea"
                placeholder="输入卡片正面内容（问题）"
                :rows="8"
              />
            </el-form-item>

            <el-form-item label="背面内容" prop="backContent">
              <el-input
                v-model="singleForm.backContent"
                type="textarea"
                placeholder="输入卡片背面内容（答案）"
                :rows="8"
              />
            </el-form-item>
          </div>

          <div class="two-column-form compact">
            <el-form-item label="内容类型">
              <el-select v-model="singleForm.contentType" class="full-width-control">
                <el-option :value="1" label="纯文本" />
                <el-option :value="2" label="Markdown" />
                <el-option :value="3" label="代码" />
              </el-select>
            </el-form-item>

            <el-form-item label="标签">
              <el-input v-model="singleForm.tags" placeholder="多个标签用逗号分隔" />
            </el-form-item>
          </div>

          <div class="form-actions">
            <el-button type="primary" :loading="submitting" @click="handleSingleSubmit">
              添加闪卡
            </el-button>
          </div>
        </el-form>
      </CnSection>

      <CnSection
        v-if="activeTab === 'batch'"
        title="批量添加"
        description="每行一张卡片，使用 ||| 分隔正面和背面内容。"
        divided
      >
        <div class="batch-hint">
          <CnStatusTag type="info" size="sm" subtle>格式示例</CnStatusTag>
          <code>什么是闭包？|||闭包是指有权访问另一个函数作用域中的变量的函数。</code>
        </div>

        <el-form :model="batchForm" label-position="top">
          <el-form-item label="批量内容">
            <el-input
              v-model="batchForm.content"
              type="textarea"
              placeholder="每行一张卡片，使用 ||| 分隔正面和背面"
              :rows="12"
            />
          </el-form-item>

          <div class="two-column-form compact">
            <el-form-item label="内容类型">
              <el-select v-model="batchForm.contentType" class="full-width-control">
                <el-option :value="1" label="纯文本" />
                <el-option :value="2" label="Markdown" />
                <el-option :value="3" label="代码" />
              </el-select>
            </el-form-item>

            <el-form-item label="标签">
              <el-input v-model="batchForm.tags" placeholder="应用到所有卡片" />
            </el-form-item>
          </div>

          <div class="form-actions">
            <el-button
              type="primary"
              :loading="submitting"
              :disabled="parsedCards.length === 0"
              @click="handleBatchSubmit"
            >
              批量添加 {{ parsedCards.length }} 张
            </el-button>
          </div>
        </el-form>
      </CnSection>

      <CnSection
        v-if="activeTab === 'import'"
        title="从题库导入"
        description="选择面试题导入为闪卡，题目标题作为正面，答案作为背面。"
        divided
      >
        <ImportDialog :deck-id="deckId" @imported="handleImported" />
      </CnSection>

      <CnSection title="录入提示" description="保持卡片短、清晰、可复习，会比一次塞入大段材料更有效。" divided>
        <div class="tips-panel">
          <div class="tip-item">
            <CnStatusTag type="brand" size="sm">1</CnStatusTag>
            <div>
              <strong>一张卡只问一个点</strong>
              <span>把复杂主题拆成多张小卡，复习时更容易判断掌握程度。</span>
            </div>
          </div>
          <div class="tip-item">
            <CnStatusTag type="success" size="sm">2</CnStatusTag>
            <div>
              <strong>答案保留关键路径</strong>
              <span>背面内容可以包含步骤、示例或代码片段，但避免长篇复制。</span>
            </div>
          </div>
          <div class="tip-item">
            <CnStatusTag type="warning" size="sm">3</CnStatusTag>
            <div>
              <strong>用标签做二次筛选</strong>
              <span>按语言、岗位、模块或难度标记，后续卡组维护会更轻。</span>
            </div>
          </div>
        </div>
      </CnSection>
    </div>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { CnPage, CnPageHeader, CnSection, CnStatusTag } from '@/design-system'
import { flashcardApi } from '@/api/flashcard'
import ImportDialog from './components/ImportDialog.vue'

type EditorTab = 'single' | 'batch' | 'import'

interface SingleCardForm {
  frontContent: string
  backContent: string
  contentType: number
  tags: string
}

interface BatchCardForm {
  content: string
  contentType: number
  tags: string
}

interface ParsedCard {
  frontContent: string
  backContent: string
}

const router = useRouter()
const route = useRoute()

const deckId = computed(() => Number(route.params.deckId))
const activeTab = ref<EditorTab>('single')
const submitting = ref(false)
const singleFormRef = ref<FormInstance>()

const singleForm = reactive<SingleCardForm>({
  frontContent: '',
  backContent: '',
  contentType: 1,
  tags: ''
})

const batchForm = reactive<BatchCardForm>({
  content: '',
  contentType: 1,
  tags: ''
})

const singleRules: FormRules<SingleCardForm> = {
  frontContent: [{ required: true, message: '请输入正面内容', trigger: 'blur' }],
  backContent: [{ required: true, message: '请输入背面内容', trigger: 'blur' }]
}

const activeModeLabel = computed(() => {
  const labels: Record<EditorTab, string> = {
    single: '单张添加',
    batch: '批量添加',
    import: '题库导入'
  }
  return labels[activeTab.value]
})

const parsedCards = computed<ParsedCard[]>(() => {
  if (!batchForm.content) return []
  return batchForm.content
    .split('\n')
    .map((line) => line.trim())
    .filter((line) => line && line.includes('|||'))
    .map((line) => {
      const [front, ...backParts] = line.split('|||')
      return {
        frontContent: front.trim(),
        backContent: backParts.join('|||').trim()
      }
    })
    .filter((card) => card.frontContent && card.backContent)
})

const handleSingleSubmit = async () => {
  const valid = await singleFormRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await flashcardApi.createCard({
      deckId: deckId.value,
      ...singleForm
    })
    ElMessage.success('闪卡添加成功')
    Object.assign(singleForm, {
      frontContent: '',
      backContent: '',
      contentType: 1,
      tags: ''
    })
  } catch (error) {
    const message = error instanceof Error ? error.message : '添加失败'
    ElMessage.error(message)
  } finally {
    submitting.value = false
  }
}

const handleBatchSubmit = async () => {
  if (parsedCards.value.length === 0) {
    ElMessage.warning('没有可添加的卡片')
    return
  }

  submitting.value = true
  try {
    const cards = parsedCards.value.map((card) => ({
      ...card,
      contentType: batchForm.contentType,
      tags: batchForm.tags
    }))

    const count = await flashcardApi.batchCreateCards({
      deckId: deckId.value,
      cards
    })
    ElMessage.success(`成功添加 ${count} 张闪卡`)
    batchForm.content = ''
  } catch (error) {
    const message = error instanceof Error ? error.message : '添加失败'
    ElMessage.error(message)
  } finally {
    submitting.value = false
  }
}

const handleImported = (count: number) => {
  ElMessage.success(`成功导入 ${count} 张闪卡`)
}

const goBack = () => {
  router.push(`/flashcard/deck/${deckId.value}`)
}
</script>

<style scoped>
.card-editor-page {
  min-height: calc(100vh - 68px);
}

.editor-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-3);
}

.editor-tabs :deep(.el-radio-group) {
  flex-wrap: wrap;
}

.editor-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: var(--cn-space-5);
  align-items: start;
}

.two-column-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.two-column-form.compact {
  gap: var(--cn-space-3);
}

.full-width-control {
  width: 100%;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--cn-space-2);
  margin-top: var(--cn-space-2);
}

.batch-hint {
  display: grid;
  gap: var(--cn-space-2);
  margin-bottom: var(--cn-space-4);
  padding: var(--cn-space-3);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
}

.batch-hint code {
  color: var(--cn-color-text-secondary);
  font-family: var(--cn-font-mono);
  font-size: 12px;
  line-height: 1.7;
  overflow-wrap: anywhere;
}

.tips-panel {
  display: grid;
  gap: var(--cn-space-4);
}

.tip-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: var(--cn-space-3);
  align-items: flex-start;
}

.tip-item div {
  display: grid;
  gap: var(--cn-space-1);
  min-width: 0;
}

.tip-item strong {
  color: var(--cn-color-text-primary);
  font-size: 14px;
  line-height: 1.4;
}

.tip-item span {
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.65;
}

@media (max-width: 980px) {
  .editor-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .two-column-form {
    grid-template-columns: 1fr;
  }

  .form-actions,
  .form-actions :deep(.el-button) {
    width: 100%;
  }
}
</style>
