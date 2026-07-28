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
const globalStylesSource = readFileSync(resolve(projectRoot, 'src/styles/index.scss'), 'utf8')
const alertRulesSource = readFileSync(resolve(repositoryRoot, 'docker/monitoring/alert_rules.yml'), 'utf8')
const monitoringReadmeSource = readFileSync(resolve(repositoryRoot, 'docker/monitoring/README.md'), 'utf8')
const databaseBaselineSource = readFileSync(resolve(repositoryRoot, 'sql/MySql/code_nest.sql'), 'utf8')
const investigationMigrationSource = readFileSync(
  resolve(repositoryRoot, 'sql/v2.5.0/sre_investigation_run.sql'),
  'utf8'
)
const evaluationMigrationSource = readFileSync(
  resolve(repositoryRoot, 'sql/v2.5.0/sre_rca_evaluation.sql'),
  'utf8'
)
const evaluationSuiteMigrationSource = readFileSync(
  resolve(repositoryRoot, 'sql/v2.5.0/sre_rca_evaluation_suite.sql'),
  'utf8'
)
const evaluationQueueMigrationSource = readFileSync(
  resolve(repositoryRoot, 'sql/v2.5.0/sre_rca_evaluation_queue.sql'),
  'utf8'
)

test('SRE API client should expose the complete administrator incident workflow', () => {
  assert.match(apiSource, /request\.get\('\/admin\/sre\/incidents\/summary'\)/)
  assert.match(apiSource, /request\.get\('\/admin\/sre\/incidents', params\)/)
  assert.match(apiSource, /`\/admin\/sre\/incidents\/\$\{id\}\/investigation-context`/)
  assert.match(apiSource, /`\/admin\/sre\/incidents\/\$\{id\}\/ack`/)
  assert.match(apiSource, /`\/admin\/sre\/incidents\/\$\{id\}\/resolve`/)
  assert.match(apiSource, /`\/admin\/sre\/incidents\/\$\{id\}\/rca`/)
  assert.match(apiSource, /`\/admin\/sre\/incidents\/\$\{id\}\/rca-runs`/)
  assert.match(apiSource, /`\/admin\/sre\/incidents\/\$\{id\}\/rca-runs\/\$\{runId\}`/)
  assert.match(apiSource, /`\/admin\/sre\/incidents\/\$\{id\}\/rca-runs\/\$\{runId\}\/feedback`/)
  assert.match(apiSource, /`\/admin\/sre\/incidents\/\$\{id\}\/rca-runs\/\$\{runId\}\/evaluation-sample`/)
  assert.match(apiSource, /`\/admin\/sre\/incidents\/\$\{id\}\/rca-runs\/\$\{runId\}\/evaluation-cases`/)
  assert.match(apiSource, /request\.get\('\/admin\/sre\/rca-evaluations\/cases'/)
  assert.match(apiSource, /request\.post\('\/admin\/sre\/rca-evaluations\/suites'/)
  assert.match(apiSource, /request\.get\('\/admin\/sre\/rca-evaluations\/suites'/)
  assert.match(apiSource, /publishRcaEvaluationSuiteVersion\(suiteId, data\)/)
  assert.match(apiSource, /getRcaEvaluationSuiteVersions\(suiteId, limit = 20\)/)
  assert.match(apiSource, /`\/admin\/sre\/rca-evaluations\/suites\/\$\{suiteId\}\/versions`/)
  assert.match(apiSource, /`\/admin\/sre\/rca-evaluations\/suite-versions\/\$\{versionId\}`/)
  assert.match(apiSource, /request\.post\(\s*'\/admin\/sre\/rca-evaluations\/runs'/)
  assert.match(apiSource, /request\.get\('\/admin\/sre\/rca-evaluations\/runs'/)
  assert.match(apiSource, /`\/admin\/sre\/rca-evaluations\/runs\/\$\{runId\}`/)
  assert.match(apiSource, /`\/admin\/sre\/rca-evaluations\/runs\/\$\{runId\}\/gate`/)
  assert.match(apiSource, /caseId != null \? \{ caseId \} : \(suiteVersionId != null \? \{ suiteVersionId \} : \{\}\)/)
  assert.match(apiSource, /timeout:\s*180000\b/)
  assert.doesNotMatch(apiSource, /timeout:\s*1800000\b/)
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
  assert.match(workbenchSource, /rcaRuns/)
  assert.match(workbenchSource, /investigationSteps/)
  assert.match(workbenchSource, /调查轨迹/)
  assert.match(workbenchSource, /rcaProvenance/)
  assert.match(workbenchSource, /回放来源/)
  assert.match(workbenchSource, /contextSha256/)
  assert.match(workbenchSource, /分析反馈/)
  assert.match(workbenchSource, /saveRcaFeedback/)
  assert.match(workbenchSource, /exportRcaEvaluationSample/)
  assert.match(workbenchSource, /promoteRcaEvaluationCase/)
  assert.match(workbenchSource, /runRcaEvaluation/)
  assert.match(workbenchSource, /getRcaEvaluationRun/)
  assert.match(workbenchSource, /evaluationSuites/)
  assert.match(workbenchSource, /name="cases"/)
  assert.match(workbenchSource, /name="suites"/)
  assert.match(workbenchSource, /name="history"/)
  assert.match(workbenchSource, /publishEvaluationSuiteVersion/)
  assert.match(workbenchSource, /门禁回放/)
  assert.match(workbenchSource, /gateFailureCodes/)
  assert.match(workbenchSource, /提升当前修订/)
  assert.match(workbenchSource, /回放全部/)
  assert.match(workbenchSource, /结论相似度/)
  assert.match(workbenchSource, /证据召回/)
  assert.match(workbenchSource, /模型来源/)
  assert.match(workbenchSource, /候选报告/)
  assert.match(workbenchSource, /READ_ONLY:\s*'只读检查'/)
  assert.match(workbenchSource, /PROPOSE_ONLY:\s*'仅建议'/)
  assert.doesNotMatch(workbenchSource, /evaluationCase\.contextJson/)
  assert.doesNotMatch(workbenchSource, /baselineReportJson/)
  assert.match(workbenchSource, /isCurrentIncident/)
  assert.match(workbenchSource, /rcaRequestVersion/)
  assert.match(workbenchSource, /const requestVersion = \+\+evaluationRequestVersion/)
  assert.match(workbenchSource, /requestVersion !== evaluationRequestVersion\s*\|\| !isCurrentIncident\(incidentId\)/)
  assert.match(workbenchSource, /evaluationPollVersion/)
  assert.match(workbenchSource, /startEvaluationRunPolling/)
  assert.match(workbenchSource, /setTimeout\([^,]+,\s*2000\)/s)
  assert.match(workbenchSource, /stopEvaluationRunPolling/)
  assert.match(workbenchSource, /sourceRevision/)
  assert.match(workbenchSource, /if \(value == null \|\| value === ''\) return '-'/)
  assert.match(workbenchSource, /aria-live="polite"/)
  assert.doesNotMatch(workbenchSource, /v-html/)
})

test('SRE incident drawer should use a splitter-compatible responsive size', () => {
  assert.match(workbenchSource, /:size="drawerSize"/)
  assert.match(workbenchSource, /window\.addEventListener\('resize', syncViewportWidth\)/)
  assert.doesNotMatch(workbenchSource, /size="min\(/)
})

test('primary button overrides should preserve plain, text and link variants', () => {
  assert.match(
    globalStylesSource,
    /\.el-button--primary:not\(\.is-plain\):not\(\.is-text\):not\(\.is-link\)/
  )
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

test('persistent RCA tables should be present in both fresh and incremental database paths', () => {
  for (const table of [
    'sre_investigation_run',
    'sre_investigation_step',
    'sre_investigation_feedback',
    'sre_investigation_artifact'
  ]) {
    const createTable = 'CREATE TABLE IF NOT EXISTS `' + table + '`'
    assert.ok(databaseBaselineSource.includes(createTable))
    assert.ok(investigationMigrationSource.includes(createTable))
  }
  assert.match(investigationMigrationSource, /`context_sha256` CHAR\(64\)/)
  assert.match(investigationMigrationSource, /UNIQUE KEY `uk_sre_artifact_run` \(`run_id`\)/)
  assert.match(monitoringReadmeSource, /sql\/v2\.5\.0\/sre_investigation_run\.sql/)
})

test('immutable RCA evaluation tables should be present in fresh and incremental database paths', () => {
  for (const table of [
    'sre_rca_evaluation_case',
    'sre_rca_evaluation_run',
    'sre_rca_evaluation_result'
  ]) {
    const createTable = 'CREATE TABLE IF NOT EXISTS `' + table + '`'
    assert.ok(databaseBaselineSource.includes(createTable))
    assert.ok(evaluationMigrationSource.includes(createTable))
  }
  assert.match(evaluationMigrationSource, /UNIQUE KEY `uk_sre_rca_eval_case_feedback` \(`source_feedback_id`\)/)
  assert.match(evaluationMigrationSource, /`context_json` MEDIUMTEXT NOT NULL/)
  assert.match(evaluationMigrationSource, /`candidate_report_json` MEDIUMTEXT/)

  for (const table of [
    'sre_rca_evaluation_suite',
    'sre_rca_evaluation_suite_version',
    'sre_rca_evaluation_suite_case'
  ]) {
    const createTable = 'CREATE TABLE IF NOT EXISTS `' + table + '`'
    assert.ok(databaseBaselineSource.includes(createTable))
    assert.ok(evaluationSuiteMigrationSource.includes(createTable))
  }
  assert.match(evaluationSuiteMigrationSource, /`case_content_sha256` CHAR\(64\)/)
  assert.match(evaluationSuiteMigrationSource, /`manifest_schema_id` VARCHAR\(128\)/)
  assert.match(evaluationSuiteMigrationSource, /`scoring_policy_id` VARCHAR\(128\)/)
  assert.match(evaluationSuiteMigrationSource, /`gate_evaluator_id` VARCHAR\(128\)/)
  assert.match(evaluationSuiteMigrationSource, /`minimum_pass_rate` DECIMAL\(5,2\)/)
  assert.match(evaluationSuiteMigrationSource, /`minimum_average_score` DECIMAL\(6,2\)/)
  assert.match(evaluationSuiteMigrationSource, /UNIQUE KEY `uk_sre_rca_eval_suite_manifest`/)
  assert.match(evaluationSuiteMigrationSource, /ADD COLUMN `suite_version_id` BIGINT/)
  assert.match(evaluationSuiteMigrationSource, /ADD COLUMN `trigger_source` VARCHAR\(16\)/)
  assert.match(evaluationSuiteMigrationSource, /ADD COLUMN `gate_status` VARCHAR\(16\)/)
  assert.match(databaseBaselineSource, /`suite_manifest_sha256` CHAR\(64\)/)
  assert.match(databaseBaselineSource, /`gate_detail_json` TEXT/)
  assert.match(monitoringReadmeSource, /sql\/v2\.5\.0\/sre_rca_evaluation_suite\.sql/)

  assert.ok(databaseBaselineSource.includes('CREATE TABLE IF NOT EXISTS `sre_rca_evaluation_run_case`'))
  assert.ok(evaluationQueueMigrationSource.includes('CREATE TABLE IF NOT EXISTS `sre_rca_evaluation_run_case`'))
  assert.match(evaluationQueueMigrationSource, /UNIQUE KEY `uk_sre_rca_eval_run_active_admin`/)
  assert.match(evaluationQueueMigrationSource, /ADD COLUMN `deadline_at` DATETIME/)
  assert.match(evaluationQueueMigrationSource, /ADD COLUMN `source_revision` VARCHAR\(64\)/)
  assert.match(monitoringReadmeSource, /sql\/v2\.5\.0\/sre_rca_evaluation_queue\.sql/)
})
