import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

const buildManualChunks = (id) => {
  if (!id.includes('node_modules')) {
    return undefined
  }

  if (id.includes('monaco-editor') || id.includes('@monaco-editor')) {
    return 'vendor-monaco'
  }

  if (id.includes('@antv') || id.includes('/g6/')) {
    return 'vendor-graph'
  }

  if (id.includes('markdown-it') || id.includes('highlight.js')) {
    return 'vendor-markdown'
  }

  if (id.includes('element-plus') || id.includes('@element-plus')) {
    return 'vendor-element'
  }

  if (id.includes('/vue/') || id.includes('vue-router') || id.includes('pinia')) {
    return 'vendor-vue'
  }

  if (
    id.includes('axios') ||
    id.includes('lodash') ||
    id.includes('js-cookie') ||
    id.includes('nprogress')
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
