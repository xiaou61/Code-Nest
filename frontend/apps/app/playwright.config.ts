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
      /**
       * 两处覆盖，环境变量优先级高于 ./config/application-local.yml 里的配置：
       * ① 数据源指向 paideia_test —— 端到端测试会注册账号、造登录记录，
       *    不该写进开发用的 paideia 库；
       * ② 额外加载 db/devdata —— 种子账号（seed-learner / seed-admin）在那里，
       *    它**不在**任何 profile 的默认迁移目录里，只有这里显式加上才会执行。
       */
      env: {
        SPRING_DATASOURCE_URL:
          'jdbc:mysql://127.0.0.1:3307/paideia_test?useSSL=false&allowPublicKeyRetrieval=true&connectionTimeZone=UTC&characterEncoding=utf8',
        SPRING_FLYWAY_LOCATIONS: 'classpath:db/migration,classpath:db/devdata',
        // paideia_test 被三种迁移配置共用（见 backend 的 application-test.yml 注释）：
        // 夹具用的 V900 会出现在这个库里，而本次 locations 不含 db/testdata，
        // 不忽略它就会被判成"已应用但本地找不到"而起不来。
        SPRING_FLYWAY_IGNORE_MIGRATION_PATTERNS: '*:future,*:missing',
        SPRING_FLYWAY_OUT_OF_ORDER: 'true',
        // 学习者端的来源显式给出：浏览器对同源 POST 也会带 Origin 头，
        // 后端按来源判定，未列入直接 403。Actuator 端点另有一份 CORS 配置。
        PAIDEIA_WEB_CORS_ALLOWED_ORIGINS: 'http://127.0.0.1:5173,http://localhost:5173',
        MANAGEMENT_ENDPOINTS_WEB_CORS_ALLOWED_ORIGINS: 'http://127.0.0.1:5173,http://localhost:5173',
      },
      url: 'http://127.0.0.1:8080/actuator/health',
      // 注意：若本机已有一个后端在跑，这里会复用它，上面的 env 就不生效，
      // 于是 e2e 会打到那个后端连接的库（通常是 paideia）而找不到种子账号。跑之前先确认没有在跑的后端。
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
