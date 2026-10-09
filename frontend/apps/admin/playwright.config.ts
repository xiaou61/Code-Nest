import { defineConfig, devices } from '@playwright/test'

const BACKEND_DIR = '../../../backend'
const BACKEND_JAR = 'paideia-app/target/paideia-app-0.0.1-SNAPSHOT.jar'

/**
 * 管理端的端到端检查。与学习者端一样同时拉起后端：要证明的是"徽标显示的是接口真实返回"，
 * 只起前端会拿到代理错误，验证不了这一点。
 *
 * 前置条件：后端 jar 已构建，且到服务器 MySQL 的本地隧道（127.0.0.1:3307）是通的。
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  reporter: [['list']],
  use: {
    baseURL: 'http://127.0.0.1:5174',
    trace: 'on-first-retry',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: [
    {
      command: `java -jar ${BACKEND_JAR} --spring.profiles.active=local`,
      cwd: BACKEND_DIR,
      url: 'http://127.0.0.1:8080/actuator/health',
      reuseExistingServer: true,
      timeout: 120_000,
    },
    {
      command: 'pnpm dev',
      url: 'http://127.0.0.1:5174',
      reuseExistingServer: true,
      timeout: 60_000,
    },
  ],
})
