import test from 'node:test'
import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const projectRoot = resolve(import.meta.dirname, '..')
const routerEntry = readFileSync(resolve(projectRoot, 'src', 'router', 'index.js'), 'utf8')
const sliceNames = ['core', 'learning', 'career', 'community', 'productivity', 'fallback']

test('user router composes feature-owned route slices', () => {
  assert.ok(routerEntry.split(/\r?\n/).length < 120)

  for (const sliceName of sliceNames) {
    const slicePath = resolve(projectRoot, 'src', 'router', 'routes', `${sliceName}.js`)
    assert.equal(existsSync(slicePath), true, `${sliceName} route slice should exist`)
    assert.match(routerEntry, new RegExp(`${sliceName}Routes`))
  }

  assert.ok(routerEntry.indexOf('fallbackRoutes') > routerEntry.indexOf('productivityRoutes'))
})
