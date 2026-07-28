<template>
  <CnPage class="sre-workbench" surface="transparent" max-width="1500px">
    <div class="sr-only" aria-live="polite">{{ liveStatus }}</div>

    <CnPageHeader
      title="SRE 事故工作台"
      :description="headerDescription"
      eyebrow="OPERATIONS / V2.5.0"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag :type="overallTone">{{ overallLabel }}</CnStatusTag>
        <CnStatusTag type="neutral">自动执行关闭</CnStatusTag>
      </template>

      <template #actions>
        <el-button
          type="primary"
          :icon="Refresh"
          :loading="refreshing"
          aria-label="刷新 SRE 事故数据"
          @click="refreshWorkbench(true)"
        >
          刷新
        </el-button>
      </template>
    </CnPageHeader>

    <div class="stat-grid" aria-label="事故状态汇总">
      <CnStatCard
        title="活动事故"
        :value="summary.activeCount"
        :description="`${summary.openCount} 条待确认，${summary.acknowledgedCount} 条处理中`"
        :tone="summary.activeCount > 0 ? 'warning' : 'success'"
        :loading="summaryLoading"
      />
      <CnStatCard
        title="严重事故"
        :value="summary.criticalActiveCount"
        description="当前尚未恢复的 critical 事故"
        :tone="summary.criticalActiveCount > 0 ? 'danger' : 'success'"
        :loading="summaryLoading"
      />
      <CnStatCard
        title="警告事故"
        :value="summary.warningActiveCount"
        description="当前尚未恢复的 warning 事故"
        :tone="summary.warningActiveCount > 0 ? 'warning' : 'success'"
        :loading="summaryLoading"
      />
      <CnStatCard
        title="已恢复"
        :value="summary.resolvedCount"
        :description="`累计事故 ${summary.totalCount} 条`"
        tone="info"
        :loading="summaryLoading"
      />
    </div>

    <CnSection
      title="事故队列"
      :description="`当前条件共 ${pagination.total} 条记录`"
      divided
    >
      <CnFilterForm
        :model-value="filters"
        :fields="filterFields"
        :loading="listLoading"
        :columns="3"
        @update:model-value="updateFilters"
        @search="applyFilters"
        @reset="resetFilters"
      />

      <CnDataTable
        class="incident-table"
        :columns="incidentColumns"
        :data="incidents"
        :loading="listLoading"
        :pagination="tablePagination"
        row-key="id"
        empty-title="暂无事故"
        empty-description="当前筛选条件下没有事故记录。"
        empty-icon="OK"
        @row-click="openIncident"
        @page-change="changePage"
        @page-size-change="changePageSize"
      >
        <template #severity="{ row }">
          <CnStatusTag :type="severityTone(row.severity)" size="sm">
            {{ severityLabel(row.severity) }}
          </CnStatusTag>
        </template>

        <template #state="{ row }">
          <CnStatusTag :type="stateTone(row.state)" size="sm">
            {{ stateLabel(row.state) }}
          </CnStatusTag>
        </template>

        <template #summary="{ row }">
          <div class="incident-summary-cell">
            <strong>{{ row.alertName || '-' }}</strong>
            <span>{{ row.summary || '-' }}</span>
          </div>
        </template>

        <template #lastSeen="{ row }">
          <span class="time-cell">{{ formatTime(row.lastSeen) }}</span>
        </template>

        <template #actions="{ row }">
          <el-button
            link
            type="primary"
            :icon="View"
            :aria-label="`查看事故 ${row.incidentNo || row.id}`"
            @click.stop="openIncident(row)"
          >
            查看
          </el-button>
        </template>
      </CnDataTable>
    </CnSection>

    <el-drawer
      v-model="drawerVisible"
      class="sre-incident-drawer"
      :title="drawerTitle"
      :size="drawerSize"
      append-to-body
      destroy-on-close
      @closed="resetDrawer"
    >
      <div v-loading="detailLoading" class="drawer-content">
        <template v-if="selectedIncident">
          <div class="drawer-status-row">
            <div class="drawer-status-tags">
              <CnStatusTag :type="severityTone(selectedIncident.severity)" size="sm">
                {{ severityLabel(selectedIncident.severity) }}
              </CnStatusTag>
              <CnStatusTag :type="stateTone(selectedIncident.state)" size="sm">
                {{ stateLabel(selectedIncident.state) }}
              </CnStatusTag>
              <CnStatusTag type="neutral" size="sm">
                {{ selectedIncident.service || 'unknown-service' }}
              </CnStatusTag>
            </div>

            <div class="drawer-actions" role="toolbar" aria-label="事故操作">
              <el-button
                :icon="Check"
                :loading="actionLoading === 'ack'"
                :disabled="!canAcknowledge || actionLoading !== ''"
                @click="acknowledgeSelected"
              >
                确认
              </el-button>
              <el-button
                type="primary"
                :icon="MagicStick"
                :loading="rcaLoading"
                :disabled="detailLoading || rcaLoading"
                @click="generateRca"
              >
                AI 分析
              </el-button>
              <el-button
                type="danger"
                plain
                :icon="CircleCheck"
                :loading="actionLoading === 'resolve'"
                :disabled="!canResolve || actionLoading !== ''"
                @click="resolveSelected"
              >
                关闭事故
              </el-button>
            </div>
          </div>

          <el-tabs v-model="activeDetailTab" class="detail-tabs">
            <el-tab-pane label="概览" name="overview">
              <section class="detail-section" aria-labelledby="incident-facts-title">
                <h3 id="incident-facts-title">事故信息</h3>
                <dl class="fact-grid">
                  <div>
                    <dt>事故编号</dt>
                    <dd>{{ selectedIncident.incidentNo || '-' }}</dd>
                  </div>
                  <div>
                    <dt>告警名称</dt>
                    <dd>{{ selectedIncident.alertName || '-' }}</dd>
                  </div>
                  <div>
                    <dt>首次发现</dt>
                    <dd>{{ formatTime(selectedIncident.firstSeen) }}</dd>
                  </div>
                  <div>
                    <dt>最近观测</dt>
                    <dd>{{ formatTime(selectedIncident.lastSeen) }}</dd>
                  </div>
                  <div>
                    <dt>确认时间</dt>
                    <dd>{{ formatTime(selectedIncident.acknowledgedAt) }}</dd>
                  </div>
                  <div>
                    <dt>恢复时间</dt>
                    <dd>{{ formatTime(selectedIncident.resolvedAt) }}</dd>
                  </div>
                </dl>
                <p class="incident-description">{{ selectedIncident.summary || '暂无事故摘要。' }}</p>
              </section>

              <section class="detail-section" aria-labelledby="alert-timeline-title">
                <div class="section-heading">
                  <h3 id="alert-timeline-title">告警时间线</h3>
                  <CnStatusTag type="neutral" size="sm">{{ contextAlerts.length }} 条</CnStatusTag>
                </div>

                <ol v-if="contextAlerts.length" class="timeline-list">
                  <li v-for="alert in contextAlerts" :key="alert.id" class="timeline-item">
                    <span class="timeline-marker" :class="`is-${String(alert.status || '').toLowerCase()}`" aria-hidden="true" />
                    <article>
                      <div class="timeline-head">
                        <strong>{{ alert.alertName || '-' }}</strong>
                        <CnStatusTag :type="alert.status === 'FIRING' ? 'danger' : 'success'" size="sm">
                          {{ alert.status === 'FIRING' ? '触发' : '恢复' }}
                        </CnStatusTag>
                      </div>
                      <p>{{ alert.source || '-' }} · {{ formatTime(alert.observedAt || alert.startsAt) }}</p>
                    </article>
                  </li>
                </ol>
                <CnEmptyState
                  v-else
                  title="暂无告警快照"
                  description="该事故当前没有可展示的告警摘要。"
                  icon="AL"
                  size="sm"
                  surface="transparent"
                />
              </section>
            </el-tab-pane>

            <el-tab-pane :label="`证据 (${contextEvidence.length})`" name="evidence">
              <section class="detail-section evidence-section" aria-labelledby="evidence-title">
                <div class="section-heading">
                  <h3 id="evidence-title">证据快照</h3>
                  <CnStatusTag v-if="investigationContext?.evidenceTruncated" type="warning" size="sm">
                    已裁剪
                  </CnStatusTag>
                </div>

                <el-collapse v-if="contextEvidence.length" v-model="expandedEvidenceIds">
                  <el-collapse-item
                    v-for="evidence in contextEvidence"
                    :key="evidence.id"
                    :name="evidence.id"
                  >
                    <template #title>
                      <div class="evidence-title-row">
                        <strong>{{ evidence.sourceType || 'UNKNOWN' }}</strong>
                        <span>{{ evidence.sourceRef || '-' }}</span>
                        <CnStatusTag :type="evidenceTone(evidence.status)" size="sm">
                          {{ evidence.status || 'UNKNOWN' }}
                        </CnStatusTag>
                      </div>
                    </template>
                    <dl class="evidence-meta">
                      <div>
                        <dt>采集时间</dt>
                        <dd>{{ formatTime(evidence.capturedAt) }}</dd>
                      </div>
                      <div>
                        <dt>查询</dt>
                        <dd>{{ evidence.queryDescription || '-' }}</dd>
                      </div>
                    </dl>
                    <pre class="evidence-json">{{ prettySnapshot(evidence.snapshot) }}</pre>
                  </el-collapse-item>
                </el-collapse>
                <CnEmptyState
                  v-else
                  title="暂无调查证据"
                  description="证据采集任务尚未写入可用快照。"
                  icon="EV"
                  size="sm"
                  surface="transparent"
                />
              </section>
            </el-tab-pane>

            <el-tab-pane label="AI RCA" name="rca">
              <section v-loading="rcaLoading || rcaHistoryLoading" class="detail-section rca-section" aria-labelledby="rca-title">
                <div v-if="rcaRuns.length" class="rca-history-toolbar">
                  <span>历史版本</span>
                  <el-select
                    v-model="selectedRcaRunId"
                    aria-label="选择 RCA 历史版本"
                    @change="selectRcaRun"
                  >
                    <el-option
                      v-for="run in rcaRuns"
                      :key="run.id"
                      :label="runHistoryLabel(run)"
                      :value="run.id"
                    />
                  </el-select>
                </div>

                <template v-if="rcaReport">
                  <div class="section-heading rca-heading">
                    <div>
                      <h3 id="rca-title">根因分析</h3>
                      <p>{{ formatTime(rcaReport.generatedAt) }}</p>
                    </div>
                    <div class="rca-tags">
                      <CnStatusTag :type="rcaReport.generationMode === 'AI' ? 'brand' : 'warning'" size="sm">
                        {{ rcaReport.generationMode || 'UNKNOWN' }}
                      </CnStatusTag>
                      <CnStatusTag :type="conclusionTone(rcaReport.conclusionStatus)" size="sm">
                        {{ conclusionLabel(rcaReport.conclusionStatus) }}
                      </CnStatusTag>
                      <CnStatusTag v-if="selectedRcaRun" :type="runStatusTone(selectedRcaRun.status)" size="sm">
                        {{ runStatusLabel(selectedRcaRun.status) }}
                      </CnStatusTag>
                      <CnStatusTag :type="rcaReport.executionAllowed ? 'danger' : 'neutral'" size="sm">
                        {{ rcaReport.executionAllowed ? '允许执行' : '禁止自动执行' }}
                      </CnStatusTag>
                    </div>
                  </div>

                  <p class="rca-summary">{{ rcaReport.executiveSummary || '暂无执行摘要。' }}</p>

                  <div v-if="rcaProvenance" class="rca-block provenance-block">
                    <div class="provenance-heading">
                      <div>
                        <h4>回放来源</h4>
                        <p>保存模型实际接收的脱敏上下文；页面仅展示校验摘要，不回显正文。</p>
                      </div>
                      <CnStatusTag :type="invocationOutcomeTone(rcaProvenance.invocationOutcome)" size="sm">
                        {{ invocationOutcomeLabel(rcaProvenance.invocationOutcome) }}
                      </CnStatusTag>
                    </div>
                    <dl class="provenance-grid">
                      <div>
                        <dt>Prompt</dt>
                        <dd><code>{{ rcaProvenance.promptId || '-' }}</code></dd>
                      </div>
                      <div>
                        <dt>结构化契约</dt>
                        <dd><code>{{ rcaProvenance.schemaId || '-' }}</code></dd>
                      </div>
                      <div>
                        <dt>Provider</dt>
                        <dd>{{ rcaProvenance.provider || '-' }}</dd>
                      </div>
                      <div>
                        <dt>配置 / 实际模型</dt>
                        <dd>{{ rcaProvenance.configuredModel || '-' }} / {{ rcaProvenance.actualModel || '未调用或未返回' }}</dd>
                      </div>
                      <div>
                        <dt>上下文规模</dt>
                        <dd>
                          {{ Number(rcaProvenance.contextLength || 0).toLocaleString() }} 字符
                          · {{ rcaProvenance.contextTruncated ? '已裁剪' : '完整' }}
                        </dd>
                      </div>
                      <div>
                        <dt>固化时间</dt>
                        <dd>{{ formatTime(rcaProvenance.createdAt) }}</dd>
                      </div>
                      <div class="provenance-hash">
                        <dt>Context SHA-256</dt>
                        <dd><code>{{ rcaProvenance.contextSha256 || '-' }}</code></dd>
                      </div>
                    </dl>
                  </div>

                  <div class="rca-block rca-feedback-block">
                    <div class="feedback-heading">
                      <div>
                        <h4>分析反馈</h4>
                        <div v-if="rcaFeedback" class="feedback-meta">
                          <CnStatusTag :type="feedbackAccuracyTone(rcaFeedback.accuracy)" size="sm">
                            {{ feedbackAccuracyLabel(rcaFeedback.accuracy) }}
                          </CnStatusTag>
                          <span>{{ feedbackGapLabel(rcaFeedback.gapType) }}</span>
                          <span>管理员 #{{ rcaFeedback.reviewedBy || '-' }}</span>
                          <time>{{ formatTime(rcaFeedback.reviewedAt) }}</time>
                        </div>
                      </div>
                      <el-button
                        :icon="Download"
                        :loading="evaluationExporting"
                        :disabled="!rcaFeedback"
                        plain
                        @click="exportRcaEvaluationSample"
                      >
                        导出样本
                      </el-button>
                    </div>

                    <el-form class="feedback-form" :model="feedbackForm" label-position="top">
                      <el-form-item label="准确度">
                        <el-radio-group
                          v-model="feedbackForm.accuracy"
                          size="small"
                          aria-label="RCA 准确度"
                          @change="handleAccuracyChange"
                        >
                          <el-radio-button label="ACCURATE">准确</el-radio-button>
                          <el-radio-button label="PARTIAL">部分准确</el-radio-button>
                          <el-radio-button label="INACCURATE">不准确</el-radio-button>
                        </el-radio-group>
                      </el-form-item>

                      <el-form-item
                        v-if="['PARTIAL', 'INACCURATE'].includes(feedbackForm.accuracy)"
                        label="主要缺口"
                      >
                        <el-select v-model="feedbackForm.gapType" aria-label="RCA 主要缺口">
                          <el-option label="证据检索不足" value="RETRIEVAL_GAP" />
                          <el-option label="推理判断偏差" value="REASONING_GAP" />
                          <el-option label="取证工具失败" value="TOOL_FAILURE" />
                          <el-option label="调查路由错误" value="ROUTING_GAP" />
                          <el-option label="尚未分类" value="UNKNOWN" />
                        </el-select>
                      </el-form-item>

                      <el-form-item
                        v-if="['PARTIAL', 'INACCURATE'].includes(feedbackForm.accuracy)"
                        class="feedback-wide"
                        label="期望结论"
                      >
                        <el-input
                          v-model="feedbackForm.expectedConclusion"
                          type="textarea"
                          :rows="3"
                          maxlength="2000"
                          show-word-limit
                        />
                      </el-form-item>

                      <el-form-item class="feedback-wide" label="管理员备注">
                        <el-input
                          v-model="feedbackForm.note"
                          type="textarea"
                          :rows="3"
                          maxlength="1000"
                          show-word-limit
                        />
                      </el-form-item>

                      <div class="feedback-actions">
                        <el-button
                          type="primary"
                          :icon="Check"
                          :loading="feedbackSaving"
                          @click="saveRcaFeedback"
                        >
                          {{ rcaFeedback ? '更新反馈' : '保存反馈' }}
                        </el-button>
                      </div>
                    </el-form>
                  </div>

                  <section
                    v-loading="evaluationCatalogLoading || evaluationHistoryLoading || evaluationDetailLoading || evaluationSuiteLoading"
                    class="rca-block evaluation-workbench"
                    aria-labelledby="rca-evaluation-title"
                  >
                    <div class="evaluation-heading">
                      <div>
                        <h4 id="rca-evaluation-title">离线评测</h4>
                        <div class="evaluation-heading-meta">
                          <CnStatusTag type="neutral" size="sm">
                            {{ evaluationCases.length }} 个用例
                          </CnStatusTag>
                          <CnStatusTag type="info" size="sm">
                            {{ evaluationSuites.length }} 个套件
                          </CnStatusTag>
                          <CnStatusTag
                            v-if="latestEvaluationRun"
                            :type="runStatusTone(latestEvaluationRun.status)"
                            size="sm"
                          >
                            最近 {{ runStatusLabel(latestEvaluationRun.status) }} ·
                            {{ latestEvaluationRun.completedCount }}/{{ latestEvaluationRun.caseCount }} 完成
                          </CnStatusTag>
                        </div>
                      </div>
                      <div class="evaluation-actions" role="toolbar" aria-label="RCA 离线评测操作">
                        <el-button
                          :icon="Plus"
                          :loading="evaluationPromoting"
                          :disabled="!canPromoteEvaluationCase || evaluationRunLoading"
                          @click="promoteRcaEvaluationCase"
                        >
                          {{ currentEvaluationCase ? '已提升' : '提升当前修订' }}
                        </el-button>
                        <el-button
                          :icon="Collection"
                          :disabled="!evaluationCases.length || evaluationRunLoading || evaluationPromoting"
                          @click="openEvaluationSuiteDialog"
                        >
                          发布套件
                        </el-button>
                        <el-button
                          type="primary"
                          plain
                          :icon="VideoPlay"
                          :loading="evaluationRunLoading
                            && evaluationRunningCaseId === null
                            && evaluationRunningSuiteVersionId === null"
                          :disabled="!evaluationCases.length || evaluationRunLoading || evaluationPromoting"
                          @click="runRcaEvaluation()"
                        >
                          临时回放全部
                        </el-button>
                        <el-tooltip content="刷新评测数据" placement="top">
                          <el-button
                            circle
                            :icon="Refresh"
                            :disabled="evaluationRunLoading || evaluationPromoting"
                            aria-label="刷新 RCA 评测数据"
                            @click="loadRcaEvaluationWorkbench"
                          />
                        </el-tooltip>
                      </div>
                    </div>

                    <el-tabs v-model="activeEvaluationTab" class="evaluation-tabs">
                      <el-tab-pane :label="`用例 (${evaluationCases.length})`" name="cases">
                        <ol v-if="evaluationCases.length" class="evaluation-case-list">
                          <li v-for="evaluationCase in evaluationCases" :key="evaluationCase.id">
                            <div class="evaluation-case-main">
                              <div class="evaluation-item-heading">
                                <strong>用例 #{{ evaluationCase.id }}</strong>
                                <CnStatusTag
                                  :type="feedbackAccuracyTone(evaluationCase.feedbackAccuracy)"
                                  size="sm"
                                >
                                  {{ feedbackAccuracyLabel(evaluationCase.feedbackAccuracy) }}
                                </CnStatusTag>
                                <CnStatusTag v-if="evaluationCase.contextTruncated" type="warning" size="sm">
                                  上下文已裁剪
                                </CnStatusTag>
                              </div>
                              <p>{{ evaluationCase.expectedConclusion || '沿用基准报告结论' }}</p>
                              <dl class="evaluation-case-meta">
                                <div>
                                  <dt>来源</dt>
                                  <dd>事故 #{{ evaluationCase.incidentId }} · RCA #{{ evaluationCase.sourceRunId }}</dd>
                                </div>
                                <div>
                                  <dt>反馈修订</dt>
                                  <dd>#{{ evaluationCase.sourceFeedbackId }} · {{ formatTime(evaluationCase.promotedAt) }}</dd>
                                </div>
                                <div>
                                  <dt>模型来源</dt>
                                  <dd>{{ evaluationCase.sourceProvider || '-' }} / {{ evaluationCase.sourceActualModel || evaluationCase.sourceConfiguredModel || '-' }}</dd>
                                </div>
                                <div>
                                  <dt>Context SHA-256</dt>
                                  <dd><code>{{ abbreviatedHash(evaluationCase.contextSha256) }}</code></dd>
                                </div>
                              </dl>
                            </div>
                            <el-button
                              type="primary"
                              text
                              :icon="VideoPlay"
                              :loading="evaluationRunLoading && evaluationRunningCaseId === evaluationCase.id"
                              :disabled="evaluationRunLoading || evaluationPromoting"
                              :aria-label="`回放评测用例 ${evaluationCase.id}`"
                              @click="runRcaEvaluation(evaluationCase.id)"
                            >
                              回放
                            </el-button>
                          </li>
                        </ol>
                        <CnEmptyState
                          v-else
                          title="暂无评测用例"
                          description="当前没有已冻结的评测用例。"
                          icon="EV"
                          size="sm"
                          surface="transparent"
                        />
                      </el-tab-pane>

                      <el-tab-pane :label="`套件 (${evaluationSuites.length})`" name="suites">
                        <template v-if="evaluationSuites.length">
                          <div class="evaluation-suite-toolbar">
                            <label for="rca-evaluation-suite-select">评测套件</label>
                            <el-select
                              id="rca-evaluation-suite-select"
                              v-model="selectedEvaluationSuiteId"
                              aria-label="选择 RCA 评测套件"
                              @change="selectRcaEvaluationSuite"
                            >
                              <el-option
                                v-for="suite in evaluationSuites"
                                :key="suite.id"
                                :label="`${suite.name} (${suite.suiteKey})`"
                                :value="suite.id"
                              />
                            </el-select>
                            <el-button :icon="Plus" @click="openEvaluationSuiteDialog">
                              发布版本
                            </el-button>
                          </div>

                          <ol v-if="evaluationSuiteVersions.length" class="evaluation-suite-list">
                            <li v-for="version in evaluationSuiteVersions" :key="version.id">
                              <div class="evaluation-suite-main">
                                <div class="evaluation-item-heading">
                                  <strong>{{ selectedEvaluationSuite?.name || '评测套件' }} v{{ version.version }}</strong>
                                  <CnStatusTag type="neutral" size="sm">
                                    {{ version.caseCount }} 个用例
                                  </CnStatusTag>
                                </div>
                                <dl class="evaluation-suite-meta">
                                  <div>
                                    <dt>质量门槛</dt>
                                    <dd>通过率 {{ evaluationScoreText(version.minimumPassRate) }}% · 均分 {{ evaluationScoreText(version.minimumAverageScore) }}</dd>
                                  </div>
                                  <div>
                                    <dt>安全策略</dt>
                                    <dd>{{ version.requireAllSafety ? '全部只读安全' : '允许安全未通过' }} · {{ version.requireNoDegraded ? '禁止降级' : '允许降级' }}</dd>
                                  </div>
                                  <div>
                                    <dt>Manifest SHA-256</dt>
                                    <dd><code>{{ abbreviatedHash(version.manifestSha256) }}</code></dd>
                                  </div>
                                  <div>
                                    <dt>发布时间</dt>
                                    <dd>{{ formatTime(version.publishedAt) }}</dd>
                                  </div>
                                </dl>
                              </div>
                              <el-button
                                type="primary"
                                text
                                :icon="VideoPlay"
                                :loading="evaluationRunLoading && evaluationRunningSuiteVersionId === version.id"
                                :disabled="evaluationRunLoading || evaluationPromoting"
                                :aria-label="`按套件版本 ${version.version} 执行质量门禁`"
                                @click="runRcaEvaluation(undefined, version.id)"
                              >
                                门禁回放
                              </el-button>
                            </li>
                          </ol>
                          <CnEmptyState
                            v-else-if="!evaluationSuiteLoading"
                            title="暂无套件版本"
                            description="当前套件还没有已发布版本。"
                            icon="EV"
                            size="sm"
                            surface="transparent"
                          />
                        </template>
                        <CnEmptyState
                          v-else
                          title="暂无评测套件"
                          description="当前没有已发布的版本化评测套件。"
                          icon="EV"
                          size="sm"
                          surface="transparent"
                        />
                      </el-tab-pane>

                      <el-tab-pane :label="`历史 (${evaluationRuns.length})`" name="history">
                        <template v-if="evaluationRuns.length">
                          <div class="evaluation-history-toolbar">
                            <label for="rca-evaluation-run-select">评测运行</label>
                            <el-select
                              id="rca-evaluation-run-select"
                              v-model="selectedEvaluationRunId"
                              aria-label="选择 RCA 评测运行"
                              @change="selectRcaEvaluationRun"
                            >
                              <el-option
                                v-for="run in evaluationRuns"
                                :key="run.id"
                                :label="evaluationRunLabel(run)"
                                :value="run.id"
                              />
                            </el-select>
                          </div>

                          <template v-if="displayedEvaluationRun">
                            <dl class="evaluation-summary-grid">
                              <div>
                                <dt>平均分</dt>
                                <dd>{{ evaluationScoreText(displayedEvaluationRun.averageScore) }}</dd>
                              </div>
                              <div>
                                <dt>通过</dt>
                                <dd>{{ displayedEvaluationRun.passedCount }} / {{ displayedEvaluationRun.caseCount }}</dd>
                              </div>
                              <div>
                                <dt>完成</dt>
                                <dd>{{ displayedEvaluationRun.completedCount }} / {{ displayedEvaluationRun.caseCount }}</dd>
                              </div>
                              <div>
                                <dt>状态</dt>
                                <dd>
                                  <CnStatusTag :type="runStatusTone(displayedEvaluationRun.status)" size="sm">
                                    {{ runStatusLabel(displayedEvaluationRun.status) }}
                                  </CnStatusTag>
                                </dd>
                              </div>
                              <div>
                                <dt>质量门禁</dt>
                                <dd>
                                  <CnStatusTag :type="gateStatusTone(displayedEvaluationRun.gateStatus)" size="sm">
                                    {{ gateStatusLabel(displayedEvaluationRun.gateStatus) }}
                                  </CnStatusTag>
                                </dd>
                              </div>
                              <div>
                                <dt>通过率</dt>
                                <dd>{{ evaluationScoreText(displayedEvaluationRun.passRate) }}%</dd>
                              </div>
                            </dl>
                            <dl class="evaluation-run-provenance">
                              <div v-if="displayedEvaluationRun.suiteVersionId">
                                <dt>套件版本</dt>
                                <dd>{{ displayedEvaluationRun.suiteKey || '-' }} v{{ displayedEvaluationRun.suiteVersion || '-' }}</dd>
                              </div>
                              <div v-if="displayedEvaluationRun.suiteManifestSha256">
                                <dt>Manifest SHA-256</dt>
                                <dd><code>{{ abbreviatedHash(displayedEvaluationRun.suiteManifestSha256) }}</code></dd>
                              </div>
                              <div>
                                <dt>Prompt / Schema</dt>
                                <dd><code>{{ displayedEvaluationRun.promptId || '-' }}</code> / <code>{{ displayedEvaluationRun.schemaId || '-' }}</code></dd>
                              </div>
                              <div>
                                <dt>模型来源</dt>
                                <dd>{{ displayedEvaluationRun.provider || '-' }} / {{ displayedEvaluationRun.configuredModel || '-' }}</dd>
                              </div>
                              <div>
                                <dt>源码 / 构建</dt>
                                <dd><code>{{ displayedEvaluationRun.sourceRevision || '-' }}</code> / {{ displayedEvaluationRun.buildId || '-' }} / {{ displayedEvaluationRun.buildVersion || '-' }}</dd>
                              </div>
                              <div>
                                <dt>预算 / 尝试</dt>
                                <dd>{{ displayedEvaluationRun.maxDurationSeconds || 0 }} 秒 / {{ displayedEvaluationRun.attempts || 0 }} 次</dd>
                              </div>
                              <div>
                                <dt>排队 / 截止</dt>
                                <dd>{{ formatTime(displayedEvaluationRun.queuedAt) }} / {{ formatTime(displayedEvaluationRun.deadlineAt) }}</dd>
                              </div>
                              <div>
                                <dt>运行时间</dt>
                                <dd>{{ formatTime(displayedEvaluationRun.startedAt) }} - {{ formatTime(displayedEvaluationRun.completedAt) }}</dd>
                              </div>
                              <div v-if="displayedEvaluationRun.failureCode">
                                <dt>失败码</dt>
                                <dd><code>{{ displayedEvaluationRun.failureCode }}</code></dd>
                              </div>
                              <div v-if="displayedEvaluationRun.gateFailureCodes?.length">
                                <dt>门禁失败码</dt>
                                <dd><code>{{ displayedEvaluationRun.gateFailureCodes.join(', ') }}</code></dd>
                              </div>
                            </dl>
                          </template>

                          <el-collapse
                            v-if="evaluationRunDetail?.results.length"
                            v-model="expandedEvaluationResultIds"
                            class="evaluation-result-list"
                          >
                            <el-collapse-item
                              v-for="result in evaluationRunDetail.results"
                              :key="result.id"
                              :name="result.id"
                            >
                              <template #title>
                                <div class="evaluation-result-title">
                                  <strong>用例 #{{ result.caseId }}</strong>
                                  <span>{{ evaluationScoreText(result.totalScore) }} 分</span>
                                  <CnStatusTag :type="result.passed ? 'success' : 'danger'" size="sm">
                                    {{ result.passed ? '通过' : '未通过' }}
                                  </CnStatusTag>
                                  <CnStatusTag :type="runStatusTone(result.status)" size="sm">
                                    {{ runStatusLabel(result.status) }}
                                  </CnStatusTag>
                                </div>
                              </template>

                              <dl class="evaluation-score-grid" aria-label="RCA 透明分项评分">
                                <div>
                                  <dt>结论相似度</dt>
                                  <dd>{{ evaluationRatioText(result.conclusionSimilarity) }} <small>权重 50</small></dd>
                                </div>
                                <div>
                                  <dt>证据召回</dt>
                                  <dd>{{ evaluationRatioText(result.evidenceRecall) }} <small>权重 25</small></dd>
                                </div>
                                <div>
                                  <dt>严重度一致</dt>
                                  <dd>{{ result.severityMatched ? '通过' : '未通过' }} <small>权重 15</small></dd>
                                </div>
                                <div>
                                  <dt>只读安全</dt>
                                  <dd>{{ result.safetyCompliant ? '通过' : '未通过' }} <small>权重 10</small></dd>
                                </div>
                              </dl>

                              <section class="evaluation-result-section" aria-label="模型来源">
                                <h5>模型来源</h5>
                                <dl class="evaluation-result-provenance">
                                  <div>
                                    <dt>Prompt</dt>
                                    <dd><code>{{ result.promptId || '-' }}</code></dd>
                                  </div>
                                  <div>
                                    <dt>Schema</dt>
                                    <dd><code>{{ result.schemaId || '-' }}</code></dd>
                                  </div>
                                  <div>
                                    <dt>Provider</dt>
                                    <dd>{{ result.provider || '-' }}</dd>
                                  </div>
                                  <div>
                                    <dt>配置 / 实际模型</dt>
                                    <dd>{{ result.configuredModel || '-' }} / {{ result.actualModel || '-' }}</dd>
                                  </div>
                                  <div>
                                    <dt>调用结果</dt>
                                    <dd>{{ invocationOutcomeLabel(result.invocationOutcome) }}</dd>
                                  </div>
                                  <div v-if="result.failureCode">
                                    <dt>失败码</dt>
                                    <dd><code>{{ result.failureCode }}</code></dd>
                                  </div>
                                </dl>
                              </section>

                              <section v-if="result.explanations.length" class="evaluation-result-section">
                                <h5>评分说明</h5>
                                <ul class="evaluation-explanations">
                                  <li v-for="(explanation, index) in result.explanations" :key="`${result.id}-explanation-${index}`">
                                    {{ explanation }}
                                  </li>
                                </ul>
                              </section>

                              <section v-if="result.candidateReport" class="evaluation-result-section candidate-report">
                                <div class="candidate-report-heading">
                                  <h5>候选报告</h5>
                                  <div>
                                    <CnStatusTag :type="conclusionTone(result.candidateReport.conclusionStatus)" size="sm">
                                      {{ conclusionLabel(result.candidateReport.conclusionStatus) }}
                                    </CnStatusTag>
                                    <CnStatusTag :type="result.candidateReport.executionAllowed ? 'danger' : 'neutral'" size="sm">
                                      {{ result.candidateReport.executionAllowed ? '允许执行' : '禁止自动执行' }}
                                    </CnStatusTag>
                                  </div>
                                </div>
                                <p class="candidate-summary">{{ result.candidateReport.executiveSummary || '暂无执行摘要。' }}</p>
                                <div class="candidate-report-columns">
                                  <div>
                                    <h6>原因假设</h6>
                                    <ul v-if="result.candidateReport.hypotheses?.length">
                                      <li v-for="(hypothesis, index) in result.candidateReport.hypotheses" :key="`${result.id}-hypothesis-${index}`">
                                        <strong>{{ hypothesis.title || '-' }}</strong>
                                        <span>{{ hypothesis.reasoning || '-' }}</span>
                                      </li>
                                    </ul>
                                    <p v-else>暂无原因假设。</p>
                                  </div>
                                  <div>
                                    <h6>建议动作</h6>
                                    <ul v-if="result.candidateReport.recommendedNextSteps?.length">
                                      <li v-for="(step, index) in result.candidateReport.recommendedNextSteps" :key="`${result.id}-step-${index}`">
                                        <strong>{{ riskLabel(step.risk) }}</strong>
                                        <span>{{ step.description || '-' }}</span>
                                      </li>
                                    </ul>
                                    <p v-else>暂无建议动作。</p>
                                  </div>
                                </div>
                              </section>
                              <CnEmptyState
                                v-else
                                title="候选报告不可用"
                                :description="result.failureCode || '该用例未形成可展示的结构化报告。'"
                                icon="AI"
                                size="sm"
                                surface="transparent"
                              />
                            </el-collapse-item>
                          </el-collapse>
                          <CnEmptyState
                            v-else-if="!evaluationDetailLoading"
                            title="暂无逐用例结果"
                            :description="isEvaluationRunActive(displayedEvaluationRun?.status)
                              ? '运行已入队，结果会在执行过程中自动刷新。'
                              : '该运行尚未写入评测结果。'"
                            icon="EV"
                            size="sm"
                            surface="transparent"
                          />
                        </template>
                        <CnEmptyState
                          v-else
                          title="暂无评测历史"
                          description="当前没有已保存的评测运行。"
                          icon="EV"
                          size="sm"
                          surface="transparent"
                        />
                      </el-tab-pane>
                    </el-tabs>
                  </section>

                  <div v-if="investigationSteps.length" class="rca-block investigation-trace">
                    <h4>调查轨迹</h4>
                    <ol>
                      <li v-for="step in investigationSteps" :key="step.id || step.order">
                        <span class="trace-index" aria-hidden="true">{{ step.order }}</span>
                        <div>
                          <div class="trace-heading">
                            <strong>{{ investigationStepLabel(step.code) }}</strong>
                            <CnStatusTag :type="runStatusTone(step.status)" size="sm">
                              {{ runStatusLabel(step.status) }}
                            </CnStatusTag>
                          </div>
                          <p>{{ step.detail || '-' }}</p>
                          <time>{{ formatTime(step.recordedAt) }}</time>
                        </div>
                      </li>
                    </ol>
                  </div>

                  <div class="rca-block">
                    <h4>观测事实</h4>
                    <ol v-if="rcaReport.observations?.length" class="rca-list">
                      <li v-for="(observation, index) in rcaReport.observations" :key="`observation-${index}`">
                        <span>{{ observation.statement }}</span>
                        <small>{{ evidenceIdsText(observation.evidenceIds) }}</small>
                      </li>
                    </ol>
                    <p v-else class="empty-copy">暂无观测事实。</p>
                  </div>

                  <div class="rca-block">
                    <h4>原因假设</h4>
                    <div v-if="rcaReport.hypotheses?.length" class="hypothesis-list">
                      <article v-for="(hypothesis, index) in rcaReport.hypotheses" :key="`hypothesis-${index}`" class="hypothesis-item">
                        <div class="hypothesis-head">
                          <strong>{{ hypothesis.title }}</strong>
                          <CnStatusTag type="info" size="sm">置信度 {{ formatConfidence(hypothesis.confidence) }}</CnStatusTag>
                        </div>
                        <p>{{ hypothesis.reasoning }}</p>
                        <small>{{ evidenceIdsText(hypothesis.evidenceIds) }}</small>
                        <ul v-if="hypothesis.nextChecks?.length">
                          <li v-for="(check, checkIndex) in hypothesis.nextChecks" :key="`check-${index}-${checkIndex}`">
                            {{ check }}
                          </li>
                        </ul>
                      </article>
                    </div>
                    <p v-else class="empty-copy">暂无原因假设。</p>
                  </div>

                  <div class="rca-block">
                    <h4>建议动作</h4>
                    <ol v-if="rcaReport.recommendedNextSteps?.length" class="recommendation-list">
                      <li v-for="(step, index) in rcaReport.recommendedNextSteps" :key="`step-${index}`">
                        <div>
                          <span>{{ step.description }}</span>
                          <small>{{ evidenceIdsText(step.evidenceIds) }}</small>
                        </div>
                        <CnStatusTag :type="riskTone(step.risk)" size="sm">{{ riskLabel(step.risk) }}</CnStatusTag>
                      </li>
                    </ol>
                    <p v-else class="empty-copy">暂无建议动作。</p>
                  </div>

                  <div class="rca-block">
                    <h4>证据引用</h4>
                    <div v-if="rcaReport.evidenceReferences?.length" class="reference-list">
                      <span v-for="reference in rcaReport.evidenceReferences" :key="reference.id">
                        #{{ reference.id }} · {{ reference.sourceType }} · {{ reference.status }}
                      </span>
                    </div>
                    <p v-else class="empty-copy">暂无证据引用。</p>
                  </div>

                  <div v-if="rcaReport.limitations?.length" class="rca-block limitation-block">
                    <h4>限制</h4>
                    <ul>
                      <li v-for="(limitation, index) in rcaReport.limitations" :key="`limitation-${index}`">
                        {{ limitation }}
                      </li>
                    </ul>
                  </div>
                </template>

                <div v-else-if="selectedRcaRun" class="rca-empty">
                  <CnEmptyState
                    :title="selectedRcaRun.status === 'RUNNING' ? '调查正在进行' : '调查报告不可用'"
                    :description="selectedRcaRun.failureCode || '该次运行尚未生成可恢复的结构化报告。'"
                    icon="AI"
                    surface="transparent"
                  />
                </div>

                <div v-else class="rca-empty">
                  <CnEmptyState
                    title="尚未生成 RCA"
                    description="当前事故没有已生成的根因分析报告。"
                    icon="AI"
                    surface="transparent"
                  />
                  <el-button type="primary" :icon="MagicStick" :loading="rcaLoading" @click="generateRca">
                    生成 AI 分析
                  </el-button>
                </div>
              </section>
            </el-tab-pane>
          </el-tabs>
        </template>
      </div>
    </el-drawer>

    <el-dialog
      v-model="evaluationSuiteDialogVisible"
      class="evaluation-suite-dialog"
      title="发布评测套件版本"
      :width="evaluationSuiteDialogWidth"
      destroy-on-close
      :close-on-click-modal="false"
      @closed="resetEvaluationSuiteForm"
    >
      <el-form label-position="top" @submit.prevent="publishEvaluationSuiteVersion">
        <el-form-item label="套件来源" required>
          <el-radio-group v-model="evaluationSuiteForm.mode" aria-label="选择套件来源">
            <el-radio-button v-if="evaluationSuites.length" value="existing">现有套件</el-radio-button>
            <el-radio-button value="new">新建套件</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="evaluationSuiteForm.mode === 'existing'" label="评测套件" required>
          <el-select v-model="evaluationSuiteForm.suiteId" aria-label="选择要发布版本的评测套件">
            <el-option
              v-for="suite in evaluationSuites"
              :key="suite.id"
              :label="`${suite.name} (${suite.suiteKey})`"
              :value="suite.id"
            />
          </el-select>
        </el-form-item>

        <div v-else class="evaluation-suite-create-fields">
          <el-form-item label="套件 Key" required>
            <el-input v-model.trim="evaluationSuiteForm.suiteKey" maxlength="64" placeholder="release-readiness" />
          </el-form-item>
          <el-form-item label="套件名称" required>
            <el-input v-model.trim="evaluationSuiteForm.name" maxlength="128" placeholder="发布准入" />
          </el-form-item>
          <el-form-item class="evaluation-suite-wide" label="套件说明">
            <el-input v-model.trim="evaluationSuiteForm.description" maxlength="500" />
          </el-form-item>
        </div>

        <fieldset class="evaluation-suite-cases">
          <legend>冻结用例</legend>
          <el-checkbox-group v-model="evaluationSuiteForm.caseIds">
            <el-checkbox
              v-for="evaluationCase in evaluationCases"
              :key="evaluationCase.id"
              :label="evaluationCase.id"
            >
              #{{ evaluationCase.id }} · {{ evaluationCase.expectedConclusion || '沿用基准报告结论' }}
            </el-checkbox>
          </el-checkbox-group>
        </fieldset>

        <div class="evaluation-suite-policy-grid">
          <el-form-item label="最低通过率 (%)" required>
            <el-input-number
              v-model="evaluationSuiteForm.minimumPassRate"
              :min="0"
              :max="100"
              :step="1"
              controls-position="right"
            />
          </el-form-item>
          <el-form-item label="最低平均分" required>
            <el-input-number
              v-model="evaluationSuiteForm.minimumAverageScore"
              :min="0"
              :max="100"
              :step="1"
              controls-position="right"
            />
          </el-form-item>
        </div>

        <div class="evaluation-suite-switches">
          <label>
            <span>全部结果只读安全</span>
            <el-switch v-model="evaluationSuiteForm.requireAllSafety" />
          </label>
          <label>
            <span>禁止降级或失败</span>
            <el-switch v-model="evaluationSuiteForm.requireNoDegraded" />
          </label>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="evaluationSuiteDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="evaluationSuitePublishing"
          :disabled="!canPublishEvaluationSuiteVersion"
          @click="publishEvaluationSuiteVersion"
        >
          发布版本
        </el-button>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, CircleCheck, Collection, Download, MagicStick, Plus, Refresh, VideoPlay, View } from '@element-plus/icons-vue'
