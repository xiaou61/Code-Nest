import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const projectRoot = resolve(import.meta.dirname, '..')
const moyuApiSource = readFileSync(resolve(projectRoot, 'src/api/moyu.js'), 'utf8')
const dailyContentControllerSource = readFileSync(
  resolve(projectRoot, '../xiaou-moyu/src/main/java/com/xiaou/moyu/controller/DailyContentController.java'),
  'utf8'
)

test('daily content collection API should use the backend toggle-collection endpoint', () => {
  assert.match(
    moyuApiSource,
    /export const collectContent = \(id\) => \{\s*return request\.post\(`\/moyu\/daily-content\/\$\{id\}\/toggle-collection`\)\s*\}/
  )
  assert.doesNotMatch(moyuApiSource, /\/moyu\/daily-content\/\$\{id\}\/collect/)
})

test('daily content detail API should use an available GET detail endpoint', () => {
  assert.match(
    moyuApiSource,
    /export const getContentDetail = \(id\) => \{\s*return request\.get\(`\/moyu\/daily-content\/\$\{id\}`\)\s*\}/
  )
  assert.match(dailyContentControllerSource, /@GetMapping\("\/\{contentId\}"\)/)
})
