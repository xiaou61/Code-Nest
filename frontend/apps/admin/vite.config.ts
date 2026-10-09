import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { themeInitPlugin } from '@paideia/ui/vite'
import { defineConfig } from 'vite'

const BACKEND = 'http://127.0.0.1:8080'

/**
 * 管理端应用。仅 Web：桌面壳只消费 apps/app 的产物，因此本应用**按构造**不进安装包。
 *
 * 端口与 apps/app（5173）、apps/ui-kit（5175）错开，便于同时起多个面互相核对。
 * 其余（相对基址、首屏主题脚本、开发期代理）与学习者端保持一致——同一个设计系统
 * 在不同宿主下不应表现不同。
 */
export default defineConfig({
  base: './',
  plugins: [react(), tailwindcss(), themeInitPlugin()],
  server: {
    port: 5174,
    proxy: {
      '/api': { target: BACKEND, changeOrigin: true },
      '/actuator': { target: BACKEND, changeOrigin: true },
    },
  },
  preview: {
    port: 5174,
  },
})
