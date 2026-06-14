<template>
  <div 
    class="flashcard-wrapper"
    :class="{ flipped: isFlipped }"
    @click="handleFlip"
  >
    <div class="flashcard">
      <div class="card-face card-front">
        <CnStatusTag class="card-label" type="brand" size="sm" :dot="false">正面</CnStatusTag>
        <div class="card-content" v-html="renderedFront"></div>
        <div class="flip-hint">
          <el-icon><RefreshRight /></el-icon>
          <span>点击翻转查看答案</span>
        </div>
      </div>
      <div class="card-face card-back">
        <CnStatusTag class="card-label" type="success" size="sm" :dot="false">背面</CnStatusTag>
        <div class="card-content" v-html="renderedBack"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { RefreshRight } from '@element-plus/icons-vue'
import { CnStatusTag } from '@/design-system'
import { renderMarkdown, sanitizeHtml } from '@/utils/markdown'

const props = withDefaults(defineProps<{
  frontContent?: string
  backContent?: string
  contentType?: number
  flipped?: boolean
}>(), {
  frontContent: '',
  backContent: '',
  contentType: 1,
  flipped: false
})

const emit = defineEmits<{
  (e: 'flip', flipped: boolean): void
}>()

const isFlipped = ref(props.flipped)

watch(() => props.flipped, (value) => {
  isFlipped.value = value
})

// 渲染内容
const renderContent = (content: string) => {
  if (!content) return ''
  if (props.contentType === 2) { // Markdown
    return renderMarkdown(content)
  }
  if (props.contentType === 3) { // 代码
    return sanitizeHtml(`<pre><code>${escapeHtml(content)}</code></pre>`)
  }
  return sanitizeHtml(escapeHtml(content).replace(/\n/g, '<br>'))
}

const escapeHtml = (text: string) => {
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
  transition: transform var(--cn-motion-hero) var(--cn-ease-in-out);
  transform-style: preserve-3d;
}

.card-face {
  position: absolute;
  width: 100%;
  height: 100%;
  backface-visibility: hidden;
  border-radius: var(--cn-radius-panel);
  padding: var(--cn-space-8);
  display: flex;
  flex-direction: column;
  box-shadow: var(--cn-shadow-md);
  overflow: hidden;
}

.card-front {
  background: color-mix(in srgb, var(--cn-color-brand-primary) 82%, var(--cn-color-bg-surface));
  color: var(--cn-button-primary-color);
  
  .flip-hint {
    position: absolute;
    bottom: 20px;
    left: 50%;
    transform: translateX(-50%);
    display: flex;
    align-items: center;
    gap: var(--cn-space-2);
    font-size: 14px;
    opacity: 0.8;
    
    .el-icon {
      animation: pulse 2s infinite;
    }
  }
}

.card-back {
  background: color-mix(in srgb, var(--cn-color-success) 82%, var(--cn-color-bg-surface));
  color: var(--cn-button-primary-color);
  transform: rotateY(180deg);
}

.card-label {
  width: fit-content;
  margin-bottom: var(--cn-space-5);
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
    background: color-mix(in srgb, var(--cn-button-primary-color) 30%, transparent);
    border-radius: var(--cn-radius-pill);
  }
  
  :deep(pre) {
    background: color-mix(in srgb, var(--cn-color-text-primary) 24%, transparent);
    padding: var(--cn-space-4);
    border-radius: var(--cn-radius-card);
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
    background: color-mix(in srgb, var(--cn-color-text-primary) 24%, transparent);
    padding: 2px 6px;
    border-radius: var(--cn-radius-control);
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
    padding: var(--cn-space-5);
  }
  
  .card-content {
    font-size: 16px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .flashcard,
  .card-front .flip-hint .el-icon {
    animation: none;
    transition: none;
  }
}
</style>
