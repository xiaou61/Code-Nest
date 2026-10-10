/**
 * 正文标题 → 锚点 id。
 *
 * <p><b>为什么自己写而不是引 `rehype-slug`</b>：它解决的问题只是"给标题一个 id"，
 * 十几行就够；我们也不需要与 GitHub 完全一致的 slug 规则。少一个依赖少一份升级负担。
 *
 * <p><b>目录与正文必须算出同一批 id</b>：两边都按**同一个顺序**遍历同一批标题，
 * 因此用同一个 {@link createSlugger} 就能得到一致结果。这也是为什么要限制在 1—4 级：
 * 若一边索引 h1—h4、另一边只索引 h2—h3，重复标题的序号会错位。
 */

export interface Heading {
  id: string
  text: string
  level: number
}

/** 能生成锚点的标题层级上限。 */
export const MAX_HEADING_LEVEL = 4

/** 单个标题 → 基础 id。中文保留（浏览器对 URL fragment 支持中文）。 */
export function slugify(text: string): string {
  const base = text
    .trim()
    .toLowerCase()
    .replace(/\s+/g, '-')
    .replace(/[^\p{L}\p{N}-]/gu, '')
  // 空标题（例如一连串标点）也要有 id，否则锚点跳转落到别处
  return base === '' ? 'section' : base
}

/**
 * 有状态的 slug 生成器：**重复标题追加序号**。
 *
 * <p>不追加序号的话，一篇里两个"常见误解"会得到同一个 id，目录里点第二个会跳到第一个——
 * 这类问题在长正文里很常见，而且不会报错。
 */
export function createSlugger(): (text: string) => string {
  const used = new Map<string, number>()
  return (text: string): string => {
    const base = slugify(text)
    const seen = used.get(base) ?? 0
    used.set(base, seen + 1)
    return seen === 0 ? base : `${base}-${seen}`
  }
}

const FENCE = /^\s*(```|~~~)/

/**
 * 从 markdown 源码里扫出标题，生成目录。
 *
 * <p>按行扫描并**跳过围栏代码块**——代码块里的 `# 注释` 不是标题，把 Python 或 shell 注释
 * 当成标题会让目录里冒出一堆无关条目。行扫描不处理"引用块里的标题"这类少见写法，
 * 这是刻意的：正文形态由管理员用 markdown 写，不是任意 HTML。
 */
export function extractHeadings(markdown: string): Heading[] {
  const nextSlug = createSlugger()
  const headings: Heading[] = []
  let inFence = false

  for (const line of markdown.split(/\r?\n/)) {
    if (FENCE.test(line)) {
      inFence = !inFence
      continue
    }
    if (inFence) {
      continue
    }
    const match = /^(#{1,4})\s+(.+?)\s*#*\s*$/.exec(line)
    if (match === null) {
      continue
    }
    const level = match[1]?.length ?? 0
    const text = (match[2] ?? '').replace(/[*_`]/g, '').trim()
    if (text === '') {
      continue
    }
    headings.push({ id: nextSlug(text), text, level })
  }
  return headings
}
