import { hydrateRoot } from 'react-dom/client'

import { PublicPage } from './PublicPage'

const container = document.getElementById('root')
if (container === null) {
  throw new Error('找不到挂载节点 #root')
}

// 预渲染已经写好了 HTML，这里只做接管，不要用 createRoot 把内容清掉重建
hydrateRoot(container, <PublicPage />)
