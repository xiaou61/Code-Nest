import test from 'node:test'
import assert from 'node:assert/strict'
import { readdirSync, readFileSync, statSync } from 'node:fs'
import { relative, resolve } from 'node:path'

const projectRoot = resolve(import.meta.dirname, '..')
const sourceRoot = resolve(projectRoot, 'src')
const sourceExtensions = new Set(['.js', '.ts', '.vue'])

function listSourceFiles(dir) {
  return readdirSync(dir).flatMap((entry) => {
    const file = resolve(dir, entry)
    const stats = statSync(file)

    if (stats.isDirectory()) {
      return listSourceFiles(file)
    }

    const extension = file.slice(file.lastIndexOf('.'))
    return sourceExtensions.has(extension) ? [file] : []
  })
}

function normalizePath(file) {
  return relative(projectRoot, file).replace(/\\/g, '/')
}

test('admin production source files should not ship debug console logs', () => {
  const offenders = listSourceFiles(sourceRoot).flatMap((filePath) => {
    const file = normalizePath(filePath)
    const source = readFileSync(filePath, 'utf8')

    return source
      .split(/\r?\n/)
      .map((line, index) => ({ file, line: index + 1, text: line.trim() }))
      .filter(({ text }) => /\bconsole\.log\s*\(/.test(text))
      .map(({ file, line, text }) => `${file}:${line}: ${text}`)
  })

  assert.deepEqual(offenders, [])
})
