import { app, BrowserWindow, ipcMain, shell } from 'electron'
import { createWindow, getMainWindow } from './window'
import { createTray } from './tray'
import { initStore, getStore } from './store'

// 单例锁定
const gotTheLock = app.requestSingleInstanceLock()

if (!gotTheLock) {
  app.quit()
} else {
  app.on('second-instance', () => {
    const mainWindow = getMainWindow()
    if (mainWindow) {
      if (mainWindow.isMinimized()) mainWindow.restore()
      mainWindow.focus()
    }
  })

  app.whenReady().then(() => {
    // 初始化存储
    initStore()

    // 创建主窗口
    createWindow()

    // 创建系统托盘
    createTray()

    // 设置 IPC 事件处理
    setupIPC()
  })
}

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow()
  }
})

// IPC 事件处理
function setupIPC() {
  // 窗口控制
  ipcMain.handle('window:minimize', () => {
    const win = getMainWindow()
    win?.minimize()
  })

  ipcMain.handle('window:maximize', () => {
    const win = getMainWindow()
    if (win?.isMaximized()) {
      win.unmaximize()
    } else {
      win?.maximize()
    }
  })

  ipcMain.handle('window:close', () => {
    const store = getStore()
    const minimizeToTray = store?.get('minimizeToTray', true)
    const win = getMainWindow()
    
    if (minimizeToTray) {
      win?.hide()
    } else {
      win?.close()
    }
  })

  ipcMain.handle('window:isMaximized', () => {
    return getMainWindow()?.isMaximized()
  })

  // 系统功能
  ipcMain.handle('shell:openExternal', async (_event, url: string) => {
    await shell.openExternal(url)
  })

  // 应用信息
  ipcMain.handle('app:getVersion', () => {
    return app.getVersion()
  })

  // 设置
  ipcMain.handle('settings:get', () => {
    const store = getStore()
    return {
      minimizeToTray: store?.get('minimizeToTray', true),
      autoStart: store?.get('autoStart', false)
    }
  })

  ipcMain.handle('settings:set', (_event, settings: Record<string, any>) => {
    const store = getStore()
    Object.entries(settings).forEach(([key, value]) => {
      store?.set(key, value)
    })
    
    // 处理开机自启动
    if ('autoStart' in settings) {
      app.setLoginItemSettings({
        openAtLogin: settings.autoStart
      })
    }
  })
}
