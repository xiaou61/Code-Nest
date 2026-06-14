<template>
  <div class="import-dialog">
    <div class="import-header">
      <h3>从面试题库导入</h3>
      <p class="hint">选择面试题导入为闪卡，题目标题作为正面，答案作为背面</p>
    </div>

    <div class="search-section">
      <el-input 
        v-model="searchKeyword" 
        placeholder="搜索题目..."
        clearable
        @input="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <div class="question-list" v-loading="loading">
      <el-checkbox-group v-model="selectedIds">
        <div 
          v-for="question in questionList" 
          :key="question.id"
          class="question-item"
        >
          <el-checkbox :value="question.id">
            <span class="question-title">{{ question.title }}</span>
          </el-checkbox>
        </div>
      </el-checkbox-group>

      <CnEmptyState v-if="!loading && questionList.length === 0" title="暂无题目" icon="Q" size="sm" surface="transparent" />
    </div>

    <div class="import-footer">
      <span class="selected-count">已选 {{ selectedIds.length }} 题</span>
      <el-button type="primary" @click="handleImport" :loading="importing" :disabled="selectedIds.length === 0">
        导入为闪卡
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { CnEmptyState } from '@/design-system'
import { interviewApi } from '@/api/interview'
import { flashcardApi } from '@/api/flashcard'

interface QuestionItem {
  id: number
  title: string
}

const props = defineProps<{
  deckId: number
}>()

const emit = defineEmits<{
  (e: 'imported', count: number): void
}>()

const loading = ref(false)
const importing = ref(false)
const searchKeyword = ref('')
const questionList = ref<QuestionItem[]>([])
const selectedIds = ref<number[]>([])

let searchTimer: ReturnType<typeof setTimeout> | null = null

// 加载题目列表
const loadQuestions = async () => {
  loading.value = true
  try {
    // 获取题单列表，然后获取题目
    const sets = await interviewApi.getPublicQuestionSets({ page: 1, size: 5 })
    const questions: QuestionItem[] = []
    
    for (const set of (sets.content || sets || []).slice(0, 3)) {
      const setQuestions = await interviewApi.getQuestionsBySetId(set.id)
      questions.push(...(setQuestions || []).slice(0, 10))
    }
    
    questionList.value = questions
  } catch (error) {
    console.error('加载题目失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  if (searchTimer) {
    clearTimeout(searchTimer)
  }
  searchTimer = setTimeout(() => {
    // TODO: 实现搜索
  }, 300)
}

// 导入
const handleImport = async () => {
  if (selectedIds.value.length === 0) return

  importing.value = true
  try {
    const count = await flashcardApi.importFromQuestionBank({
      deckId: props.deckId,
      questionIds: selectedIds.value
    })
    ElMessage.success(`成功导入 ${count} 张闪卡`)
    selectedIds.value = []
    emit('imported', count)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '导入失败')
  } finally {
    importing.value = false
  }
}

onMounted(() => {
  loadQuestions()
})
</script>

<style lang="scss" scoped>
.import-dialog {
  min-height: 400px;
}

.import-header {
  margin-bottom: var(--cn-space-5);
  
  h3 {
    font-size: 16px;
    font-weight: 600;
    margin: 0 0 8px 0;
    color: var(--cn-color-text-primary);
  }
  
  .hint {
    margin: 0;
    font-size: 13px;
    color: var(--cn-color-text-secondary);
  }
}

.search-section {
  margin-bottom: var(--cn-space-4);
}

.question-list {
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  padding: var(--cn-space-2);
  margin-bottom: var(--cn-space-4);
  background: var(--cn-color-bg-surface);
}

.question-item {
  padding: var(--cn-space-3);
  border-radius: var(--cn-radius-control);
  transition: background-color var(--cn-motion-fast) var(--cn-ease-out);
  
  &:hover {
    background: var(--cn-color-bg-surface-muted);
  }
  
  .question-title {
    font-size: 14px;
    color: var(--cn-color-text-primary);
  }
}

.import-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  
  .selected-count {
    font-size: 13px;
    color: var(--cn-color-text-secondary);
  }
}
</style>
