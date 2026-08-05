import test from 'node:test'
import assert from 'node:assert/strict'

import {
  ApiError,
  classifyApiFailure,
  getApiErrorMessage,
  unwrapApiResponse
} from '../src/index.js'

test('unwrapApiResponse returns payload from a successful envelope', () => {
  assert.deepEqual(unwrapApiResponse({
    code: 200,
    message: 'ok',
    data: { id: 7 },
    timestamp: 1
  }), { id: 7 })
})

test('unwrapApiResponse preserves responses that do not use the envelope', () => {
  assert.deepEqual(unwrapApiResponse({ id: 7 }), { id: 7 })
  assert.equal(unwrapApiResponse(null), null)
})

test('unwrapApiResponse raises a typed business error', () => {
  assert.throws(
    () => unwrapApiResponse({ code: 603, message: 'already exists', data: null, timestamp: 1 }),
    (error) => {
      assert.ok(error instanceof ApiError)
      assert.equal(error.kind, 'conflict')
      assert.equal(error.code, 603)
      assert.equal(error.httpStatus, null)
      assert.equal(error.message, 'already exists')
      return true
    }
  )
})

test('classifyApiFailure reads an error envelope from a rejected HTTP response', () => {
  const error = classifyApiFailure({
    response: {
      status: 401,
      data: { code: 702, message: 'expired', data: null, timestamp: 1 }
    }
  })

  assert.ok(error instanceof ApiError)
  assert.equal(error.kind, 'authentication')
  assert.equal(error.code, 702)
  assert.equal(error.httpStatus, 401)
  assert.equal(error.message, 'expired')
})

test('login failures do not trigger the expired-session flow', () => {
  const error = classifyApiFailure({
    response: {
      status: 401,
      data: { code: 705, message: 'invalid credentials', data: null, timestamp: 1 }
    }
  })

  assert.equal(error.kind, 'business')
  assert.equal(error.code, 705)
})

test('classifyApiFailure has stable categories for rate limits and unavailable services', () => {
  assert.equal(classifyApiFailure({ response: { status: 429, data: {} } }).kind, 'rate-limit')
  assert.equal(classifyApiFailure({ response: { status: 503, data: {} } }).kind, 'unavailable')
  assert.equal(classifyApiFailure({ code: 'ECONNABORTED' }).kind, 'timeout')
  assert.equal(classifyApiFailure({ code: 'ERR_NETWORK' }).kind, 'network')
})

test('getApiErrorMessage supports typed and legacy errors', () => {
  assert.equal(getApiErrorMessage(new ApiError('failed', { kind: 'business' })), 'failed')
  assert.equal(getApiErrorMessage(new Error('legacy')), 'legacy')
  assert.equal(getApiErrorMessage({}, 'fallback'), 'fallback')
})
