import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const pageSource = readFileSync(
  resolve(import.meta.dirname, '..', 'src', 'views', 'notification', 'index.vue'),
  'utf8'
)

test('user notifications consume the shared response contract', () => {
  assert.match(pageSource, /NotificationRecord/)
  assert.match(pageSource, /PageResult<NotificationRecord>/)
  assert.doesNotMatch(pageSource, /interface NotificationMessage/)
  assert.doesNotMatch(pageSource, /\.createTime/)
})
