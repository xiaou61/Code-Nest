<template>
  <CnPage class="playground-shell" surface="transparent" max-width="1440px" full-height>
    <CnPageHeader
      class="cn-learn-reveal"
      title="练习场"
      description="在线运行多语言代码，快速验证思路与输入输出，专注练习反馈闭环。"
      eyebrow="Code Playground"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">
          语言 {{ selectedLanguageLabel }}
        </CnStatusTag>
        <CnStatusTag :type="running ? 'warning' : 'success'" size="sm">
          {{ running ? '执行中' : '待命' }}
        </CnStatusTag>
        <CnStatusTag v-if="result" :type="getResultTone(result.status)" size="sm">
          {{ getResultStatusLabel(result.status) }}
        </CnStatusTag>
      </template>

      <template #actions>
        <el-button plain @click="handleClear">
          <el-icon><Delete /></el-icon>
          清空
        </el-button>
        <el-button type="primary" :loading="running" @click="handleRun">
          <el-icon><CaretRight /></el-icon>
          运行
        </el-button>
      </template>
    </CnPageHeader>

    <div class="playground-wrap cn-learn-reveal">
      <div class="playground">
        <!-- 顶部工具栏 -->
        <div class="toolbar">
          <div class="toolbar-left">
            <h3 class="page-title">练习场</h3>
            <el-select v-model="selectedLanguage" class="language-select" size="small">
              <el-option v-for="lang in languages" :key="lang.value" :label="lang.label" :value="lang.value" />
            </el-select>
          </div>
          <div class="toolbar-right">
            <el-button type="primary" size="small" :loading="running" @click="handleRun">
              <el-icon><CaretRight /></el-icon>
              运行
            </el-button>
            <el-button size="small" @click="handleClear">
              <el-icon><Delete /></el-icon>
              清空
            </el-button>
          </div>
        </div>

        <!-- 主体区域 -->
        <div class="main-area">
          <!-- 编辑器 -->
          <div class="editor-section">
            <div class="editor-container" ref="editorContainer"></div>
          </div>

          <!-- 右侧 IO 面板 -->
          <div class="io-section">
            <!-- 输入 -->
            <div class="io-block input-block">
              <div class="io-header">
                <span>标准输入 (stdin)</span>
              </div>
              <textarea
                v-model="stdin"
                class="io-textarea"
                placeholder="在此输入程序的标准输入数据..."
                spellcheck="false"
              ></textarea>
            </div>

            <!-- 输出 -->
            <div class="io-block output-block">
              <div class="io-header">
                <span>输出</span>
                <span v-if="result" class="run-info">
                  {{ result.timeUsed }}ms · {{ formatMemory(result.memoryUsed) }}
                </span>
              </div>

              <!-- 状态提示 -->
              <div v-if="running" class="io-status loading">
                <el-icon class="is-loading"><Loading /></el-icon>
                运行中...
              </div>

              <div v-else-if="result" class="io-output-content">
                <!-- 错误状态 -->
                <div v-if="result.status === 'compile_error'" class="status-badge error">编译错误</div>
                <div v-else-if="result.status === 'runtime_error'" class="status-badge error">运行错误</div>
                <div v-else-if="result.status === 'time_limit_exceeded'" class="status-badge warn">超时</div>
                <div v-else-if="result.status === 'memory_limit_exceeded'" class="status-badge warn">超内存</div>
                <div v-else-if="result.status === 'error'" class="status-badge error">错误</div>
                <div v-else class="status-badge success">运行成功</div>

                <!-- stdout -->
                <pre v-if="result.stdout" class="output-pre stdout">{{ result.stdout }}</pre>

                <!-- stderr -->
                <pre v-if="result.stderr" class="output-pre stderr">{{ result.stderr }}</pre>

                <div v-if="!result.stdout && !result.stderr" class="empty-output">（无输出）</div>
              </div>

              <div v-else class="io-placeholder">
                点击「运行」查看输出结果
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { CaretRight, Delete, Loading } from '@element-plus/icons-vue'
import { CnPage, CnPageHeader, CnStatusTag } from '@/design-system'
import type { CnTone } from '@/design-system'
import { ojApi } from '@/api/oj'
import loader from '@monaco-editor/loader'
import { useRevealMotion } from '@/utils/reveal-motion'

const editorContainer = ref(null)
let editorInstance: any = null
useRevealMotion('.playground-shell .cn-learn-reveal')

const selectedLanguage = ref('java')
const stdin = ref('')
const running = ref(false)
const result = ref<any>(null)

const breadcrumbs = [
  { label: '首页', to: '/' },
  { label: '在线判题', to: '/oj' },
  { label: '练习场' }
]

