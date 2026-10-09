import { defineConfig } from 'vitest/config'

export default defineConfig({
  test: {
    // 主题初始值取自 <html> 的类、切换要写 document，所以需要 DOM 环境
    environment: 'jsdom',
    include: ['src/**/*.test.{ts,tsx}'],
  },
})
