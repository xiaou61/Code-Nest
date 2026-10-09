import { defineConfig, devices } from '@playwright/test'

/** 后端工作目录与产物。运行 e2e 前需要先在 backend 目录执行 `mvn -B -DskipTests package`。 */
const BACKEND_DIR = '../../../backend'
const BACKEND_JAR = 'paideia-app/target/paideia-app-0.0.1-SNAPSHOT.jar'

export default defineConfig({
  testDir: './e2e',
  // 后端是有状态资源，并行执行会让多个 worker 抢同一端口与同一份数据
  fullyParallel: false,
  workers: 1,
  reporter: [['list']],
  use: {
    baseURL: 'http://127.0.0.1:5173',
    // 首次重试才留 trace：稳定运行时零开销，失败时会话可完整回放
    trace: 'on-first-retry',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],

  /**
   * 同时拉起后端与前端。只起前端会拿到代理错误而不是后端真实返回，
   * 那验证不了「页面展示的是接口真实数据」这条验收标准。
   */
  webServer: [
    {
      /**
       * 用 local profile 启动后端：应用现在必须有数据源才能起来，
       * 而数据源连接信息在被 gitignore 的 backend/config/application-local.yml 里（指向本机 3307 隧道）。
       * 因此 e2e 的前置条件是那条 SSH 隧道是通的。
       */
      command: `java -jar ${BACKEND_JAR} --spring.profiles.active=local`,
      // 必须在 backend 目录启动：Spring Boot 从工作目录读 ./config/ 下的本地配置
      cwd: BACKEND_DIR,
      url: 'http://127.0.0.1:8080/actuator/health',
      reuseExistingServer: true,
      timeout: 120_000,
    },
    {
      command: 'pnpm dev',
      url: 'http://127.0.0.1:5173',
      reuseExistingServer: true,
      timeout: 60_000,
    },
  ],
})
