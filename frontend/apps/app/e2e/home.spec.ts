import { expect, test } from '@playwright/test'

test('首页展示后端返回的真实健康状态', async ({ page }) => {
  await page.goto('/')

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

test('受保护接口在浏览器里确实被保护', async ({ page }) => {
  await page.goto('/')

  // 没有登录流程就没有令牌，受保护接口必须拒绝——这证明它不是"浏览器能拿到就能读"
  await expect(page.getByTestId('identity-subject')).toHaveText('未登录')

  const response = await page.request.get('/api/v1/me')
  expect(response.status()).toBe(401)
})
