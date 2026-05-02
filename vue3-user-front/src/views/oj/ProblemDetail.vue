<template>
  <div class="problem-detail" v-loading="loading">
    <!-- 顶部栏 -->
    <div class="top-bar">
      <div class="top-left">
        <el-button text @click="handleBack">
          <el-icon><ArrowLeft /></el-icon>
          {{ backText }}
        </el-button>
        <h3 class="problem-name" v-if="problem.title">
          {{ problem.id }}. {{ problem.title }}
        </h3>
        <el-tag
          v-if="problem.difficulty"
          :type="getDifficultyTag(problem.difficulty)"
          size="small"
          effect="dark"
        >
          {{ getDifficultyLabel(problem.difficulty) }}
        </el-tag>
      </div>
      <div class="top-right">
        <el-tag
          v-for="tag in (problem.tags || [])"
          :key="tag.id"
          size="small"
          class="tag-chip"
        >
          {{ tag.name }}
        </el-tag>
      </div>
    </div>

    <!-- 主体分栏 -->
    <div class="split-panels">
      <!-- 左面板: 题目描述 / 题解 -->
      <div class="left-panel">
        <el-tabs v-model="activeTab" class="left-tabs">
          <el-tab-pane label="题目描述" name="description" />
          <el-tab-pane label="题解" name="solution" />
          <el-tab-pane name="discussion">
            <template #label>
              <span>讨论</span>
              <span class="comment-count-badge" v-if="commentTotal > 0">{{ commentTotal }}</span>
            </template>
          </el-tab-pane>
        </el-tabs>
        <el-scrollbar height="100%">
          <!-- 题目描述 -->
          <div class="problem-content" v-show="activeTab === 'description'">
            <div class="section">
              <h4>题目描述</h4>
              <div class="markdown-body" v-html="renderedDescription"></div>
            </div>

            <div class="section" v-if="problem.inputDescription">
              <h4>输入说明</h4>
              <p>{{ problem.inputDescription }}</p>
            </div>

            <div class="section" v-if="problem.outputDescription">
              <h4>输出说明</h4>
              <p>{{ problem.outputDescription }}</p>
            </div>

            <div class="section" v-if="problem.sampleInput || problem.sampleOutput">
              <h4>示例</h4>
              <div class="sample-block" v-if="problem.sampleInput">
                <div class="sample-label">输入</div>
                <pre class="sample-code">{{ problem.sampleInput }}</pre>
              </div>
              <div class="sample-block" v-if="problem.sampleOutput">
                <div class="sample-label">输出</div>
                <pre class="sample-code">{{ problem.sampleOutput }}</pre>
              </div>
            </div>

            <div class="section constraints">
              <h4>限制</h4>
              <p>时间限制: {{ problem.timeLimit || 1000 }}ms &nbsp;|&nbsp; 内存限制: {{ problem.memoryLimit || 256 }}MB</p>
            </div>
          </div>

          <!-- 讨论区 -->
          <div class="problem-content discussion-section" v-show="activeTab === 'discussion'">
            <!-- 发表评论 -->
            <div class="comment-input-box">
              <textarea
                class="comment-textarea"
                v-model="newCommentContent"
                placeholder="写下你的想法或解题思路..."
                maxlength="1000"
                rows="3"
              ></textarea>
              <div class="comment-input-footer">
                <span class="char-count">{{ newCommentContent.length }}/1000</span>
                <el-button type="primary" size="small" :loading="commentSubmitting" @click="handleCreateComment" :disabled="!newCommentContent.trim()">
                  发表评论
                </el-button>
              </div>
            </div>

            <!-- 排序栏 -->
            <div class="comment-sort-bar" v-if="comments.length > 0 || commentTotal > 0">
              <span class="comment-total">{{ commentTotal }} 条评论</span>
              <div class="sort-tabs">
                <span class="sort-tab" :class="{ active: commentSort === 'time' }" @click="changeCommentSort('time')">最新</span>
                <span class="sort-tab" :class="{ active: commentSort === 'hot' }" @click="changeCommentSort('hot')">最热</span>
              </div>
            </div>

            <!-- 加载中 -->
            <div v-if="commentsLoading" style="text-align: center; padding: 40px 0;">
              <el-icon class="is-loading" :size="24"><Loading /></el-icon>
              <p style="color: #9ca3af; margin-top: 8px;">加载中...</p>
            </div>

            <!-- 空状态 -->
            <div v-else-if="comments.length === 0" class="comment-empty">
              <p>暂无评论，来发表第一条评论吧 🎉</p>
            </div>

            <!-- 评论列表 -->
            <div v-else class="comment-list">
              <div v-for="comment in comments" :key="comment.id" class="comment-item">
                <!-- 一级评论 -->
                <div class="comment-main">
                  <div class="comment-avatar">{{ comment.authorName?.charAt(0) || '?' }}</div>
                  <div class="comment-body">
                    <div class="comment-meta">
                      <span class="comment-author">{{ comment.authorName }}</span>
                      <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
                    </div>
                    <div class="comment-text">{{ comment.content }}</div>
                    <div class="comment-actions">
                      <span class="action-btn" :class="{ liked: comment.isLiked }" @click="handleToggleLike(comment)">
                        {{ comment.isLiked ? '👍' : '👍' }} {{ comment.likeCount || 0 }}
                      </span>
                      <span class="action-btn" @click="openReply(comment)">
                        💬 回复
                      </span>
                    </div>

                    <!-- 回复输入框 -->
                    <div class="reply-input-box" v-if="replyTarget && replyTarget.id === comment.id && !replyTarget._isChild">
                      <textarea
                        class="reply-textarea"
                        v-model="replyContent"
                        :placeholder="'回复 ' + comment.authorName"
                        maxlength="500"
                        rows="2"
                      ></textarea>
                      <div class="reply-input-footer">
                        <el-button size="small" text @click="closeReply">取消</el-button>
                        <el-button type="primary" size="small" :loading="replySubmitting" @click="handleReplyComment(comment)" :disabled="!replyContent.trim()">
                          回复
                        </el-button>
                      </div>
                    </div>

                    <!-- 二级回复 -->
                    <div class="reply-list" v-if="comment.replies && comment.replies.length > 0">
                      <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
                        <div class="comment-avatar reply-avatar">{{ reply.authorName?.charAt(0) || '?' }}</div>
                        <div class="comment-body">
                          <div class="comment-meta">
                            <span class="comment-author">{{ reply.authorName }}</span>
                            <span class="reply-to" v-if="reply.replyToUserName">
                              回复 <span class="reply-to-name">{{ reply.replyToUserName }}</span>
                            </span>
                            <span class="comment-time">{{ formatTime(reply.createTime) }}</span>
                          </div>
                          <div class="comment-text">{{ reply.content }}</div>
                          <div class="comment-actions">
                            <span class="action-btn" :class="{ liked: reply.isLiked }" @click="handleToggleLike(reply)">
                              👍 {{ reply.likeCount || 0 }}
                            </span>
                            <span class="action-btn" @click="openChildReply(comment, reply)">
                              💬 回复
                            </span>
                          </div>

                          <!-- 子回复输入框 -->
                          <div class="reply-input-box" v-if="replyTarget && replyTarget.id === reply.id && replyTarget._isChild">
                            <textarea
                              class="reply-textarea"
                              v-model="replyContent"
                              :placeholder="'回复 ' + reply.authorName"
                              maxlength="500"
                              rows="2"
                            ></textarea>
                            <div class="reply-input-footer">
                              <el-button size="small" text @click="closeReply">取消</el-button>
                              <el-button type="primary" size="small" :loading="replySubmitting" @click="handleReplyChild(comment, reply)" :disabled="!replyContent.trim()">
                                回复
                              </el-button>
                            </div>
                          </div>
                        </div>
                      </div>

                      <!-- 查看更多回复 -->
                      <div v-if="comment.hasMoreReplies" class="load-more-replies" @click="loadMoreReplies(comment)">
                        查看全部 {{ comment.replyCount }} 条回复 ▸
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 加载更多 -->
              <div class="load-more-comments" v-if="hasMoreComments">
                <el-button text :loading="commentsLoadingMore" @click="loadMoreComments">
                  加载更多评论
                </el-button>
              </div>
            </div>
          </div>

          <!-- 题解 -->
          <div class="problem-content" v-show="activeTab === 'solution'">
            <div v-if="solutionsLoading" style="text-align: center; padding: 40px 0;">
              <el-icon class="is-loading" :size="24"><Loading /></el-icon>
              <p style="color: #9ca3af; margin-top: 8px;">加载中...</p>
            </div>
            <div v-else-if="solutions.length === 0" style="text-align: center; padding: 40px 0; color: #9ca3af;">
              暂无题解
            </div>
            <div v-else>
              <div v-for="sol in solutions" :key="sol.id" class="solution-item">
                <div class="solution-header">
                  <span class="solution-title">{{ sol.title || '标准答案' }}</span>
                  <el-tag size="small" type="info">{{ sol.language }}</el-tag>
                </div>
                <div v-if="sol.description" class="markdown-body solution-desc" v-html="md.render(sol.description)"></div>
                <div class="solution-code">
                  <pre><code>{{ sol.code }}</code></pre>
                </div>
              </div>
            </div>
          </div>
        </el-scrollbar>
      </div>

      <!-- 右面板: 代码编辑器 + 结果 -->
      <div class="right-panel">
        <!-- 编辑器工具栏 -->
        <div class="editor-toolbar">
          <el-select v-model="selectedLanguage" style="width: 140px" size="small">
            <el-option
              v-for="lang in languages"
              :key="lang.value"
              :label="lang.label"
              :value="lang.value"
            />
          </el-select>
          <div class="toolbar-right">
            <el-button
              size="small"
              @click="toggleTestPanel"
              :type="testPanelOpen ? 'info' : ''"
            >
              自定义测试
            </el-button>
            <el-button
              size="small"
              :loading="selfTesting"
              @click="handleSelfTest"
              type="success"
            >
              自测
            </el-button>
            <el-button
              type="primary"
              size="small"
              :loading="submitting"
              @click="handleSubmit"
            >
              <el-icon><CaretRight /></el-icon>
              提交
            </el-button>
          </div>
        </div>

        <!-- Monaco 编辑器 -->
        <div class="editor-container" ref="editorContainer"></div>

        <!-- 自测面板 -->
        <div class="test-panel" v-if="testPanelOpen">
          <div class="test-panel-header">
            <span class="test-panel-title">自测运行</span>
            <div class="test-panel-actions">
              <el-button type="primary" size="small" :loading="running" @click="handleRunTest">
                运行
              </el-button>
              <el-button size="small" text @click="testPanelOpen = false" class="close-btn">✕</el-button>
            </div>
          </div>
          <div class="test-panel-body">
            <div class="io-col">
              <div class="io-label">输入</div>
              <textarea
                class="io-textarea"
                v-model="customInput"
                placeholder="输入测试数据..."
                spellcheck="false"
              ></textarea>
            </div>
            <div class="io-col">
              <div class="io-label">
                输出
                <span class="io-metrics" v-if="runResult && runResult.status === 'success'">
                  {{ runResult.timeUsed }}ms · {{ formatMemory(runResult.memoryUsed) }}
                </span>
              </div>
              <pre class="io-output" :class="{ 'io-error': runResult && runResult.status !== 'success' }">{{ runOutputText }}</pre>
            </div>
          </div>
        </div>

        <!-- 自测结果区 -->
        <div class="result-panel self-test-panel" v-if="selfTestResult">
          <div class="result-header">
            <span class="result-status" v-if="selfTestResult.compileError" style="color: #ef4444;">编译错误</span>
            <span class="result-status" v-else
              :style="{ color: selfTestResult.passCount === selfTestResult.totalCount && selfTestResult.totalCount > 0 ? '#10b981' : '#ef4444' }">
              <span v-if="selfTestResult.passCount === selfTestResult.totalCount && selfTestResult.totalCount > 0" class="status-icon">✓</span>
              <span v-else class="status-icon">✗</span>
              自测结果：通过 {{ selfTestResult.passCount }}/{{ selfTestResult.totalCount }}
              <span v-if="selfTestResult.totalCount > 0" class="pass-percent">
                ({{ Math.round(selfTestResult.passCount / selfTestResult.totalCount * 100) }}%)
              </span>
            </span>
            <el-button size="small" text class="close-btn" @click="selfTestResult = null">✕</el-button>
          </div>
          <div class="result-error" v-if="selfTestResult.compileError">
            <pre>{{ selfTestResult.compileError }}</pre>
          </div>
          <div v-else class="self-test-cases">
            <div v-for="(tc, idx) in selfTestResult.results" :key="idx"
              class="self-test-case" :class="tc.passed ? 'case-passed' : 'case-failed'">
              <div class="case-header">
                <span class="case-badge" :class="tc.passed ? 'badge-pass' : 'badge-fail'">
                  {{ tc.passed ? '✓' : '✗' }}
                </span>
                <span>用例 {{ idx + 1 }}</span>
                <span class="case-metrics" v-if="tc.timeUsed != null">
                  {{ tc.timeUsed }}ms · {{ formatMemory(tc.memoryUsed) }}
                </span>
              </div>
              <div class="case-detail">
                <div class="case-field">
                  <span class="field-label">输入</span>
                  <pre class="field-value">{{ tc.input }}</pre>
                </div>
                <div class="case-field">
                  <span class="field-label">期望输出</span>
                  <pre class="field-value">{{ tc.expectedOutput }}</pre>
                </div>
                <div class="case-field">
                  <span class="field-label">实际输出</span>
                  <pre class="field-value" :class="{ 'field-error': !tc.passed }">{{ tc.actualOutput || '(无输出)' }}</pre>
                </div>
                <div class="case-field" v-if="tc.errorMessage">
                  <span class="field-label">错误</span>
                  <pre class="field-value field-error">{{ tc.errorMessage }}</pre>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 提交结果区 -->
        <div class="result-panel" v-if="submissionResult">
          <div class="result-header" :class="'status-' + submissionResult.status">
            <span class="result-status">
              <span v-if="submissionResult.status === 'accepted'" class="status-icon">✓</span>
              <span v-else-if="['wrong_answer','runtime_error','compile_error'].includes(submissionResult.status)" class="status-icon">✗</span>
              {{ getStatusLabel(submissionResult.status) }}
            </span>
          </div>
          <div class="result-metrics" v-if="submissionResult.status !== 'compile_error'">
            <span class="metric" v-if="submissionResult.timeUsed != null">⏱ {{ submissionResult.timeUsed }}ms</span>
            <span class="metric" v-if="submissionResult.memoryUsed != null">💾 {{ formatMemory(submissionResult.memoryUsed) }}</span>
          </div>
          <div class="points-reward" v-if="submissionResult.status === 'accepted' && submissionResult.pointsEarned > 0">
            +{{ submissionResult.pointsEarned }} 积分
          </div>
          <div class="pass-rate" v-if="submissionResult.passCount != null && submissionResult.totalCount">
            <div class="pass-rate-text">
              通过 {{ submissionResult.passCount }}/{{ submissionResult.totalCount }} 个测试用例
              <span class="pass-percent">({{ Math.round(submissionResult.passCount / submissionResult.totalCount * 100) }}%)</span>
            </div>
            <div class="pass-bar">
              <div class="pass-bar-fill"
                :class="submissionResult.passCount === submissionResult.totalCount ? 'pass-bar-full' : 'pass-bar-partial'"
                :style="{ width: (submissionResult.passCount / submissionResult.totalCount * 100) + '%' }"></div>
            </div>
          </div>
          <div class="result-error" v-if="submissionResult.errorMessage">
            <pre>{{ submissionResult.errorMessage }}</pre>
          </div>
          <div class="result-actions">
            <el-button size="small" text @click="$router.push(`/oj/submission/${submissionResult.id}`)">
              查看详情
            </el-button>
          </div>
        </div>

        <!-- 等待判题 -->
        <div class="result-panel judging" v-if="polling">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>正在判题中...</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, CaretRight, Loading } from '@element-plus/icons-vue'
