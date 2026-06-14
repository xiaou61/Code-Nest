<template>
  <router-view />
</template>

<script setup lang="ts">
import { useTheme } from '@/design-system'

useTheme()
</script>

<style>
#app {
  height: 100vh;
  width: 100vw;
  background: var(--cn-color-bg-page);
  color: var(--cn-color-text-primary);
}
</style> 
