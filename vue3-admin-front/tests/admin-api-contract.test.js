import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const projectRoot = resolve(import.meta.dirname, '..')
const communityApiSource = readFileSync(resolve(projectRoot, 'src/api/community.js'), 'utf8')
const knowledgeApiSource = readFileSync(resolve(projectRoot, 'src/api/knowledge.js'), 'utf8')
const knowledgeMapsViewSource = readFileSync(resolve(projectRoot, 'src/views/knowledge/maps/index.vue'), 'utf8')
const logApiSource = readFileSync(resolve(projectRoot, 'src/api/log.js'), 'utf8')
const moyuApiSource = readFileSync(resolve(projectRoot, 'src/api/moyu.js'), 'utf8')
const requestSource = readFileSync(resolve(projectRoot, 'src/utils/request.js'), 'utf8')

test('community admin actions should match backend HTTP method contracts', () => {
  assert.match(
    communityApiSource,
    /export function unbanUser\(id\) \{\s*return request\.delete\(`\/admin\/community\/users\/\$\{id\}\/ban`\)\s*\}/
  )
  assert.match(
    communityApiSource,
    /export function toggleCategoryStatus\(id\) \{\s*return request\.patch\(`\/admin\/community\/categories\/\$\{id\}\/status`\)\s*\}/
  )
  assert.match(
    communityApiSource,
    /export function toggleTagStatus\(id\) \{\s*return request\.patch\(`\/admin\/community\/tags\/\$\{id\}\/status`\)\s*\}/
  )
})

test('admin request wrapper should expose PATCH for backend patch endpoints', () => {
  assert.match(requestSource, /\bpatch\(url, data = \{\}, config = \{\}\) \{/)
})

test('knowledge map batch delete should use the backend DELETE batch endpoint', () => {
  assert.match(
    knowledgeApiSource,
    /export function deleteBatchKnowledgeMaps\(ids\) \{\s*return request\.delete\('\/admin\/knowledge\/maps\/batch', \{ data: ids \}\)\s*\}/
  )
  assert.doesNotMatch(knowledgeApiSource, /\/admin\/knowledge\/maps\/batch-delete/)
})

test('knowledge maps view should not expose unsupported copy action', () => {
  assert.doesNotMatch(knowledgeMapsViewSource, /copyKnowledgeMap/)
  assert.doesNotMatch(knowledgeMapsViewSource, /command="copy"/)
})

test('operation log batch delete should send ids in the DELETE request body', () => {
  assert.match(
    logApiSource,
    /deleteOperationLogs\(ids\) \{\s*return request\.delete\('\/log\/operation', \{ data: ids \}\)\s*\}/
  )
})

test('moyu admin API should not expose stale or missing backend contracts', () => {
  assert.match(
    moyuApiSource,
    /getEventList\(params\) \{\s*return request\.get\('\/admin\/moyu\/developer-calendar\/events', params\)\s*\}/
  )
  assert.match(
    moyuApiSource,
    /batchDeleteEvents\(ids\) \{\s*return request\.post\('\/admin\/moyu\/developer-calendar\/events\/batch-delete', ids\)\s*\}/
  )
  assert.match(
    moyuApiSource,
    /batchDeleteContents\(ids\) \{\s*return request\.post\('\/admin\/moyu\/daily-content\/batch-delete', ids\)\s*\}/
  )
  assert.match(
    moyuApiSource,
    /collectContent\(id\) \{\s*return request\.post\(`\/moyu\/daily-content\/\$\{id\}\/toggle-collection`\)\s*\}/
  )
  assert.match(
    moyuApiSource,
    /getUserDailyContent\(params\) \{\s*return request\.get\('\/moyu\/daily-content\/recommend', params\)\s*\}/
  )
  assert.doesNotMatch(moyuApiSource, /\/admin\/moyu\/developer-calendar\/events\/list/)
  assert.doesNotMatch(moyuApiSource, /events\/batch-delete', \{ ids \}/)
  assert.doesNotMatch(moyuApiSource, /daily-content\/batch-delete', \{ ids \}/)
  assert.doesNotMatch(moyuApiSource, /request\.get\('\/moyu\/developer-calendar\/events'/)
  assert.doesNotMatch(moyuApiSource, /request\.get\('\/moyu\/daily-content'/)
  assert.doesNotMatch(moyuApiSource, /\/moyu\/daily-content\/\$\{id\}\/collect/)
  assert.doesNotMatch(moyuApiSource, /\/admin\/moyu\/statistics\/user-behavior/)
  assert.doesNotMatch(moyuApiSource, /\/admin\/moyu\/statistics\/trend\/\$\{type\}/)
})
