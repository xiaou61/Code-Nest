import { defineConfig } from 'vitest/config'

export default defineConfig({
  test: {
    // 认证包要测表单与上下文，需要 DOM 环境
    environment: 'jsdom',
    include: ['src/**/*.test.{ts,tsx}'],
  },
})