import { ojApi } from '@/api/oj'
import loader from '@monaco-editor/loader'
import { registerCompletionProviders, disposeCompletionProviders } from '@/utils/monacoCompletions'
import { renderMarkdown } from '@/utils/markdown'

const route = useRoute()
const router = useRouter()
const md = { render: renderMarkdown }
const contestId = computed(() => {
  const raw = route.query.contestId
  if (raw == null || raw === '') {
    return null
  }
  const value = Array.isArray(raw) ? raw[0] : raw
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null
})
const backText = computed(() => (contestId.value ? '返回赛事详情' : '返回题目列表'))

// ============ 状态 ============
const loading = ref(false)
const problem = ref({})
const selectedLanguage = ref('java')
const submitting = ref(false)
const polling = ref(false)
const submissionResult = ref(null)
const editorContainer = ref(null)
let editorInstance = null
let monacoInstance = null
let pollTimer = null

// 自测
const testPanelOpen = ref(false)
const customInput = ref('')
const runResult = ref(null)
const running = ref(false)
const selfTesting = ref(false)
const selfTestResult = ref(null)

// 题解
const activeTab = ref('description')
const solutions = ref([])
const solutionsLoading = ref(false)
const solutionsLoaded = ref(false)

// 评论
const comments = ref([])
const commentTotal = ref(0)
const commentSort = ref('time')
const commentPage = ref(1)
const commentsLoading = ref(false)
const commentsLoadingMore = ref(false)
const commentsLoaded = ref(false)
const newCommentContent = ref('')
const commentSubmitting = ref(false)
const replyTarget = ref(null)
const replyContent = ref('')
const replySubmitting = ref(false)

