import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { themeInitPlugin } from '@paideia/ui/vite'
import { defineConfig } from 'vite'

const BACKEND = 'http://127.0.0.1:8080'

export default defineConfig({
  /**
   * 相对基址：桌面壳从自定义协议加载产物，绝对路径（/assets/...）会全部 404。
   * 这个值不能改成 '/'。
   */
  base: './',
  plugins: [react(), tailwindcss(), themeInitPlugin()],
  server: {
    port: 5173,
    // 开发期由 Vite 代理转发到后端，省去跨域配置。
    // 桌面壳没有代理，那时必须把它的来源加入后端的 CORS 允许列表。
    proxy: {
      '/api': { target: BACKEND, changeOrigin: true },
      '/actuator': { target: BACKEND, changeOrigin: true },
    },
  },
})
