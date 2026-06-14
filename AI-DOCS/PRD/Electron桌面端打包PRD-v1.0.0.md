# Electron 桌面端打包 PRD v1.0.0

## 📋 项目概述

### 🎯 项目背景
Code-Nest 目前包含两个前端项目：
- **vue3-admin-front**: 管理端（基于 Vue3 + Vite + Element Plus）
- **vue3-user-front**: 用户端（基于 Vue3 + Vite + Element Plus）

为了提升用户体验、支持离线使用场景、提供原生桌面能力，计划使用 Electron 将这两个 Web 应用打包为桌面客户端（Windows .exe / macOS .dmg / Linux .AppImage）。

### 💡 核心价值
- **独立运行**: 无需打开浏览器，双击即可启动应用
- **原生体验**: 系统托盘、全局快捷键、通知推送等原生能力
- **离线支持**: 部分功能支持离线使用（如本地缓存、离线查看）
- **自动更新**: 支持应用版本自动检测和更新
- **品牌塑造**: 独立应用图标、启动画面，增强品牌辨识度
- **安全增强**: 可控的窗口环境，减少浏览器扩展干扰

## 🚀 功能需求

### 1. 基础功能

#### 1.1 应用窗口管理
- **自定义标题栏**: 自定义窗口标题栏样式（可选无边框设计）
- **窗口控制**: 最小化、最大化、关闭、置顶等操作
- **窗口状态记忆**: 记住上次关闭时的窗口大小和位置
- **多窗口支持**: 支持同时打开多个功能窗口

#### 1.2 系统集成
- **系统托盘**: 最小化到系统托盘，后台运行
- **托盘菜单**: 右键托盘图标显示快捷操作菜单
- **开机自启**: 可选择开机自动启动应用
- **全局快捷键**: 自定义全局快捷键唤起应用
- **系统通知**: 支持原生系统通知推送

#### 1.3 自动更新
- **版本检测**: 启动时自动检测新版本
- **增量更新**: 支持增量更新减少下载量
- **静默更新**: 后台下载，重启时自动应用
- **更新日志**: 显示版本更新内容

### 2. 管理端特有功能

#### 2.1 数据导出
- **本地导出**: 数据导出到本地文件（Excel/CSV/JSON）
- **报表打印**: 支持直接调用系统打印功能
- **批量下载**: 文件批量下载到指定目录

#### 2.2 开发者工具
- **调试模式**: 开发环境可打开 DevTools
- **日志查看**: 查看应用运行日志
- **性能监控**: 内存和CPU使用情况监控

### 3. 用户端特有功能

#### 3.1 学习体验优化
- **专注模式**: 无干扰的沉浸式学习界面
- **阅读模式**: 护眼模式、字体调整等
- **笔记本地化**: 笔记内容本地存储备份

#### 3.2 快捷操作
- **快捷搜索**: 全局快捷键快速搜索
- **剪贴板增强**: 代码片段快速复制
- **截图功能**: 内置截图并上传功能

## 🎨 界面设计

### 1. 应用启动画面 (Splash Screen)
```
+------------------------------------------+
|                                          |
|              [Code-Nest Logo]            |
|                                          |
|             Code-Nest v1.0.0             |
|                                          |
|         ████████████░░░░░░░ 60%          |
|            正在加载资源...                |
|                                          |
+------------------------------------------+
```

### 2. 自定义标题栏
```
+----------------------------------------------------------+
| [Logo] Code-Nest 管理端                    [_] [□] [×]    |
+----------------------------------------------------------+
|                                                          |
|                    (Web Content Area)                    |
|                                                          |
+----------------------------------------------------------+
```

### 3. 系统托盘菜单
```
+-------------------------+
| ✓ Code-Nest            |
+-------------------------+
| 📊 打开管理端           |
| 👤 打开用户端           |
+-------------------------+
| ⚙️  设置               |
| 📝 检查更新            |
| 📋 关于               |
+-------------------------+
| 🚪 退出               |
+-------------------------+
```

