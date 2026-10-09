import { readFileSync } from 'node:fs'
import path from 'node:path'

import { describe, expect, it } from 'vitest'

/**
 * 对比度检查。令牌是手写的感观值，改一个数字就可能把某处文字压到 AA 以下，
 * 而那种问题肉眼看不出来（尤其在半透明层上）。所以直接在测试里算 WCAG 对比度。
 *
 * 为什么算"合成后颜色"：半透明层上的文字落在合成色上，而不是落在令牌原值上；
 * 按原值算会高估对比度。
 *
 * 已知未覆盖：静止状态的 input/border 是极细的低对比线（这是刻意的观感方向，
 * Geist 自己的 --ds-gray-400 就是 hsl(0 0% 92%)）。WCAG 1.4.11 真正要求的是**焦点指示**
 * 可辨识，所以这里断言的是 ring 合成后的对比度；若连静止边框也提到 3:1，观感会明显变重。
 */

const CSS_PATH = path.join(process.cwd(), 'src', 'styles', 'globals.css')

interface Color {
  rgb: [number, number, number]
  alpha: number
}

/** 解析 :root 与 .dark 里的颜色令牌，oklch 与 hsl 两种写法都支持。 */
function parseTokens(css: string, selector: string): Record<string, Color> {
  // 必须按"行首的选择器 {"去匹配，不能简单 indexOf('.dark')：
  // 文件上方 @custom-variant 那行里也有 ".dark"，否则会静默解析成 :root，
  // 于是两个主题都拿浅色的值去比，深色永远测不出来（本文件踩过一次）。
  const escaped = selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const blockStart = new RegExp(`^\\s*${escaped}\\s*\\{`, 'm').exec(css)
  if (blockStart === null) {
    throw new Error(`globals.css 里找不到 ${selector} 的令牌块`)
  }
  const open = css.indexOf('{', blockStart.index)
  const close = css.indexOf('}', open)
  const body = css.slice(open + 1, close)

  const tokens: Record<string, Color> = {}
  for (const line of body.split(';')) {
    const declaration = /--([a-z0-9-]+)\s*:\s*(oklch|hsl)\(([^)]*)\)/.exec(line)
    if (declaration === null) {
      continue
    }
    const name = declaration[1]
    const space = declaration[2]
    const [channelPart = '', alphaPart] = (declaration[3] ?? '').trim().split(/\s*\/\s*/)
    const channels = channelPart.trim().split(/\s+/)
    const rawAlpha = alphaPart?.trim()
    const alpha =
      rawAlpha === undefined || rawAlpha === ''
        ? 1
        : rawAlpha.endsWith('%')
          ? Number(rawAlpha.slice(0, -1)) / 100
          : Number(rawAlpha)
    if (name === undefined) {
      continue
    }
    const rgb =
      space === 'oklch'
        ? oklchToSrgb(Number(channels[0]), Number(channels[1]), Number(channels[2]))
        : hslToSrgb(Number(channels[0]), Number(channels[1]?.replace('%', '')), Number(channels[2]?.replace('%', '')))
    tokens[name] = { rgb, alpha }
  }
  return tokens
}

/** OKLCH → 线性 sRGB → sRGB（gamma 编码），通道范围 0..1。 */
function oklchToSrgb(l: number, c: number, h: number): [number, number, number] {
  const hue = (h * Math.PI) / 180
  const a = c * Math.cos(hue)
  const b = c * Math.sin(hue)

  const ll = (l + 0.3963377774 * a + 0.2158037573 * b) ** 3
  const mm = (l - 0.1055613458 * a - 0.0638541728 * b) ** 3
  const ss = (l - 0.0894841775 * a - 1.291485548 * b) ** 3

  return [
    encodeGamma(4.0767416621 * ll - 3.3077115913 * mm + 0.2309699292 * ss),
    encodeGamma(-1.2684380046 * ll + 2.6097574011 * mm - 0.3413193965 * ss),
    encodeGamma(-0.0041960863 * ll - 0.7034186147 * mm + 1.707614701 * ss),
  ]
}

function hslToSrgb(h: number, s: number, l: number): [number, number, number] {
  const saturation = s / 100
  const lightness = l / 100
  const chroma = (1 - Math.abs(2 * lightness - 1)) * saturation
  const sector = (h / 60) % 6
  const second = chroma * (1 - Math.abs((sector % 2) - 1))
  const offset = lightness - chroma / 2

  const table: [number, number, number][] = [
    [chroma, second, 0],
    [second, chroma, 0],
    [0, chroma, second],
    [0, second, chroma],
    [second, 0, chroma],
    [chroma, 0, second],
  ]
  const base = table[Math.floor(sector) % 6] ?? [0, 0, 0]
  return [base[0] + offset, base[1] + offset, base[2] + offset]
}

