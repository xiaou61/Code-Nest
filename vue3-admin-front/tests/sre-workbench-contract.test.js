import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const projectRoot = resolve(import.meta.dirname, '..')
const repositoryRoot = resolve(projectRoot, '..')
const apiSource = readFileSync(resolve(projectRoot, 'src/api/sre.js'), 'utf8')
const routerSource = readFileSync(resolve(projectRoot, 'src/router/index.js'), 'utf8')
const layoutSource = readFileSync(resolve(projectRoot, 'src/layout/index.vue'), 'utf8')
const workbenchSource = readFileSync(resolve(projectRoot, 'src/views/sre/incidents/index.vue'), 'utf8')
const alertRulesSource = readFileSync(resolve(repositoryRoot, 'docker/monitoring/alert_rules.yml'), 'utf8')

test('SRE API client should expose the complete administrator incident workflow', () => {
  assert.match(apiSource, /request\.get\('\/admin\/sre\/incidents\/summary'\)/)
  assert.match(apiSource, /request\.get\('\/admin\/sre\/incidents', params\)/)
  assert.match(apiSource, /`\/admin\/sre\/incidents\/\$\{id\}\/investigation-context`/)
  assert.match(apiSource, /`\/admin\/sre\/incidents\/\$\{id\}\/ack`/)
  assert.match(apiSource, /`\/admin\/sre\/incidents\/\$\{id\}\/resolve`/)
  assert.match(apiSource, /`\/admin\/sre\/incidents\/\$\{id\}\/rca`/)
  assert.match(apiSource, /timeout:\s*180000/)
})

test('SRE workbench should be reachable from both router and sidebar', () => {
  assert.match(routerSource, /path:\s*'\/sre'/)
  assert.match(routerSource, /path:\s*'incidents'/)
  assert.match(routerSource, /@\/views\/sre\/incidents\/index\.vue/)
  assert.match(layoutSource, /path:\s*'\/sre\/incidents'/)
})

test('SRE workbench should keep incident actions, evidence and AI output inspectable', () => {
  assert.match(workbenchSource, /CnDataTable/)
  assert.match(workbenchSource, /generateRca/)
  assert.match(workbenchSource, /acknowledgeIncident/)
  assert.match(workbenchSource, /resolveIncident/)
  assert.match(workbenchSource, /evidenceReferences/)
  assert.match(workbenchSource, /executionAllowed/)
  assert.match(workbenchSource, /aria-live="polite"/)
  assert.doesNotMatch(workbenchSource, /v-html/)
})

test('SRE incident drawer should use a splitter-compatible responsive size', () => {
  assert.match(workbenchSource, /:size="drawerSize"/)
  assert.match(workbenchSource, /window\.addEventListener\('resize', syncViewportWidth\)/)
  assert.doesNotMatch(workbenchSource, /size="min\(/)
})

test('admin shell should expose the full content width behind mobile navigation', () => {
  assert.match(layoutSource, /const isMobile = ref\(isMobileViewport\(\)\)/)
  assert.match(layoutSource, /return collapsed\.value \? '0px' : '240px'/)
  assert.match(layoutSource, /window\.addEventListener\('resize', syncViewportMode\)/)
  assert.match(layoutSource, /class="sidebar-backdrop"/)
  assert.match(layoutSource, /inset:\s*0 0 0 240px/)
  assert.match(layoutSource, /:inert="isMobile && collapsed"/)
  assert.match(layoutSource, /v-if="!isMobile \|\| !collapsed"/)
})

test('disk alerts should ignore the container overlay mirror of the root filesystem', () => {
  const exclusions = alertRulesSource.match(
    /mountpoint!~"\/var\/lib\/containers\/storage\/overlay\(\$\|\/\)"/g
  ) || []

  assert.equal(exclusions.length, 4)
})