### 4. 设置界面
```
+----------------------------------------------------------+
| ⚙️ 应用设置                                               |
+----------------------------------------------------------+
| 通用设置                                                  |
|   [x] 开机自动启动                                        |
|   [x] 关闭时最小化到托盘                                  |
|   [x] 启动时检查更新                                      |
|                                                          |
| 外观设置                                                  |
|   主题: [跟随系统 ▼]                                      |
|   窗口透明度: [████████░░] 80%                            |
|                                                          |
| 快捷键设置                                                |
|   唤起应用: [Ctrl + Shift + N]                           |
|   快捷搜索: [Ctrl + Shift + S]                           |
|                                                          |
| 网络设置                                                  |
|   服务器地址: [http://localhost:8080]                    |
|   [ ] 使用代理                                           |
|                                                          |
| [恢复默认]                              [保存设置]        |
+----------------------------------------------------------+
```

## 🔧 技术方案

### 1. 技术栈选型
| 类别 | 技术选型 | 说明 |
|------|---------|------|
| 桌面框架 | Electron 28+ | 成熟稳定，社区活跃 |
| 构建工具 | electron-vite | 专为 Electron + Vite 设计 |
| 打包工具 | electron-builder | 支持多平台打包 |
| 自动更新 | electron-updater | 配合 GitHub Releases 或私有服务器 |
| 本地存储 | electron-store | 配置和数据本地持久化 |
| 进程通信 | IPC (preload script) | 安全的主进程与渲染进程通信 |

### 2. 项目结构设计
```
code-nest-desktop/
├── electron/                    # Electron 主进程代码
│   ├── main/
│   │   ├── index.ts            # 主进程入口
│   │   ├── window.ts           # 窗口管理
│   │   ├── tray.ts             # 系统托盘
│   │   ├── updater.ts          # 自动更新
│   │   ├── ipc.ts              # IPC 事件处理
│   │   └── store.ts            # 本地存储
│   ├── preload/
│   │   └── index.ts            # 预加载脚本
│   └── resources/              # 静态资源
│       ├── icon.ico            # Windows 图标
│       ├── icon.icns           # macOS 图标
│       └── icon.png            # Linux 图标
├── src/                        # 渲染进程代码（Vue3 应用）
│   └── (从原 vue3-xxx-front 迁移)
├── build/                      # 打包配置
│   ├── entitlements.mac.plist  # macOS 权限配置
│   └── installer.nsh           # Windows 安装脚本
├── electron-builder.yml        # electron-builder 配置
├── electron.vite.config.ts     # electron-vite 配置
├── package.json
└── tsconfig.json
```

### 3. 开发方案选择

**方案A: 独立项目（推荐）**
- 创建两个独立的 Electron 项目
- `code-nest-admin-desktop/` - 管理端桌面应用
- `code-nest-user-desktop/` - 用户端桌面应用
- 优点：独立维护、独立发布、代码清晰
- 缺点：部分代码需要重复

**方案B: 统一项目**
- 单一 Electron 项目，内置两个应用
- 启动时选择打开管理端或用户端
- 优点：统一维护、共享代码
- 缺点：打包体积大、耦合度高

**推荐方案A**，理由：
1. 管理端和用户端面向不同用户群体
2. 独立项目更易于版本管理和发布
3. 安装包体积更小
4. 权限控制更清晰

### 4. 关键技术实现

#### 4.1 主进程入口 (main/index.ts)
```typescript
import { app, BrowserWindow, ipcMain } from 'electron'
import { createWindow } from './window'
import { createTray } from './tray'
import { setupUpdater } from './updater'
import { initStore } from './store'

let mainWindow: BrowserWindow | null = null

app.whenReady().then(() => {
  initStore()
  mainWindow = createWindow()
  createTray(mainWindow)
  setupUpdater()
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    mainWindow = createWindow()
  }
})
```

