import { expect, test, type Page } from '@playwright/test'

/**
 * 学习者端知识库的端到端检查。
 *
 * <p>数据来自 db/devdata 的种子（两级分类、三条已发布、一条草稿、两条关系）。断言都挑
 * **只有真实链路跑通才可能成立**的点：草稿不可见、中文检索能命中、标记刷新后仍在。
 *
 * <p>地址用的是 Hash 路由（`/#/knowledge`）——桌面壳从自定义协议加载页面，路径路由在那
 * 种来源下会失效，所以整个应用用 Hash。
 */

const SEED_LEARNER = { identifier: 'seed-learner', password: 'seed-learner-password' }

async function signIn(page: Page, account: { identifier: string; password: string }) {
  await page.goto('/')
  await expect(page.getByTestId('login-identifier')).toBeVisible()
  await page.getByTestId('login-identifier').fill(account.identifier)
  await page.getByTestId('login-password').fill(account.password)
  await page.getByTestId('login-submit').click()
}

test('未登录访问知识库被导向登录页', async ({ page }) => {
  await page.goto('/#/knowledge')

  await expect(page.getByTestId('login-identifier')).toBeVisible()
  await expect(page.getByTestId('entry-list')).toHaveCount(0)
})

test('按分类浏览并打开条目，正文与目录都渲染出来', async ({ page }) => {
  await signIn(page, SEED_LEARNER)
  await page.goto('/#/knowledge')

  // 分类树来自后端真实返回
  await expect(page.getByTestId('category-tree')).toBeVisible()
  await expect(page.getByTestId('category-backend-basics')).toBeVisible()
  await expect(page.getByTestId('entry-list')).toBeVisible()

  // 条目 3 的正文里有表格：markdown 真的被渲染成元素，而不是把源码当纯文本显示
  await page.getByTestId('entry-link-3').click()
  await expect(page.getByRole('heading', { name: '模块化单体的边界' })).toBeVisible()
  await expect(page.getByTestId('markdown-body').getByRole('table')).toBeVisible()
  await expect(page.getByTestId('markdown-body')).not.toContainText('## 边界靠什么保证')
  // 它只有一个二级标题，而目录刻意在标题少于两个时不出现（没有导航价值）
  await expect(page.getByTestId('entry-toc')).toHaveCount(0)

  // 条目 1 有两个标题：目录该出现，且点击能滚动而不改变路由
  await page.goto('/#/knowledge/entries/1')
  await expect(page.getByTestId('entry-toc')).toBeVisible()
  const hashBefore = new URL(page.url()).hash
  await page.getByTestId('entry-toc').getByRole('button', { name: '常见误解' }).click()
  // Hash 路由下地址栏的 hash 就是路由；目录若用 href="#x" 会跳到空白页
  expect(new URL(page.url()).hash).toBe(hashBefore)
})

test('同分类内的上一篇／下一篇可用', async ({ page }) => {
  await signIn(page, SEED_LEARNER)
  // 条目 1 与「接口与实现分离」同属分类 1，且后者更晚发布（见 devdata 的 V952）。
  // 别的已发布条目各占一个分类，所以那两个字段只有在同分类有两篇时才非空。
  await page.goto('/#/knowledge/entries/1')

  await expect(page.getByTestId('entry-next')).toContainText('接口与实现分离')
  await page.getByTestId('entry-next').getByRole('link').click()
  await expect(page.getByRole('heading', { name: '接口与实现分离' })).toBeVisible()
  await expect(page.getByTestId('entry-previous')).toContainText('什么是依赖注入')
})

test('草稿对学习者不可见', async ({ page }) => {
  await signIn(page, SEED_LEARNER)
  await page.goto('/#/knowledge')

  // 条目 4 是种子里的草稿：列表里没有，直接开地址也打不开
  await expect(page.getByTestId('entry-list')).not.toContainText('虚拟线程')
  await page.goto('/#/knowledge/entries/4')
  await expect(page.getByTestId('entry-state')).toContainText('打不开这条内容')
})

test('中文关键词能搜到，且搜不到草稿', async ({ page }) => {
  await signIn(page, SEED_LEARNER)
  await page.goto('/#/knowledge')

  await page.getByTestId('knowledge-search').fill('依赖注入')
  await page.getByTestId('knowledge-search-submit').click()
  await expect(page.getByTestId('entry-list')).toContainText('什么是依赖注入')

  // 「草稿」只出现在未发布条目的正文里；学习者侧任何列表都不该返回它
  await page.getByTestId('knowledge-search').fill('草稿')
  await page.getByTestId('knowledge-search-submit').click()
  await expect(page.getByTestId('entry-empty')).toBeVisible()
})

test('自评标记刷新后仍在，并出现在「我标记过的内容」里', async ({ page }) => {
  await signIn(page, SEED_LEARNER)
  await page.goto('/#/knowledge/entries/1')

  const understood = page.getByTestId('self-assessment-understood')
  await expect(understood).toBeVisible()
  // 先清干净：种子库可能带着上一次运行留下的标记
  if ((await understood.getAttribute('aria-pressed')) === 'true') {
    await understood.click()
    await expect(understood).toHaveAttribute('aria-pressed', 'false')
  }

  await understood.click()
  await expect(understood).toHaveAttribute('aria-pressed', 'true')

  // 刷新后仍在——这条才证明标记真的落库了，而不是只存在内存里
  await page.reload()
  await expect(page.getByTestId('self-assessment-understood')).toHaveAttribute('aria-pressed', 'true')

  await page.goto('/#/my-assessments')
  await expect(page.getByTestId('my-assessment-list')).toContainText('什么是依赖注入')

  // 收尾：留下标记会污染后续断言（这个端点本来就是幂等的）
  await page.goto('/#/knowledge/entries/1')
  await page.getByTestId('self-assessment-understood').click()
  await expect(page.getByTestId('self-assessment-understood')).toHaveAttribute('aria-pressed', 'false')
})
