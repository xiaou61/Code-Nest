/**
 * Electron preload：把桌面壳的能力以最小面积暴露给渲染进程。
 *
 * 沙箱下这里只能 require('electron')，所以渲染来源通过启动参数传入。
 * 暴露面只包含 Platform 端口真正需要的方法——不暴露 Node、不暴露文件系统。
 */
const { contextBridge, ipcRenderer } = require('electron')

const ORIGIN_PREFIX = '--paideia-renderer-origin='
const rendererOrigin =
  process.argv.find((argument) => argument.startsWith(ORIGIN_PREFIX))?.slice(ORIGIN_PREFIX.length) ?? ''

contextBridge.exposeInMainWorld('paideiaDesktop', {
  rendererOrigin,
  cache: {
    get: (key) => ipcRenderer.invoke('cache:get', String(key)),
    set: (key, value) => ipcRenderer.invoke('cache:set', String(key), String(value)),
    remove: (key) => ipcRenderer.invoke('cache:remove', String(key)),
  },
})
