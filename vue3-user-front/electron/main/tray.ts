import { Tray, Menu, app, nativeImage, dialog } from 'electron'
import { join } from 'path'
import { showWindow } from './window'

let tray: Tray | null = null

export function createTray(): void {
  const iconPath = join(__dirname, '../../electron/resources/icon.png')
  const icon = nativeImage.createFromPath(iconPath)
  
  tray = new Tray(icon.resize({ width: 16, height: 16 }))
  
  const contextMenu = Menu.buildFromTemplate([
    {
      label: '打开 Code-Nest',
      click: () => {
        showWindow()
      }
    },
    {
      type: 'separator'
    },
    {
      label: '关于',
      click: () => {
        dialog.showMessageBox({
          type: 'info',
          title: '关于 Code-Nest',
          message: 'Code-Nest 用户端',
          detail: `版本: ${app.getVersion()}\n基于 Electron 构建`
        })
      }
    },
    {
      type: 'separator'
    },
    {
      label: '退出',
      click: () => {
        app.quit()
      }
    }
  ])

  tray.setToolTip('Code-Nest')
  tray.setContextMenu(contextMenu)

  tray.on('double-click', () => {
    showWindow()
  })
}

export function destroyTray(): void {
  if (tray) {
    tray.destroy()
    tray = null
  }
}
