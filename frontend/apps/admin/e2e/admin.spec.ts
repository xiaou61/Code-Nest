import { expect, test, type Page } from '@playwright/test'

/**
 * 管理端的端到端检查。
 *
 * <p>三条最小可信检查：未登录看到登录视图、非管理员被明确拒绝、管理员放行；
 * 以及被拒状态下仍能证明接口链路是通的（后端状态徽标取真实返回）。
 *
 * <p>种子账号来自 db/devdata（只有测试会加载它）。注册产生的账号一律是学习者，
 * 所以 `seed-admin` 是本项里唯一能进管理区的账号。
 */

const SEED_LEARNER = { identifier: 'seed-learner', password: 'seed-learner-password' }
const SEED_ADMIN = { identifier: 'seed-admin', password: 'seed-admin-password' }

async function signIn(page: Page, account: { identifier: string; password: string }) {
  await page.goto('/')
  await expect(page.getByTestId('admin-sign-in')).toBeVisible()
  await page.getByTestId('login-identifier').fill(account.identifier)
  await page.getByTestId('login-password').fill(account.password)
  await page.getByTestId('login-submit').click()
}

test('未登录时显示登录视图，而不是管理区', async ({ page }) => {
  await page.goto('/')

  await expect(page.getByTestId('admin-sign-in')).toBeVisible()
  await expect(page.getByTestId('admin-console')).toHaveCount(0)
  await expect(page.getByTestId('admin-denied')).toHaveCount(0)
})

test('学习者登录后被明确拒绝，并显示当前身份', async ({ page }) => {
  await signIn(page, SEED_LEARNER)

  await expect(page.getByTestId('admin-denied')).toBeVisible()
  await expect(page.getByTestId('admin-console')).toHaveCount(0)
  await expect(page.getByText('无管理权限')).toBeVisible()
  await expect(page.getByTestId('admin-identity')).toHaveText('学习者')
})

test('管理员登录后进入管理区', async ({ page }) => {
  await signIn(page, SEED_ADMIN)

  await expect(page.getByTestId('admin-console')).toBeVisible()
  await expect(page.getByTestId('admin-denied')).toHaveCount(0)
  await expect(page.getByTestId('admin-current-user')).toHaveText(SEED_ADMIN.identifier)
})

test('被拒页面上仍显示后端真实状态', async ({ page }) => {
  await signIn(page, SEED_LEARNER)

  // 徽标取的是 /actuator/health 的真实返回，不是硬编码；被拒不等于什么都连不上
  await expect(page.getByTestId('admin-health-status')).toHaveText('UP')
})

test('静态资源用相对路径，产物可放在任意来源下', async ({ page }) => {
  const response = await page.goto('/')
  expect(response?.status()).toBe(200)

  const html = await page.content()
  expect(html).not.toContain('src="/assets/')
})
