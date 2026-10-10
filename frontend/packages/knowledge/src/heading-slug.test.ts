import { describe, expect, it } from 'vitest'

import { createSlugger, extractHeadings, slugify } from './heading-slug'

describe('slugify', () => {
  it('keeps Chinese characters and lowers latin', () => {
    expect(slugify('常见误解')).toBe('常见误解')
    expect(slugify('Java Concurrency')).toBe('java-concurrency')
  })

  it('drops punctuation that would break a fragment', () => {
    expect(slugify('它解决什么？')).toBe('它解决什么')
    expect(slugify('A / B')).toBe('a--b')
  })

  it('still yields an id for punctuation-only titles', () => {
    // 空 id 会让锚点落到别处，不如给一个占位
    expect(slugify('？！')).toBe('section')
  })
})

describe('createSlugger', () => {
  it('appends an ordinal for repeated titles', () => {
    const next = createSlugger()
    expect(next('常见误解')).toBe('常见误解')
    expect(next('常见误解')).toBe('常见误解-1')
    expect(next('常见误解')).toBe('常见误解-2')
  })

  it('does not let one title consume another title’s id', () => {
    const next = createSlugger()
    expect(next('甲')).toBe('甲')
    expect(next('乙')).toBe('乙')
    expect(next('甲')).toBe('甲-1')
  })
})

describe('extractHeadings', () => {
  it('collects levels 1 to 4 in document order', () => {
    const headings = extractHeadings('# 一\n\n## 二\n\n### 三\n\n#### 四\n\n##### 五\n')
    expect(headings.map((heading) => heading.level)).toEqual([1, 2, 3, 4])
    expect(headings.map((heading) => heading.text)).toEqual(['一', '二', '三', '四'])
  })

  it('skips fenced code blocks so shell comments are not mistaken for headings', () => {
    const markdown = ['## 真标题', '', '```bash', '# 这是注释不是标题', '```', '', '## 另一个真标题'].join('\n')
    expect(extractHeadings(markdown).map((heading) => heading.text)).toEqual(['真标题', '另一个真标题'])
  })

  it('strips markdown emphasis from the displayed text', () => {
    expect(extractHeadings('## 带**强调**的标题').map((heading) => heading.text)).toEqual(['带强调的标题'])
  })

  it('produces the same ids as walking the headings through one slugger', () => {
    // 目录与正文各自调用 extractHeadings，两边必须得到同一批 id，否则目录点了没反应
    const markdown = '## 常见误解\n\n## 常见误解\n'
    const first = extractHeadings(markdown).map((heading) => heading.id)
    const second = extractHeadings(markdown).map((heading) => heading.id)
    expect(first).toEqual(['常见误解', '常见误解-1'])
    expect(second).toEqual(first)
  })
})
