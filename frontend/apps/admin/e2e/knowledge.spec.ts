import { expect, test, type Page } from '@playwright/test'

/**
 * 管理端知识库的端到端检查：**录入 → 预览 → 发布 → 学习者能看到**。
 *
 * <p>这条链路是本项的核心交付，也是唯一能把"管理端写、学习者读"两侧的契约同时钉住的地方。
 * 发布的动作就是一次把状态置为已发布的保存，管理端没有单独的发布接口。
 *
 * <p>数据用带时间戳的唯一名字，跑完删掉：种子库是共享的，重跑时不能让上一次的残留
 * （分类 slug 唯一）把自己顶掉。
 */

const SEED_ADMIN = { identifier: 'seed-admin', password: 'seed-admin-password' }

async function signIn(page: Page, account: { identifier: string; password: string }) {
  await page.goto('/')
  await expect(page.getByTestId('admin-sign-in')).toBeVisible()
  await page.getByTestId('login-identifier').fill(account.identifier)
  await page.getByTestId('login-password').fill(account.password)
  await page.getByTestId('login-submit').click()
  await expect(page.getByTestId('admin-console')).toBeVisible()
}

test('非管理员进不了管理区', async ({ page }) => {
  await page.goto('/')
  await page.getByTestId('login-identifier').fill('seed-learner')
  await page.getByTestId('login-password').fill('seed-learner-password')
  await page.getByTestId('login-submit').click()

  await expect(page.getByTestId('admin-denied')).toBeVisible()
  await expect(page.getByTestId('admin-console')).toHaveCount(0)
})

test('管理员从首页进知识库管理区，完成录入到发布的链路', async ({ page }) => {
  const stamp = Date.now()
  const categoryName = `e2e 分类 ${stamp}`
  const entryTitle = `e2e 条目 ${stamp}`

  await signIn(page, SEED_ADMIN)

  // 门与路由：首页的入口真的能到管理区
  await page.getByTestId('admin-knowledge-link').click()
  await expect(page.getByTestId('admin-category-list')).toBeVisible()

  // 建分类
  await page.getByTestId('admin-category-name').fill(categoryName)
  await page.getByTestId('admin-category-slug').fill(`e2e-${stamp}`)
  await page.getByTestId('admin-category-create').click()
  await expect(page.getByTestId('admin-category-list')).toContainText(categoryName)

  // 建草稿
  await page.getByTestId('admin-entry-new').click()
  await page.getByTestId('admin-entry-title').fill(entryTitle)
  await page.getByTestId('admin-entry-body').fill('## 小节标题\n\n这是正文。')
  await page.getByTestId('admin-entry-save').click()

  const row = page.locator('li', { hasText: entryTitle }).first()
  await expect(row).toContainText('草稿')

  // 草稿预览：与学习者端共用同一个渲染组件，因此这里能看到渲染后的标题而不是 markdown 源码
  await row.getByRole('button', { name: '编辑' }).click()
  await page.getByTestId('admin-entry-preview-toggle').click()
  await expect(page.getByTestId('admin-entry-preview')).toContainText('小节标题')
  await expect(page.getByTestId('admin-entry-preview')).not.toContainText('## 小节标题')
  await page.getByTestId('admin-entry-preview-toggle').click()

  // 发布：把状态改成已发布再保存
  await page.getByTestId('admin-entry-status').selectOption('published')
  await page.getByTestId('admin-entry-save').click()
  await expect(page.locator('li', { hasText: entryTitle }).first()).toContainText('已发布')

  // 收尾：删掉本次造的数据
  await page.locator('li', { hasText: entryTitle }).first().getByRole('button', { name: '删除' }).click()
  await expect(page.getByTestId('admin-entry-list')).not.toContainText(entryTitle)
  await page
    .locator('li', { hasText: categoryName })
    .first()
    .getByRole('button', { name: '删除' })
    .click()
  await expect(page.getByTestId('admin-category-list')).not.toContainText(categoryName)
})
