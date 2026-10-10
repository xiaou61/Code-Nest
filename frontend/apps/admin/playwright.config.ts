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
      // 与管理端的说明一致：数据源指向 paideia_test，并额外加载 db/devdata 里的种子账号
      env: {
        SPRING_DATASOURCE_URL:
          'jdbc:mysql://127.0.0.1:3307/paideia_test?useSSL=false&allowPublicKeyRetrieval=true&connectionTimeZone=UTC&characterEncoding=utf8',
        SPRING_FLYWAY_LOCATIONS: 'classpath:db/migration,classpath:db/devdata',
        // 与管理端说明一致：paideia_test 被三种迁移配置共用，夹具的 V900 需要被忽略
        SPRING_FLYWAY_IGNORE_MIGRATION_PATTERNS: '*:future,*:missing',
        SPRING_FLYWAY_OUT_OF_ORDER: 'true',
        // 管理端的来源必须在后端白名单里。**即使走 Vite 代理也一样**：
        // 浏览器对同源的 POST 也会带 Origin 头，后端按来源判定，未列入直接 403。
        // Actuator 端点有独立的一份 CORS 配置，两处都要给。
        PAIDEIA_WEB_CORS_ALLOWED_ORIGINS: 'http://127.0.0.1:5174,http://localhost:5174',
        MANAGEMENT_ENDPOINTS_WEB_CORS_ALLOWED_ORIGINS: 'http://127.0.0.1:5174,http://localhost:5174',
      },
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
