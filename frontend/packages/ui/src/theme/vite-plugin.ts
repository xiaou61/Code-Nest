import type { Plugin } from 'vite'

/*
 * 这里的相对导入必须带 .ts 扩展名，全仓唯一一处，不要"顺手统一掉"：
 * vite 配置加载器把工作区包当作外部依赖交给 Node 直接加载，而 Node 的 ESM 解析
 * 不做无扩展名的补全 —— 少了这个扩展名，任何应用启动都会报 ERR_MODULE_NOT_FOUND。
 * 该扩展名要求 tsconfig.base 打开 allowImportingTsExtensions。
 */
import { THEME_STORAGE_KEY } from './keys.ts'

/**
 * 首屏防闪烁脚本。
 *
 * <p>它必须在浏览器首次绘制之前把主题类落到 <html> 上，所以只能是内联脚本，
 * 不能等打包后的模块执行 —— 那之间已经绘制过一帧，用户会看到浅色闪一下再变深。
 *
 * <p>取值优先级：桌面壳的缓存快照 → localStorage → 系统偏好。
 * 桌面壳走快照是因为它把缓存放在主进程（渲染进程读缓存要经 IPC，是异步的，
 * 首屏脚本等不了）；Web 走 localStorage。
 */
const THEME_INIT_SCRIPT = `
(function () {
  var KEY = 'theme';
  var STORAGE_KEY = '${THEME_STORAGE_KEY}';
  var stored = null;
  try {
    var snapshot = window.paideiaDesktop && window.paideiaDesktop.cacheSnapshot;
    if (snapshot && typeof snapshot[KEY] === 'string') {
      stored = snapshot[KEY];
    }
  } catch (error) {
    /* 快照不可用就退回后面的来源，不能因此挡住首屏 */
  }
  if (stored === null) {
    try {
      stored = window.localStorage.getItem(STORAGE_KEY);
    } catch (error) {
      stored = null;
    }
  }
  var prefersDark = false;
  try {
    prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
  } catch (error) {
    /* 拿不到系统偏好时按浅色处理 */
  }
  var dark = stored === 'dark' || (stored !== 'light' && prefersDark);
  document.documentElement.classList.toggle('dark', dark);
})();
`.trim()

/**
 * 把首屏脚本注入每个应用的 HTML。由 `@paideia/ui/vite` 导出，各应用在 vite 配置里加一行。
 *
 * <p>放在 ui 包里导出，而不是在四个 index.html 里各抄一份：四份拷贝必然漂移，
 * 而漂移的症状（某个端首屏闪一下）很难被注意到。
 */
export function themeInitPlugin(): Plugin {
  return {
    name: 'paideia:theme-init',
    transformIndexHtml() {
      return [
        {
          tag: 'script',
          children: THEME_INIT_SCRIPT,
          injectTo: 'head-prepend',
        },
      ]
    },
  }
}
