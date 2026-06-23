import test from 'node:test'
import assert from 'node:assert/strict'

import {
  normalizeBodyRequestConfig,
  normalizeQueryRequestConfig
} from '../src/utils/request-options.js'

test('normalizeQueryRequestConfig should keep direct query params', () => {
  assert.deepEqual(normalizeQueryRequestConfig({ page: 1, size: 20 }), {
    params: { page: 1, size: 20 }
  })
})

test('normalizeQueryRequestConfig should unwrap axios-style params config', () => {
  assert.deepEqual(normalizeQueryRequestConfig({ params: { limit: 10 } }), {
    params: { limit: 10 }
  })
})

test('normalizeQueryRequestConfig should preserve delete request bodies', () => {
  assert.deepEqual(normalizeQueryRequestConfig({ data: [1, 2, 3] }), {
    data: [1, 2, 3]
  })
})

test('normalizeBodyRequestConfig should treat plain third-argument objects as query params', () => {
  assert.deepEqual(normalizeBodyRequestConfig({ status: 1 }), {
    params: { status: 1 }
  })
})

test('normalizeBodyRequestConfig should preserve axios config options', () => {
  assert.deepEqual(normalizeBodyRequestConfig({
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  }), {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
})
