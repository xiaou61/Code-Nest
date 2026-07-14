<template>
  <el-sub-menu v-if="hasChildren" :index="itemIndex">
    <template #title>
      <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
      <span>{{ item.label }}</span>
    </template>

    <CnSidebarItem
      v-for="child in item.children"
      :key="child.index || child.path || child.label"
      :item="child"
    />
  </el-sub-menu>

  <el-menu-item v-else :index="itemIndex">
    <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
    <span>{{ item.label }}</span>
  </el-menu-item>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { CnSidebarItem } from '../../types/components'

defineOptions({
  name: 'CnSidebarItem'
})

const props = defineProps<{
  item: CnSidebarItem
}>()

const hasChildren = computed(() => Boolean(props.item.children?.length))
const itemIndex = computed(() => props.item.index || props.item.path || props.item.label)
</script>
