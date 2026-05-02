import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(process.cwd(), 'src'),
    },
  },
  server: {
    port: 3000,
    open: true,
    proxy: {
      '/api': {
        target: 'http://localhost:9999',
        changeOrigin: true,
        secure: false,
        // 不使用 rewrite，因为后端 context-path 已经有 /api 前缀
      },
    },
  },
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
    minify: 'esbuild',
    sourcemap: false,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) {
            return
          }

          if (id.includes('echarts')) {
            return 'vendor-charts'
          }

          if (id.includes('@antv/g6') || id.includes('d3')) {
            return 'vendor-visualization'
          }

          if (id.includes('markdown-it') || id.includes('highlight.js') || id.includes('dompurify')) {
            return 'vendor-markdown'
          }

          if (id.includes('element-plus') || id.includes('@element-plus')) {
            return 'vendor-element'
          }

          if (id.includes('vue-router')) {
            return 'vendor-router'
          }

          if (id.includes('pinia') || id.includes('@vueuse') || id.includes('vue-demi')) {
            return 'vendor-state'
          }

          if (id.includes('vue')) {
            return 'vendor-vue'
          }

          return 'vendor-misc'
        }
      }
    }
  },
}) 
