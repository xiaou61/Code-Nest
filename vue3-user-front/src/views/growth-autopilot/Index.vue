<template>
  <div class="growth-autopilot-page cn-learn-shell">
    <div class="cn-learn-shell__inner">
      <GrowthAutopilotPanel />
    </div>
  </div>
</template>

<script setup>
import GrowthAutopilotPanel from './GrowthAutopilotPanel.vue'
</script>

<style scoped>
.growth-autopilot-page {
  min-height: calc(100vh - 68px);
}
</style>