function encodeGamma(value: number): number {
  const clamped = Math.min(1, Math.max(0, value))
  return clamped <= 0.0031308 ? clamped * 12.92 : 1.055 * clamped ** (1 / 2.4) - 0.055
}

/** CSS 的 alpha 合成发生在 sRGB（非线性）空间，与浏览器行为一致。 */
function composite(top: Color, bottom: [number, number, number]): [number, number, number] {
  return [
    top.alpha * top.rgb[0] + (1 - top.alpha) * bottom[0],
    top.alpha * top.rgb[1] + (1 - top.alpha) * bottom[1],
    top.alpha * top.rgb[2] + (1 - top.alpha) * bottom[2],
  ]
}

function relativeLuminance(rgb: [number, number, number]): number {
  const linear = rgb.map((channel) =>
    channel <= 0.04045 ? channel / 12.92 : ((channel + 0.055) / 1.055) ** 2.4,
  ) as [number, number, number]
  return 0.2126 * linear[0] + 0.7152 * linear[1] + 0.0722 * linear[2]
}

function contrastRatio(a: number, b: number): number {
  const lighter = Math.max(a, b)
  const darker = Math.min(a, b)
  return (lighter + 0.05) / (darker + 0.05)
}

const css = readFileSync(CSS_PATH, 'utf8')
const TEXT_MINIMUM = 4.5

describe.each([
  { name: '浅色', selector: ':root' },
  { name: '深色', selector: '.dark' },
])('$name 主题的对比度', ({ selector }) => {
  const tokens = parseTokens(css, selector)
  const background = relativeLuminance(tokens['background']!.rgb)
  const card = relativeLuminance(tokens['card']!.rgb)
  const solid = relativeLuminance(tokens['card-solid']!.rgb)
  // 半透明玻璃层上的文字：最坏情形是它叠在页面底色上（页面底色就是它背后最亮/最暗的东西）
  const glass = relativeLuminance(composite(tokens['glass']!, tokens['background']!.rgb))

  it('正文在页面底色、卡面、不透明底与玻璃层上都达到 4.5:1', () => {
    const foreground = relativeLuminance(tokens['foreground']!.rgb)

    expect(contrastRatio(foreground, background)).toBeGreaterThanOrEqual(TEXT_MINIMUM)
    expect(contrastRatio(foreground, card)).toBeGreaterThanOrEqual(TEXT_MINIMUM)
    expect(contrastRatio(foreground, solid)).toBeGreaterThanOrEqual(TEXT_MINIMUM)
    expect(contrastRatio(foreground, glass)).toBeGreaterThanOrEqual(TEXT_MINIMUM)
  })

  it('次要文本在页面底色、卡面与玻璃层上都达到 4.5:1', () => {
    const muted = relativeLuminance(tokens['muted-foreground']!.rgb)

    expect(contrastRatio(muted, background)).toBeGreaterThanOrEqual(TEXT_MINIMUM)
    expect(contrastRatio(muted, card)).toBeGreaterThanOrEqual(TEXT_MINIMUM)
    expect(contrastRatio(muted, glass)).toBeGreaterThanOrEqual(TEXT_MINIMUM)
  })

  it('主按钮与次级面上的文字都达到 4.5:1', () => {
    const onPrimary = relativeLuminance(tokens['primary-foreground']!.rgb)
    const primary = relativeLuminance(tokens['primary']!.rgb)
    const onSecondary = relativeLuminance(tokens['secondary-foreground']!.rgb)
    const secondary = relativeLuminance(tokens['secondary']!.rgb)

    expect(contrastRatio(onPrimary, primary)).toBeGreaterThanOrEqual(TEXT_MINIMUM)
    expect(contrastRatio(onSecondary, secondary)).toBeGreaterThanOrEqual(TEXT_MINIMUM)
  })

  it('危险色当文字用（提示与危险按钮）也达到 4.5:1', () => {
    // 组件里危险色是以文字形式出现的：Alert 的 text-destructive 落在卡面上，
    // 危险按钮是 bg-destructive/10 + text-destructive，底色近于卡面。
    const destructive = relativeLuminance(tokens['destructive']!.rgb)

    expect(contrastRatio(destructive, card)).toBeGreaterThanOrEqual(TEXT_MINIMUM)
  })

  it('焦点指示（ring 合成后）达到 UI 组件要求的 3:1', () => {
    const ring = relativeLuminance(composite(tokens['ring']!, tokens['background']!.rgb))

    expect(contrastRatio(ring, background)).toBeGreaterThanOrEqual(3)
  })
})
