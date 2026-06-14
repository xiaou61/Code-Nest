<template>
  <CnPage class="error-page" surface="transparent" max-width="860px">
    <CnSection surface="panel" class="error-panel">
      <div class="error-code">404</div>
      <CnStatusTag type="warning" size="lg">页面不存在</CnStatusTag>
      <h1>您访问的后台页面不存在</h1>
      <p>该页面可能已被移动、删除，或当前账号没有从此路径进入的上下文。</p>

      <div class="error-actions">
        <el-button type="primary" @click="goHome">
          <el-icon><HomeFilled /></el-icon>
          返回首页
        </el-button>
        <el-button @click="goBack">
          <el-icon><Back /></el-icon>
          返回上一页
        </el-button>
      </div>
    </CnSection>
  </CnPage>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Back, HomeFilled } from '@element-plus/icons-vue'
import { CnPage, CnSection, CnStatusTag } from '@/design-system'

const router = useRouter()

const goHome = () => {
  router.push('/')
}

const goBack = () => {
  if (window.history.length > 1) {
    router.go(-1)
    return
  }
  router.push('/')
}
</script>

<style scoped>
.error-page {
  min-height: 70vh;
  display: grid;
  align-items: center;
}

.error-panel :deep(.cn-section__body) {
  display: grid;
  justify-items: center;
  gap: var(--cn-space-4);
  text-align: center;
}

.error-code {
  color: var(--cn-color-brand-primary);
  font-family: var(--cn-font-heading);
  font-size: clamp(76px, 16vw, 132px);
  font-weight: 800;
  line-height: 0.95;
}

.error-panel h1 {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-size: clamp(22px, 4vw, 30px);
  font-weight: 760;
}

.error-panel p {
  max-width: 560px;
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 15px;
  line-height: 1.75;
}

.error-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: var(--cn-space-3);
  margin-top: var(--cn-space-2);
}
</style>