#### 4.2 窗口管理 (main/window.ts)
```typescript
import { BrowserWindow, screen } from 'electron'
import { join } from 'path'
import Store from 'electron-store'

const store = new Store()

export function createWindow(): BrowserWindow {
  const bounds = store.get('windowBounds', {
    width: 1400,
    height: 900
  })

  const win = new BrowserWindow({
    ...bounds,
    minWidth: 1200,
    minHeight: 800,
    frame: false,  // 无边框窗口（自定义标题栏）
    webPreferences: {
      preload: join(__dirname, '../preload/index.js'),
      contextIsolation: true,
      nodeIntegration: false
    }
  })

  // 记住窗口状态
  win.on('close', () => {
    store.set('windowBounds', win.getBounds())
  })

  // 开发环境加载 dev server，生产环境加载打包文件
  if (process.env.NODE_ENV === 'development') {
    win.loadURL('http://localhost:5173')
    win.webContents.openDevTools()
  } else {
    win.loadFile(join(__dirname, '../renderer/index.html'))
  }

  return win
}
```

#### 4.3 预加载脚本 (preload/index.ts)
```typescript
import { contextBridge, ipcRenderer } from 'electron'

contextBridge.exposeInMainWorld('electronAPI', {
  // 窗口控制
  minimizeWindow: () => ipcRenderer.invoke('window:minimize'),
  maximizeWindow: () => ipcRenderer.invoke('window:maximize'),
  closeWindow: () => ipcRenderer.invoke('window:close'),
  
  // 系统功能
  openExternal: (url: string) => ipcRenderer.invoke('shell:openExternal', url),
  showNotification: (title: string, body: string) => 
    ipcRenderer.invoke('notification:show', title, body),
  
  // 文件操作
  saveFile: (data: any, filename: string) => 
    ipcRenderer.invoke('file:save', data, filename),
  
  // 应用信息
  getVersion: () => ipcRenderer.invoke('app:getVersion'),
  checkUpdate: () => ipcRenderer.invoke('updater:check'),
  
  // 设置
  getSettings: () => ipcRenderer.invoke('settings:get'),
  setSettings: (settings: any) => ipcRenderer.invoke('settings:set', settings)
})
```

#### 4.4 自动更新 (main/updater.ts)
```typescript
import { autoUpdater } from 'electron-updater'
import { BrowserWindow, ipcMain } from 'electron'

export function setupUpdater() {
  autoUpdater.autoDownload = false
  autoUpdater.autoInstallOnAppQuit = true

  autoUpdater.on('update-available', (info) => {
    BrowserWindow.getAllWindows().forEach(win => {
      win.webContents.send('update:available', info)
    })
  })

  autoUpdater.on('download-progress', (progress) => {
    BrowserWindow.getAllWindows().forEach(win => {
      win.webContents.send('update:progress', progress)
    })
  })

  autoUpdater.on('update-downloaded', () => {
    BrowserWindow.getAllWindows().forEach(win => {
      win.webContents.send('update:downloaded')
    })
  })

  ipcMain.handle('updater:check', async () => {
    return await autoUpdater.checkForUpdates()
  })

  ipcMain.handle('updater:download', () => {
    autoUpdater.downloadUpdate()
  })

  ipcMain.handle('updater:install', () => {
    autoUpdater.quitAndInstall()
  })
}
```

### 5. 打包配置 (electron-builder.yml)
```yaml
appId: com.codenest.admin  # 或 com.codenest.user
productName: Code-Nest 管理端
copyright: Copyright © 2024

directories:
  buildResources: build
  output: release/${version}

files:
  - "!**/.vscode/*"
  - "!src/*"
  - "!electron.vite.config.*"
  - "!{.eslintignore,.eslintrc.js,.prettierignore,.prettierrc.yaml}"
  - "!{tsconfig.json,tsconfig.node.json}"

win:
  executableName: CodeNest-Admin
  icon: electron/resources/icon.ico
  target:
    - target: nsis
      arch:
        - x64
        - ia32

nsis:
  oneClick: false
  perMachine: false
  allowToChangeInstallationDirectory: true
  deleteAppDataOnUninstall: false
  installerIcon: electron/resources/icon.ico
  uninstallerIcon: electron/resources/icon.ico
  installerHeaderIcon: electron/resources/icon.ico
  createDesktopShortcut: true
  createStartMenuShortcut: true
  shortcutName: Code-Nest 管理端

mac:
  category: public.app-category.developer-tools
  icon: electron/resources/icon.icns
  target:
    - target: dmg
      arch:
        - x64
        - arm64
    - target: zip
      arch:
        - x64
        - arm64

linux:
  icon: electron/resources/icon.png
  target:
    - target: AppImage
    - target: deb

publish:
  provider: github
  owner: your-github-username
  repo: code-nest-desktop
```