import { sreApi } from '@/api/sre'
import {
  CnDataTable,
  CnEmptyState,
  CnFilterForm,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag
} from '@/design-system'
import type { CnBreadcrumbItem, CnFilterField, CnPagination, CnTableColumn, CnTone } from '@/design-system'

interface IncidentSummary {
  totalCount: number
  activeCount: number
  openCount: number
  acknowledgedCount: number
  criticalActiveCount: number
  warningActiveCount: number
  resolvedCount: number
  lastObservedAt?: string
}

interface Incident {
  id: number
  incidentNo?: string
  service?: string
  alertName?: string
  severity?: string
  state?: string
  summary?: string
  firstSeen?: string
  lastSeen?: string
  acknowledgedAt?: string
  resolvedAt?: string
}

interface IncidentPage {
  pageNum?: number
  pageSize?: number
  total?: number
  records?: Incident[]
}

interface InvestigationAlert {
  id: number
  source?: string
  alertName?: string
  status?: string
  startsAt?: string
  observedAt?: string
}

interface InvestigationEvidence {
  id: number
  sourceType?: string
  sourceRef?: string
  queryDescription?: string
  capturedAt?: string
  status?: string
  snapshot?: Record<string, unknown>
  snapshotTruncated?: boolean
}

interface InvestigationContext {
  incident?: Incident
  alerts?: InvestigationAlert[]
  evidence?: InvestigationEvidence[]
  alertsTruncated?: boolean
  evidenceTruncated?: boolean
}

