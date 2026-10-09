import { renderToString } from 'react-dom/server'

import { PublicPage } from './PublicPage'

/** 供预渲染脚本调用：产出与客户端首屏一致的 HTML。 */
export function render(): string {
  return renderToString(<PublicPage />)
}
