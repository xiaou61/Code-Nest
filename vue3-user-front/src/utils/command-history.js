const STORAGE_KEY = 'code-nest.command-history'
const MAX_HISTORY = 6

export function readCommandHistory() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    const parsed = raw ? JSON.parse(raw) : []
    return Array.isArray(parsed) ? parsed : []
  } catch (error) {
    return []
  }
}

export function writeCommandHistory(path) {
  if (!path) {
    return []
  }

  const nextPaths = [path, ...readCommandHistory().filter((item) => item !== path)].slice(0, MAX_HISTORY)
  localStorage.setItem(STORAGE_KEY, JSON.stringify(nextPaths))
  return nextPaths
}
