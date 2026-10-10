import { describe, expect, it } from 'vitest'

import { resolveMediaUrl } from './media'

describe('resolveMediaUrl', () => {
  it('leaves already-absolute and browser-managed URLs alone', () => {
    expect(resolveMediaUrl('https://cdn.example.com/a.png', 'http://api.test')).toBe(
      'https://cdn.example.com/a.png',
    )
    expect(resolveMediaUrl('//cdn.example.com/a.png', 'http://api.test')).toBe('//cdn.example.com/a.png')
    expect(resolveMediaUrl('data:image/png;base64,AAA', 'http://api.test')).toBe('data:image/png;base64,AAA')
    expect(resolveMediaUrl('blob:http://localhost/abc', 'http://api.test')).toBe('blob:http://localhost/abc')
  })

  it('prefixes the store-relative attachment path with the api base', () => {
    // 桌面壳与后端不同源，这个前缀就是它能不能显示图片的关键
    expect(resolveMediaUrl('/api/v1/knowledge/files/abc', 'http://127.0.0.1:8080')).toBe(
      'http://127.0.0.1:8080/api/v1/knowledge/files/abc',
    )
  })

  it('does not double the slash when the base ends with one', () => {
    expect(resolveMediaUrl('/api/v1/knowledge/files/abc', 'http://127.0.0.1:8080/')).toBe(
      'http://127.0.0.1:8080/api/v1/knowledge/files/abc',
    )
  })

  it('keeps the path as-is when there is no base (same origin)', () => {
    expect(resolveMediaUrl('/api/v1/knowledge/files/abc', '')).toBe('/api/v1/knowledge/files/abc')
    expect(resolveMediaUrl('/api/v1/knowledge/files/abc', undefined)).toBe('/api/v1/knowledge/files/abc')
  })

  it('adds a slash when a relative path does not start with one', () => {
    expect(resolveMediaUrl('api/v1/knowledge/files/abc', 'http://api.test')).toBe(
      'http://api.test/api/v1/knowledge/files/abc',
    )
  })

  it('passes the empty string through', () => {
    expect(resolveMediaUrl('', 'http://api.test')).toBe('')
  })
})
