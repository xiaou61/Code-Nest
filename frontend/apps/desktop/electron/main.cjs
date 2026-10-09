/**
 * Electron 主进程。
 *
 * 渲染进程不从 file:// 加载，而是由这里的本地静态服务提供：这样页面的来源是一个真实的
 * http 来源（http://127.0.0.1:PORT），后端的 CORS 白名单可以按来源放行，不必去放行
 * 语义含糊的 "null" 来源。
 *
 * 端口固定（RendererServer.PORT），因为来源一变，后端就得跟着改白名单。
 */
const { app, BrowserWindow, ipcMain, shell } = require('electron')
const http = require('node:http')
const fs = require('node:fs')
const path = require('node:path')

const RENDERER_DIR = path.join(__dirname, '..', 'renderer')
const RENDERER_PORT = 5310
const RENDERER_ORIGIN = `http://127.0.0.1:${RENDERER_PORT}`

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.ico': 'image/x-icon',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.map': 'application/json; charset=utf-8',
}

function startRendererServer() {
  return new Promise((resolve, reject) => {
    const server = http.createServer((request, response) => {
      const pathname = decodeURIComponent(new URL(request.url, RENDERER_ORIGIN).pathname)
      const relative = pathname === '/' ? 'index.html' : pathname.replace(/^\/+/, '')
      const target = path.resolve(RENDERER_DIR, relative)

      // 目录穿越防护：解析后必须仍在渲染目录内
      if (!target.startsWith(RENDERER_DIR + path.sep)) {
        response.writeHead(403).end()
        return
      }

      fs.readFile(target, (error, data) => {
        if (error) {
          // 兜底回落到 index.html；Hash 路由下正常不会走到这里
          fs.readFile(path.join(RENDERER_DIR, 'index.html'), (fallbackError, fallback) => {
            if (fallbackError) {
              response.writeHead(404).end('not found')
              return
            }
            response.writeHead(200, { 'content-type': MIME_TYPES['.html'] }).end(fallback)
          })
          return
        }
        const type = MIME_TYPES[path.extname(target).toLowerCase()] ?? 'application/octet-stream'
        response.writeHead(200, { 'content-type': type }).end(data)
      })
    })
    server.on('error', reject)
    server.listen(RENDERER_PORT, '127.0.0.1', () => resolve(server))
  })
}

/** 缓存落在一个 JSON 文件里：骨架期够用，也不需要额外依赖。 */
function createCacheStore() {
  const file = path.join(app.getPath('userData'), 'cache.json')
  let data = {}
  try {
    data = JSON.parse(fs.readFileSync(file, 'utf8'))
  } catch {
    data = {}
  }
  const flush = () => {
    fs.mkdirSync(path.dirname(file), { recursive: true })
    fs.writeFileSync(file, JSON.stringify(data), 'utf8')
  }
  return {
    get: (key) => (typeof data[key] === 'string' ? data[key] : null),
    set: (key, value) => {
      data[key] = value
      flush()
    },
    remove: (key) => {
      delete data[key]
      flush()
    },
  }
}

function registerCacheIpc() {
  const cache = createCacheStore()
  ipcMain.handle('cache:get', (_event, key) => cache.get(String(key)))
  ipcMain.handle('cache:set', (_event, key, value) => cache.set(String(key), String(value)))
  ipcMain.handle('cache:remove', (_event, key) => cache.remove(String(key)))
}

function createWindow() {
  const window = new BrowserWindow({
    width: 1200,
    height: 800,
    title: 'Paideia',
    webPreferences: {
      preload: path.join(__dirname, 'preload.cjs'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
      // preload 在沙箱里只能 require('electron')，所以来源要这样传进去
      additionalArguments: [`--paideia-renderer-origin=${RENDERER_ORIGIN}`],
    },
  })

  // 外部链接交给系统浏览器，不在应用窗口里打开
  window.webContents.setWindowOpenHandler(({ url }) => {
    void shell.openExternal(url)
    return { action: 'deny' }
  })

  void window.loadURL(`${RENDERER_ORIGIN}/`)
}

app.whenReady().then(async () => {
  if (!fs.existsSync(path.join(RENDERER_DIR, 'index.html'))) {
    throw new Error(`找不到渲染产物：${RENDERER_DIR}。请先执行 pnpm --filter @paideia/desktop build`)
  }
  registerCacheIpc()
  await startRendererServer()
  createWindow()

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow()
    }
  })
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})