const hasMoreComments = computed(() => {
  return comments.value.length < commentTotal.value
})

const languages = [
  { value: 'java', label: 'Java', monacoLang: 'java' },
  { value: 'cpp', label: 'C++', monacoLang: 'cpp' },
  { value: 'c', label: 'C', monacoLang: 'c' },
  { value: 'python', label: 'Python3', monacoLang: 'python' },
  { value: 'go', label: 'Go', monacoLang: 'go' },
  { value: 'javascript', label: 'JavaScript', monacoLang: 'javascript' }
]

const defaultCode = {
  java: `import java.util.Scanner;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        // 在此编写代码\n    }\n}`,
  cpp: `#include <iostream>\nusing namespace std;\n\nint main() {\n    // 在此编写代码\n    return 0;\n}`,
  c: `#include <stdio.h>\n\nint main() {\n    // 在此编写代码\n    return 0;\n}`,
  python: `# 在此编写代码\n`,
  go: `package main\n\nimport "fmt"\n\nfunc main() {\n    // 在此编写代码\n    fmt.Println()\n}`,
  javascript: `// 在此编写代码\nconst readline = require('readline');\nconst rl = readline.createInterface({ input: process.stdin });\nrl.on('line', (line) => {\n    \n});\n`
}

const renderedDescription = computed(() => {
  return problem.value.description ? md.render(problem.value.description) : ''
})