const languages = [
  { value: 'java', label: 'Java', monacoLang: 'java' },
  { value: 'cpp', label: 'C++', monacoLang: 'cpp' },
  { value: 'c', label: 'C', monacoLang: 'c' },
  { value: 'python', label: 'Python3', monacoLang: 'python' },
  { value: 'go', label: 'Go', monacoLang: 'go' },
  { value: 'javascript', label: 'JavaScript', monacoLang: 'javascript' }
]

const defaultCode = {
  java: 'import java.util.Scanner;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        String line = sc.nextLine();\n        System.out.println("Hello, " + line + "!");\n    }\n}',
  cpp: '#include <iostream>\nusing namespace std;\n\nint main() {\n    string s;\n    getline(cin, s);\n    cout << "Hello, " << s << "!" << endl;\n    return 0;\n}',
  c: '#include <stdio.h>\n\nint main() {\n    char s[100];\n    fgets(s, 100, stdin);\n    printf("Hello, %s!\\n", s);\n    return 0;\n}',
  python: '# 在此编写代码\nname = input()\nprint(f"Hello, {name}!")\n',
  go: 'package main\n\nimport (\n    "bufio"\n    "fmt"\n    "os"\n)\n\nfunc main() {\n    scanner := bufio.NewScanner(os.Stdin)\n    scanner.Scan()\n    fmt.Printf("Hello, %s!\\n", scanner.Text())\n}',
  javascript: 'const readline = require("readline");\nconst rl = readline.createInterface({ input: process.stdin });\nrl.on("line", (line) => {\n    console.log(`Hello, ${line}!`);\n    rl.close();\n});\n'
}

const getMonacoLang = (lang) => {
  const found = languages.find(l => l.value === lang)
  return found ? found.monacoLang : 'plaintext'
}

const formatMemory = (kb) => {
  if (!kb) return '0KB'
  return kb > 1024 ? `${(kb / 1024).toFixed(1)}MB` : `${kb}KB`
}

const selectedLanguageLabel = computed(() => {
  return languages.find((lang) => lang.value === selectedLanguage.value)?.label || selectedLanguage.value
})

const getResultStatusLabel = (status) => {
  const map = {
    compile_error: '编译错误',
    runtime_error: '运行错误',
    time_limit_exceeded: '超时',
    memory_limit_exceeded: '超内存',
    error: '错误',
    success: '运行成功'
  }
  return map[status] || '运行完成'
}

const getResultTone = (status): CnTone => {
  if (['compile_error', 'runtime_error', 'error'].includes(status)) return 'danger'
  if (['time_limit_exceeded', 'memory_limit_exceeded'].includes(status)) return 'warning'
  return 'success'
}

// ============ Monaco 编辑器 ============

const initEditor = async () => {
  const monaco = await loader.init()
  editorInstance = monaco.editor.create(editorContainer.value, {
    value: defaultCode[selectedLanguage.value] || '',
    language: getMonacoLang(selectedLanguage.value),
    theme: 'vs-dark',
    fontSize: 14,
    lineNumbers: 'on',
    minimap: { enabled: false },
    automaticLayout: true,
    scrollBeyondLastLine: false,
    tabSize: 4,
    wordWrap: 'on'
  })
}

watch(selectedLanguage, (newLang) => {
  if (editorInstance) {
    if (window.monaco) {
      window.monaco.editor.setModelLanguage(editorInstance.getModel(), getMonacoLang(newLang))
    }
    const currentCode = editorInstance.getValue()
    const isDefault = Object.values(defaultCode).some(c => c.trim() === currentCode.trim())
    if (!currentCode.trim() || isDefault) {
      editorInstance.setValue(defaultCode[newLang] || '')
    }
  }
})

// ============ 运行 ============

const handleRun = async () => {
  if (!editorInstance) return
  const code = editorInstance.getValue()
  if (!code.trim()) {
    ElMessage.warning('请先编写代码')
    return
  }

  running.value = true
  result.value = null
  try {
    result.value = await ojApi.runCode({
      language: selectedLanguage.value,
      code,
      stdin: stdin.value
    })
  } catch {
    result.value = { status: 'error', stderr: '请求失败，请检查网络或后端服务', stdout: '' }
  } finally {
    running.value = false
  }
}

const handleClear = () => {
  if (editorInstance) {
    editorInstance.setValue(defaultCode[selectedLanguage.value] || '')
  }
  stdin.value = ''
  result.value = null
}

onMounted(async () => {
  await nextTick()
  initEditor()
})

onBeforeUnmount(() => {
  if (editorInstance) {
    editorInstance.dispose()
    editorInstance = null
  }
})
</script>

