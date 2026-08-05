import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const projectRoot = resolve(import.meta.dirname, '..')
const designSystemRoot = resolve(projectRoot, '..', 'code-nest-design-system')
const apiContractRoot = resolve(projectRoot, '..', 'code-nest-api-contract')

function readProjectSource(...segments) {
  return readFileSync(resolve(projectRoot, ...segments), 'utf8')
}

function readDesignSystemSource(...segments) {
  return readFileSync(resolve(designSystemRoot, ...segments), 'utf8')
}

test('mobile navigation drawer should render above the top bar and scroll its contents', () => {
  const source = readDesignSystemSource('src', 'components', 'navigation', 'CnTopNav.vue')

  assert.match(source, /<el-drawer[\s\S]*?append-to-body/)
  assert.match(source, /\.cn-top-nav-mobile \.el-drawer__body\s*{[\s\S]*?overflow-y:\s*auto/)
  assert.match(source, /@media \(max-width: 1120px\)/)
})

test('mobile authentication should place the form before the marketing panel', () => {
  const source = readProjectSource('src', 'views', 'auth', 'Auth.vue')
  const mobileLayout = source.slice(source.indexOf('@media (max-width: 1100px)'))

  assert.match(mobileLayout, /\.auth-form-panel\s*{[\s\S]*?order:\s*1/)
  assert.match(mobileLayout, /\.auth-brand-panel\s*{[\s\S]*?order:\s*2/)
})

test('primary navigation should be organized around today, learning, career, and community', () => {
  const source = readProjectSource('src', 'config', 'navigation.js')
  const primaryNav = source.slice(source.indexOf('export const primaryNavItems'), source.indexOf('export const learningMenuGroups'))

  assert.match(primaryNav, /path:\s*'\/'[\s\S]*?label:\s*'今天'/)
  assert.match(primaryNav, /path:\s*'\/learning-cockpit'[\s\S]*?label:\s*'学习'/)
  assert.match(primaryNav, /path:\s*'\/career-loop'[\s\S]*?label:\s*'求职'/)
  assert.match(primaryNav, /path:\s*'\/community'[\s\S]*?label:\s*'社区'/)
  assert.match(source, /label:\s*'练习与工具'/)
  assert.match(source, /label:\s*'更多'/)
})

test('career loop should show the next action before summary analytics without implying a reset', () => {
  const source = readProjectSource('src', 'views', 'career-loop', 'Index.vue')

  assert.ok(source.indexOf('title="下一步行动"') < source.indexOf('class="summary-grid"'))
  assert.match(source, /开始求职准备/)
  assert.doesNotMatch(source, /重启闭环会话|闭环会话已重启/)
})

test('interview page should put question sets ahead of the learning heatmap', () => {
  const source = readProjectSource('src', 'views', 'interview', 'Index.vue')

  assert.doesNotMatch(source, /title="当前页码"/)
  assert.ok(source.indexOf('class="question-set-section') < source.indexOf('<LearningHeatmap'))
  assert.match(source, /\.interview-summary-grid\s*{[\s\S]*?grid-template-columns:\s*repeat\(3, minmax\(0, 1fr\)\)/)
})

test('home aggregate requests should suppress duplicate global failure messages', () => {
  const requestOptionsSource = readFileSync(resolve(apiContractRoot, 'src', 'index.js'), 'utf8')
  const requestSource = readProjectSource('src', 'utils', 'request.js')
  const homeDataSource = readProjectSource('src', 'utils', 'home-data.js')

  assert.match(requestOptionsSource, /'silent'/)
  assert.match(requestSource, /config\?\.silent/)
  assert.match(homeDataSource, /HOME_REQUEST_CONFIG/)
  assert.match(homeDataSource, /silent:\s*true/)
})
