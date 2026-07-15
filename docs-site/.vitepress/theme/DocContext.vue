<script setup lang="ts">
import { computed } from 'vue'
import { useData, useRoute, withBase } from 'vitepress'

const route = useRoute()
const { frontmatter } = useData()

const sections = [
  { prefix: '/guide/', label: '开始与开发', link: '/guide/' },
  { prefix: '/architecture/', label: '系统架构', link: '/architecture/' },
  { prefix: '/modules/', label: '功能模块', link: '/modules/' },
  { prefix: '/api/', label: 'API', link: '/api/' },
  { prefix: '/operations/', label: '部署运维', link: '/operations/' },
  { prefix: '/manuals/', label: '操作手册', link: '/manuals/' },
  { prefix: '/reference/', label: '参考索引', link: '/reference/' },
  { prefix: '/roadmap/', label: '版本历史', link: '/roadmap/' }
]

const section = computed(() => sections.find(item => route.path.startsWith(item.prefix)))
const isVisible = computed(() => {
  const layout = frontmatter.value.layout
  return Boolean(section.value) && layout !== 'home' && layout !== 'not-found'
})
</script>

<template>
  <nav v-if="isVisible && section" class="cn-doc-context" aria-label="当前文档分类">
    <a class="cn-doc-context__section" :href="withBase(section.link)">
      {{ section.label }}
    </a>
    <span class="cn-doc-context__divider" aria-hidden="true"></span>
    <span class="cn-doc-context__version">v2.4.x</span>
  </nav>
</template>