interface RcaReport {
  incidentId?: number
  incidentNo?: string
  generationMode?: string
  conclusionStatus?: string
  severityAssessment?: string
  executiveSummary?: string
  observations?: Array<{ statement?: string; evidenceIds?: number[] }>
  hypotheses?: Array<{
    title?: string
    reasoning?: string
    confidence?: number
    evidenceStatus?: string
    evidenceIds?: number[]
    counterEvidenceIds?: number[]
    nextChecks?: string[]
  }>
  recommendedNextSteps?: Array<{ description?: string; risk?: string; evidenceIds?: number[] }>
  evidenceReferences?: Array<{ id: number; sourceType?: string; status?: string }>
  limitations?: string[]
  executionAllowed?: boolean
  generatedAt?: string
}

interface RcaRunSummary {
  id: number
  incidentId?: number
  status?: string
  triggerSource?: string
  requestedBy?: number
  generationMode?: string
  conclusionStatus?: string
  alertCount?: number
  evidenceCount?: number
  contextTruncated?: boolean
  failureCode?: string
  startedAt?: string
  completedAt?: string
}

interface RcaFeedback {
  id?: number
  runId?: number
  accuracy?: string
  gapType?: string
  note?: string
  expectedConclusion?: string
  reviewedBy?: number
  reviewedAt?: string
}

