<template>
  <div 
    class="flashcard-wrapper"
    :class="{ flipped: isFlipped }"
    @click="handleFlip"
  >
    <div class="flashcard">
      <div class="card-face card-front">
        <div class="card-label">正面</div>
        <div class="card-content" v-html="renderedFront"></div>
        <div class="flip-hint">
          <el-icon><RefreshRight /></el-icon>
          <span>点击翻转查看答案</span>
        </div>
      </div>
      <div class="card-face card-back">
        <div class="card-label">背面</div>
        <div class="card-content" v-html="renderedBack"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { RefreshRight } from '@element-plus/icons-vue'
import { renderMarkdown, sanitizeHtml } from '@/utils/markdown'

const props = defineProps({
  frontContent: {
    type: String,
    default: ''
  },
  backContent: {
    type: String,
    default: ''
  },
  contentType: {
    type: Number,
    default: 1 // 1-文本 2-Markdown 3-代码
  },
  flipped: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['flip'])

const isFlipped = ref(props.flipped)

// 渲染内容
const renderContent = (content) => {
  if (!content) return ''
  if (props.contentType === 2) { // Markdown
    return renderMarkdown(content)
  }
  if (props.contentType === 3) { // 代码
    return sanitizeHtml(`<pre><code>${escapeHtml(content)}</code></pre>`)
  }
  return sanitizeHtml(escapeHtml(content).replace(/\n/g, '<br>'))
}

const escapeHtml = (text) => {
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

const renderedFront = computed(() => renderContent(props.frontContent))
const renderedBack = computed(() => renderContent(props.backContent))

const handleFlip = () => {
  isFlipped.value = !isFlipped.value
  emit('flip', isFlipped.value)
}

// 重置状态
const reset = () => {
  isFlipped.value = false
}

defineExpose({ reset, flip: handleFlip })
</script>

<style lang="scss" scoped>
.flashcard-wrapper {
  perspective: 1000px;
  width: 100%;
  max-width: 600px;
  height: 400px;
  cursor: pointer;
  
  &.flipped .flashcard {
    transform: rotateY(180deg);
  }
}

.flashcard {
  position: relative;
  width: 100%;
  height: 100%;
  transition: transform 0.6s cubic-bezier(0.4, 0, 0.2, 1);
  transform-style: preserve-3d;
}

.card-face {
  position: absolute;
  width: 100%;
  height: 100%;
  backface-visibility: hidden;
  border-radius: 16px;
  padding: 32px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.card-front {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  
  .card-label {
    background: rgba(255, 255, 255, 0.2);
  }
  
  .flip-hint {
    position: absolute;
    bottom: 20px;
    left: 50%;
    transform: translateX(-50%);
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    opacity: 0.8;
    
    .el-icon {
      animation: pulse 2s infinite;
    }
  }
}

.card-back {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  color: #fff;
  transform: rotateY(180deg);
  
  .card-label {
    background: rgba(255, 255, 255, 0.2);
  }
}

.card-label {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  width: fit-content;
  margin-bottom: 20px;
}

.card-content {
  flex: 1;
  overflow-y: auto;
  font-size: 18px;
  line-height: 1.8;
  
  &::-webkit-scrollbar {
    width: 6px;
  }
  
  &::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.3);
    border-radius: 3px;
  }
  
  :deep(pre) {
    background: rgba(0, 0, 0, 0.2);
    padding: 16px;
    border-radius: 8px;
    overflow-x: auto;
    font-size: 14px;
    
    code {
      font-family: 'Fira Code', monospace;
    }
  }
  
  :deep(p) {
    margin: 0 0 12px 0;
    
    &:last-child {
      margin-bottom: 0;
    }
  }
  
  :deep(ul), :deep(ol) {
    padding-left: 24px;
    margin: 12px 0;
  }
  
  :deep(code) {
    background: rgba(0, 0, 0, 0.2);
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 14px;
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

@media (max-width: 640px) {
  .flashcard-wrapper {
    height: 300px;
  }
  
  .card-face {
    padding: 20px;
  }
  
  .card-content {
    font-size: 16px;
  }
}
</style>
