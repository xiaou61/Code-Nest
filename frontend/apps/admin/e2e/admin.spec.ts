import { expect, test } from '@playwright/test'

/**
 * 管理端的两条最小可信检查：管理区确实被拒、被拒页面上仍然展示后端的真实状态。
 * 前者证明守卫生效，后者证明"被拒"不是"什么都连不上"。
 */

test('管理区被拒，并说明当前身份', async ({ page }) => {
  await page.goto('/')

  await expect(page.getByTestId('admin-denied')).toBeVisible()
  await expect(page.getByTestId('admin-console')).toHaveCount(0)
  await expect(page.getByText('无管理权限')).toBeVisible()

  // 没有登录流程就没有令牌，会话必须落到"未登录"，而不是被当成某种默认角色放行
  await expect(page.getByTestId('admin-identity')).toHaveText('未登录（没有可用令牌）')
})

test('被拒页面上仍显示后端真实状态', async ({ page }) => {
  await page.goto('/')

  // 徽标取的是 /actuator/health 的真实返回，不是硬编码
  await expect(page.getByTestId('admin-health-status')).toHaveText('UP')
})

test('静态资源用相对路径，产物可放在任意来源下', async ({ page }) => {
  const response = await page.goto('/')
  expect(response?.status()).toBe(200)

  const html = await page.content()
  expect(html).not.toContain('src="/assets/')
})
