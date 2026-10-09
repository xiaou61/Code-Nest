import { expect, test } from '@playwright/test'

/**
 * 展览页的验收：组件真的渲染出来了、覆盖层键盘可达、主题可切换且刷新后保持。
 * 这些是"设计系统没坏"的最小可信证据。
 */

const SECTIONS = [
  '字号阶梯',
  '层次与描边',
  'Button',
  '表单基元',
  '展示基元',
  'Table',
  'Empty 与 Alert',
  'Dialog / DropdownMenu / Tooltip',
  'Tabs',
  'Toast',
]

test('展览页渲染出全部组件节', async ({ page }) => {
  await page.goto('/')

  await expect(page.getByTestId('ui-kit-page')).toBeVisible()
  for (const title of SECTIONS) {
    await expect(page.getByRole('heading', { name: title, exact: true })).toBeVisible()
  }

  // 抽查几个有实际渲染结构的组件，避免"标题在但内容是空的"
  await expect(page.getByTestId('glass-sample')).toBeVisible()
  await expect(page.getByTestId('empty-state')).toBeVisible()
  await expect(page.getByTestId('alert-destructive')).toBeVisible()
  await expect(page.getByRole('table').first()).toBeVisible()
})

test('对话框键盘可打开、Esc 可关闭', async ({ page }) => {
  await page.goto('/')

  const trigger = page.getByRole('button', { name: '打开对话框' })
  await trigger.focus()
  await page.keyboard.press('Enter')

  const dialog = page.getByTestId('dialog-content')
  await expect(dialog).toBeVisible()

  await page.keyboard.press('Escape')
  await expect(dialog).toBeHidden()
})

test('下拉菜单可打开', async ({ page }) => {
  await page.goto('/')

  await page.getByRole('button', { name: '打开菜单' }).click()
  await expect(page.getByRole('menu')).toBeVisible()
  await expect(page.getByRole('menuitem', { name: '个人资料' })).toBeVisible()
})

test('通知可触发', async ({ page }) => {
  await page.goto('/')

  await page.getByRole('button', { name: '普通通知' }).click()
  await expect(page.getByText('已保存')).toBeVisible()
})

test('主题切换后刷新保持，且首屏不闪', async ({ page }) => {
  await page.goto('/')

  // Playwright 默认 colorScheme 是 light，且每个用例的存储是隔离的，因此初始为浅色
  await expect(page.getByTestId('current-theme')).toHaveText('当前主题：浅色')

  await page.getByTestId('theme-toggle').click()
  await expect(page.getByTestId('current-theme')).toHaveText('当前主题：深色')
  await expect(page.locator('html')).toHaveClass(/dark/)

  await page.reload()

  // 刷新后仍是深色，且**未经任何交互** html 上就已经有 dark 类 ——
  // 说明是首屏脚本落的，不是 React 挂载后才补上的（那会先闪一帧浅色）
  await expect(page.locator('html')).toHaveClass(/dark/)
  await expect(page.getByTestId('current-theme')).toHaveText('当前主题：深色')
})