## 📅 实施计划

### 阶段一：环境搭建 (1周)
- [ ] 初始化 Electron 项目结构
- [ ] 配置 electron-vite 开发环境
- [ ] 迁移 Vue3 源代码
- [ ] 配置开发热重载

### 阶段二：核心功能开发 (2周)
- [ ] 实现自定义标题栏
- [ ] 实现窗口管理功能
- [ ] 实现系统托盘功能
- [ ] 实现 IPC 通信机制
- [ ] 实现本地存储功能

### 阶段三：增强功能开发 (2周)
- [ ] 实现自动更新功能
- [ ] 实现设置界面
- [ ] 实现全局快捷键
- [ ] 实现系统通知
- [ ] 实现启动画面

### 阶段四：打包与测试 (1周)
- [ ] 配置 electron-builder
- [ ] Windows 打包测试
- [ ] macOS 打包测试 (如有条件)
- [ ] Linux 打包测试
- [ ] 安装包签名配置

### 阶段五：发布准备 (1周)
- [ ] 编写用户使用文档
- [ ] 配置自动更新服务
- [ ] 准备 GitHub Release
- [ ] 性能优化和最终测试

**总计预估: 7周**

## 📦 产出物

### 1. 安装包
- `CodeNest-Admin-Setup-{version}.exe` - Windows 安装包
- `CodeNest-Admin-{version}.dmg` - macOS 安装包
- `CodeNest-Admin-{version}.AppImage` - Linux 安装包
- `CodeNest-User-Setup-{version}.exe` - Windows 安装包 (用户端)
- 以此类推...

### 2. 便携版
- `CodeNest-Admin-{version}-win-portable.zip` - Windows 便携版

### 3. 更新包
- 增量更新文件 (自动更新使用)

## ✅ 验收标准

### 1. 功能完整性
- ✅ 应用能正常启动，显示启动画面
- ✅ Web 功能与浏览器版本一致
- ✅ 系统托盘功能正常工作
- ✅ 设置能正确保存和读取
- ✅ 自动更新功能正常工作

### 2. 性能要求
- ✅ 冷启动时间 < 5秒
- ✅ 内存占用 < 500MB (空闲状态)
- ✅ 安装包大小 < 150MB

### 3. 兼容性要求
- ✅ Windows 10/11 (x64)
- ✅ macOS 10.15+ (Intel/Apple Silicon)
- ✅ Ubuntu 20.04+ / Debian 10+

### 4. 安全要求
- ✅ 启用上下文隔离
- ✅ 禁用 nodeIntegration
- ✅ 使用 preload 脚本暴露安全 API
- ✅ 安装包数字签名 (可选)

## ⚠️ 风险与挑战

### 1. 技术风险
- **打包体积**: Electron 应用体积较大，需优化
- **内存占用**: Chromium 内核内存占用高，需关注性能
- **跨平台兼容**: 不同系统可能存在兼容性问题

### 2. 维护成本
- 需要同时维护 Web 和 Desktop 两套代码
- 自动更新服务需要额外的服务器资源
- 各平台打包需要对应的开发环境

### 3. 应对策略
- 使用共享组件库减少重复代码
- 采用 GitHub Actions 实现 CI/CD 自动打包
- 编写完善的文档减少维护难度

## 🔗 参考资源

- [Electron 官方文档](https://www.electronjs.org/docs)
- [electron-vite](https://electron-vite.org/)
- [electron-builder](https://www.electron.build/)
- [electron-updater](https://www.electron.build/auto-update)
- [Electron 安全最佳实践](https://www.electronjs.org/docs/latest/tutorial/security)

---

**文档版本**: v1.0.0  
**创建日期**: 2026-01-19  
**作者**: Warp AI