interface RcaFeedbackForm {
  accuracy: string
  gapType: string
  note: string
  expectedConclusion: string
}

interface RcaProvenance {
  artifactId?: number
  promptId?: string
  schemaId?: string
  provider?: string
  configuredModel?: string
  actualModel?: string
  invocationOutcome?: string
  contextSha256?: string
  contextLength?: number
  contextTruncated?: boolean
  createdAt?: string
}

interface InvestigationStep {
  id?: number
  order: number
  code?: string
  status?: string
  detail?: string
  recordedAt?: string
}

interface RcaRunDetail {
  run?: RcaRunSummary
  steps?: InvestigationStep[]
  report?: RcaReport
  provenance?: RcaProvenance
  feedback?: RcaFeedback
}

interface RcaEvaluationCaseSummary {
  id: number
  incidentId: number
  sourceRunId: number
  sourceArtifactId?: number
  sourceFeedbackId: number
  contextSha256?: string
  contextLength?: number
  contextTruncated?: boolean
  expectedConclusion?: string
  feedbackAccuracy?: string
  feedbackGapType?: string
  sourcePromptId?: string
  sourceSchemaId?: string
  sourceProvider?: string
  sourceConfiguredModel?: string
  sourceActualModel?: string
  sourceInvocationOutcome?: string
  promotedBy?: number
  promotedAt?: string
}

interface RcaEvaluationRunSummary {
  id: number
  status?: string
  requestedCaseId?: number
  suiteVersionId?: number
  suiteKey?: string
  suiteVersion?: number
  suiteManifestSha256?: string
  triggerSource?: string
  requestedBy?: number
  caseCount: number
  completedCount: number
  passedCount: number
  failedCount: number
  averageScore?: number | string | null
  passRate?: number | string | null
  unsafeCount: number
  degradedCount: number
  promptId?: string
  schemaId?: string
  scoringPolicyId?: string
  gateEvaluatorId?: string
  gateStatus?: string
  gateMinimumPassRate?: number | string | null
  gateMinimumAverageScore?: number | string | null
  gateRequireAllSafety: boolean
  gateRequireNoDegraded: boolean
  gateFailureCodes?: string[]
  provider?: string
  configuredModel?: string
  failureCode?: string
  attempts: number
  maxDurationSeconds: number
  sourceRevision?: string
  buildId?: string
  buildVersion?: string
  queuedAt?: string
  claimedAt?: string
  heartbeatAt?: string
  deadlineAt?: string
  startedAt?: string
  completedAt?: string
}

interface RcaEvaluationCaseResult {
  id: number
  runId: number
  caseId: number
  status?: string
  candidateReport?: RcaReport | null
  promptId?: string
  schemaId?: string
  provider?: string
  configuredModel?: string
  actualModel?: string
  invocationOutcome?: string
  conclusionSimilarity?: number | string
  evidenceRecall?: number | string
  severityMatched: boolean
  safetyCompliant: boolean
  totalScore?: number | string
  passed: boolean
  explanations: string[]
  failureCode?: string
  startedAt?: string
  completedAt?: string
}

interface RcaEvaluationRunDetail {
  run: RcaEvaluationRunSummary
  results: RcaEvaluationCaseResult[]
}

interface RcaEvaluationSuiteSummary {
  id: number
  suiteKey: string
  name: string
  description?: string
  createdBy?: number
  createdAt?: string
}

interface RcaEvaluationSuiteVersionSummary {
  id: number
  suiteId: number
  version: number
  caseCount: number
  manifestSha256?: string
  manifestSchemaId?: string
  scoringPolicyId?: string
  gateEvaluatorId?: string
  minimumPassRate?: number | string | null
  minimumAverageScore?: number | string | null
  requireAllSafety: boolean
  requireNoDegraded: boolean
  publishedBy?: number
  publishedAt?: string
}

interface RcaEvaluationSuiteForm {
  mode: 'existing' | 'new'
  suiteId: number | null
  suiteKey: string
  name: string
  description: string
  caseIds: number[]
  minimumPassRate: number
  minimumAverageScore: number
  requireAllSafety: boolean
  requireNoDegraded: boolean
}

interface FilterState extends Record<string, unknown> {
  state: string
  severity: string
  service: string
}

const emptySummary = (): IncidentSummary => ({
  totalCount: 0,
  activeCount: 0,
  openCount: 0,
  acknowledgedCount: 0,
  criticalActiveCount: 0,
  warningActiveCount: 0,
  resolvedCount: 0
})

const breadcrumbs: CnBreadcrumbItem[] = [
  { label: '管理后台' },
  { label: 'SRE 运维中心' },
  { label: '事故工作台' }
]

const filterFields: CnFilterField[] = [
  {
    prop: 'state',
    label: '事故状态',
    type: 'select',
    options: [
      { label: '待确认', value: 'OPEN' },
      { label: '处理中', value: 'ACKNOWLEDGED' },
      { label: '已恢复', value: 'RESOLVED' },
      { label: '已关闭', value: 'CLOSED' }
    ]
  },
  {
    prop: 'severity',
    label: '严重程度',
    type: 'select',
    options: [
      { label: '严重', value: 'critical' },
      { label: '警告', value: 'warning' },
      { label: '信息', value: 'info' }
    ]
  },
  {
    prop: 'service',
    label: '服务',
    type: 'input',
    placeholder: '例如 code-nest'
  }
]

const incidentColumns: CnTableColumn<Incident>[] = [
  { label: '级别', prop: 'severity', slot: 'severity', width: 92, align: 'center' },
  { label: '状态', prop: 'state', slot: 'state', width: 104, align: 'center' },
  { label: '事故编号', prop: 'incidentNo', width: 190, showOverflowTooltip: true },
  { label: '告警与摘要', prop: 'summary', slot: 'summary', minWidth: 300 },
  { label: '服务', prop: 'service', width: 130, showOverflowTooltip: true },
  { label: '最近观测', prop: 'lastSeen', slot: 'lastSeen', width: 168 },
  { label: '操作', prop: 'actions', slot: 'actions', width: 92, align: 'center', fixed: 'right' }
]

const filters = reactive<FilterState>({ state: '', severity: '', service: '' })
const summary = ref<IncidentSummary>(emptySummary())
const incidents = ref<Incident[]>([])
const summaryLoading = ref(false)
const listLoading = ref(false)
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const liveStatus = ref('')
const viewportWidth = ref(typeof window === 'undefined' ? 1440 : window.innerWidth)

const drawerVisible = ref(false)
const detailLoading = ref(false)
const selectedIncident = ref<Incident | null>(null)
const investigationContext = ref<InvestigationContext | null>(null)
const activeDetailTab = ref('overview')
const expandedEvidenceIds = ref<Array<number | string>>([])
const rcaLoading = ref(false)
const rcaHistoryLoading = ref(false)
const rcaReport = ref<RcaReport | null>(null)
const rcaRuns = ref<RcaRunSummary[]>([])
const selectedRcaRunId = ref<number | null>(null)
const investigationSteps = ref<InvestigationStep[]>([])
const rcaProvenance = ref<RcaProvenance | null>(null)
const rcaFeedback = ref<RcaFeedback | null>(null)
const feedbackForm = reactive<RcaFeedbackForm>({
  accuracy: '',
  gapType: '',
  note: '',
  expectedConclusion: ''
})
const feedbackSaving = ref(false)
const evaluationExporting = ref(false)
const evaluationCases = ref<RcaEvaluationCaseSummary[]>([])
const evaluationSuites = ref<RcaEvaluationSuiteSummary[]>([])
const evaluationSuiteVersions = ref<RcaEvaluationSuiteVersionSummary[]>([])
const selectedEvaluationSuiteId = ref<number | null>(null)
const evaluationRuns = ref<RcaEvaluationRunSummary[]>([])
const selectedEvaluationRunId = ref<number | null>(null)
const evaluationRunDetail = ref<RcaEvaluationRunDetail | null>(null)
const expandedEvaluationResultIds = ref<number[]>([])
const activeEvaluationTab = ref('cases')
const evaluationCatalogLoading = ref(false)
const evaluationHistoryLoading = ref(false)
const evaluationDetailLoading = ref(false)
const evaluationPromoting = ref(false)
const evaluationRunLoading = ref(false)
const evaluationRunningCaseId = ref<number | null>(null)
const evaluationRunningSuiteVersionId = ref<number | null>(null)
const evaluationActiveRunId = ref<number | null>(null)
const evaluationSuiteLoading = ref(false)
const evaluationSuiteDialogVisible = ref(false)
const evaluationSuitePublishing = ref(false)
const evaluationSuiteForm = reactive<RcaEvaluationSuiteForm>({
  mode: 'new',
  suiteId: null,
  suiteKey: '',
  name: '',
  description: '',
  caseIds: [],
  minimumPassRate: 100,
  minimumAverageScore: 70,
  requireAllSafety: true,
  requireNoDegraded: true
})
const actionLoading = ref<'ack' | 'resolve' | ''>('')
let investigationRequestVersion = 0
let rcaRequestVersion = 0
let evaluationRequestVersion = 0
let evaluationPollVersion = 0
let evaluationPollTimer: ReturnType<typeof setTimeout> | null = null

const refreshing = computed(() => summaryLoading.value || listLoading.value)
const headerDescription = computed(() => `最近观测：${formatTime(summary.value.lastObservedAt)}`)
const overallTone = computed<CnTone>(() => {
  if (summary.value.criticalActiveCount > 0) return 'danger'
  if (summary.value.activeCount > 0) return 'warning'
  return 'success'
})
const overallLabel = computed(() => {
  if (summary.value.criticalActiveCount > 0) return '存在严重事故'
  if (summary.value.activeCount > 0) return '存在待处理事故'
  return '无活动事故'
})
const pagination = computed(() => ({ total: total.value }))
const tablePagination = computed<CnPagination>(() => ({
  page: pageNum.value,
  pageSize: pageSize.value,
  total: total.value,
  pageSizes: [20, 50, 100],
  background: true
}))
const drawerSize = computed(() => Math.min(780, Math.max(280, Math.floor(viewportWidth.value * 0.94))))
const evaluationSuiteDialogWidth = computed(() => `${Math.min(640, Math.max(280, viewportWidth.value - 32))}px`)
const drawerTitle = computed(() => selectedIncident.value?.incidentNo || '事故详情')
const contextAlerts = computed(() => investigationContext.value?.alerts || [])
const contextEvidence = computed(() => investigationContext.value?.evidence || [])
const selectedRcaRun = computed(() => (
  rcaRuns.value.find((run) => run.id === selectedRcaRunId.value) || null
))
const latestEvaluationRun = computed(() => evaluationRuns.value[0] || null)
const selectedEvaluationSuite = computed(() => (
  evaluationSuites.value.find((suite) => suite.id === selectedEvaluationSuiteId.value) || null
))
const displayedEvaluationRun = computed(() => (
  evaluationRunDetail.value?.run
  || evaluationRuns.value.find((run) => run.id === selectedEvaluationRunId.value)
  || null
))
const currentEvaluationCase = computed(() => {
  const feedbackId = rcaFeedback.value?.id
  if (!feedbackId) return null
  return evaluationCases.value.find((item) => item.sourceFeedbackId === feedbackId) || null
})
const canPromoteEvaluationCase = computed(() => (
  Boolean(selectedIncident.value?.id && selectedRcaRunId.value && rcaFeedback.value?.id)
  && !currentEvaluationCase.value
  && !evaluationPromoting.value
))
const canPublishEvaluationSuiteVersion = computed(() => {
  if (!evaluationSuiteForm.caseIds.length || evaluationSuitePublishing.value) return false
  if (evaluationSuiteForm.mode === 'existing') return Boolean(evaluationSuiteForm.suiteId)
  return /^[a-z][a-z0-9-]{2,63}$/.test(evaluationSuiteForm.suiteKey)
    && Boolean(evaluationSuiteForm.name.trim())
})
const isCurrentIncident = (incidentId: number) => (
  drawerVisible.value && selectedIncident.value?.id === incidentId
)
const canAcknowledge = computed(() => selectedIncident.value?.state === 'OPEN')
const canResolve = computed(() => !['RESOLVED', 'CLOSED'].includes(selectedIncident.value?.state || ''))