const runOutputText = computed(() => {
  if (!runResult.value) return '点击「运行」查看结果'
  const r = runResult.value
  if (r.status === 'compile_error') return r.stderr || '编译错误'
  if (r.status === 'runtime_error') return r.stderr || '运行时错误'
  if (r.status === 'time_limit_exceeded') return '运行超时'
  if (r.status === 'memory_limit_exceeded') return '超出内存限制'
  if (r.status === 'error') return r.stderr || '系统错误'
  return r.stdout || '(无输出)'
})

const handleBack = () => {
  if (contestId.value) {
    router.push(`/oj/contests/${contestId.value}`)
    return
  }
  router.push('/oj')
}

// ============ Monaco 编辑器 ============

const initEditor = async () => {
  monacoInstance = await loader.init()
  registerCompletionProviders(monacoInstance)

  editorInstance = monacoInstance.editor.create(editorContainer.value, {
    value: defaultCode[selectedLanguage.value] || '',
    language: getMonacoLang(selectedLanguage.value),
    theme: 'vs-dark',
    fontSize: 14,
    fontFamily: "'JetBrains Mono', 'Fira Code', 'Cascadia Code', Consolas, 'Courier New', monospace",
    fontLigatures: true,
    lineNumbers: 'on',
    minimap: { enabled: false },
    automaticLayout: true,
    scrollBeyondLastLine: false,
    tabSize: 4,
    wordWrap: 'on',
    cursorBlinking: 'smooth',
    cursorSmoothCaretAnimation: 'on',
    smoothScrolling: true,
    renderLineHighlight: 'all',
    bracketPairColorization: { enabled: true },
    guides: { bracketPairs: true, indentation: true },
    padding: { top: 12, bottom: 12 },
    suggest: {
      showSnippets: true,
      snippetsPreventQuickSuggestions: false,
      preview: true,
    },
    quickSuggestions: { other: true, comments: false, strings: false },
    parameterHints: { enabled: true },
    formatOnPaste: true,
    matchBrackets: 'always',
    renderWhitespace: 'selection',
    lineHeight: 22,
  })
}

