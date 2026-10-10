/**
 * Electron 主进程。
 *
 * 渲染进程不从 file:// 加载，而是由这里的本地静态服务提供：这样页面的来源是一个真实的
 * http 来源（http://127.0.0.1:PORT），后端的 CORS 白名单可以按来源放行，不必去放行
 * 语义含糊的 "null" 来源。
 *
 * 端口固定（RendererServer.PORT），因为来源一变，后端就得跟着改白名单。
 */
const { app, BrowserWindow, dialog, ipcMain, shell } = require('electron')
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
      // 解析畸形路径（如 /%E0%A4%A、含未转义的 %）时 URL/decodeURIComponent 会直接抛错。
      // 不接住的话它会从请求回调里冒出去，把主进程带崩——一个畸形的请求不该能杀掉应用。
      let pathname
      try {
        pathname = decodeURIComponent(new URL(request.url, RENDERER_ORIGIN).pathname)
      } catch {
        response.writeHead(400).end('bad request')
        return
      }
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
  } catch (error) {
    if (error.code !== 'ENOENT') {
      // 内容坏掉不再静默当作空缓存：那会让"整份设置丢了"看起来像"从没设置过"
      console.warn('缓存文件无法解析，按空缓存继续运行：', file, error)
    }
    data = {}
  }
  const flush = () => {
    try {
      fs.mkdirSync(path.dirname(file), { recursive: true })
      // 先写临时文件再原子替换：直接覆写时若进程在写中途退出，cache.json 会被截断，
      // 而截断的 JSON 解析失败又会被当成空缓存——用户的选择就这么整份丢了。
      // ponytail: 仍是同步写；缓存很小且必须保证两次写入的先后顺序，异步写反而要额外排队。
      const temp = `${file}.tmp`
      fs.writeFileSync(temp, JSON.stringify(data), 'utf8')
      fs.renameSync(temp, file)
    } catch (error) {
      // 磁盘只读/空间不足时不能让它沿 IPC handler 抛出去变成未处理拒绝
      console.warn('写入缓存失败，本次修改未落盘：', file, error)
    }
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
    /**
     * 当前缓存内容。启动时经 additionalArguments 注入渲染进程，
     * 因为渲染进程的同步 cache.get 需要一份"启动时就存在"的镜像 ——
     * 单靠 IPC 拿不到，IPC 是异步的，而首屏主题脚本等不了。
     */
    snapshot: () => ({ ...data }),
  }
}

function registerCacheIpc(cache) {
  ipcMain.handle('cache:get', (_event, key) => cache.get(String(key)))
  ipcMain.handle('cache:set', (_event, key, value) => cache.set(String(key), String(value)))
  ipcMain.handle('cache:remove', (_event, key) => cache.remove(String(key)))
}

/**
 * 只放行 http/https。
 *
 * <p>其余协议（file:、ms-msdt:、smb: 等）在部分平台可以拉起本地程序，而 url 来自页面，
 * 页面里可能混进第三方内容——等于把"执行本地程序"的能力交给了页面。
 */
function isSafeExternalUrl(url) {
  try {
    const { protocol } = new URL(url)
    return protocol === 'http:' || protocol === 'https:'
  } catch {
    return false
  }
}

function createWindow(cacheSnapshot) {
  const window = new BrowserWindow({
    width: 1200,
    height: 800,
    title: 'Paideia',
    webPreferences: {
      preload: path.join(__dirname, 'preload.cjs'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
      // preload 在沙箱里只能 require('electron')，读不了文件，所以来源与缓存快照都要这样传进去
      additionalArguments: [
        `--paideia-renderer-origin=${RENDERER_ORIGIN}`,
        `--paideia-cache-snapshot=${Buffer.from(JSON.stringify(cacheSnapshot), 'utf8').toString('base64')}`,
      ],
    },
  })

  // 外部链接交给系统浏览器，不在应用窗口里打开
  window.webContents.setWindowOpenHandler(({ url }) => {
    if (isSafeExternalUrl(url)) {
      shell.openExternal(url).catch((error) => console.warn('打开外部链接失败：', url, error))
    } else {
      console.warn('拒绝打开非 http(s) 链接：', url)
    }
    return { action: 'deny' }
  })

  window.loadURL(`${RENDERER_ORIGIN}/`).catch((error) => console.error('加载渲染页面失败：', error))
}

app.whenReady().then(async () => {
  try {
    if (!fs.existsSync(path.join(RENDERER_DIR, 'index.html'))) {
      throw new Error(`找不到渲染产物：${RENDERER_DIR}。请先执行 pnpm --filter @paideia/desktop build`)
    }
    const cache = createCacheStore()
    registerCacheIpc(cache)
    await startRendererServer()
    createWindow(cache.snapshot())

    app.on('activate', () => {
      if (BrowserWindow.getAllWindows().length === 0) {
        createWindow(cache.snapshot())
      }
    })
  } catch (error) {
    // 端口被占用、渲染产物缺失等都会走到这里。不接住的话是"进程悄无声息地退出"，
    // 用户只看到什么都没发生，连原因都没有。
    dialog.showErrorBox('Paideia 启动失败', error instanceof Error ? error.message : String(error))
    app.exit(1)
  }
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})
