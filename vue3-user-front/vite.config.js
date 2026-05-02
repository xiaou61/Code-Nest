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

          if (id.includes('monaco-editor')) {
            return 'vendor-monaco'
          }

          if (id.includes('@antv/g6') || id.includes('d3')) {
            return 'vendor-graph'
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