const resetFeedbackForm = (feedback?: RcaFeedback | null) => {
  Object.assign(feedbackForm, {
    accuracy: feedback?.accuracy || '',
    gapType: feedback?.gapType || '',
    note: feedback?.note || '',
    expectedConclusion: feedback?.expectedConclusion || ''
  })
}

const buildQuery = () => ({
  state: filters.state || undefined,
  severity: filters.severity || undefined,
  service: String(filters.service || '').trim() || undefined,
  pageNum: pageNum.value,
  pageSize: pageSize.value
})

const updateFilters = (value: Record<string, unknown>) => {
  Object.assign(filters, value)
}

const loadSummary = async () => {
  summaryLoading.value = true
  try {
    const response = await sreApi.getIncidentSummary() as Partial<IncidentSummary> | null
    summary.value = { ...emptySummary(), ...(response || {}) }
  } finally {
    summaryLoading.value = false
  }
}

const loadIncidents = async () => {
  listLoading.value = true
  try {
    const response = await sreApi.getIncidents(buildQuery()) as IncidentPage | null
    incidents.value = response?.records || []
    total.value = Number(response?.total || 0)
  } finally {
    listLoading.value = false
  }
}

const refreshWorkbench = async (notify = false) => {
  liveStatus.value = '正在刷新事故数据'
  try {
    await Promise.all([loadSummary(), loadIncidents()])
    liveStatus.value = `刷新完成，共 ${total.value} 条事故记录`
    if (notify) ElMessage.success('事故数据已刷新')
  } catch (error) {
    console.error('刷新 SRE 事故数据失败:', error)
    liveStatus.value = '事故数据刷新失败'
  }
}

const applyFilters = () => {
  pageNum.value = 1
  refreshWorkbench()
}

const resetFilters = () => {
  Object.assign(filters, { state: '', severity: '', service: '' })
  pageNum.value = 1
  refreshWorkbench()
}

const changePage = (page: number) => {
  pageNum.value = page
  loadIncidents().catch((error) => console.error('加载 SRE 事故列表失败:', error))
}

const changePageSize = (size: number) => {
  pageSize.value = size
  pageNum.value = 1
  loadIncidents().catch((error) => console.error('加载 SRE 事故列表失败:', error))
}

const openIncident = async (incident: Incident) => {
  stopEvaluationRunPolling()
  investigationRequestVersion += 1
  rcaRequestVersion += 1
  evaluationRequestVersion += 1
  selectedIncident.value = { ...incident }
  investigationContext.value = null
  rcaReport.value = null
  rcaRuns.value = []
  selectedRcaRunId.value = null
  investigationSteps.value = []
  rcaProvenance.value = null
  rcaFeedback.value = null
  resetFeedbackForm()
  evaluationCases.value = []
  evaluationSuites.value = []
  evaluationSuiteVersions.value = []
  selectedEvaluationSuiteId.value = null
  evaluationRuns.value = []
  selectedEvaluationRunId.value = null
  evaluationRunDetail.value = null
  expandedEvaluationResultIds.value = []
  evaluationRunLoading.value = false
  evaluationRunningCaseId.value = null
  evaluationRunningSuiteVersionId.value = null
  activeEvaluationTab.value = 'cases'
  activeDetailTab.value = 'overview'
  expandedEvidenceIds.value = []
  drawerVisible.value = true
  await Promise.all([
    loadInvestigationContext(incident.id),
    loadRcaRuns(incident.id),
    loadRcaEvaluationWorkbench()
  ])
}

const loadInvestigationContext = async (incidentId: number) => {
  const requestVersion = ++investigationRequestVersion
  detailLoading.value = true
  try {
    const response = await sreApi.getInvestigationContext(incidentId) as InvestigationContext | null
    if (requestVersion !== investigationRequestVersion || !isCurrentIncident(incidentId)) return
    investigationContext.value = response
    if (response?.incident) selectedIncident.value = { ...selectedIncident.value, ...response.incident }
    expandedEvidenceIds.value = (response?.evidence || []).slice(0, 1).map((item) => item.id)
  } catch (error) {
    console.error('加载 SRE 事故详情失败:', error)
  } finally {
    if (requestVersion === investigationRequestVersion) detailLoading.value = false
  }
}

const loadRcaRuns = async (incidentId: number) => {
  const requestVersion = ++rcaRequestVersion
  rcaHistoryLoading.value = true
  try {
    const response = await sreApi.getRcaRuns(incidentId, 20) as RcaRunSummary[] | null
    if (requestVersion !== rcaRequestVersion || !isCurrentIncident(incidentId)) return
    rcaRuns.value = Array.isArray(response) ? response : []
    if (!rcaRuns.value.length) {
      selectedRcaRunId.value = null
      rcaReport.value = null
      investigationSteps.value = []
      rcaProvenance.value = null
      rcaFeedback.value = null
      resetFeedbackForm()
      return
    }
    const selectedStillExists = rcaRuns.value.some((run) => run.id === selectedRcaRunId.value)
    const runId = selectedStillExists ? selectedRcaRunId.value : rcaRuns.value[0].id
    if (runId != null) await loadRcaRun(incidentId, runId, false, requestVersion)
  } catch (error) {
    console.error('加载 SRE RCA 历史失败:', error)
  } finally {
    if (requestVersion === rcaRequestVersion) rcaHistoryLoading.value = false
  }
}

const loadRcaRun = async (
  incidentId: number,
  runId: number,
  manageLoading = true,
  inheritedRequestVersion?: number
) => {
  const requestVersion = inheritedRequestVersion ?? ++rcaRequestVersion
  if (manageLoading) rcaHistoryLoading.value = true
  rcaProvenance.value = null
  rcaFeedback.value = null
  resetFeedbackForm()
  try {
    const response = await sreApi.getRcaRun(incidentId, runId) as RcaRunDetail | null
    if (requestVersion !== rcaRequestVersion || !isCurrentIncident(incidentId)) return
    selectedRcaRunId.value = runId
    rcaReport.value = response?.report || null
    investigationSteps.value = response?.steps || []
    rcaProvenance.value = response?.provenance || null
    rcaFeedback.value = response?.feedback || null
    resetFeedbackForm(rcaFeedback.value)
    if (response?.run) {
      const index = rcaRuns.value.findIndex((run) => run.id === response.run?.id)
      if (index >= 0) rcaRuns.value[index] = response.run
    }
  } catch (error) {
    console.error('加载 SRE RCA 详情失败:', error)
    if (requestVersion === rcaRequestVersion && isCurrentIncident(incidentId)) {
      rcaReport.value = null
      investigationSteps.value = []
      rcaProvenance.value = null
      rcaFeedback.value = null
      resetFeedbackForm()
    }
  } finally {
    if (manageLoading && requestVersion === rcaRequestVersion) rcaHistoryLoading.value = false
  }
}

const selectRcaRun = (value: number | string) => {
  const incidentId = selectedIncident.value?.id
  const runId = Number(value)
  if (!incidentId || !Number.isInteger(runId) || runId <= 0) return
  loadRcaRun(incidentId, runId).catch((error) => console.error('切换 SRE RCA 历史失败:', error))
}

const handleAccuracyChange = (value: string | number | boolean | undefined) => {
  if (value === 'ACCURATE') {
    feedbackForm.gapType = ''
    feedbackForm.expectedConclusion = ''
  }
}

const saveRcaFeedback = async () => {
  const incidentId = selectedIncident.value?.id
  const runId = selectedRcaRunId.value
  if (!incidentId || !runId || feedbackSaving.value) return
  if (!feedbackForm.accuracy) {
    ElMessage.warning('请选择 RCA 准确度')
    return
  }
  const requiresCorrection = ['PARTIAL', 'INACCURATE'].includes(feedbackForm.accuracy)
  if (requiresCorrection && !feedbackForm.gapType) {
    ElMessage.warning('请选择主要缺口')
    return
  }
  if (requiresCorrection && !feedbackForm.expectedConclusion.trim()) {
    ElMessage.warning('请填写期望结论')
    return
  }

  const requestVersion = rcaRequestVersion
  feedbackSaving.value = true
  try {
    const response = await sreApi.saveRcaFeedback(incidentId, runId, {
      accuracy: feedbackForm.accuracy,
      gapType: requiresCorrection ? feedbackForm.gapType : null,
      note: feedbackForm.note.trim() || null,
      expectedConclusion: requiresCorrection ? feedbackForm.expectedConclusion.trim() : null
    }) as RcaFeedback
    if (requestVersion !== rcaRequestVersion || !isCurrentIncident(incidentId)
      || selectedRcaRunId.value !== runId) return
    rcaFeedback.value = response
    resetFeedbackForm(response)
    liveStatus.value = 'RCA 分析反馈已保存'
    ElMessage.success('RCA 分析反馈已保存')
  } catch (error) {
    console.error('保存 SRE RCA 反馈失败:', error)
    liveStatus.value = 'RCA 分析反馈保存失败'
  } finally {
    feedbackSaving.value = false
  }
}

const exportRcaEvaluationSample = async () => {
  const incidentId = selectedIncident.value?.id
  const runId = selectedRcaRunId.value
  if (!incidentId || !runId || !rcaFeedback.value || evaluationExporting.value) return
  const requestVersion = rcaRequestVersion
  evaluationExporting.value = true
  try {
    const sample = await sreApi.getRcaEvaluationSample(incidentId, runId)
    if (requestVersion !== rcaRequestVersion || !isCurrentIncident(incidentId)
      || selectedRcaRunId.value !== runId) return
    const blobUrl = URL.createObjectURL(new Blob(
      [JSON.stringify(sample, null, 2)],
      { type: 'application/json;charset=utf-8' }
    ))
    const link = document.createElement('a')
    link.href = blobUrl
    link.download = `sre-rca-eval-${incidentId}-${runId}.json`
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(blobUrl)
    liveStatus.value = 'RCA 评测样本已导出'
    ElMessage.success('RCA 评测样本已导出')
  } catch (error) {
    console.error('导出 SRE RCA 评测样本失败:', error)
    liveStatus.value = 'RCA 评测样本导出失败'
  } finally {
    evaluationExporting.value = false
  }
}

const EVALUATION_TERMINAL_STATUSES = new Set(['SUCCEEDED', 'DEGRADED', 'FAILED'])

const normalizeEvaluationRunDetail = (
  response: RcaEvaluationRunDetail | null
): RcaEvaluationRunDetail | null => response
  ? {
      ...response,
      results: Array.isArray(response.results) ? response.results.map((item) => ({
        ...item,
        explanations: Array.isArray(item.explanations) ? item.explanations : []
      })) : []
    }
  : null

const upsertEvaluationRun = (run: RcaEvaluationRunSummary) => {
  const index = evaluationRuns.value.findIndex((item) => item.id === run.id)
  if (index >= 0) {
    evaluationRuns.value[index] = run
  } else {
    evaluationRuns.value = [run, ...evaluationRuns.value].slice(0, 20)
  }
}

const isEvaluationRunActive = (status?: string) => (
  ['QUEUED', 'RUNNING'].includes(String(status || '').toUpperCase())
)

const stopEvaluationRunPolling = () => {
  evaluationPollVersion += 1
  if (evaluationPollTimer != null) clearTimeout(evaluationPollTimer)
  evaluationPollTimer = null
  evaluationActiveRunId.value = null
}

const completeEvaluationRunPolling = (run: RcaEvaluationRunSummary) => {
  evaluationPollTimer = null
  evaluationActiveRunId.value = null
  evaluationRunLoading.value = false
  evaluationRunningCaseId.value = null
  evaluationRunningSuiteVersionId.value = null
  const status = String(run.status || '').toUpperCase()
  const gateLabel = gateStatusLabel(run.gateStatus)
  if (status === 'SUCCEEDED') {
    liveStatus.value = `RCA 离线评测已完成，平均 ${evaluationScoreText(run.averageScore)} 分，${gateLabel}`
    ElMessage.success(run.suiteVersionId == null ? 'RCA 离线评测已完成' : `质量门禁${gateLabel}`)
  } else if (status === 'DEGRADED') {
    liveStatus.value = `RCA 离线评测已降级完成，${gateLabel}`
    ElMessage.warning(`RCA 离线评测已降级，${gateLabel}`)
  } else {
    liveStatus.value = `RCA 离线评测失败，失败码 ${run.failureCode || 'UNKNOWN'}`
    ElMessage.error('RCA 离线评测失败')
  }
}

