import { describe, expect, it } from 'vitest'

import { canAccessAdmin, createSessionReader } from './session'

/** 造一个只有载荷是真的、签名是假的令牌——这正是"客户端解析不校验签名"的意思。 */
function tokenWithPayload(payload: unknown): string {
  const body = Buffer.from(JSON.stringify(payload), 'utf8').toString('base64url')
  return `eyJhbGciOiJub25lIn0.${body}.not-a-real-signature`
}

describe('会话与角色', () => {
  it('没有令牌时是未登录', () => {
    expect(createSessionReader().current()).toBeNull()
    expect(createSessionReader({ getToken: () => null }).current()).toBeNull()
    expect(createSessionReader({ getToken: () => '' }).current()).toBeNull()
  })

  it('令牌畸形时是未登录，而不是抛错', () => {
    for (const token of ['abc', 'a.b', 'a.b.c.d.e', 'a.!!!not-base64!!!.c', 'a.bm90LWpzb24.c']) {
      expect(createSessionReader({ getToken: () => token }).current(), token).toBeNull()
    }
  })

  it('载荷里没有角色时按学习者处理（失败即拒绝）', () => {
    const reader = createSessionReader({ getToken: () => tokenWithPayload({ sub: 'u-1' }) })

    expect(reader.current()).toEqual({ role: 'learner', signedIn: true })
  })

  it('角色不可识别时也按学习者处理', () => {
    const reader = createSessionReader({
      getToken: () => tokenWithPayload({ sub: 'u-1', role: 'teacher' }),
    })

    expect(reader.current()?.role).toBe('learner')
  })

  it('角色为 admin 时放行', () => {
    const reader = createSessionReader({
      getToken: () => tokenWithPayload({ sub: 'u-1', role: 'admin' }),
    })
    const session = reader.current()

    expect(session).toEqual({ role: 'admin', signedIn: true })
    expect(canAccessAdmin(session)).toBe(true)
  })

  it('学习者与未登录都不能进管理端', () => {
    const learner = createSessionReader({
      getToken: () => tokenWithPayload({ role: 'learner' }),
    }).current()

    expect(canAccessAdmin(learner)).toBe(false)
    expect(canAccessAdmin(null)).toBe(false)
  })
})
