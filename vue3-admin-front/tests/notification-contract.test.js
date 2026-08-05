import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const pageSource = readFileSync(
  resolve(import.meta.dirname, '..', 'src', 'views', 'notification', 'index.vue'),
  'utf8'
)

test('admin notifications consume backend-owned shared DTO names', () => {
  assert.match(pageSource, /NotificationRecord/)
  assert.match(pageSource, /NotificationStatistics/)
  assert.match(pageSource, /NotificationTemplateRecord/)
  assert.match(pageSource, /titleTemplate/)
  assert.match(pageSource, /contentTemplate/)
  assert.match(pageSource, /createdTime/)
  assert.doesNotMatch(pageSource, /unwrapApiData/)
  assert.doesNotMatch(pageSource, /interface MessageRecord/)
})