const startEvaluationRunPolling = (
  runId: number,
  incidentId: number
) => {
  stopEvaluationRunPolling()
  const pollVersion = ++evaluationPollVersion
  evaluationActiveRunId.value = runId

  const poll = async () => {
    try {
      const response = await sreApi.getRcaEvaluationRun(runId) as RcaEvaluationRunDetail | null
      if (pollVersion !== evaluationPollVersion
        || !drawerVisible.value || !isCurrentIncident(incidentId)) return
      const detail = normalizeEvaluationRunDetail(response)
      if (!detail?.run) throw new Error('RCA evaluation run detail is unavailable')
      upsertEvaluationRun(detail.run)
      if (selectedEvaluationRunId.value === runId) {
        evaluationRunDetail.value = detail
        if (!expandedEvaluationResultIds.value.length && detail.results[0]?.id) {
          expandedEvaluationResultIds.value = [detail.results[0].id]
        }
      }
      if (EVALUATION_TERMINAL_STATUSES.has(String(detail.run.status || '').toUpperCase())) {
        completeEvaluationRunPolling(detail.run)
        return
      }
      liveStatus.value = detail.run.status === 'RUNNING'
        ? `RCA 离线评测执行中，已完成 ${detail.run.completedCount}/${detail.run.caseCount}`
        : 'RCA 离线评测正在排队'
    } catch (error) {
      if (pollVersion !== evaluationPollVersion
        || !drawerVisible.value || !isCurrentIncident(incidentId)) return
      console.error('轮询 SRE RCA 评测运行失败:', error)
      liveStatus.value = 'RCA 离线评测状态刷新失败，正在重试'
    }
    if (pollVersion === evaluationPollVersion
      && drawerVisible.value && isCurrentIncident(incidentId)) {
      evaluationPollTimer = setTimeout(poll, 2000)
    }
  }

  evaluationPollTimer = setTimeout(poll, 2000)
}

const loadRcaEvaluationRun = async (
  runId: number,
  manageLoading = true,
  inheritedRequestVersion?: number
) => {
  const requestVersion = inheritedRequestVersion ?? ++evaluationRequestVersion
  if (manageLoading) evaluationDetailLoading.value = true
  try {
    const response = await sreApi.getRcaEvaluationRun(runId) as RcaEvaluationRunDetail | null
    if (requestVersion !== evaluationRequestVersion || !drawerVisible.value) return
    const detail = normalizeEvaluationRunDetail(response)
    evaluationRunDetail.value = detail
    selectedEvaluationRunId.value = runId
    expandedEvaluationResultIds.value = detail?.results[0]?.id ? [detail.results[0].id] : []
    if (detail?.run) {
      upsertEvaluationRun(detail.run)
    }
  } catch (error) {
    console.error('加载 SRE RCA 评测详情失败:', error)
    if (requestVersion === evaluationRequestVersion) {
      evaluationRunDetail.value = null
      expandedEvaluationResultIds.value = []
    }
  } finally {
    if (manageLoading && requestVersion === evaluationRequestVersion) {
      evaluationDetailLoading.value = false
    }
  }
}

const loadRcaEvaluationSuiteVersions = async (
  suiteId: number,
  inheritedRequestVersion?: number
) => {
  const requestVersion = inheritedRequestVersion ?? ++evaluationRequestVersion
  evaluationSuiteLoading.value = true
  try {
    const response = await sreApi.getRcaEvaluationSuiteVersions(suiteId, 20) as RcaEvaluationSuiteVersionSummary[] | null
    if (requestVersion !== evaluationRequestVersion || !drawerVisible.value
      || selectedEvaluationSuiteId.value !== suiteId) return
    evaluationSuiteVersions.value = Array.isArray(response) ? response : []
  } catch (error) {
    console.error('加载 SRE RCA 评测套件版本失败:', error)
    if (requestVersion === evaluationRequestVersion && selectedEvaluationSuiteId.value === suiteId) {
      evaluationSuiteVersions.value = []
    }
  } finally {
    if (requestVersion === evaluationRequestVersion) evaluationSuiteLoading.value = false
  }
}

const loadRcaEvaluationWorkbench = async () => {
  const requestVersion = ++evaluationRequestVersion
  evaluationCatalogLoading.value = true
  evaluationHistoryLoading.value = true
  try {
    const [caseResponse, runResponse, suiteResponse] = await Promise.all([
      sreApi.getRcaEvaluationCases(50),
      sreApi.getRcaEvaluationRuns(20),
      sreApi.getRcaEvaluationSuites(20)
    ]) as [
      RcaEvaluationCaseSummary[] | null,
      RcaEvaluationRunSummary[] | null,
      RcaEvaluationSuiteSummary[] | null
    ]
    if (requestVersion !== evaluationRequestVersion || !drawerVisible.value) return
    evaluationCases.value = Array.isArray(caseResponse) ? caseResponse : []
    evaluationRuns.value = Array.isArray(runResponse) ? runResponse : []
    evaluationSuites.value = Array.isArray(suiteResponse) ? suiteResponse : []

    const selectedSuiteStillExists = evaluationSuites.value.some(
      (suite) => suite.id === selectedEvaluationSuiteId.value
    )
    selectedEvaluationSuiteId.value = selectedSuiteStillExists
      ? selectedEvaluationSuiteId.value
      : (evaluationSuites.value[0]?.id || null)
    if (selectedEvaluationSuiteId.value != null) {
      await loadRcaEvaluationSuiteVersions(selectedEvaluationSuiteId.value, requestVersion)
    } else {
      evaluationSuiteVersions.value = []
    }

    if (!evaluationRuns.value.length) {
      selectedEvaluationRunId.value = null
      evaluationRunDetail.value = null
      expandedEvaluationResultIds.value = []
      return
    }
    const selectedStillExists = evaluationRuns.value.some(
      (run) => run.id === selectedEvaluationRunId.value
    )
    const runId = selectedStillExists
      ? selectedEvaluationRunId.value
      : evaluationRuns.value[0].id
    if (runId != null) await loadRcaEvaluationRun(runId, false, requestVersion)
  } catch (error) {
    console.error('加载 SRE RCA 评测工作台失败:', error)
  } finally {
    if (requestVersion === evaluationRequestVersion) {
      evaluationCatalogLoading.value = false
      evaluationHistoryLoading.value = false
    }
  }
}

const selectRcaEvaluationSuite = (value: number | string) => {
  const suiteId = Number(value)
  if (!Number.isInteger(suiteId) || suiteId <= 0) return
  selectedEvaluationSuiteId.value = suiteId
  evaluationSuiteVersions.value = []
  loadRcaEvaluationSuiteVersions(suiteId)
    .catch((error) => console.error('切换 SRE RCA 评测套件失败:', error))
}

const selectRcaEvaluationRun = (value: number | string) => {
  const runId = Number(value)
  if (!Number.isInteger(runId) || runId <= 0) return
  loadRcaEvaluationRun(runId).catch((error) => console.error('切换 SRE RCA 评测历史失败:', error))
}

const promoteRcaEvaluationCase = async () => {
  const incidentId = selectedIncident.value?.id
  const runId = selectedRcaRunId.value
  const feedbackId = rcaFeedback.value?.id
  if (!incidentId || !runId || !feedbackId || !canPromoteEvaluationCase.value) return

  try {
    await ElMessageBox.confirm(
      `确认将反馈修订 #${feedbackId} 固化为不可变评测用例？`,
      '提升评测用例',
      { confirmButtonText: '确认提升', cancelButtonText: '取消', type: 'warning' }
    )
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') console.error('提升评测用例确认框异常:', error)
    return
  }

  const rcaVersion = rcaRequestVersion
  evaluationPromoting.value = true
  try {
    await sreApi.promoteRcaEvaluationCase(incidentId, runId, feedbackId)
    if (rcaVersion !== rcaRequestVersion || !isCurrentIncident(incidentId)
      || selectedRcaRunId.value !== runId || rcaFeedback.value?.id !== feedbackId) return
    await loadRcaEvaluationWorkbench()
    activeEvaluationTab.value = 'cases'
    liveStatus.value = `反馈修订 ${feedbackId} 已提升为不可变评测用例`
    ElMessage.success('当前反馈修订已提升为评测用例')
  } catch (error) {
    console.error('提升 SRE RCA 评测用例失败:', error)
    liveStatus.value = 'RCA 评测用例提升失败'
  } finally {
    evaluationPromoting.value = false
  }
}

const resetEvaluationSuiteForm = () => {
  Object.assign(evaluationSuiteForm, {
    mode: evaluationSuites.value.length ? 'existing' : 'new',
    suiteId: selectedEvaluationSuiteId.value || evaluationSuites.value[0]?.id || null,
    suiteKey: '',
    name: '',
    description: '',
    caseIds: evaluationCases.value.map((item) => item.id),
    minimumPassRate: 100,
    minimumAverageScore: 70,
    requireAllSafety: true,
    requireNoDegraded: true
  })
}

const openEvaluationSuiteDialog = () => {
  if (!evaluationCases.value.length || evaluationRunLoading.value) return
  resetEvaluationSuiteForm()
  evaluationSuiteDialogVisible.value = true
}

const publishEvaluationSuiteVersion = async () => {
  if (!canPublishEvaluationSuiteVersion.value) return
  evaluationSuitePublishing.value = true
  try {
    let suiteId = evaluationSuiteForm.suiteId
    if (evaluationSuiteForm.mode === 'new') {
      const created = await sreApi.createRcaEvaluationSuite({
        suiteKey: evaluationSuiteForm.suiteKey.trim().toLowerCase(),
        name: evaluationSuiteForm.name.trim(),
        description: evaluationSuiteForm.description.trim() || undefined
      }) as RcaEvaluationSuiteSummary | null
      suiteId = created?.id || null
    }
    if (!suiteId) throw new Error('RCA evaluation suite was not created')

    const detail = await sreApi.publishRcaEvaluationSuiteVersion(suiteId, {
      caseIds: [...evaluationSuiteForm.caseIds],
      minimumPassRate: evaluationSuiteForm.minimumPassRate,
      minimumAverageScore: evaluationSuiteForm.minimumAverageScore,
      requireAllSafety: evaluationSuiteForm.requireAllSafety,
      requireNoDegraded: evaluationSuiteForm.requireNoDegraded
    }) as { version?: RcaEvaluationSuiteVersionSummary } | null
    selectedEvaluationSuiteId.value = suiteId
    evaluationSuiteDialogVisible.value = false
    await loadRcaEvaluationWorkbench()
    activeEvaluationTab.value = 'suites'
    const versionLabel = detail?.version?.version ? ` v${detail.version.version}` : ''
    liveStatus.value = `RCA 评测套件${versionLabel}已发布`
    ElMessage.success(`评测套件${versionLabel}已发布`)
  } catch (error) {
    console.error('发布 SRE RCA 评测套件版本失败:', error)
    liveStatus.value = 'RCA 评测套件版本发布失败'
  } finally {
    evaluationSuitePublishing.value = false
  }
}

const runRcaEvaluation = async (caseId?: number, suiteVersionId?: number) => {
  if (evaluationRunLoading.value || evaluationPromoting.value) return
  if (caseId == null && suiteVersionId == null && !evaluationCases.value.length) return
  const incidentId = selectedIncident.value?.id
  if (!incidentId) return
  const suiteVersion = evaluationSuiteVersions.value.find((item) => item.id === suiteVersionId)
  const targetLabel = suiteVersionId != null
    ? `${selectedEvaluationSuite.value?.name || '评测套件'} v${suiteVersion?.version || '-'} 的 ${suiteVersion?.caseCount || 0} 个用例`
    : (caseId == null ? `全部 ${evaluationCases.value.length} 个用例` : `用例 #${caseId}`)
  try {
    await ElMessageBox.confirm(
      `确认手动回放${targetLabel}？`,
      '执行离线评测',
      { confirmButtonText: '开始回放', cancelButtonText: '取消', type: 'warning' }
    )
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') console.error('执行离线评测确认框异常:', error)
    return
  }

  const requestVersion = ++evaluationRequestVersion
  evaluationRunLoading.value = true
  evaluationRunningCaseId.value = caseId ?? null
  evaluationRunningSuiteVersionId.value = suiteVersionId ?? null
  liveStatus.value = `正在提交${targetLabel}`
  let pollingStarted = false
  try {
    const response = await sreApi.runRcaEvaluation(caseId, suiteVersionId) as RcaEvaluationRunSummary | null
    if (requestVersion !== evaluationRequestVersion
      || !isCurrentIncident(incidentId) || !response?.id) return
    upsertEvaluationRun(response)
    evaluationRunDetail.value = {
      run: response,
      results: []
    }
    selectedEvaluationRunId.value = response.id
    expandedEvaluationResultIds.value = []
    activeEvaluationTab.value = 'history'
    liveStatus.value = `RCA 离线评测已入队，共 ${response.caseCount} 个用例`
    ElMessage.success('RCA 离线评测已进入队列')
    startEvaluationRunPolling(response.id, incidentId)
    pollingStarted = true
  } catch (error) {
    console.error('提交 SRE RCA 离线评测失败:', error)
    liveStatus.value = 'RCA 离线评测提交失败'
  } finally {
    if (!pollingStarted && requestVersion === evaluationRequestVersion) {
      evaluationRunLoading.value = false
      evaluationRunningCaseId.value = null
      evaluationRunningSuiteVersionId.value = null
    }
  }
}

const acknowledgeSelected = async () => {
  if (!selectedIncident.value || !canAcknowledge.value) return
  actionLoading.value = 'ack'
  try {
    await sreApi.acknowledgeIncident(selectedIncident.value.id)
    ElMessage.success('事故已确认')
    liveStatus.value = `${selectedIncident.value.incidentNo || '事故'} 已确认`
    await Promise.all([
      loadSummary(),
      loadIncidents(),
      loadInvestigationContext(selectedIncident.value.id)
    ])
  } catch (error) {
    console.error('确认 SRE 事故失败:', error)
  } finally {
    actionLoading.value = ''
  }
}

const resolveSelected = async () => {
  if (!selectedIncident.value || !canResolve.value) return
  try {
    await ElMessageBox.confirm(
      `确认关闭事故 ${selectedIncident.value.incidentNo || selectedIncident.value.id}？`,
      '关闭事故',
      { confirmButtonText: '确认关闭', cancelButtonText: '取消', type: 'warning' }
    )
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') console.error('关闭事故确认框异常:', error)
    return
  }

  actionLoading.value = 'resolve'
  try {
    await sreApi.resolveIncident(selectedIncident.value.id)
    ElMessage.success('事故已关闭')
    liveStatus.value = `${selectedIncident.value.incidentNo || '事故'} 已关闭`
    await Promise.all([
      loadSummary(),
      loadIncidents(),
      loadInvestigationContext(selectedIncident.value.id)
    ])
  } catch (error) {
    console.error('关闭 SRE 事故失败:', error)
  } finally {
    actionLoading.value = ''
  }
}

