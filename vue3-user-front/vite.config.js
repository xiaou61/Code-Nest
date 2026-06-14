import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

const buildManualChunks = (id) => {
  const normalizedId = id.replace(/\\/g, '/')

  if (!normalizedId.includes('node_modules')) {
    return undefined
  }

  if (normalizedId.includes('monaco-editor') || normalizedId.includes('@monaco-editor')) {
    return 'vendor-monaco'
  }

  if (normalizedId.includes('@antv') || normalizedId.includes('/g6/')) {
    return 'vendor-graph'
  }

  if (
    normalizedId.includes('markdown-it') ||
    normalizedId.includes('highlight.js') ||
    normalizedId.includes('dompurify')
  ) {
    return 'vendor-markdown'
  }

  if (normalizedId.includes('element-plus') || normalizedId.includes('@element-plus')) {
    return 'vendor-element'
  }

  if (normalizedId.includes('vue-router')) {
    return 'vendor-router'
  }

  if (
    normalizedId.includes('pinia') ||
    normalizedId.includes('@vueuse') ||
    normalizedId.includes('vue-demi')
  ) {
    return 'vendor-state'
  }

  if (normalizedId.includes('/vue/') || normalizedId.includes('/@vue/')) {
    return 'vendor-vue'
  }

  if (
    normalizedId.includes('axios') ||
    normalizedId.includes('lodash') ||
    normalizedId.includes('js-cookie') ||
    normalizedId.includes('nprogress')
  ) {
    return 'vendor-utils'
  }

  return 'vendor'
}

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(process.cwd(), 'src'),
    },
  },
  server: {
    port: 3001,
    open: true,
    proxy: {
      '/api': {
        target: 'http://localhost:9999',
        changeOrigin: true,
        secure: false,
        ws: true,
      },
    },
  },
  css: {
    preprocessorOptions: {
      scss: {
        api: 'modern',
      },
    },
  },
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
    minify: 'esbuild',
    sourcemap: false,
    chunkSizeWarningLimit: 1200,
    rollupOptions: {
      output: {
        manualChunks: buildManualChunks,
      },
    },
  },
})
