<template>
  <CnPage class="growth-autopilot-page" surface="transparent" full-height max-width="1440px">
    <GrowthAutopilotPanel />
  </CnPage>
</template>

<script setup lang="ts">
import { CnPage } from '@/design-system'
import GrowthAutopilotPanel from './GrowthAutopilotPanel.vue'
</script>

<style scoped>
.growth-autopilot-page {
  min-height: calc(100vh - 68px);
}
</style>