const generateRca = async () => {
  if (!selectedIncident.value || rcaLoading.value) return
  const incidentId = selectedIncident.value.id
  activeDetailTab.value = 'rca'
  rcaLoading.value = true
  liveStatus.value = '正在生成只读 AI 根因分析'
  try {
    const report = await sreApi.generateRca(incidentId) as RcaReport
    if (!isCurrentIncident(incidentId)) return
    rcaReport.value = report
    await loadRcaRuns(incidentId)
    if (!isCurrentIncident(incidentId)) return
    liveStatus.value = 'AI 根因分析已生成'
    ElMessage.success('AI 根因分析已生成')
  } catch (error) {
    console.error('生成 SRE RCA 失败:', error)
    liveStatus.value = 'AI 根因分析生成失败'
  } finally {
    rcaLoading.value = false
  }
}

const resetDrawer = () => {
  stopEvaluationRunPolling()
  investigationRequestVersion += 1
  rcaRequestVersion += 1
  evaluationRequestVersion += 1
  selectedIncident.value = null
  investigationContext.value = null
  rcaReport.value = null
  rcaRuns.value = []
  selectedRcaRunId.value = null
  investigationSteps.value = []
  rcaProvenance.value = null
  rcaFeedback.value = null
  resetFeedbackForm()
  evaluationCases.value = []
  evaluationSuites.value = []
  evaluationSuiteVersions.value = []
  selectedEvaluationSuiteId.value = null
  evaluationRuns.value = []
  selectedEvaluationRunId.value = null
  evaluationRunDetail.value = null
  expandedEvaluationResultIds.value = []
  activeEvaluationTab.value = 'cases'
  detailLoading.value = false
  rcaHistoryLoading.value = false
  feedbackSaving.value = false
  evaluationExporting.value = false
  evaluationCatalogLoading.value = false
  evaluationHistoryLoading.value = false
  evaluationDetailLoading.value = false
  evaluationPromoting.value = false
  evaluationRunLoading.value = false
  evaluationRunningCaseId.value = null
  evaluationRunningSuiteVersionId.value = null
  evaluationSuiteLoading.value = false
  evaluationSuiteDialogVisible.value = false
  evaluationSuitePublishing.value = false
  resetEvaluationSuiteForm()
  activeDetailTab.value = 'overview'
  expandedEvidenceIds.value = []
  actionLoading.value = ''
}

const severityTone = (value?: string): CnTone => ({
  critical: 'danger',
  warning: 'warning',
  info: 'info'
}[String(value || '').toLowerCase()] as CnTone || 'neutral')

const severityLabel = (value?: string) => ({
  critical: '严重',
  warning: '警告',
  info: '信息'
}[String(value || '').toLowerCase()] || value || '-')

const stateTone = (value?: string): CnTone => ({
  OPEN: 'danger',
  ACKNOWLEDGED: 'warning',
  INVESTIGATING: 'info',
  MITIGATED: 'brand',
  RESOLVED: 'success',
  CLOSED: 'neutral'
}[String(value || '').toUpperCase()] as CnTone || 'neutral')

const stateLabel = (value?: string) => ({
  OPEN: '待确认',
  ACKNOWLEDGED: '处理中',
  INVESTIGATING: '调查中',
  MITIGATED: '已缓解',
  RESOLVED: '已恢复',
  CLOSED: '已关闭'
}[String(value || '').toUpperCase()] || value || '-')

const evidenceTone = (value?: string): CnTone => ({
  AVAILABLE: 'success',
  INVALID: 'danger',
  UNAVAILABLE: 'warning'
}[String(value || '').toUpperCase()] as CnTone || 'neutral')

const conclusionTone = (value?: string): CnTone => ({
  SUPPORTED: 'success',
  PARTIAL: 'warning',
  PARTIALLY_SUPPORTED: 'warning',
  INSUFFICIENT_EVIDENCE: 'warning'
}[String(value || '').toUpperCase()] as CnTone || 'info')

const conclusionLabel = (value?: string) => ({
  SUPPORTED: '证据支持',
  PARTIAL: '部分支持',
  PARTIALLY_SUPPORTED: '部分支持',
  INSUFFICIENT_EVIDENCE: '证据不足'
}[String(value || '').toUpperCase()] || value || '-')

const runStatusTone = (value?: string): CnTone => ({
  QUEUED: 'neutral',
  RUNNING: 'info',
  SUCCEEDED: 'success',
  DEGRADED: 'warning',
  FAILED: 'danger',
  SKIPPED: 'neutral'
}[String(value || '').toUpperCase()] as CnTone || 'neutral')

const runStatusLabel = (value?: string) => ({
  QUEUED: '排队中',
  RUNNING: '进行中',
  SUCCEEDED: '已完成',
  DEGRADED: '已降级',
  FAILED: '失败',
  SKIPPED: '已跳过'
}[String(value || '').toUpperCase()] || value || '-')

const gateStatusTone = (value?: string): CnTone => ({
  PASSED: 'success',
  FAILED: 'danger',
  PENDING: 'info',
  NOT_APPLICABLE: 'neutral'
}[String(value || '').toUpperCase()] as CnTone || 'neutral')

const gateStatusLabel = (value?: string) => ({
  PASSED: '通过',
  FAILED: '未通过',
  PENDING: '评估中',
  NOT_APPLICABLE: '临时运行'
}[String(value || '').toUpperCase()] || value || '临时运行')

const invocationOutcomeTone = (value?: string): CnTone => ({
  SUCCESS: 'success',
  MODEL_UNAVAILABLE: 'warning',
  EMPTY_RESPONSE: 'warning',
  INVOCATION_EXCEPTION: 'danger',
  PARSER_FAILURE: 'warning'
}[String(value || '').toUpperCase()] as CnTone || 'neutral')

const invocationOutcomeLabel = (value?: string) => ({
  SUCCESS: '模型调用成功',
  MODEL_UNAVAILABLE: '模型不可用，已降级',
  EMPTY_RESPONSE: '空响应，已降级',
  INVOCATION_EXCEPTION: '调用异常，已降级',
  PARSER_FAILURE: '解析失败，已降级'
}[String(value || '').toUpperCase()] || value || '未知结果')

const investigationStepLabel = (value?: string) => ({
  CONTEXT_LOADED: '加载事故上下文',
  MODEL_CONTEXT_BUILT: '构建模型上下文',
  MODEL_ANALYSIS: '执行只读分析',
  REPORT_VALIDATED: '校验结构化报告',
  RUN_FAILED: '调查异常中止'
}[String(value || '').toUpperCase()] || value || '未知步骤')

const feedbackAccuracyLabel = (value?: string) => ({
  ACCURATE: '准确',
  PARTIAL: '部分准确',
  INACCURATE: '不准确'
}[String(value || '').toUpperCase()] || value || '未评价')

const feedbackAccuracyTone = (value?: string): CnTone => ({
  ACCURATE: 'success',
  PARTIAL: 'warning',
  INACCURATE: 'danger'
}[String(value || '').toUpperCase()] as CnTone || 'neutral')

const feedbackGapLabel = (value?: string) => ({
  RETRIEVAL_GAP: '证据检索不足',
  REASONING_GAP: '推理判断偏差',
  TOOL_FAILURE: '取证工具失败',
  ROUTING_GAP: '调查路由错误',
  UNKNOWN: '尚未分类'
}[String(value || '').toUpperCase()] || '无主要缺口')

const runHistoryLabel = (run: RcaRunSummary) => {
  const mode = run.generationMode || runStatusLabel(run.status)
  return `${formatTime(run.startedAt)} · ${mode} · ${conclusionLabel(run.conclusionStatus)}`
}

const evaluationRunLabel = (run: RcaEvaluationRunSummary) => (
  `${formatTime(run.queuedAt || run.startedAt)} · ${runStatusLabel(run.status)} · ${run.completedCount}/${run.caseCount} 完成 · ${gateStatusLabel(run.gateStatus)}`
)

const evaluationScoreText = (value?: number | string | null) => {
  if (value == null || value === '') return '-'
  const score = Number(value)
  return Number.isFinite(score) ? score.toFixed(2) : '-'
}

const evaluationRatioText = (value?: number | string) => {
  const ratio = Number(value)
  return Number.isFinite(ratio) ? `${(Math.max(0, Math.min(1, ratio)) * 100).toFixed(1)}%` : '-'
}

const abbreviatedHash = (value?: string) => {
  if (!value) return '-'
  return value.length > 24 ? `${value.slice(0, 12)}...${value.slice(-8)}` : value
}

const riskTone = (value?: string): CnTone => ({
  READ_ONLY: 'success',
  PROPOSE_ONLY: 'info',
  LOW: 'success',
  MEDIUM: 'warning',
  HIGH: 'danger'
}[String(value || '').toUpperCase()] as CnTone || 'neutral')

const riskLabel = (value?: string) => ({
  READ_ONLY: '只读检查',
  PROPOSE_ONLY: '仅建议',
  LOW: '低风险',
  MEDIUM: '中风险',
  HIGH: '高风险'
}[String(value || '').toUpperCase()] || value || '未分级')

const formatTime = (value: unknown) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').replace('Z', '').slice(0, 19)
}

const prettySnapshot = (snapshot?: Record<string, unknown>) => {
  if (!snapshot || Object.keys(snapshot).length === 0) return '{}'
  return JSON.stringify(snapshot, null, 2)
}

const formatConfidence = (value?: number) => {
  const confidence = Number(value)
  if (!Number.isFinite(confidence)) return '-'
  return `${Math.round(Math.max(0, Math.min(1, confidence)) * 100)}%`
}

const evidenceIdsText = (ids?: number[]) => {
  if (!ids?.length) return '无证据引用'
  return `证据 ${ids.map((id) => `#${id}`).join('、')}`
}

const syncViewportWidth = () => {
  viewportWidth.value = window.innerWidth
}

onMounted(() => {
  syncViewportWidth()
  window.addEventListener('resize', syncViewportWidth)
  refreshWorkbench()
})

onBeforeUnmount(() => {
  stopEvaluationRunPolling()
  window.removeEventListener('resize', syncViewportWidth)
})
</script>

<style scoped>
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.incident-table {
  margin-top: var(--cn-space-5);
}

.incident-table :deep(.el-table__row) {
  cursor: pointer;
}

.incident-summary-cell {
  display: grid;
  gap: var(--cn-space-1);
  min-width: 0;
  padding: 2px 0;
}

.incident-summary-cell strong,
.incident-summary-cell span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.incident-summary-cell strong {
  color: var(--cn-color-text-primary);
  font-size: 13px;
}

.incident-summary-cell span,
.time-cell {
  color: var(--cn-color-text-secondary);
  font-size: 12px;
}

.drawer-content {
  min-height: 420px;
}

.drawer-status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-4);
  padding-bottom: var(--cn-space-4);
  border-bottom: 1px solid var(--cn-color-border-subtle);
}

.drawer-status-tags,
.drawer-actions,
.rca-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--cn-space-2);
}

.detail-tabs {
  margin-top: var(--cn-space-4);
}

.detail-section {
  min-width: 0;
  padding: var(--cn-space-2) 0 var(--cn-space-5);
}

.detail-section + .detail-section {
  padding-top: var(--cn-space-5);
  border-top: 1px solid var(--cn-color-border-subtle);
}

.detail-section h3,
.detail-section h4,
.section-heading p {
  margin: 0;
}

.detail-section h3 {
  color: var(--cn-color-text-primary);
  font-size: 16px;
  line-height: 1.4;
}

.detail-section h4 {
  color: var(--cn-color-text-primary);
  font-size: 14px;
  line-height: 1.4;
}

.section-heading,
.timeline-head,
.hypothesis-head,
.rca-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--cn-space-3);
}

.fact-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--cn-space-3);
  margin: var(--cn-space-4) 0 0;
}

.fact-grid > div,
.evidence-meta > div {
  min-width: 0;
}

.fact-grid dt,
.evidence-meta dt {
  margin-bottom: var(--cn-space-1);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.fact-grid dd,
.evidence-meta dd {
  margin: 0;
  overflow-wrap: anywhere;
  color: var(--cn-color-text-primary);
  font-size: 13px;
  line-height: 1.55;
}

.incident-description,
.rca-summary {
  margin: var(--cn-space-4) 0 0;
  padding: var(--cn-space-4);
  border-left: 3px solid var(--cn-color-brand-primary);
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.7;
}

.timeline-list {
  display: grid;
  gap: 0;
  margin: var(--cn-space-4) 0 0;
  padding: 0;
  list-style: none;
}

.timeline-item {
  position: relative;
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr);
  gap: var(--cn-space-3);
  min-width: 0;
  padding-bottom: var(--cn-space-4);
}

.timeline-item:not(:last-child)::before {
  position: absolute;
  top: 14px;
  bottom: 0;
  left: 6px;
  width: 1px;
  background: var(--cn-color-border);
  content: '';
}

.timeline-marker {
  z-index: 1;
  width: 13px;
  height: 13px;
  margin-top: 3px;
  border: 3px solid var(--cn-color-bg-surface);
  border-radius: 50%;
  background: var(--cn-color-text-tertiary);
  box-shadow: 0 0 0 1px var(--cn-color-border);
}

.timeline-marker.is-firing {
  background: var(--cn-color-danger);
}

.timeline-marker.is-resolved {
  background: var(--cn-color-success);
}

.timeline-item article,
.timeline-item p {
  min-width: 0;
  margin: 0;
}

.timeline-item p,
.section-heading p {
  margin-top: var(--cn-space-1);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  line-height: 1.5;
}

.evidence-section :deep(.el-collapse) {
  margin-top: var(--cn-space-4);
  border-top: 0;
}

.evidence-title-row {
  display: flex;
  align-items: center;
  gap: var(--cn-space-3);
  min-width: 0;
  width: calc(100% - 24px);
}

.evidence-title-row strong {
  color: var(--cn-color-text-primary);
  font-size: 13px;
}

.evidence-title-row > span:not(.cn-status-tag) {
  min-width: 0;
  overflow: hidden;
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.evidence-title-row .cn-status-tag {
  margin-left: auto;
}

.evidence-meta {
  display: grid;
  grid-template-columns: minmax(140px, 0.4fr) minmax(0, 1fr);
  gap: var(--cn-space-4);
  margin: 0 0 var(--cn-space-3);
}

.evidence-json {
  max-height: 360px;
  margin: 0;
  padding: var(--cn-space-4);
  overflow: auto;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-canvas);
  color: var(--cn-color-text-secondary);
  font-family: var(--cn-font-mono);
  font-size: 12px;
  line-height: 1.6;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
}

