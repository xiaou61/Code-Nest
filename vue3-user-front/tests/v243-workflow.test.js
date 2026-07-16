import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const projectRoot = resolve(import.meta.dirname, '..')

function readProjectSource(...segments) {
  return readFileSync(resolve(projectRoot, ...segments), 'utf8')
}

test('first login should route an incomplete target profile through onboarding', () => {
  const authSource = readProjectSource('src', 'views', 'auth', 'Auth.vue')
  const routerSource = readProjectSource('src', 'router', 'index.js')

  assert.match(authSource, /careerLoopApi\.getCurrent\(\)/)
  assert.match(authSource, /router\.push\('\/onboarding'\)/)
  assert.match(routerSource, /path:\s*'\/onboarding'/)
  assert.match(routerSource, /views\/onboarding\/Index\.vue/)
})

test('onboarding should save the target profile and create the first weekly task plan', () => {
  const source = readProjectSource('src', 'views', 'onboarding', 'Index.vue')

  assert.match(source, /careerLoopApi\.updateProfile/)
  assert.match(source, /growthAutopilotApi\.generate/)
  assert.match(source, /currentStage/)
  assert.match(source, /learning-cockpit/)
})

test('career loop should expose automatic status and reserve sync for recovery', () => {
  const source = readProjectSource('src', 'views', 'career-loop', 'Index.vue')

  assert.match(source, /自动同步/)
  assert.match(source, /恢复同步/)
  assert.doesNotMatch(source, />手动同步</)
})

test('home should load one overview contract instead of independently requesting every module', () => {
  const source = readProjectSource('src', 'utils', 'home-data.js')
  const apiSource = readProjectSource('src', 'api', 'home.js')

  assert.match(source, /homeApi\.getOverview/)
  assert.doesNotMatch(source, /Promise\.allSettled/)
  assert.match(apiSource, /\/user\/home\/overview/)
})

test('Element Plus should use Vite component resolution while keeping the loading directive available', () => {
  const mainSource = readProjectSource('src', 'main.js')
  const viteSource = readProjectSource('vite.config.js')

  assert.doesNotMatch(mainSource, /app\.use\(ElementPlus\)/)
  assert.match(mainSource, /app\.directive\('loading', ElLoading\.directive\)/)
  assert.match(viteSource, /unplugin-vue-components\/vite/)
  assert.match(viteSource, /ElementPlusResolver/)
})
