import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './tests/e2e',
  outputDir: '../output/playwright/test-results',
  timeout: 30_000,
  use: {
    baseURL: process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:5173',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'off'
  },
  projects: [
    {
      name: 'mobile-chromium',
      use: {
        ...devices['iPhone 13'],
        browserName: 'chromium',
        channel: process.env.CI ? undefined : 'chrome',
        viewport: { width: 390, height: 844 }
      }
    }
  ],
  webServer: {
    command: 'npm run dev2 -- --host 127.0.0.1 --port 5173',
    url: 'http://127.0.0.1:5173/design-system/components',
    reuseExistingServer: !process.env.CI,
    timeout: 120_000
  }
})