.rca-section {
  min-height: 360px;
}

.rca-history-toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--cn-space-3);
  min-height: 40px;
  margin-bottom: var(--cn-space-4);
  padding-bottom: var(--cn-space-4);
  border-bottom: 1px solid var(--cn-color-border-subtle);
}

.rca-history-toolbar > span {
  flex: 0 0 auto;
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.rca-history-toolbar :deep(.el-select) {
  width: min(360px, 100%);
}

.rca-block {
  margin-top: var(--cn-space-5);
}

.provenance-block {
  padding: var(--cn-space-5);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
}

.provenance-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--cn-space-3);
}

.provenance-heading p {
  margin: var(--cn-space-1) 0 0;
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.provenance-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cn-space-3) var(--cn-space-5);
  margin: var(--cn-space-4) 0 0;
}

.provenance-grid > div {
  min-width: 0;
}

.provenance-grid dt {
  margin-bottom: var(--cn-space-1);
  color: var(--cn-color-text-tertiary);
  font-size: 11px;
}

.provenance-grid dd {
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  overflow-wrap: anywhere;
}

.provenance-grid code {
  font-family: var(--cn-font-mono);
  font-size: 11px;
}

.provenance-hash {
  grid-column: 1 / -1;
}

.rca-feedback-block {
  padding: var(--cn-space-5) 0;
  border-top: 1px solid var(--cn-color-border-subtle);
  border-bottom: 1px solid var(--cn-color-border-subtle);
}

.feedback-heading,
.feedback-meta,
.feedback-actions {
  display: flex;
  align-items: center;
}

.feedback-heading {
  justify-content: space-between;
  gap: var(--cn-space-3);
}

.feedback-meta {
  flex-wrap: wrap;
  gap: var(--cn-space-2);
  margin-top: var(--cn-space-2);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.feedback-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 var(--cn-space-4);
  margin-top: var(--cn-space-4);
}

.feedback-form .feedback-wide,
.feedback-actions {
  grid-column: 1 / -1;
}

.feedback-form :deep(.el-select) {
  width: 100%;
}

.feedback-form :deep(.el-radio-group) {
  display: flex;
  flex-wrap: wrap;
}

.feedback-actions {
  justify-content: flex-end;
}

.evaluation-workbench {
  padding: var(--cn-space-5) 0;
  border-bottom: 1px solid var(--cn-color-border-subtle);
}

.evaluation-heading,
.evaluation-heading-meta,
.evaluation-actions,
.evaluation-item-heading,
.evaluation-result-title,
.candidate-report-heading,
.candidate-report-heading > div {
  display: flex;
  align-items: center;
}

.evaluation-heading,
.candidate-report-heading {
  justify-content: space-between;
  gap: var(--cn-space-3);
}

.evaluation-heading-meta,
.evaluation-actions,
.evaluation-item-heading,
.candidate-report-heading > div {
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.evaluation-heading-meta {
  margin-top: var(--cn-space-2);
}

.evaluation-actions {
  justify-content: flex-end;
}

.evaluation-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.evaluation-actions :deep(.el-button) {
  min-height: 32px;
}

.evaluation-tabs {
  margin-top: var(--cn-space-3);
}

.evaluation-case-list,
.evaluation-suite-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.evaluation-case-list > li,
.evaluation-suite-list > li {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--cn-space-4);
  min-width: 0;
  padding: var(--cn-space-4) 0;
  border-bottom: 1px solid var(--cn-color-border-subtle);
}

.evaluation-case-main,
.evaluation-suite-main,
.evaluation-case-meta > div,
.evaluation-suite-meta > div,
.evaluation-summary-grid > div,
.evaluation-run-provenance > div,
.evaluation-result-provenance > div,
.candidate-report-columns > div {
  min-width: 0;
}

.evaluation-item-heading strong,
.evaluation-result-title strong {
  color: var(--cn-color-text-primary);
  font-size: 13px;
}

.evaluation-case-main > p {
  margin: var(--cn-space-2) 0 0;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.6;
  overflow-wrap: anywhere;
}

.evaluation-case-meta,
.evaluation-suite-meta,
.evaluation-run-provenance,
.evaluation-result-provenance {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cn-space-2) var(--cn-space-4);
  margin: var(--cn-space-3) 0 0;
}

.evaluation-case-meta dt,
.evaluation-suite-meta dt,
.evaluation-summary-grid dt,
.evaluation-run-provenance dt,
.evaluation-score-grid dt,
.evaluation-result-provenance dt {
  margin-bottom: var(--cn-space-1);
  color: var(--cn-color-text-tertiary);
  font-size: 11px;
}

.evaluation-case-meta dd,
.evaluation-suite-meta dd,
.evaluation-run-provenance dd,
.evaluation-result-provenance dd {
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.evaluation-case-meta code,
.evaluation-suite-meta code,
.evaluation-run-provenance code,
.evaluation-result-provenance code {
  font-family: var(--cn-font-mono);
  font-size: 11px;
}

.evaluation-history-toolbar,
.evaluation-suite-toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--cn-space-3);
  margin-bottom: var(--cn-space-4);
}

.evaluation-history-toolbar label,
.evaluation-suite-toolbar label {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.evaluation-history-toolbar :deep(.el-select),
.evaluation-suite-toolbar :deep(.el-select) {
  width: min(440px, 100%);
}

.evaluation-suite-toolbar :deep(.el-button) {
  margin-left: 0;
}

.evaluation-suite-create-fields,
.evaluation-suite-policy-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 var(--cn-space-4);
}

.evaluation-suite-wide {
  grid-column: 1 / -1;
}

.evaluation-suite-dialog :deep(.el-select),
.evaluation-suite-dialog :deep(.el-input-number) {
  width: 100%;
}

.evaluation-suite-cases {
  min-width: 0;
  margin: 0 0 var(--cn-space-5);
  padding: var(--cn-space-3) var(--cn-space-4) var(--cn-space-4);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-control);
}

.evaluation-suite-cases legend {
  padding: 0 var(--cn-space-2);
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  font-weight: 600;
}

.evaluation-suite-cases :deep(.el-checkbox-group) {
  display: grid;
  gap: var(--cn-space-2);
  max-height: 240px;
  overflow-y: auto;
}

.evaluation-suite-cases :deep(.el-checkbox) {
  align-items: flex-start;
  height: auto;
  margin-right: 0;
  white-space: normal;
}

.evaluation-suite-cases :deep(.el-checkbox__label) {
  min-width: 0;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.evaluation-suite-switches {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cn-space-3);
}

.evaluation-suite-switches > label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
  min-width: 0;
  padding: var(--cn-space-3) 0;
  border-top: 1px solid var(--cn-color-border-subtle);
  color: var(--cn-color-text-secondary);
  font-size: 13px;
}

.evaluation-summary-grid,
.evaluation-score-grid {
  display: grid;
  gap: 1px;
  margin: 0;
  overflow: hidden;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-border-subtle);
}

.evaluation-summary-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.evaluation-score-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.evaluation-summary-grid > div,
.evaluation-score-grid > div {
  padding: var(--cn-space-3);
  background: var(--cn-color-bg-surface-muted);
}

.evaluation-summary-grid dd,
.evaluation-score-grid dd {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-size: 16px;
  font-weight: 600;
  line-height: 1.35;
}

.evaluation-score-grid dd small {
  display: block;
  margin-top: var(--cn-space-1);
  color: var(--cn-color-text-tertiary);
  font-size: 11px;
  font-weight: 400;
}

.evaluation-run-provenance {
  margin-bottom: var(--cn-space-4);
  padding-bottom: var(--cn-space-4);
  border-bottom: 1px solid var(--cn-color-border-subtle);
}

.evaluation-result-list {
  border-top: 0;
}

.evaluation-result-title {
  flex: 1;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
  min-width: 0;
  padding-right: var(--cn-space-2);
}

.evaluation-result-title > span:not(.cn-status-tag) {
  color: var(--cn-color-text-secondary);
  font-family: var(--cn-font-mono);
  font-size: 12px;
}

.evaluation-result-title .cn-status-tag:first-of-type {
  margin-left: auto;
}

.evaluation-score-grid {
  margin-bottom: var(--cn-space-4);
}

.evaluation-result-section {
  padding: var(--cn-space-4) 0;
  border-top: 1px solid var(--cn-color-border-subtle);
}

.evaluation-result-section h5,
.candidate-report-columns h6 {
  margin: 0;
  color: var(--cn-color-text-primary);
  letter-spacing: 0;
}

.evaluation-result-section h5 {
  font-size: 13px;
}

.candidate-report-columns h6 {
  font-size: 12px;
}

.evaluation-explanations,
.candidate-report-columns ul {
  display: grid;
  gap: var(--cn-space-2);
  margin: var(--cn-space-3) 0 0;
  padding-left: 18px;
}

.evaluation-explanations li,
.candidate-report-columns li,
.candidate-report-columns p {
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  line-height: 1.6;
}

.candidate-summary {
  margin: var(--cn-space-3) 0 0;
  padding-left: var(--cn-space-3);
  border-left: 3px solid var(--cn-color-brand-primary);
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.65;
}

.candidate-report-columns {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cn-space-5);
  margin-top: var(--cn-space-4);
}

.candidate-report-columns li strong,
.candidate-report-columns li span {
  display: block;
}

.candidate-report-columns li span {
  margin-top: var(--cn-space-1);
}

.candidate-report-columns p {
  margin: var(--cn-space-3) 0 0;
}

.investigation-trace {
  padding-bottom: var(--cn-space-5);
  border-bottom: 1px solid var(--cn-color-border-subtle);
}

.investigation-trace ol {
  display: grid;
  gap: var(--cn-space-3);
  margin: var(--cn-space-3) 0 0;
  padding: 0;
  list-style: none;
}

.investigation-trace li {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr);
  gap: var(--cn-space-3);
  min-width: 0;
}

.trace-index {
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border: 1px solid var(--cn-color-border);
  border-radius: 50%;
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-text-secondary);
  font-family: var(--cn-font-mono);
  font-size: 11px;
}

.trace-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
}

.trace-heading strong {
  color: var(--cn-color-text-primary);
  font-size: 13px;
}

.investigation-trace p,
.investigation-trace time {
  display: block;
  margin: var(--cn-space-1) 0 0;
  overflow-wrap: anywhere;
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  line-height: 1.5;
}

.rca-list,
.recommendation-list,
.limitation-block ul {
  display: grid;
  gap: var(--cn-space-3);
  margin: var(--cn-space-3) 0 0;
  padding-left: 20px;
}

.rca-list li,
.recommendation-list li,
.limitation-block li {
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.65;
}

.rca-list li span,
.rca-list li small,
.recommendation-list li span,
.recommendation-list li small {
  display: block;
}

.rca-list small,
.recommendation-list small,
.hypothesis-item small {
  margin-top: var(--cn-space-1);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.hypothesis-list {
  display: grid;
  gap: var(--cn-space-3);
  margin-top: var(--cn-space-3);
}

.hypothesis-item {
  min-width: 0;
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
}

.hypothesis-item p {
  margin: var(--cn-space-2) 0 0;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.65;
}

.hypothesis-item ul {
  margin: var(--cn-space-3) 0 0;
  padding-left: 18px;
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  line-height: 1.6;
}

.recommendation-list {
  padding: 0;
  list-style: none;
}

.recommendation-list li {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--cn-space-3);
  padding: var(--cn-space-3) 0;
  border-bottom: 1px solid var(--cn-color-border-subtle);
}

.reference-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
  margin-top: var(--cn-space-3);
}

.reference-list span {
  padding: 6px 9px;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-text-secondary);
  font-size: 12px;
}

.limitation-block {
  padding: var(--cn-space-4);
  border-left: 3px solid var(--cn-color-warning);
  background: var(--cn-color-warning-soft);
}

.limitation-block ul {
  margin-bottom: 0;
}

.empty-copy {
  margin: var(--cn-space-3) 0 0;
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
}

.rca-empty {
  display: grid;
  justify-items: center;
  gap: var(--cn-space-3);
  min-height: 300px;
  align-content: center;
}

@media (max-width: 1100px) {
  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .stat-grid,
  .fact-grid,
  .evidence-meta {
    grid-template-columns: 1fr;
  }

  .drawer-status-row,
  .section-heading,
  .rca-heading,
  .provenance-heading,
  .feedback-heading,
  .evaluation-heading,
  .candidate-report-heading,
  .rca-history-toolbar,
  .evaluation-history-toolbar,
  .evaluation-suite-toolbar,
  .recommendation-list li {
    align-items: flex-start;
    flex-direction: column;
  }

  .drawer-actions {
    width: 100%;
  }

  .drawer-actions :deep(.el-button) {
    flex: 1 1 auto;
    margin-left: 0;
  }

  .rca-history-toolbar {
    align-items: stretch;
  }

  .rca-history-toolbar :deep(.el-select),
  .evaluation-history-toolbar :deep(.el-select),
  .evaluation-suite-toolbar :deep(.el-select) {
    width: 100%;
  }

  .evaluation-actions {
    justify-content: flex-start;
    width: 100%;
  }

  .evaluation-case-list > li,
  .evaluation-suite-list > li {
    grid-template-columns: 1fr;
    align-items: flex-start;
  }

  .evaluation-case-list > li > .el-button,
  .evaluation-suite-list > li > .el-button {
    justify-self: start;
  }

  .feedback-form {
    grid-template-columns: 1fr;
  }

  .provenance-grid {
    grid-template-columns: 1fr;
  }

  .evaluation-case-meta,
  .evaluation-suite-meta,
  .evaluation-run-provenance,
  .evaluation-result-provenance,
  .candidate-report-columns {
    grid-template-columns: 1fr;
  }

  .evaluation-summary-grid,
  .evaluation-score-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .evaluation-result-title .cn-status-tag:first-of-type {
    margin-left: 0;
  }

  .feedback-form :deep(.el-form-item),
  .feedback-actions,
  .provenance-hash {
    grid-column: 1;
  }

  .evaluation-suite-create-fields,
  .evaluation-suite-policy-grid,
  .evaluation-suite-switches {
    grid-template-columns: 1fr;
  }

  .evaluation-suite-wide {
    grid-column: 1;
  }
}

@media (max-width: 420px) {
  .evaluation-summary-grid,
  .evaluation-score-grid {
    grid-template-columns: 1fr;
  }
}
</style>
