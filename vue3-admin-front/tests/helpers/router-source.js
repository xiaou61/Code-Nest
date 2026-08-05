import { readdirSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'

export function readRouterSource(projectRoot) {
  const routerRoot = resolve(projectRoot, 'src', 'router')
  const routeRoot = resolve(routerRoot, 'routes')
  const routeSources = readdirSync(routeRoot)
    .filter((file) => file.endsWith('.js'))
    .sort()
    .map((file) => readFileSync(resolve(routeRoot, file), 'utf8'))

  return [readFileSync(resolve(routerRoot, 'index.js'), 'utf8'), ...routeSources].join('\n')
}
