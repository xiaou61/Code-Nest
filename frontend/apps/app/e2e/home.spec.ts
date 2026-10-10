import { expect, test, type Page } from '@playwright/test'

/**
 * 学习者端的端到端检查。
 *
 * <p>**登录成了所有断言的入口**：整个学习者端要求登录，所以每个用例都以"登录种子账号"开场。
 * 原来那条"受保护接口在浏览器里确实被保护"的断言换了个形式：现在浏览器里已经带着令牌，
 * 因此用**不带令牌的直连请求**来证明接口仍然受保护——那才是这条断言真正要说的事。
 *
 * <p>种子账号来自 db/devdata（只有测试会加载它），口令是固定的测试值。
 */

const SEED_LEARNER = { identifier: 'seed-learner', password: 'seed-learner-password' }

async function signIn(page: Page) {
  await page.goto('/')
  await expect(page.getByTestId('login-form')).toBeVisible()
  await page.getByTestId('login-identifier').fill(SEED_LEARNER.identifier)
  await page.getByTestId('login-password').fill(SEED_LEARNER.password)
  await page.getByTestId('login-submit').click()
}

test('未登录访问学习者端会被导向登录页，登录后进入首页', async ({ page }) => {
  await page.goto('/')

  // 未登录时看到的是登录表单，而不是任何内容
  await expect(page.getByTestId('login-form')).toBeVisible()
  await expect(page.getByTestId('health-status')).toHaveCount(0)

  await signIn(page)

  // 登录后能看到原本要去的首页，并显示当前用户
  await expect(page.getByTestId('current-user')).toHaveText(SEED_LEARNER.identifier)
})

test('首页展示后端返回的真实健康状态', async ({ page }) => {
  await signIn(page)

  // 平台能力来自 Platform 端口，说明适配器链路是通的
  await expect(page.getByTestId('platform-kind')).toHaveText('web')

  // 必须是后端真实返回：状态值与原始响应体都来自接口
  await expect(page.getByTestId('health-status')).toHaveText('UP')
  await expect(page.getByTestId('health-raw')).toContainText('"status"')
  await expect(page.getByTestId('health-raw')).toContainText('liveness')
})

test('每个请求都带上后端返回的追踪标识', async ({ page }) => {
  const response = await page.request.get('/actuator/health')

  expect(response.status()).toBe(200)
  expect(response.headers()['x-trace-id']).toBeTruthy()
})

test('受保护接口在没有令牌时被拒，带着身份时返回主体', async ({ page }) => {
  // 不带 Authorization：能拿到接口地址，不等于能读到内容
  const unauthorized = await page.request.get('/api/v1/me')
  expect(unauthorized.status()).toBe(401)

  // 带着令牌（已登录）时它返回身份——两者对照才说明授权确实在起作用
  await signIn(page)
  await expect(page.getByTestId('identity-subject')).not.toHaveText('未登录')
})

test('登出后回到登录页', async ({ page }) => {
  await signIn(page)
  await expect(page.getByTestId('current-user')).toBeVisible()

  await page.getByTestId('logout').click()

  await expect(page.getByTestId('login-form')).toBeVisible()
})