<style scoped>
.playground-shell {
  --playground-bg: var(--cn-color-text-primary);
  --playground-panel: color-mix(in srgb, var(--cn-color-text-primary) 92%, var(--cn-color-bg-page));
  --playground-panel-elevated: color-mix(in srgb, var(--cn-color-text-primary) 86%, var(--cn-color-bg-page));
  --playground-border: color-mix(in srgb, var(--cn-color-bg-surface) 18%, var(--cn-color-text-primary));
  --playground-text: color-mix(in srgb, var(--cn-color-bg-surface) 84%, var(--cn-color-text-primary));
  --playground-text-muted: color-mix(in srgb, var(--cn-color-bg-surface) 48%, var(--cn-color-text-primary));
  --playground-text-subtle: color-mix(in srgb, var(--cn-color-bg-surface) 32%, var(--cn-color-text-primary));
  --playground-success-bg: color-mix(in srgb, var(--cn-color-success) 15%, transparent);
  --playground-danger-bg: color-mix(in srgb, var(--cn-color-danger) 15%, transparent);
  --playground-warning-bg: color-mix(in srgb, var(--cn-color-warning) 15%, transparent);
  --playground-stdout-bg: color-mix(in srgb, var(--cn-color-success) 13%, var(--playground-bg));
  --playground-stdout-text: color-mix(in srgb, var(--cn-color-success) 44%, var(--cn-color-bg-surface));
  --playground-stderr-bg: color-mix(in srgb, var(--cn-color-danger) 13%, var(--playground-bg));
  --playground-stderr-text: color-mix(in srgb, var(--cn-color-danger) 44%, var(--cn-color-bg-surface));

  min-height: calc(100vh - 68px);
}

.playground-wrap {
  min-width: 0;
  border: 1px solid color-mix(in srgb, var(--cn-color-brand-primary) 22%, var(--cn-color-border-subtle));
  border-radius: var(--cn-radius-panel);
  background: var(--playground-bg);
  box-shadow: var(--cn-shadow-md);
  overflow: hidden;
}

.playground {
  height: calc(100vh - 260px);
  display: flex;
  flex-direction: column;
  background: var(--playground-bg);
  overflow: hidden;
}

.toolbar {
  height: 50px;
  background: var(--playground-panel);
  border-bottom: 1px solid var(--playground-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  flex-shrink: 0;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.language-select {
  width: 140px;
}

.page-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--playground-text);
}

.toolbar-right {
  display: flex;
  gap: 8px;
}

.main-area {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.editor-section {
  flex: 1;
  min-width: 0;
}

.editor-container {
  width: 100%;
  height: 100%;
}

.io-section {
  width: 380px;
  display: flex;
  flex-direction: column;
  border-left: 1px solid var(--playground-border);
  background: var(--playground-bg);
  flex-shrink: 0;
}

.io-block {
  display: flex;
  flex-direction: column;
}

.input-block {
  height: 40%;
  border-bottom: 1px solid var(--playground-border);
}

.output-block {
  flex: 1;
  overflow: hidden;
}

.io-header {
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  background: var(--playground-panel);
  color: var(--playground-text);
  font-size: 13px;
  font-weight: 500;
  flex-shrink: 0;
}

.run-info {
  color: var(--playground-text-muted);
  font-size: 12px;
}

.io-textarea {
  flex: 1;
  background: var(--playground-bg);
  color: var(--playground-text);
  border: none;
  outline: none;
  padding: 10px 12px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  line-height: 1.6;
  resize: none;
}

.io-textarea::placeholder {
  color: var(--playground-text-subtle);
}

.io-status.loading {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 12px;
  color: var(--playground-text-muted);
  font-size: 13px;
}

.io-output-content {
  padding: 10px 12px;
  overflow-y: auto;
  flex: 1;
}

.status-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 10px;
}

.status-badge.success {
  background: var(--playground-success-bg);
  color: var(--cn-color-success);
}

.status-badge.error {
  background: var(--playground-danger-bg);
  color: var(--cn-color-danger);
}

.status-badge.warn {
  background: var(--playground-warning-bg);
  color: var(--cn-color-warning);
}

.output-pre {
  margin: 0 0 8px;
  padding: 8px 10px;
  border-radius: 4px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

.output-pre.stdout {
  background: var(--playground-stdout-bg);
  color: var(--playground-stdout-text);
}

.output-pre.stderr {
  background: var(--playground-stderr-bg);
  color: var(--playground-stderr-text);
}

.empty-output {
  color: var(--playground-text-subtle);
  font-size: 13px;
}

.io-placeholder {
  padding: 16px 12px;
  color: var(--playground-text-subtle);
  font-size: 13px;
}

@media (max-width: 1024px) {
  .playground {
    height: calc(100vh - 270px);
  }
}

@media (max-width: 768px) {
  .playground {
    height: calc(100vh - 292px);
  }

  .main-area {
    flex-direction: column;
  }

  .io-section {
    width: 100%;
    height: 46%;
    border-left: 0;
    border-top: 1px solid var(--playground-border);
  }
}
</style>