const getMonacoLang = (lang) => {
  const found = languages.find(l => l.value === lang)
  return found ? found.monacoLang : 'plaintext'
}

watch(selectedLanguage, (newLang) => {
  if (editorInstance && monacoInstance) {
    monacoInstance.editor.setModelLanguage(editorInstance.getModel(), getMonacoLang(newLang))
    const currentCode = editorInstance.getValue()
    const isDefault = Object.values(defaultCode).some(c => c.trim() === currentCode.trim())
    if (!currentCode.trim() || isDefault) {
      editorInstance.setValue(defaultCode[newLang] || '')
    }
  }
})

// ============ 自测 ============

const toggleTestPanel = () => {
  testPanelOpen.value = !testPanelOpen.value
  if (testPanelOpen.value && !customInput.value && problem.value.sampleInput) {
    customInput.value = problem.value.sampleInput
  }
}

const handleRunTest = async () => {
  if (!editorInstance) return
  const code = editorInstance.getValue()
  if (!code.trim()) {
    ElMessage.warning('请先编写代码')
    return
  }

  running.value = true
  runResult.value = null
  try {
    runResult.value = await ojApi.runCode({
      language: selectedLanguage.value,
      code,
      stdin: customInput.value || ''
    })
  } catch (error) {
    // error handled by interceptor
  } finally {
    running.value = false
  }
}

const handleSelfTest = async () => {
  if (!editorInstance) return
  const code = editorInstance.getValue()
  if (!code.trim()) {
    ElMessage.warning('请先编写代码')
    return
  }

  selfTesting.value = true
  selfTestResult.value = null
  submissionResult.value = null
  try {
    selfTestResult.value = await ojApi.selfTest({
      problemId: problem.value.id,
      language: selectedLanguage.value,
      code
    })
  } catch (error) {
    // error handled by interceptor
  } finally {
    selfTesting.value = false
  }
}

// ============ 提交 ============

const handleSubmit = async () => {
  if (!editorInstance) return
  const code = editorInstance.getValue()
  if (!code.trim()) {
    ElMessage.warning('请先编写代码')
    return
  }

  submitting.value = true
  submissionResult.value = null
  try {
    const payload = {
      problemId: problem.value.id,
      language: selectedLanguage.value,
      code
    }
    if (contestId.value) {
      payload.contestId = contestId.value
    }
    const submissionId = await ojApi.submitCode(payload)
    ElMessage.success('提交成功，正在判题...')
    startPolling(submissionId)
  } catch (error) {
    // error handled by interceptor
  } finally {
    submitting.value = false
  }
}

// ============ 轮询判题结果 ============

const startPolling = (submissionId) => {
  polling.value = true
  let count = 0
  pollTimer = setInterval(async () => {
    count++
    try {
      const result = await ojApi.getSubmissionDetail(submissionId)
      if (result.status !== 'pending' && result.status !== 'judging') {
        stopPolling()
        submissionResult.value = result
      }
    } catch {
      stopPolling()
    }
    if (count > 60) {
      stopPolling()
      ElMessage.warning('判题超时，请稍后查看结果')
    }
  }, 2000)
}

