import { expect, test } from '@playwright/test'

test.use({
  viewport: { width: 390, height: 844 },
  isMobile: true,
  hasTouch: true
})

test('mobile navigation drawer is teleported, viewport-height, and scrollable', async ({ page }) => {
  await page.goto('/design-system/components')

  await page.getByRole('banner').getByRole('button', { name: '打开导航菜单' }).click()

  const drawer = page.locator('.el-drawer:visible').filter({
    has: page.getByRole('button', { name: '关闭' })
  })
  await expect(drawer).toBeVisible()

  const isTeleportedToBody = await drawer.evaluate((element) => element.closest('#app') === null)
  expect(isTeleportedToBody).toBe(true)

  const drawerBox = await drawer.boundingBox()
  expect(drawerBox).not.toBeNull()
  expect(drawerBox.height).toBeGreaterThanOrEqual(840)

  const drawerBody = drawer.locator('.el-drawer__body')
  await expect(drawerBody).toHaveCSS('overflow-y', 'auto')
  const metrics = await drawerBody.evaluate((element) => ({
    clientHeight: element.clientHeight,
    scrollHeight: element.scrollHeight
  }))
  expect(metrics.scrollHeight).toBeGreaterThan(metrics.clientHeight)
})
