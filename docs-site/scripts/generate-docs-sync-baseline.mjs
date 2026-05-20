import { execSync } from 'node:child_process'
import { mkdirSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const docsRoot = path.resolve(__dirname, '..')
const repoRoot = path.resolve(docsRoot, '..')
const outputPath = path.resolve(docsRoot, '.vitepress', 'cache', 'docs-sync-baseline.json')

function runGit(command, fallback = '') {
  try {
    return execSync(command, {
      cwd: repoRoot,
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'ignore']
    }).trim()
  } catch {
    return fallback
  }
}

const branch = runGit('git branch --show-current', 'unknown')
const shortSha = runGit('git rev-parse --short HEAD', 'unknown')
const fullSha = runGit('git rev-parse HEAD', 'unknown')
const commitCount = runGit('git rev-list --count HEAD', 'unknown')
const commitTime = runGit('git log -1 --date=iso --pretty=format:%cd', 'unknown')
const subject = runGit('git log -1 --pretty=format:%s', 'unknown')
const upstream = runGit('git rev-parse --abbrev-ref --symbolic-full-name @{u}', '')
const status = runGit('git status --porcelain', '')
const isDirty = status.length > 0

const baseline = {
  branch,
  upstream,
  shortSha,
  fullSha,
  commitCount,
  commitTime,
  subject,
  isDirty,
  workingTree: isDirty ? '有未提交变更' : '工作区已清洁',
  generatedAt: new Date().toISOString(),
  note: isDirty
    ? '当前页面基线对应最新 HEAD，但工作区还有未提交改动。'
    : '当前页面基线对应当前 HEAD，且工作区已清洁。'
}

mkdirSync(path.dirname(outputPath), { recursive: true })
writeFileSync(outputPath, `${JSON.stringify(baseline, null, 2)}\n`, 'utf8')

console.log(`docs sync baseline generated: ${shortSha}`)