const stopPolling = () => {
  polling.value = false
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

// ============ 题解 ============

watch(activeTab, async (tab) => {
  if (tab === 'solution' && !solutionsLoaded.value) {
    solutionsLoading.value = true
    try {
      solutions.value = await ojApi.getSolutions(problem.value.id) || []
    } catch {
      solutions.value = []
    } finally {
      solutionsLoading.value = false
      solutionsLoaded.value = true
    }
  }
  if (tab === 'discussion' && !commentsLoaded.value) {
    await fetchComments()
  }
})

// ============ 评论 ============

const fetchComments = async (append = false) => {
  if (!append) {
    commentsLoading.value = true
    commentPage.value = 1
  } else {
    commentsLoadingMore.value = true
  }
  try {
    const res = await ojApi.getComments(problem.value.id, {
      pageNum: commentPage.value,
      pageSize: 10,
      sort: commentSort.value
    })
    if (append) {
      comments.value.push(...(res.records || []))
    } else {
      comments.value = res.records || []
    }
    commentTotal.value = res.total || 0
    commentsLoaded.value = true
  } catch {
    if (!append) comments.value = []
  } finally {
    commentsLoading.value = false
    commentsLoadingMore.value = false
  }
}

const changeCommentSort = (sort) => {
  if (commentSort.value === sort) return
  commentSort.value = sort
  fetchComments()
}

const loadMoreComments = () => {
  commentPage.value++
  fetchComments(true)
}

const handleCreateComment = async () => {
  if (!newCommentContent.value.trim()) return
  commentSubmitting.value = true
  try {
    await ojApi.createComment(problem.value.id, { content: newCommentContent.value })
    newCommentContent.value = ''
    ElMessage.success('评论发表成功')
    await fetchComments()
  } catch {
    // handled by interceptor
  } finally {
    commentSubmitting.value = false
  }
}

const openReply = (comment) => {
  replyTarget.value = { ...comment, _isChild: false }
  replyContent.value = ''
}

const openChildReply = (parentComment, reply) => {
  replyTarget.value = { ...reply, _isChild: true, _parentId: parentComment.id }
  replyContent.value = ''
}

const closeReply = () => {
  replyTarget.value = null
  replyContent.value = ''
}

const handleReplyComment = async (comment) => {
  if (!replyContent.value.trim()) return
  replySubmitting.value = true
  try {
    await ojApi.replyComment(comment.id, {
      content: replyContent.value,
      replyToUserId: comment.authorId
    })
    closeReply()
    ElMessage.success('回复成功')
    await fetchComments()
  } catch {
    // handled by interceptor
  } finally {
    replySubmitting.value = false
  }
}

const handleReplyChild = async (parentComment, reply) => {
  if (!replyContent.value.trim()) return
  replySubmitting.value = true
  try {
    await ojApi.replyComment(reply.id, {
      content: replyContent.value,
      replyToUserId: reply.authorId
    })
    closeReply()
    ElMessage.success('回复成功')
    await fetchComments()
  } catch {
    // handled by interceptor
  } finally {
    replySubmitting.value = false
  }
}

const handleToggleLike = async (comment) => {
  try {
    if (comment.isLiked) {
      await ojApi.unlikeComment(comment.id)
      comment.isLiked = false
      comment.likeCount = Math.max(0, (comment.likeCount || 1) - 1)
    } else {
      await ojApi.likeComment(comment.id)
      comment.isLiked = true
      comment.likeCount = (comment.likeCount || 0) + 1
    }
  } catch {
    // handled by interceptor
  }
}

const loadMoreReplies = async (comment) => {
  try {
    const res = await ojApi.getCommentReplies(comment.id, { pageNum: 1, pageSize: 50 })
    comment.replies = res.records || []
    comment.hasMoreReplies = false
  } catch {
    // handled by interceptor
  }
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now - date
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + ' 分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + ' 小时前'
  if (diff < 2592000000) return Math.floor(diff / 86400000) + ' 天前'
  return timeStr.slice(0, 10)
}

// ============ 工具方法 ============

const getDifficultyTag = (d) => ({ easy: 'success', medium: 'warning', hard: 'danger' }[d] || 'info')
const getDifficultyLabel = (d) => ({ easy: '简单', medium: '中等', hard: '困难' }[d] || d)

const getStatusLabel = (status) => {
  const map = {
    pending: '等待判题', judging: '判题中', accepted: '通过',
    wrong_answer: '答案错误', time_limit_exceeded: '超时',
    memory_limit_exceeded: '超内存', runtime_error: '运行错误',
    compile_error: '编译错误', system_error: '系统错误'
  }
  return map[status] || status
}

const formatMemory = (kb) => {
  if (!kb) return '-'
  return kb > 1024 ? `${(kb / 1024).toFixed(1)}MB` : `${kb}KB`
}

// ============ 生命周期 ============

onMounted(async () => {
  loading.value = true
  try {
    const id = route.params.id
    problem.value = await ojApi.getProblemDetail(id) || {}
  } catch {
    ElMessage.error('题目不存在')
    handleBack()
  } finally {
    loading.value = false
  }
  await nextTick()
  initEditor()
})

onBeforeUnmount(() => {
  stopPolling()
  disposeCompletionProviders()
  if (editorInstance) {
    editorInstance.dispose()
    editorInstance = null
  }
})
</script>

<style scoped>
.problem-detail {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.top-bar {
  height: 50px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  flex-shrink: 0;
}

.top-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.problem-name {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.top-right {
  display: flex;
  gap: 6px;
}

.tag-chip {
  margin-right: 4px;
}

.split-panels {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.left-panel {
  width: 45%;
  background: #fff;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
}

.left-tabs {
  padding: 0 16px;
  flex-shrink: 0;
}

.left-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}

/* 题解样式 */
.solution-item {
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid #f3f4f6;
}

.solution-item:last-child {
  border-bottom: none;
}

.solution-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.solution-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.solution-desc {
  margin-bottom: 12px;
}

.solution-code {
  background: #1e1e1e;
  border-radius: 8px;
  overflow: auto;
}

.solution-code pre {
  margin: 0;
  padding: 16px;
}

.solution-code code {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  color: #d4d4d4;
  white-space: pre;
}

.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.problem-content {
  padding: 24px;
}

.section {
  margin-bottom: 24px;
}

.section h4 {
  font-size: 15px;
  font-weight: 600;
  color: #374151;
  margin: 0 0 10px;
  padding-bottom: 6px;
  border-bottom: 2px solid #e5e7eb;
}

.section p {
  font-size: 14px;
  color: #4b5563;
  line-height: 1.7;
  margin: 0;
  white-space: pre-wrap;
}

.markdown-body {
  font-size: 14px;
  color: #374151;
  line-height: 1.8;
}

.markdown-body :deep(pre) {
  background: #f3f4f6;
  padding: 12px;
  border-radius: 6px;
  overflow-x: auto;
}

.markdown-body :deep(code) {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
}

.sample-block {
  margin-bottom: 12px;
}

.sample-label {
  font-size: 13px;
  font-weight: 500;
  color: #6b7280;
  margin-bottom: 4px;
}

.sample-code {
  background: #f3f4f6;
  padding: 12px 16px;
  border-radius: 6px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}

.constraints {
  color: #6b7280;
}

/* 编辑器工具栏 */
.editor-toolbar {
  height: 44px;
  background: #1e1e1e;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  flex-shrink: 0;
  border-bottom: 1px solid #2d2d2d;
}

.toolbar-right {
  display: flex;
  gap: 8px;
}

.editor-container {
  flex: 1;
  min-height: 0;
}

/* 自测面板 */
.test-panel {
  flex-shrink: 0;
  border-top: 1px solid #2d2d2d;
  background: #1e1e1e;
  max-height: 240px;
  display: flex;
  flex-direction: column;
}

.test-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #252526;
  flex-shrink: 0;
}

.test-panel-title {
  font-size: 13px;
  font-weight: 600;
  color: #cccccc;
}

.test-panel-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.close-btn {
  color: #9ca3af !important;
  font-size: 14px;
  padding: 2px 6px !important;
}

.test-panel-body {
  display: flex;
  flex: 1;
  min-height: 0;
  gap: 1px;
  background: #2d2d2d;
}

.io-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #1e1e1e;
  min-width: 0;
}

.io-label {
  font-size: 11px;
  font-weight: 500;
  color: #858585;
  padding: 6px 12px 4px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}

.io-metrics {
  font-size: 11px;
  color: #6a9955;
  text-transform: none;
  letter-spacing: 0;
}

.io-textarea {
  flex: 1;
  background: #1e1e1e;
  color: #d4d4d4;
  border: none;
  outline: none;
  resize: none;
  padding: 4px 12px 8px;
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
  font-size: 13px;
  line-height: 1.6;
}

.io-textarea::placeholder {
  color: #555;
}

.io-output {
  flex: 1;
  margin: 0;
  padding: 4px 12px 8px;
  color: #d4d4d4;
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  overflow-y: auto;
}

.io-output.io-error {
  color: #f48771;
}

/* 结果面板 */
.result-panel {
  background: #fff;
  border-top: 1px solid #e5e7eb;
  padding: 12px 16px;
  flex-shrink: 0;
  max-height: 240px;
  overflow-y: auto;
}

.result-panel.judging {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #6b7280;
  font-size: 14px;
}

.result-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 8px;
}

