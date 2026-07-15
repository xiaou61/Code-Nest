import { resolve } from 'path'
import { defineConfig, externalizeDepsPlugin } from 'electron-vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  main: {
    plugins: [externalizeDepsPlugin()],
    build: {
      rollupOptions: {
        input: {
          index: resolve(__dirname, 'electron/main/index.ts')
        }
      }
    }
  },
  preload: {
    plugins: [externalizeDepsPlugin()],
    build: {
      rollupOptions: {
        input: {
          index: resolve(__dirname, 'electron/preload/index.ts')
        }
      }
    }
  },
  renderer: {
    root: '.',
    build: {
      rollupOptions: {
        input: {
          index: resolve(__dirname, 'index.html')
        }
      }
    },
    plugins: [vue()],
    resolve: {
      preserveSymlinks: true,
      alias: {
        '@/design-system': resolve(__dirname, 'node_modules/@code-nest/design-system/src'),
        '@': resolve(__dirname, 'src')
      }
    },
    server: {
      port: 3001,
      fs: {
        allow: [resolve(__dirname), resolve(__dirname, '../code-nest-design-system')]
      },
      proxy: {
        '/api': {
          target: 'http://localhost:9999',
          changeOrigin: true,
          secure: false
        }
      }
    }
  }
})
