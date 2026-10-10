import { createDesktopPlatform } from '@paideia/platform-desktop'
import { createWebPlatform } from '@paideia/platform-web'

/**
 * 宿主实例。单独成文件是因为认证接线与页面都要用它，而它依赖构建模式。
 *
 * <p>只有这里知道宿主是什么：桌面构建注入 preload 桥，其余走浏览器实现。
 * `platform-desktop` 不 import Electron 的任何东西（只读 preload 注入的 `window.paideiaDesktop`），
 * 所以静态引入它打进 Web 产物也无害。
 */
export const platform = import.meta.env.MODE === 'desktop' ? createDesktopPlatform() : createWebPlatform()
