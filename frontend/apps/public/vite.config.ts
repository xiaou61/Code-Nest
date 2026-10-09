import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  base: './',
  plugins: [react()],
  ssr: {
    /**
     * 工作区内的包直接导出 TypeScript 源码，被当成外部依赖时 Node 无法直接加载。
     * 必须让 Vite 把它打进 SSR 产物。
     */
    noExternal: ['@paideia/ui'],
  },
})