.result-status {
  font-size: 16px;
  font-weight: 700;
}

.status-icon {
  margin-right: 4px;
}

.status-accepted .result-status { color: #10b981; }
.status-wrong_answer .result-status { color: #ef4444; }
.status-time_limit_exceeded .result-status { color: #f59e0b; }
.status-memory_limit_exceeded .result-status { color: #f59e0b; }
.status-runtime_error .result-status { color: #ef4444; }
.status-compile_error .result-status { color: #ef4444; }
.status-system_error .result-status { color: #6b7280; }

.result-metrics {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 8px;
}

.metric {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 积分奖励 */
.points-reward {
  display: inline-block;
  background: linear-gradient(135deg, #f59e0b, #f97316);
  color: #fff;
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 8px;
  animation: pointsBounce 0.5s ease;
}

@keyframes pointsBounce {
  0% { transform: scale(0.5); opacity: 0; }
  60% { transform: scale(1.2); }
  100% { transform: scale(1); opacity: 1; }
}

/* 通过率 */
.pass-rate {
  margin-bottom: 8px;
}

.pass-rate-text {
  font-size: 13px;
  color: #4b5563;
  margin-bottom: 4px;
}

.pass-percent {
  font-weight: 600;
  color: #1f2937;
}

.pass-bar {
  height: 6px;
  background: #e5e7eb;
  border-radius: 3px;
  overflow: hidden;
}

.pass-bar-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.5s ease;
}

.pass-bar-full {
  background: #10b981;
}

.pass-bar-partial {
  background: #ef4444;
}

.result-error pre {
  background: #fef2f2;
  color: #991b1b;
  padding: 10px;
  border-radius: 6px;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 8px 0;
}

.result-actions {
  margin-top: 4px;
}

/* 自测结果面板 */
.self-test-panel {
  max-height: 320px;
}

.self-test-panel .result-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.self-test-cases {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 4px;
}

.self-test-case {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
}

.self-test-case.case-passed {
  border-color: #a7f3d0;
}

.self-test-case.case-failed {
  border-color: #fca5a5;
}

.case-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: #f9fafb;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.case-passed .case-header {
  background: #ecfdf5;
}

.case-failed .case-header {
  background: #fef2f2;
}

.case-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
}

.badge-pass {
  background: #10b981;
}

.badge-fail {
  background: #ef4444;
}

.case-metrics {
  margin-left: auto;
  font-size: 11px;
  font-weight: 400;
  color: #9ca3af;
}

.case-detail {
  padding: 8px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.case-field {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.field-label {
  font-size: 11px;
  font-weight: 500;
  color: #9ca3af;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.field-value {
  background: #f3f4f6;
  padding: 6px 10px;
  border-radius: 4px;
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
  font-size: 12px;
  color: #374151;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}

.field-value.field-error {
  background: #fef2f2;
  color: #991b1b;
}

/* ============ 评论区样式 ============ */

.comment-count-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  background: #409eff;
  color: #fff;
  border-radius: 9px;
  font-size: 11px;
  font-weight: 600;
  padding: 0 5px;
  margin-left: 6px;
}

.discussion-section {
  padding-bottom: 40px;
}

.comment-input-box {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 16px;
  transition: border-color 0.2s;
}

.comment-input-box:focus-within {
  border-color: #409eff;
}

.comment-textarea {
  width: 100%;
  border: none;
  outline: none;
  resize: none;
  padding: 12px;
  font-size: 14px;
  line-height: 1.6;
  color: #374151;
  font-family: inherit;
  box-sizing: border-box;
}

.comment-textarea::placeholder {
  color: #9ca3af;
}

.comment-input-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #f9fafb;
  border-top: 1px solid #f3f4f6;
}

.char-count {
  font-size: 12px;
  color: #9ca3af;
}

.comment-sort-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f3f4f6;
}

.comment-total {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.sort-tabs {
  display: flex;
  gap: 12px;
}

.sort-tab {
  font-size: 13px;
  color: #9ca3af;
  cursor: pointer;
  padding: 2px 0;
  transition: color 0.2s;
}

.sort-tab.active {
  color: #409eff;
  font-weight: 600;
}

.sort-tab:hover {
  color: #409eff;
}

.comment-empty {
  text-align: center;
  padding: 40px 0;
  color: #9ca3af;
  font-size: 14px;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.comment-item {
  padding: 16px 0;
  border-bottom: 1px solid #f3f4f6;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-main {
  display: flex;
  gap: 12px;
}

.comment-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}

.reply-avatar {
  width: 28px;
  height: 28px;
  font-size: 12px;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  flex-wrap: wrap;
}

.comment-author {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.comment-time {
  font-size: 12px;
  color: #9ca3af;
}

.reply-to {
  font-size: 12px;
  color: #9ca3af;
}

.reply-to-name {
  color: #409eff;
  font-weight: 500;
}

.comment-text {
  font-size: 14px;
  color: #4b5563;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  margin-bottom: 6px;
}

.comment-actions {
  display: flex;
  gap: 16px;
}

.action-btn {
  font-size: 12px;
  color: #9ca3af;
  cursor: pointer;
  transition: color 0.2s;
  user-select: none;
}

.action-btn:hover {
  color: #6b7280;
}

.action-btn.liked {
  color: #409eff;
}

.reply-list {
  margin-top: 12px;
  padding-left: 4px;
  border-left: 2px solid #f3f4f6;
}

.reply-item {
  display: flex;
  gap: 10px;
  padding: 10px 0 10px 12px;
}

.reply-input-box {
  margin-top: 10px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  overflow: hidden;
}

.reply-input-box:focus-within {
  border-color: #409eff;
}

.reply-textarea {
  width: 100%;
  border: none;
  outline: none;
  resize: none;
  padding: 8px 12px;
  font-size: 13px;
  line-height: 1.5;
  color: #374151;
  font-family: inherit;
  box-sizing: border-box;
}

.reply-textarea::placeholder {
  color: #9ca3af;
}

.reply-input-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
  padding: 6px 8px;
  background: #f9fafb;
}

.load-more-replies {
  font-size: 13px;
  color: #409eff;
  cursor: pointer;
  padding: 8px 0 8px 12px;
  transition: color 0.2s;
}

.load-more-replies:hover {
  color: #337ecc;
}

.load-more-comments {
  text-align: center;
  padding: 12px 0;
}

@media (max-width: 900px) {
  .split-panels {
    flex-direction: column;
  }
  .left-panel {
    width: 100%;
    height: 50%;
    border-right: none;
    border-bottom: 1px solid #e5e7eb;
  }
}
</style>
