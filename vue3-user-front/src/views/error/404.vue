<template>
  <CnPage class="not-found-page" max-width="820px" full-height>
    <CnSection class="not-found-panel" surface="panel">
      <div class="not-found-content">
        <div class="not-found-code">404</div>
        <CnStatusTag type="warning" size="lg">页面不存在</CnStatusTag>
        <h1>找不到这个页面</h1>
        <p>您访问的页面不存在、已被删除，或当前账号没有访问权限。</p>

        <div class="not-found-actions">
          <el-button type="primary" size="large" :icon="House" @click="goHome">返回首页</el-button>
          <el-button size="large" :icon="ArrowLeft" @click="goBack">返回上页</el-button>
        </div>
      </div>
    </CnSection>
  </CnPage>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ArrowLeft, House } from '@element-plus/icons-vue'
import { CnPage, CnSection, CnStatusTag } from '@/design-system'

const router = useRouter()

const goHome = () => {
  router.push('/')
}

const goBack = () => {
  router.go(-1)
}
</script>

<style scoped>
.not-found-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 96px);
}

.not-found-panel {
  width: 100%;
}

.not-found-content {
  display: grid;
  justify-items: center;
  gap: var(--cn-space-5);
  min-width: 0;
  padding: var(--cn-space-8) var(--cn-space-5);
  text-align: center;
}

.not-found-code {
  color: var(--cn-color-brand-primary);
  font-family: var(--cn-font-heading);
  font-size: clamp(76px, 15vw, 132px);
  font-weight: 850;
  line-height: 0.95;
  letter-spacing: 0;
}

.not-found-content h1 {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-size: 30px;
  font-weight: 760;
  line-height: 1.25;
}

.not-found-content p {
  max-width: 520px;
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 15px;
  line-height: 1.8;
}

.not-found-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: var(--cn-space-3);
}

@media (max-width: 560px) {
  .not-found-content {
    padding: var(--cn-space-6) var(--cn-space-3);
  }

  .not-found-content h1 {
    font-size: 24px;
  }

  .not-found-actions {
    width: 100%;
  }

  .not-found-actions :deep(.el-button) {
    width: 100%;
  }
}
</style>
