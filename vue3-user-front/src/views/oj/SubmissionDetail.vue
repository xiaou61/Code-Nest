<template>
  <div class="submission-detail">
    <div class="page-header">
      <el-button text @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
      <h2>提交详情</h2>
    </div>

    <div class="detail-content" v-loading="loading">
      <!-- 状态卡片 -->
      <el-card shadow="never" class="status-card" v-if="submission.id">
        <div class="status-header" :class="'status-' + submission.status">
          <span class="status-text">{{ getStatusLabel(submission.status) }}</span>
        </div>
        <div class="status-info">
          <div class="info-item">
            <span class="info-label">题目</span>
            <router-link :to="`/oj/problem/${submission.problemId}`" class="info-link">
              {{ submission.problemTitle || `#${submission.problemId}` }}
            </router-link>
          </div>
          <div class="info-item">
            <span class="info-label">语言</span>
            <span>{{ getLanguageLabel(submission.language) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">耗时</span>
            <span>{{ submission.timeUsed != null ? submission.timeUsed + 'ms' : '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">内存</span>
            <span>{{ formatMemory(submission.memoryUsed) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">通过用例</span>
            <span>{{ submission.passCount ?? '-' }} / {{ submission.totalCount ?? '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">提交时间</span>
            <span>{{ submission.createTime || '-' }}</span>
          </div>
        </div>
      </el-card>

      <!-- 错误信息 -->
      <el-card shadow="never" class="error-card" v-if="submission.errorMessage">
        <template #header>
          <span class="card-title">错误信息</span>
        </template>
        <pre class="error-message">{{ submission.errorMessage }}</pre>
      </el-card>

      <!-- 代码展示 -->
      <el-card shadow="never" class="code-card" v-if="submission.code">
        <template #header>
          <span class="card-title">提交代码</span>
        </template>
        <pre class="code-block"><code>{{ submission.code }}</code></pre>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ojApi } from '@/api/oj'

const route = useRoute()
const loading = ref(false)
const submission = ref({})

const getStatusLabel = (status) => {
  const map = {
    pending: '等待判题', judging: '判题中', accepted: '通过',
    wrong_answer: '答案错误', time_limit_exceeded: '超时',
    memory_limit_exceeded: '超内存', runtime_error: '运行错误',
    compile_error: '编译错误', system_error: '系统错误'
  }
  return map[status] || status
}

const getLanguageLabel = (lang) => {
  const map = { java: 'Java', cpp: 'C++', c: 'C', python: 'Python3', go: 'Go', javascript: 'JavaScript' }
  return map[lang] || lang
}

const formatMemory = (kb) => {
  if (!kb) return '-'
  return kb > 1024 ? `${(kb / 1024).toFixed(1)}MB` : `${kb}KB`
}

onMounted(async () => {
  loading.value = true
  try {
    submission.value = await ojApi.getSubmissionDetail(route.params.id) || {}
  } catch {
    ElMessage.error('提交记录不存在')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.submission-detail {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
}

.status-card,
.error-card,
.code-card {
  border-radius: 12px;
  margin-bottom: 16px;
}

.status-header {
  padding: 16px 0;
  text-align: center;
  border-radius: 8px;
  margin-bottom: 16px;
}

.status-text {
  font-size: 24px;
  font-weight: 700;
}

.status-accepted .status-text { color: #10b981; }
.status-accepted { background: #ecfdf5; }
.status-wrong_answer .status-text { color: #ef4444; }
.status-wrong_answer { background: #fef2f2; }
.status-time_limit_exceeded .status-text,
.status-memory_limit_exceeded .status-text { color: #f59e0b; }
.status-time_limit_exceeded,
.status-memory_limit_exceeded { background: #fffbeb; }
.status-runtime_error .status-text,
.status-compile_error .status-text { color: #ef4444; }
.status-runtime_error,
.status-compile_error { background: #fef2f2; }
.status-system_error .status-text { color: #6b7280; }
.status-system_error { background: #f3f4f6; }
.status-pending .status-text,
.status-judging .status-text { color: #3b82f6; }
.status-pending,
.status-judging { background: #eff6ff; }

.status-info {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 12px;
  color: #9ca3af;
}

.info-link {
  color: #2563eb;
  text-decoration: none;
  font-weight: 500;
}

.info-link:hover {
  text-decoration: underline;
}

.card-title {
  font-weight: 600;
  color: #374151;
}

.error-message {
  background: #fef2f2;
  color: #991b1b;
  padding: 12px;
  border-radius: 6px;
  font-size: 13px;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
}

.code-block {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 16px;
  border-radius: 8px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  line-height: 1.6;
  overflow-x: auto;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}

@media (max-width: 600px) {
  .status-info {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
