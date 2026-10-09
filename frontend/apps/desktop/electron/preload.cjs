/**
 * Electron preload：把桌面壳的能力以最小面积暴露给渲染进程。
 *
 * 沙箱下这里只能 require('electron')，读不了文件，所以渲染来源与缓存快照
 * 都由主进程经启动参数传进来。
 * 暴露面只包含 Platform 端口真正需要的方法——不暴露 Node、不暴露文件系统。
 */
const { contextBridge, ipcRenderer } = require('electron')

function readArgument(prefix) {
  return process.argv.find((argument) => argument.startsWith(prefix))?.slice(prefix.length) ?? ''
}

const rendererOrigin = readArgument('--paideia-renderer-origin=')

/**
 * 解析主进程注入的缓存快照。
 *
 * 用 atob + TextDecoder 而不是 Buffer：沙箱化的 preload 只有 Web 全局，
 * 没有完整的 Node 运行时。任何解析失败都退回空对象——首屏不能因为缓存读不出来而起不来。
 */
function readCacheSnapshot() {
  const encoded = readArgument('--paideia-cache-snapshot=')
  if (encoded === '') {
    return {}
  }
  try {
    const binary = atob(encoded)
    const bytes = Uint8Array.from(binary, (character) => character.charCodeAt(0))
    const parsed = JSON.parse(new TextDecoder().decode(bytes))
    return parsed !== null && typeof parsed === 'object' ? parsed : {}
  } catch {
    return {}
  }
}

contextBridge.exposeInMainWorld('paideiaDesktop', {
  rendererOrigin,
  cacheSnapshot: readCacheSnapshot(),
  cache: {
    get: (key) => ipcRenderer.invoke('cache:get', String(key)),
    set: (key, value) => ipcRenderer.invoke('cache:set', String(key), String(value)),
    remove: (key) => ipcRenderer.invoke('cache:remove', String(key)),
  },
})
