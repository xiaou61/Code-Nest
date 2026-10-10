/**
 * 把正文里的附件地址补成可访问的绝对地址。
 *
 * <p><b>为什么正文里存的是相对标识而不是绝对地址</b>：桌面壳的页面来源是
 * `http://127.0.0.1:5310`，与后端不同源；Web 端开发期走 Vite 代理、生产期同源。同一个
 * 相对标识在这三种情形下要拼出三个不同的地址，所以**地址在渲染时才算**，存进正文就死了。
 *
 * <p>已经是绝对的（`http://`、`https://`、`//`）或浏览器自管的（`data:`、`blob:`）一律原样返回——
 * 用户完全可以在正文里贴一张外站图片，那不是我们要改写的东西。
 */
export function resolveMediaUrl(src: string, baseUrl: string | undefined): string {
  if (src === '') {
    return src
  }
  if (/^(?:https?:)?\/\//i.test(src) || src.startsWith('data:') || src.startsWith('blob:')) {
    return src
  }
  const base = (baseUrl ?? '').replace(/\/+$/, '')
  if (base === '') {
    // 空基址表示同源：相对路径直接可用，不做任何改动
    return src
  }
  return `${base}${src.startsWith('/') ? '' : '/'}${src}`
}
