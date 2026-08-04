<template>
  <div class="growth-autopilot-panel">
      <CnPageHeader
        title="成长闭环自动驾驶"
        eyebrow="Growth Autopilot MVP"
        description="把周目标自动拆解成今日任务包，实时纠偏，一键重排，持续推进学习闭环。"
      >
        <template #meta>
          <CnStatusTag :type="hasPlan ? 'success' : 'warning'" size="sm">
            {{ hasPlan ? '本周计划运行中' : '等待生成计划' }}
          </CnStatusTag>
          <CnStatusTag v-if="hasPlan" :type="riskTone(summary.riskLevel)" size="sm">
            {{ riskText(summary.riskLevel) }}
          </CnStatusTag>
          <CnStatusTag v-if="dashboard.generatedAt" type="info" size="sm" subtle>
            {{ dashboard.generatedAt }}
          </CnStatusTag>
        </template>
        <template #actions>
          <el-button plain :disabled="!hasPlan" :loading="loading.adjustPreview" @click="openAdjustmentDialog">
            <el-icon><MagicStick /></el-icon>
            调整本周
          </el-button>
          <el-button type="primary" :loading="loading.generate" @click="openGenerateDialog">
            <el-icon><Plus /></el-icon>
            {{ hasPlan ? '重新生成周计划' : '生成本周计划' }}
          </el-button>
          <el-button plain :loading="loading.completeToday" :disabled="!hasPlan" @click="handleCompleteToday">
            <el-icon><Check /></el-icon>
            完成今日待办
          </el-button>
          <el-button plain :loading="loading.replan" :disabled="!hasPlan" @click="handleReplan">
            <el-icon><RefreshRight /></el-icon>
            一键重排
          </el-button>
          <el-button plain :loading="loading.dashboard || loading.briefing || loading.skillInsights || loading.evidenceProfile || loading.applicationOutcomes || loading.jobBattleGap || loading.jobPreparationLoop || loading.jobMarketSignal || loading.codeArtifacts || loading.githubConnection || loading.weeklyReview" @click="refreshDashboard">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </template>
      </CnPageHeader>

      <CnSection
        v-if="briefing?.primaryAction || loading.briefing"
        class="coach-briefing-section"
        title="教练焦点"
        compact
        divided
      >
        <div class="coach-briefing-content" v-loading="loading.briefing">
          <template v-if="briefing?.primaryAction">
            <div class="coach-briefing-meta">
              <CnStatusTag :type="briefingTone(briefing.primaryAction.riskLevel)" size="sm">
                {{ briefingSourceText(briefing.primaryAction.source) }}
              </CnStatusTag>
              <span v-if="briefing.primaryAction.expectedMinutes">约 {{ briefing.primaryAction.expectedMinutes }} 分钟</span>
            </div>
            <h2>{{ briefing.primaryAction.title }}</h2>
            <p>{{ briefing.primaryAction.description }}</p>
            <dl class="coach-briefing-details">
              <div v-if="briefing.primaryAction.reason">
                <dt>为什么是这件事</dt>
                <dd>{{ briefing.primaryAction.reason }}</dd>
              </div>
              <div v-if="briefing.primaryAction.expectedChange">
                <dt>完成后</dt>
                <dd>{{ briefing.primaryAction.expectedChange }}</dd>
              </div>
            </dl>
            <div class="coach-briefing-actions">
              <el-button type="primary" @click="handleBriefingAction">
                {{ briefingActionText(briefing.primaryAction.actionType) }}
              </el-button>
            </div>
          </template>
        </div>
      </CnSection>

      <section class="summary-grid">
        <CnStatCard
          title="本周完成率"
          :value="summary.completionRate || 0"
          unit="%"
          description="按自动驾驶任务完成情况计算"
          tone="brand"
          :loading="loading.dashboard"
        />
        <CnStatCard
          title="任务完成"
          :value="`${summary.completedTasks || 0} / ${summary.totalTasks || 0}`"
          :description="`今日完成 ${summary.todayCompleted || 0} / ${summary.todayTasks || 0}`"
          tone="success"
          :loading="loading.dashboard"
        />
        <CnStatCard
          title="逾期任务"
          :value="summary.overdueTasks || 0"
          description="建议优先处理逾期与 P1 任务"
          :tone="(summary.overdueTasks || 0) > 0 ? 'danger' : 'neutral'"
          :loading="loading.dashboard"
        />
        <CnStatCard
          title="风险等级"
          :value="riskText(summary.riskLevel)"
          :description="summary.riskText || '执行节奏稳定'"
          :tone="riskTone(summary.riskLevel)"
          :loading="loading.dashboard"
        />
      </section>

      <CnSection v-if="hasPlan" class="filter-section" surface="plain" compact>
        <div class="filter-wrap">
        <div class="filter-item">
          <span class="filter-label">模块</span>
          <el-select v-model="filters.moduleKey" size="small" class="module-filter-select">
            <el-option label="全部模块" value="all" />
            <el-option
              v-for="item in moduleProgress"
              :key="item.moduleKey"
              :label="item.moduleName"
              :value="item.moduleKey"
            />
          </el-select>
        </div>
        <div class="filter-item">
          <span class="filter-label">状态</span>
          <el-select v-model="filters.status" size="small" class="status-filter-select">
            <el-option label="全部状态" value="all" />
            <el-option label="待完成" value="todo" />
            <el-option label="已完成" value="done" />
            <el-option label="已错过" value="missed" />
          </el-select>
        </div>
        <div class="filter-item keyword-item">
          <el-input v-model.trim="filters.keyword" size="small" clearable placeholder="筛选任务关键词" />
        </div>
        <div class="filter-item switch-item">
          <el-switch v-model="filters.onlyToday" inline-prompt active-text="仅看今日" inactive-text="全周" />
        </div>
        </div>
      </CnSection>

      <CnSection
        v-if="evidenceProfile?.recentEvidenceCount || loading.evidenceProfile"
        class="evidence-profile-section"
        title="成长证据档案"
        description="基于近期可回溯的任务、作答、面试和求职阶段事实"
        compact
        divided
      >
        <div class="evidence-profile-content" v-loading="loading.evidenceProfile">
          <template v-if="evidenceProfile">
            <div class="evidence-profile-meta">
              <CnStatusTag type="success" size="sm">已验证 {{ evidenceProfile.verifiedEvidenceCount || 0 }} 条</CnStatusTag>
              <span v-if="evidenceProfile.latestObservedAt">最近记录 {{ evidenceProfile.latestObservedAt }}</span>
              <span v-if="evidenceProfile.latestCareerStage">最近求职阶段 {{ careerStageText(evidenceProfile.latestCareerStage) }}</span>
            </div>
            <div class="evidence-profile-stats">
              <div><strong>{{ evidenceProfile.completedTaskCount || 0 }}</strong><span>完成任务</span></div>
              <div><strong>{{ evidenceProfile.acceptedOjCount || 0 }}</strong><span>通过 OJ</span></div>
              <div><strong>{{ evidenceProfile.interviewEvidenceCount || 0 }}</strong><span>面试记录</span></div>
              <div><strong>{{ evidenceProfile.sqlReviewCount || 0 }}</strong><span>SQL Review</span></div>
              <div><strong>{{ evidenceProfile.codeReviewCount || 0 }}</strong><span>CodePen Review</span></div>
              <div><strong>{{ evidenceProfile.publicCodeArtifactCount || 0 }}</strong><span>公开代码来源</span></div>
              <div><strong>{{ evidenceProfile.lowMasteryRecordCount || 0 }}</strong><span>待巩固题目</span></div>
              <div><strong>{{ evidenceProfile.selfReportedApplicationCount || 0 }}</strong><span>自报投递</span></div>
            </div>
            <div v-if="evidenceProfile.highlights?.length" class="evidence-highlight-list">
              <div v-for="highlight in evidenceProfile.highlights" :key="highlight.type" class="evidence-highlight-item">
                <strong>{{ highlight.title }}</strong>
                <span>{{ highlight.description }}</span>
              </div>
            </div>
          </template>
        </div>
      </CnSection>

      <CnSection
        v-if="weeklyReview || loading.weeklyReview"
        class="weekly-review-section"
        title="本周节奏复盘"
        description="基于本周任务、顺延记录和剩余可用容量"
        compact
        divided
      >
        <div class="weekly-review-content" v-loading="loading.weeklyReview">
          <template v-if="weeklyReview">
            <div class="weekly-review-meta">
              <CnStatusTag :type="weeklyReviewTone(weeklyReview.level)" size="sm">
                {{ weeklyReviewText(weeklyReview.level) }}
              </CnStatusTag>
              <span>{{ weeklyReview.weekStart }} 至 {{ weeklyReview.weekEnd }}</span>
              <span>完成 {{ weeklyReview.completedTasks || 0 }} / {{ weeklyReview.totalTasks || 0 }}</span>
            </div>
            <h3>{{ weeklyReview.title }}</h3>
            <p>{{ weeklyReview.summary }}</p>
            <div v-if="weeklyReview.signals?.length" class="weekly-review-signal-list">
              <div v-for="signal in weeklyReview.signals" :key="signal.type" class="weekly-review-signal">
                <CnStatusTag :type="weeklySignalTone(signal.level)" size="sm" subtle>
                  {{ signal.title }}
                </CnStatusTag>
                <span>{{ signal.description }}</span>
              </div>
            </div>
            <div class="weekly-review-capacity">
              <span>剩余待办约 {{ weeklyReview.remainingMinutes || 0 }} 分钟</span>
              <span>本周剩余容量约 {{ weeklyReview.remainingCapacityMinutes || 0 }} 分钟</span>
            </div>
            <div v-if="weeklyReview.suggestedAdjustmentMessage" class="weekly-review-actions">
              <el-button link type="primary" @click="openWeeklyReviewAdjustment">
                <el-icon><MagicStick /></el-icon>
                按建议调整本周
              </el-button>
            </div>
          </template>
        </div>
      </CnSection>

      <CnSection
        v-if="skillInsights.length || loading.skillInsights"
        class="skill-insights-section"
        title="能力弱项与下一练习"
        description="基于已验证的作答和面试表现"
        compact
        divided
      >
        <div class="skill-insight-list" v-loading="loading.skillInsights">
          <div v-for="item in skillInsights" :key="item.skillKey" class="skill-insight-item">
            <div class="skill-insight-head">
              <CnStatusTag :type="skillInsightTone(item.level)" size="sm">
                {{ skillInsightText(item.level) }}
              </CnStatusTag>
              <span v-if="item.evidenceCount" class="skill-evidence-count">{{ item.evidenceCount }} 条证据</span>
            </div>
            <h4>{{ item.title }}</h4>
            <p>{{ item.explanation }}</p>
            <small v-if="item.evidenceRefs?.[0]?.observedAt">
              最近证据 {{ item.evidenceRefs[0].observedAt }}
            </small>
            <div v-if="item.recommendation" class="skill-practice">
              <strong>下一练习：{{ item.recommendation.title }}</strong>
              <p>{{ item.recommendation.description }}</p>
              <div class="skill-practice-meta">
                <span v-if="item.recommendation.expectedMinutes">约 {{ item.recommendation.expectedMinutes }} 分钟</span>
                <span v-if="item.recheck?.successCriteria">验证：{{ item.recheck.successCriteria }}</span>
              </div>
              <el-button link type="primary" @click="goRoute(item.recommendation.routePath)">开始练习</el-button>
            </div>
          </div>
        </div>
      </CnSection>

      <CnSection
        v-if="careerNextAction || loading.careerNextAction"
        class="career-next-action-section"
        title="求职闭环下一动作"
        description="基于当前求职阶段的已存在行动清单"
        compact
        divided
      >
        <div class="career-next-content" v-loading="loading.careerNextAction">
          <template v-if="careerNextAction">
            <div class="career-next-meta">
              <CnStatusTag type="brand" size="sm">{{ careerStageText(careerNextAction.stage) }}</CnStatusTag>
              <CnStatusTag v-if="careerNextAction.priority" type="warning" size="sm" subtle>
                {{ careerNextAction.priority }}
              </CnStatusTag>
              <span v-if="careerNextAction.dueDate">截至 {{ careerNextAction.dueDate }}</span>
            </div>
            <h3>{{ careerNextAction.title }}</h3>
            <p>{{ careerNextAction.description }}</p>
            <small>{{ careerNextAction.expectedChange }}</small>
            <div class="career-next-actions">
              <el-button link type="primary" @click="goRoute(careerNextAction.routePath)">去执行</el-button>
              <el-button
                link
                type="success"
                :loading="loading.completeCareerAction"
                @click="handleCompleteCareerAction"
              >
                标记完成
              </el-button>
            </div>
          </template>
        </div>
      </CnSection>

      <CnSection
        v-if="applicationOutcomes || loading.applicationOutcomes"
        class="application-outcome-section"
        title="投递进展与跟进"
        description="基于你主动维护的投递状态，不作为已验证能力成果"
        compact
        divided
      >
        <div class="application-outcome-content" v-loading="loading.applicationOutcomes">
          <template v-if="applicationOutcomes">
            <div class="application-outcome-meta">
              <CnStatusTag type="brand" size="sm">进行中 {{ applicationOutcomes.activeCount || 0 }}</CnStatusTag>
              <CnStatusTag v-if="applicationOutcomes.interviewingCount" type="warning" size="sm" subtle>
                面试中 {{ applicationOutcomes.interviewingCount }}
              </CnStatusTag>
              <CnStatusTag v-if="applicationOutcomes.offerCount" type="success" size="sm" subtle>
                Offer {{ applicationOutcomes.offerCount }}
              </CnStatusTag>
              <CnStatusTag v-if="applicationOutcomes.dueFollowUpCount" type="danger" size="sm" subtle>
                待跟进 {{ applicationOutcomes.dueFollowUpCount }}
              </CnStatusTag>
              <span v-if="applicationOutcomes.nextFollowUpDate">最近跟进 {{ applicationOutcomes.nextFollowUpDate }}</span>
            </div>
            <h3>{{ applicationOutcomes.nextAction?.title || '查看投递进展' }}</h3>
            <p>{{ applicationOutcomes.nextAction?.description || '在求职闭环中维护真实投递与结果状态。' }}</p>
            <small>{{ applicationOutcomes.nextAction?.expectedChange }}</small>
            <div class="application-outcome-actions">
              <el-button link type="primary" @click="goRoute(applicationOutcomes.nextAction?.routePath || '/career-loop?focus=applications')">
                去维护
              </el-button>
            </div>
          </template>
        </div>
      </CnSection>

      <CnSection
        v-if="jobBattleGap || loading.jobBattleGap"
        class="job-battle-gap-section"
        title="岗位差距与下一行动"
        description="基于最近一次岗位匹配和补短板计划"
        compact
        divided
      >
        <div class="job-battle-gap-content" v-loading="loading.jobBattleGap">
          <template v-if="jobBattleGap">
            <div class="job-battle-gap-meta">
              <CnStatusTag v-if="jobBattleGap.targetRole" type="brand" size="sm">
                {{ jobBattleGap.targetRole }}
              </CnStatusTag>
              <CnStatusTag
                v-if="jobBattleGap.matchScore !== null && jobBattleGap.matchScore !== undefined"
                :type="jobBattleScoreTone(jobBattleGap.matchScore)"
                size="sm"
                subtle
              >
                匹配 {{ jobBattleGap.matchScore }} 分
              </CnStatusTag>
              <CnStatusTag v-if="jobBattleGap.p0GapCount" type="danger" size="sm" subtle>
                P0 差距 {{ jobBattleGap.p0GapCount }} 项
              </CnStatusTag>
              <span v-if="jobBattleGap.estimatedPassRate !== null && jobBattleGap.estimatedPassRate !== undefined">
                预估通过率 {{ jobBattleGap.estimatedPassRate }}%
              </span>
            </div>
            <h3>{{ jobBattleGap.nextAction?.title || '查看当前岗位匹配结论' }}</h3>
            <p v-if="jobBattleGap.nextAction?.description">{{ jobBattleGap.nextAction.description }}</p>
            <div v-if="jobBattleGap.nextAction" class="job-battle-action-meta">
              <span v-if="jobBattleGap.nextAction.expectedMinutes">约 {{ jobBattleGap.nextAction.expectedMinutes }} 分钟</span>
              <span v-if="jobBattleGap.nextAction.deliverable">产出：{{ jobBattleGap.nextAction.deliverable }}</span>
            </div>
            <div v-if="jobBattleGap.gaps?.length" class="job-battle-gap-list">
              <div v-for="gap in jobBattleGap.gaps" :key="`${gap.priority}-${gap.skill}-${gap.suggestedAction}`" class="job-battle-gap-item">
                <div>
                  <CnStatusTag :type="jobBattleGapTone(gap.priority)" size="sm">{{ gap.priority || '差距' }}</CnStatusTag>
                  <strong>{{ gap.skill || '待补充能力证据' }}</strong>
                </div>
                <p>{{ gap.suggestedAction || gap.why }}</p>
              </div>
            </div>
            <small v-if="jobBattleSourceText(jobBattleGap.sourceRefs)" class="job-battle-source-text">
              依据：{{ jobBattleSourceText(jobBattleGap.sourceRefs) }}
            </small>
            <div class="job-battle-gap-actions">
              <el-button link type="primary" @click="goRoute(jobBattleGap.nextAction?.routePath || '/job-match-engine')">
                {{ jobBattleGap.nextAction?.routePath ? '开始这一步' : '查看匹配' }}
              </el-button>
            </div>
          </template>
        </div>
      </CnSection>

      <CnSection
        v-if="jobPreparationLoop || loading.jobPreparationLoop"
        class="job-preparation-loop-section"
        title="岗位准备闭环"
        description="岗位差距、补短板计划、模拟面试与投递跟进的当前阶段"
        compact
        divided
      >
        <div class="job-preparation-loop-content" v-loading="loading.jobPreparationLoop">
          <template v-if="jobPreparationLoop">
            <div class="job-preparation-loop-meta">
              <CnStatusTag type="brand" size="sm">
                {{ jobPreparationStageText(jobPreparationLoop.stage) }}
              </CnStatusTag>
              <CnStatusTag v-if="jobPreparationLoop.targetRole" type="info" size="sm" subtle>
                {{ jobPreparationLoop.targetRole }}
              </CnStatusTag>
              <span v-if="jobPreparationLoop.recommendedDirectionName">
                推荐方向 {{ jobPreparationLoop.recommendedDirectionName }}
              </span>
            </div>
            <h3>{{ jobPreparationLoop.currentAction?.title || '查看当前岗位准备阶段' }}</h3>
            <p>{{ jobPreparationLoop.currentAction?.description || jobPreparationLoop.summary }}</p>
            <small v-if="jobPreparationLoop.currentAction?.expectedChange">
              {{ jobPreparationLoop.currentAction.expectedChange }}
            </small>
            <div v-if="jobPreparationLoop.focusSkills?.length" class="job-preparation-focus-list">
              <CnStatusTag v-for="skill in jobPreparationLoop.focusSkills" :key="skill" type="warning" size="sm" subtle>
                {{ skill }}
              </CnStatusTag>
            </div>
            <div class="job-preparation-step-list" aria-label="岗位准备阶段">
              <div v-for="step in jobPreparationLoop.steps || []" :key="step.key" class="job-preparation-step">
                <CnStatusTag :type="jobPreparationStepTone(step.status)" size="sm" subtle>
                  {{ jobPreparationStepText(step.status) }}
                </CnStatusTag>
                <strong>{{ step.title }}</strong>
                <span>{{ step.description }}</span>
              </div>
            </div>
            <small v-if="jobBattleSourceText(jobPreparationLoop.sourceRefs)" class="job-preparation-source-text">
              依据：{{ jobBattleSourceText(jobPreparationLoop.sourceRefs) }}
            </small>
            <div class="job-preparation-loop-actions">
              <el-button link type="primary" @click="goRoute(jobPreparationLoop.currentAction?.routePath)">
                开始这一步
              </el-button>
            </div>
          </template>
        </div>
      </CnSection>

      <CnSection
        v-if="jobMarketSignal || loading.jobMarketSignal"
        class="job-market-signal-section"
        title="岗位样本信号"
        description="基于最近一次你录入的 JD 样本，不等同于实时岗位市场"
        compact
        divided
      >
        <div class="job-market-signal-content" v-loading="loading.jobMarketSignal">
          <template v-if="jobMarketSignal">
            <template v-if="jobMarketSignal.sampleReady">
              <div class="job-market-signal-meta">
                <CnStatusTag type="brand" size="sm">样本 {{ jobMarketSignal.sampleCount }} 条</CnStatusTag>
                <span v-if="jobMarketSignal.sourceObservedAt">分析于 {{ jobMarketSignal.sourceObservedAt }}</span>
              </div>
              <h3>{{ jobMarketSignal.nextAction?.title || '查看岗位样本' }}</h3>
              <p>{{ jobMarketSignal.summary }}</p>
              <div v-if="jobMarketSignal.topRequiredSkills?.length" class="job-market-skill-list">
                <CnStatusTag v-for="item in jobMarketSignal.topRequiredSkills" :key="item.skill" type="info" size="sm" subtle>
                  {{ item.skill }} {{ item.coveragePercent }}%
                </CnStatusTag>
              </div>
              <div class="job-market-signal-actions">
                <el-button link type="primary" @click="handleMarketSignalAction">
                  按此调整本周
                </el-button>
                <el-button link @click="goRoute(jobMarketSignal.nextAction?.routePath || '/job-match-engine')">
                  查看样本
                </el-button>
              </div>
            </template>
            <template v-else>
              <h3>样本暂不足</h3>
              <p>{{ jobMarketSignal.insufficientReason || jobMarketSignal.summary }}</p>
              <div class="job-market-signal-actions">
                <el-button link type="primary" @click="goRoute('/job-match-engine')">补充 JD 样本</el-button>
              </div>
            </template>
          </template>
        </div>
      </CnSection>

      <CnSection
        class="github-connection-section"
        title="GitHub 身份"
        description="绑定后，公开代码来源可按 GitHub 用户 ID 核对贡献归属"
        compact
        divided
      >
        <div class="github-connection-content" v-loading="loading.githubConnection">
          <template v-if="githubConnection?.connected">
            <div class="github-connection-meta">
              <CnStatusTag type="success" size="sm">已绑定</CnStatusTag>
              <strong>{{ githubConnection.githubName || `@${githubConnection.githubLogin}` }}</strong>
              <span v-if="githubConnection.githubName">@{{ githubConnection.githubLogin }}</span>
            </div>
            <div class="github-connection-actions">
              <span>归属匹配只使用 GitHub 稳定用户 ID</span>
              <el-button link type="danger" :loading="loading.githubDisconnect" @click="handleGithubUnlink">
                <el-icon><Delete /></el-icon>
                解除绑定
              </el-button>
            </div>
          </template>
          <template v-else-if="githubConnection?.available">
            <div class="github-connection-meta">
              <CnStatusTag type="warning" size="sm">未绑定</CnStatusTag>
              <span>绑定后可核对公开代码贡献归属</span>
            </div>
            <el-button link type="primary" :loading="loading.githubAuthorize" @click="handleGithubAuthorize">
              <el-icon><Link /></el-icon>
              绑定 GitHub
            </el-button>
          </template>
          <CnStatusTag v-else type="info" size="sm" subtle>当前环境未开放</CnStatusTag>
        </div>
      </CnSection>

      <CnSection
        class="code-artifact-section"
        title="公开代码来源"
        :description="githubConnection?.connected ? '公开来源存在性与 GitHub 身份归属分开核对' : '只验证公开 GitHub commit/PR 是否存在'"
        compact
        divided
      >
        <template #actions>
          <el-button link type="primary" @click="openCodeArtifactDialog">附加来源</el-button>
        </template>
        <div class="code-artifact-content" v-loading="loading.codeArtifacts">
          <div v-if="codeArtifacts.length" class="code-artifact-list">
            <div v-for="artifact in codeArtifacts" :key="artifact.id" class="code-artifact-item">
              <div class="code-artifact-head">
                <div>
                  <CnStatusTag type="info" size="sm" subtle>{{ artifactTypeText(artifact.artifactType) }}</CnStatusTag>
                  <CnStatusTag :type="artifact.ownershipVerified ? 'success' : 'warning'" size="sm" subtle>
                    {{ artifact.ownershipVerified ? '归属已验证' : '归属未验证' }}
                  </CnStatusTag>
                  <strong>{{ artifact.repository }} #{{ artifact.externalId }}</strong>
                </div>
                <el-button
                  link
                  type="danger"
                  :loading="loading.codeArtifactDeleteId === artifact.id"
                  @click="handleDeleteCodeArtifact(artifact)"
                >
                  移除
                </el-button>
              </div>
              <div class="code-artifact-meta">
                <span v-if="artifact.changedFiles !== null && artifact.changedFiles !== undefined">{{ artifact.changedFiles }} 个变更文件</span>
                <span v-if="artifact.additions !== null && artifact.additions !== undefined">+{{ artifact.additions }}</span>
                <span v-if="artifact.deletions !== null && artifact.deletions !== undefined">-{{ artifact.deletions }}</span>
                <span v-if="artifact.verifiedAt">验证于 {{ artifact.verifiedAt }}</span>
              </div>
              <small>{{ artifact.ownershipNotice }}</small>
            </div>
          </div>
          <p v-else class="empty-inline">尚未附加公开代码来源。</p>
        </div>
      </CnSection>

      <CnEmptyState
        v-if="!hasPlan"
        class="empty-wrap"
        title="本周还没有自动驾驶计划"
        description="生成后会自动拆解每日任务包，并根据完成情况给出重排建议。"
        icon="GA"
        surface="panel"
      >
        <template #actions>
          <el-button type="primary" @click="openGenerateDialog">立即生成</el-button>
        </template>
      </CnEmptyState>

      <section v-else class="main-grid">
        <div class="main-left">
          <CnSection
            class="timeline-section"
            title="本周任务时间线"
            :description="`${dashboard.weekStart || '--'} 至 ${dashboard.weekEnd || '--'}`"
            divided
          >
            <div class="day-grid" v-loading="loading.dashboard">
              <div
                v-for="bucket in filteredDayBuckets"
                :key="bucket.date"
                class="day-card"
                :class="{ today: bucket.today }"
              >
                <div class="day-head">
                  <span class="date">{{ shortDate(bucket.date) }}</span>
                  <span class="weekday">{{ bucket.dayLabel }}</span>
                </div>
                <div v-if="!bucket.tasks?.length" class="day-empty">暂无任务</div>
                <div v-else class="task-list">
                  <div v-for="task in bucket.tasks" :key="task.taskId" class="task-item">
                    <div class="task-top">
                      <CnStatusTag :type="moduleTone(task.moduleKey)" size="sm">
                        {{ task.moduleName || '模块' }}
                      </CnStatusTag>
                      <CnStatusTag :type="statusTone(task.status)" size="sm">
                        {{ task.statusText || '待完成' }}
                      </CnStatusTag>
                    </div>
                    <h4>{{ task.title }}</h4>
                    <p>{{ task.description }}</p>
                    <div class="task-meta">
                      <span>{{ task.plannedMinutes }} 分钟</span>
                      <span>{{ task.priority }}</span>
                      <span>+{{ task.taskScore }} 分</span>
                    </div>
                    <div class="task-actions">
                      <el-button link type="primary" @click="goRoute(task.routePath)">去执行</el-button>
                      <el-button
                        link
                        type="success"
                        :disabled="task.status === 'done'"
                        :loading="loading.completeTaskId === task.taskId"
                        @click="handleCompleteTask(task)"
                      >
                        标记完成
                      </el-button>
                      <el-button
                        link
                        type="warning"
                        :disabled="task.status !== 'todo' || !canPostpone(task)"
                        :loading="loading.postponeTaskId === task.taskId"
                        @click="handlePostponeTask(task)"
                      >
                        顺延一天
                      </el-button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </CnSection>
        </div>

        <div class="main-right">
          <CnSection title="周进度趋势" description="按天完成率" compact divided>
            <CnEmptyState
              v-if="!dailyProgress.length"
              title="暂无趋势数据"
              description="完成任务后会生成每日进度趋势。"
              icon="TR"
              size="sm"
              surface="transparent"
            />
            <template v-else>
              <div class="trend-chart">
                <div v-for="item in dailyProgress" :key="item.date" class="trend-bar-item">
                  <div class="trend-bar-bg">
                    <div class="trend-bar-value" :style="{ height: `${Math.max(item.completionRate || 0, 4)}%` }"></div>
                  </div>
                  <small>{{ item.dayLabel }}</small>
                  <span>{{ item.completedTasks }}/{{ item.totalTasks }}</span>
                </div>
              </div>
              <div class="status-row">
                <CnStatusTag type="info" size="sm">待完成 {{ statusSummary.todoTasks || 0 }}</CnStatusTag>
                <CnStatusTag type="success" size="sm">已完成 {{ statusSummary.doneTasks || 0 }}</CnStatusTag>
                <CnStatusTag type="danger" size="sm">已错过 {{ statusSummary.missedTasks || 0 }}</CnStatusTag>
              </div>
              <div class="score-row">
                分值进度 {{ statusSummary.doneScore || 0 }} / {{ statusSummary.targetScore || 0 }}
              </div>
            </template>
          </CnSection>

          <CnSection
            title="模块进度"
            :description="`${dashboard.targetProfile?.targetRole || '通用'} · ${dashboard.targetProfile?.weeklyHours || 0}h`"
            compact
            divided
          >
            <CnEmptyState
              v-if="!moduleProgress.length"
              title="暂无模块任务"
              description="生成计划后会按能力模块展示完成情况。"
              icon="MO"
              size="sm"
              surface="transparent"
            />
            <div v-else class="module-progress-list">
              <div v-for="item in moduleProgress" :key="item.moduleKey" class="module-progress-item">
                <div class="module-row">
                  <strong>{{ item.moduleName }}</strong>
                  <span>{{ item.completedTasks }}/{{ item.totalTasks }}</span>
                </div>
                <el-progress :percentage="item.completionRate" :show-text="false" :stroke-width="8" />
                <div class="module-action">
                  <el-button link type="primary" @click="goRoute(item.routePath)">进入模块</el-button>
                </div>
              </div>
            </div>
          </CnSection>

          <CnSection title="推荐下一步" description="系统建议" compact divided>
            <CnEmptyState
              v-if="!quickActions.length"
              title="暂无建议"
              description="任务推进后会根据当前风险给出下一步。"
              icon="NX"
              size="sm"
              surface="transparent"
            />
            <div v-else class="action-list">
              <div v-for="(item, idx) in quickActions" :key="`${item.title}-${idx}`" class="action-item">
                <div class="action-order">{{ idx + 1 }}</div>
                <div class="action-main">
                  <h4>{{ item.title }}</h4>
                  <p>{{ item.description }}</p>
                  <el-button link type="primary" @click="goRoute(item.routePath)">去处理</el-button>
                </div>
              </div>
            </div>
          </CnSection>

          <CnSection title="事件日志" description="最近操作" compact divided>
            <CnEmptyState
              v-if="!events.length"
              title="暂无事件"
              description="生成、重排和完成动作会记录在这里。"
              icon="EV"
              size="sm"
              surface="transparent"
            />
            <ul v-else class="event-list">
              <li v-for="(event, index) in events" :key="`${event.createTime}-${index}`">
                <CnStatusTag type="brand" size="sm">{{ event.eventLabel || '事件' }}</CnStatusTag>
                <p>{{ event.detail }}</p>
                <small>{{ event.createTime }}</small>
              </li>
            </ul>
          </CnSection>
        </div>
      </section>

      <el-dialog v-model="generateDialogVisible" title="生成自动驾驶计划" width="520px">
        <el-form label-width="100px" class="generate-form">
          <el-form-item label="目标岗位">
            <el-select
              v-model="generateForm.targetRole"
              filterable
              allow-create
              default-first-option
              placeholder="选择或输入岗位"
              class="full-width-control"
            >
              <el-option v-for="item in roleOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="每周投入">
            <div class="slider-wrap">
              <el-slider v-model="generateForm.weeklyHours" :min="3" :max="40" :step="1" />
              <span>{{ generateForm.weeklyHours }} 小时/周</span>
            </div>
          </el-form-item>
          <el-form-item label="周起始日期">
            <el-date-picker
              v-model="generateForm.weekStart"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="默认当前周"
              class="full-width-control"
            />
          </el-form-item>
          <el-alert
            title="系统会按模块权重自动拆解任务，并给出可执行的每日任务包。"
            type="info"
            :closable="false"
            show-icon
          />
        </el-form>
        <template #footer>
          <el-button @click="generateDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="loading.generate" @click="handleGenerate">开始生成</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="adjustmentDialogVisible" title="调整本周计划" width="600px" :close-on-click-modal="false">
        <template v-if="!adjustmentRun">
          <el-form label-position="top" class="adjustment-form">
            <el-form-item label="这周发生了什么变化？">
              <el-input
                v-model.trim="adjustmentMessage"
                type="textarea"
                :rows="5"
                maxlength="1000"
                show-word-limit
                placeholder="例如：这周临时加班，只剩 3 小时，下周要面试 Java 后端岗位。"
              />
            </el-form-item>
          </el-form>
        </template>

        <template v-else>
          <div class="adjustment-preview">
            <el-alert
              v-if="adjustmentRun.status !== 'PREVIEW'"
              :title="adjustmentRun.errorMessage || '当前预览不可确认'"
              type="warning"
              :closable="false"
              show-icon
            />
            <div v-if="adjustmentRun.preview" class="adjustment-summary">
              <div>
                <span>可投入时间</span>
                <strong>{{ adjustmentRun.preview.budgetMinutes || 0 }} 分钟</strong>
              </div>
              <div>
                <span>计划任务</span>
                <strong>{{ adjustmentRun.preview.plannedMinutes || 0 }} 分钟</strong>
              </div>
              <div>
                <span>目标岗位</span>
                <strong>{{ adjustmentRun.preview.targetRole || dashboard.targetProfile?.targetRole || '通用' }}</strong>
              </div>
            </div>

            <div v-if="adjustmentRun.preview?.changes?.length" class="adjustment-change-list">
              <div v-for="change in adjustmentRun.preview.changes" :key="change.taskId" class="adjustment-change-item">
                <CnStatusTag :type="changeTone(change.operation)" size="sm">
                  {{ changeLabel(change.operation) }}
                </CnStatusTag>
                <div>
                  <strong>{{ change.title || `${change.plannedMinutes || 0} 分钟任务` }}</strong>
                  <p>{{ change.reason || '根据当前约束调整' }}</p>
                </div>
                <small v-if="change.toDate">{{ shortDate(change.toDate) }}</small>
              </div>
            </div>
            <CnEmptyState
              v-else
              title="没有可调整的任务"
              description="当前约束下没有可安全变更的本周任务。"
              icon="AI"
              size="sm"
              surface="transparent"
            />
          </div>
        </template>

        <template #footer>
          <template v-if="!adjustmentRun">
            <el-button @click="adjustmentDialogVisible = false">取消</el-button>
            <el-button type="primary" :loading="loading.adjustPreview" @click="handleAdjustmentPreview">生成预览</el-button>
          </template>
          <template v-else>
            <el-button @click="resetAdjustmentPreview">返回修改</el-button>
            <el-button :loading="loading.adjustCancel" @click="handleAdjustmentCancel">取消预览</el-button>
            <el-button
              type="primary"
              :disabled="adjustmentRun.status !== 'PREVIEW'"
              :loading="loading.adjustConfirm"
              @click="handleAdjustmentConfirm"
            >
              确认调整
            </el-button>
          </template>
        </template>
      </el-dialog>

      <el-dialog v-model="codeArtifactDialogVisible" title="附加公开代码来源" width="560px" destroy-on-close>
        <div class="code-artifact-dialog">
          <el-input
            v-model.trim="codeArtifactUrl"
            clearable
            placeholder="https://github.com/{owner}/{repo}/commit/{sha}"
          />
          <div v-if="codeArtifactPreview" class="code-artifact-preview">
            <div class="code-artifact-preview-meta">
              <CnStatusTag type="success" size="sm">来源已验证</CnStatusTag>
              <CnStatusTag :type="codeArtifactPreview.ownershipVerified ? 'success' : 'warning'" size="sm" subtle>
                {{ codeArtifactPreview.ownershipVerified ? '归属已验证' : '归属未验证' }}
              </CnStatusTag>
            </div>
            <strong>{{ codeArtifactPreview.repository }} #{{ codeArtifactPreview.externalId }}</strong>
            <span>{{ artifactTypeText(codeArtifactPreview.artifactType) }}</span>
            <p>{{ codeArtifactPreview.ownershipNotice }}</p>
          </div>
        </div>
        <template #footer>
          <el-button :loading="loading.codeArtifactPreview" @click="handlePreviewCodeArtifact">验证来源</el-button>
          <el-button type="primary" :disabled="!codeArtifactPreview" :loading="loading.codeArtifactAttach" @click="handleAttachCodeArtifact">
            确认附加
          </el-button>
        </template>
      </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, RefreshRight, Refresh, Check, MagicStick, Link, Delete } from '@element-plus/icons-vue'
import { CnEmptyState, CnPageHeader, CnSection, CnStatCard, CnStatusTag } from '@/design-system'
import { growthAutopilotApi } from '@/api/growthAutopilot'
import { growthCoachApi } from '@/api/growthCoach'

const router = useRouter()
const route = useRoute()

const roleOptions = ['后端开发', '前端开发', '全栈开发', '算法工程师', '测试开发', '产品经理', '运维开发']
const loading = reactive({
  dashboard: false,
  generate: false,
  replan: false,
  completeToday: false,
  completeTaskId: null,
  postponeTaskId: null,
  adjustPreview: false,
  adjustConfirm: false,
  adjustCancel: false,
  briefing: false,
  codeArtifacts: false,
  githubConnection: false,
  githubAuthorize: false,
  githubDisconnect: false,
  codeArtifactPreview: false,
  codeArtifactAttach: false,
  codeArtifactDeleteId: null,
  skillInsights: false,
  evidenceProfile: false,
  careerNextAction: false,
  applicationOutcomes: false,
  jobBattleGap: false,
  jobPreparationLoop: false,
  jobMarketSignal: false,
  weeklyReview: false,
  completeCareerAction: false
})

const dashboard = ref({
  hasPlan: false,
  weekStart: '',
  weekEnd: '',
  generatedAt: '',
  targetProfile: {},
  summary: {},
  moduleProgress: [],
  dayBuckets: [],
  dailyProgress: [],
  statusSummary: {},
  quickActions: [],
  events: []
})

const generateDialogVisible = ref(false)
const adjustmentDialogVisible = ref(false)
const codeArtifactDialogVisible = ref(false)
const adjustmentMessage = ref('')
const codeArtifactUrl = ref('')
const codeArtifactPreview = ref<any>(null)
const adjustmentRun = ref<any>(null)
const adjustmentRequestId = ref('')
const generateForm = reactive({
  targetRole: '',
  weeklyHours: 8,
  weekStart: ''
})
const filters = reactive({
  moduleKey: 'all',
  status: 'all',
  keyword: '',
  onlyToday: false
})

const hasPlan = computed(() => Boolean(dashboard.value?.hasPlan))
const summary = computed(() => dashboard.value?.summary || {})
const moduleProgress = computed(() => dashboard.value?.moduleProgress || [])
const dayBuckets = computed(() => dashboard.value?.dayBuckets || [])
const dailyProgress = computed(() => dashboard.value?.dailyProgress || [])
const statusSummary = computed(() => dashboard.value?.statusSummary || {})
const quickActions = computed(() => dashboard.value?.quickActions || [])
const events = computed(() => dashboard.value?.events || [])
const skillInsights = ref([])
const briefing = ref<any>(null)
const codeArtifacts = ref<any[]>([])
const githubConnection = ref<any>(null)
const evidenceProfile = ref<any>(null)
const careerNextAction = ref<any>(null)
const applicationOutcomes = ref<any>(null)
const jobBattleGap = ref<any>(null)
const jobPreparationLoop = ref<any>(null)
const jobMarketSignal = ref<any>(null)
const weeklyReview = ref<any>(null)

const filteredDayBuckets = computed(() => {
  const keyword = (filters.keyword || '').toLowerCase()
  return dayBuckets.value
    .map((bucket) => {
      if (filters.onlyToday && !bucket.today) {
        return { ...bucket, tasks: [] }
      }
      const tasks = (bucket.tasks || []).filter((task) => {
        if (filters.moduleKey !== 'all' && task.moduleKey !== filters.moduleKey) return false
        if (filters.status !== 'all' && task.status !== filters.status) return false
        if (keyword) {
          const content = `${task.title || ''} ${task.description || ''} ${task.moduleName || ''}`.toLowerCase()
          return content.includes(keyword)
        }
        return true
      })
      return { ...bucket, tasks }
    })
    .filter((bucket) => !filters.onlyToday || bucket.today)
})

const moduleTone = (moduleKey) => {
  const map = {
    oj: 'brand',
    interview: 'success',
    flashcard: 'warning',
    plan: 'info',
    mock: 'danger',
    points: 'neutral'
  }
  return map[moduleKey] || 'info'
}

const statusTone = (status) => {
  if (status === 'done') return 'success'
  if (status === 'missed') return 'danger'
  return 'info'
}

const riskTone = (riskLevel) => {
  if (riskLevel === 'high') return 'danger'
  if (riskLevel === 'medium') return 'warning'
  return 'success'
}

const riskText = (riskLevel) => {
  if (riskLevel === 'high') return '高风险'
  if (riskLevel === 'medium') return '中风险'
  return '低风险'
}

const skillInsightTone = (level) => {
  if (level === 'urgent') return 'danger'
  return 'warning'
}

const skillInsightText = (level) => {
  return level === 'urgent' ? '优先巩固' : '建议巩固'
}

const careerStageText = (stage) => {
  const stageMap = {
    INIT: 'JD 准备',
    JD_PARSED: '简历匹配',
    RESUME_MATCHED: '行动计划',
    PLAN_READY: '开始执行',
    PLAN_EXECUTING: '计划执行',
    INTERVIEW_DONE: '面试复盘',
    REVIEWED: '投递准备',
    OFFER_TRACKING: '投递跟踪'
  }
  return stageMap[stage] || '求职闭环'
}

const jobBattleScoreTone = (score) => {
  if (Number(score) < 60) return 'danger'
  if (Number(score) < 80) return 'warning'
  return 'success'
}

const jobBattleGapTone = (priority) => {
  if (priority === 'P0') return 'danger'
  if (priority === 'P1') return 'warning'
  return 'info'
}

const jobPreparationStageText = (stage) => {
  const stageMap = {
    GAP_ANALYSIS: '复核匹配',
    STUDY_PLAN: '补短板计划',
    MOCK_INTERVIEW: '模拟验证',
    APPLICATION_TRACKING: '投递跟进'
  }
  return stageMap[stage] || '岗位准备'
}

const jobPreparationStepTone = (status) => {
  if (status === 'completed') return 'success'
  if (status === 'current') return 'brand'
  if (status === 'observed') return 'info'
  return 'neutral'
}

const jobPreparationStepText = (status) => {
  if (status === 'completed') return '已完成'
  if (status === 'current') return '当前'
  if (status === 'observed') return '已记录'
  return '待进行'
}

const weeklyReviewTone = (level) => {
  if (level === 'high_risk') return 'danger'
  if (level === 'attention') return 'warning'
  return 'success'
}

const weeklyReviewText = (level) => {
  if (level === 'high_risk') return '需要调整'
  if (level === 'attention') return '建议关注'
  return '节奏稳定'
}

const weeklySignalTone = (level) => (level === 'urgent' ? 'danger' : 'warning')

const briefingTone = (riskLevel) => {
  if (riskLevel === 'high_risk') return 'danger'
  if (riskLevel === 'attention') return 'warning'
  return 'brand'
}

const briefingSourceText = (source) => {
  const sourceMap = {
    weekly_review: '本周节奏',
    application_outcomes: '投递跟进',
    career_loop: '求职闭环',
    job_battle_gap: '岗位差距',
    skill_insight: '能力练习',
    job_market_signal: '岗位样本',
    today_action: '今日行动',
    growth_autopilot: '本周计划'
  }
  return sourceMap[source] || '成长教练'
}

const briefingActionText = (actionType) => {
  if (actionType === 'PLAN_ADJUSTMENT') return hasPlan.value ? '生成调整预览' : '建立本周计划'
  if (actionType === 'PLAN_SETUP') return '建立本周计划'
  return '开始这一步'
}

const artifactTypeText = (artifactType) => {
  return artifactType === 'PULL_REQUEST' ? '公开 PR' : '公开提交'
}

const jobBattleSourceText = (sources) => {
  if (!Array.isArray(sources)) return ''
  return sources
    .filter((source) => source?.label)
    .map((source) => `${source.label}${source.observedAt ? `（${source.observedAt}）` : ''}`)
    .join('；')
}

const shortDate = (dateText) => {
  if (!dateText || dateText.length < 10) return dateText || '--'
  return dateText.slice(5)
}

const clampWeeklyHours = (value) => Math.max(3, Math.min(40, Number(value) || 8))

const canPostpone = (task) => {
  if (!task?.taskDate || !dashboard.value?.weekEnd) return false
  return task.taskDate < dashboard.value.weekEnd
}

const createClientRequestId = () => {
  if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID()
  return `growth-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

const changeLabel = (operation) => {
  if (operation === 'MOVE') return '移动'
  if (operation === 'SUPERSEDE') return '替代'
  return '保留'
}

const changeTone = (operation) => {
  if (operation === 'MOVE') return 'warning'
  if (operation === 'SUPERSEDE') return 'danger'
  return 'success'
}

const loadDashboard = async () => {
  loading.dashboard = true
  try {
    const data = await growthAutopilotApi.getDashboard()
    dashboard.value = data || {
      hasPlan: false,
      summary: {},
      moduleProgress: [],
      dayBuckets: [],
      dailyProgress: [],
      statusSummary: {},
      quickActions: [],
      events: []
    }
  } catch (error) {
    console.error('加载自动驾驶看板失败', error)
    ElMessage.error('加载自动驾驶看板失败，请稍后重试')
  } finally {
    loading.dashboard = false
  }
}

const openGenerateDialog = () => {
  generateForm.targetRole = dashboard.value?.targetProfile?.targetRole || ''
  generateForm.weeklyHours = clampWeeklyHours(dashboard.value?.targetProfile?.weeklyHours || 8)
  generateForm.weekStart = dashboard.value?.weekStart || ''
  generateDialogVisible.value = true
}

const loadBriefing = async () => {
  loading.briefing = true
  try {
    briefing.value = (await growthCoachApi.getBriefing()) || null
  } catch (error) {
    console.error('加载成长教练焦点失败', error)
    briefing.value = null
  } finally {
    loading.briefing = false
  }
}

const loadCodeArtifacts = async () => {
  loading.codeArtifacts = true
  try {
    const data = await growthCoachApi.getCodeArtifacts()
    codeArtifacts.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.error('加载公开代码来源失败', error)
    codeArtifacts.value = []
  } finally {
    loading.codeArtifacts = false
  }
}

const loadGithubConnection = async () => {
  loading.githubConnection = true
  try {
    githubConnection.value = (await growthCoachApi.getGithubConnection()) || null
  } catch (error) {
    console.error('加载 GitHub 绑定状态失败', error)
    githubConnection.value = null
  } finally {
    loading.githubConnection = false
  }
}

const handleGithubAuthorize = async () => {
  loading.githubAuthorize = true
  try {
    const data = await growthCoachApi.authorizeGithubConnection()
    if (!data?.authorizationUrl) {
      throw new Error('GitHub 授权地址为空')
    }
    window.location.assign(data.authorizationUrl)
  } catch (error) {
    console.error('发起 GitHub 绑定失败', error)
    ElMessage.error('暂时无法发起 GitHub 绑定，请稍后重试')
  } finally {
    loading.githubAuthorize = false
  }
}

const handleGithubUnlink = async () => {
  try {
    await ElMessageBox.confirm('解除绑定后，公开代码来源将不再按当前 GitHub 身份核对归属。', '解除 GitHub 绑定', {
      type: 'warning',
      confirmButtonText: '解除绑定',
      cancelButtonText: '取消'
    })
  } catch (error) {
    return
  }
  loading.githubDisconnect = true
  try {
    await growthCoachApi.unlinkGithubConnection()
    ElMessage.success('GitHub 绑定已解除')
    await Promise.all([loadGithubConnection(), loadCodeArtifacts(), loadEvidenceViews()])
  } catch (error) {
    console.error('解除 GitHub 绑定失败', error)
    ElMessage.error('解除绑定失败，请稍后重试')
  } finally {
    loading.githubDisconnect = false
  }
}

const openAdjustmentDialog = () => {
  if (!hasPlan.value) return
  adjustmentMessage.value = ''
  adjustmentRun.value = null
  adjustmentRequestId.value = createClientRequestId()
  adjustmentDialogVisible.value = true
}

const openCodeArtifactDialog = () => {
  codeArtifactUrl.value = ''
  codeArtifactPreview.value = null
  codeArtifactDialogVisible.value = true
}

const handlePreviewCodeArtifact = async () => {
  if (!codeArtifactUrl.value) {
    ElMessage.warning('请先填写公开 GitHub commit 或 PR 链接')
    return
  }
  loading.codeArtifactPreview = true
  try {
    codeArtifactPreview.value = await growthCoachApi.previewCodeArtifact({ url: codeArtifactUrl.value })
    ElMessage.success('公开来源已验证')
  } catch (error) {
    console.error('验证公开代码来源失败', error)
    codeArtifactPreview.value = null
  } finally {
    loading.codeArtifactPreview = false
  }
}

const handleAttachCodeArtifact = async () => {
  if (!codeArtifactPreview.value || !codeArtifactUrl.value) return
  loading.codeArtifactAttach = true
  try {
    await growthCoachApi.attachCodeArtifact({ url: codeArtifactUrl.value })
    codeArtifactDialogVisible.value = false
    codeArtifactPreview.value = null
    ElMessage.success('公开代码来源已附加')
    await Promise.all([loadCodeArtifacts(), loadEvidenceViews()])
  } catch (error) {
    console.error('附加公开代码来源失败', error)
  } finally {
    loading.codeArtifactAttach = false
  }
}

const handleDeleteCodeArtifact = async (artifact) => {
  if (!artifact?.id) return
  loading.codeArtifactDeleteId = artifact.id
  try {
    await growthCoachApi.deleteCodeArtifact(artifact.id)
    ElMessage.success('公开代码来源已移除')
    await Promise.all([loadCodeArtifacts(), loadEvidenceViews()])
  } catch (error) {
    console.error('移除公开代码来源失败', error)
  } finally {
    loading.codeArtifactDeleteId = null
  }
}

const resetAdjustmentPreview = () => {
  adjustmentRun.value = null
  adjustmentRequestId.value = createClientRequestId()
}

const handleAdjustmentPreview = async () => {
  if (!adjustmentMessage.value) {
    ElMessage.warning('请先描述本周发生的变化')
    return
  }
  loading.adjustPreview = true
  try {
    adjustmentRun.value = await growthCoachApi.previewPlanAdjustment({
      message: adjustmentMessage.value,
      weekStart: dashboard.value.weekStart || undefined,
      clientRequestId: adjustmentRequestId.value || createClientRequestId()
    })
    if (adjustmentRun.value?.status === 'PREVIEW') {
      ElMessage.success('计划变更预览已生成')
    }
  } catch (error) {
    console.error('生成计划调整预览失败', error)
  } finally {
    loading.adjustPreview = false
  }
}

const loadSkillInsights = async () => {
  loading.skillInsights = true
  try {
    const data = await growthCoachApi.getSkillInsights()
    skillInsights.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.error('加载能力弱项失败', error)
    skillInsights.value = []
  } finally {
    loading.skillInsights = false
  }
}

const loadEvidenceProfile = async () => {
  loading.evidenceProfile = true
  try {
    evidenceProfile.value = (await growthCoachApi.getEvidenceProfile()) || null
  } catch (error) {
    console.error('加载成长证据档案失败', error)
    evidenceProfile.value = null
  } finally {
    loading.evidenceProfile = false
  }
}

const loadEvidenceViews = async () => {
  await loadSkillInsights()
  await loadEvidenceProfile()
}

const loadCareerNextAction = async () => {
  loading.careerNextAction = true
  try {
    careerNextAction.value = (await growthCoachApi.getCareerNextAction()) || null
  } catch (error) {
    console.error('加载求职下一动作失败', error)
    careerNextAction.value = null
  } finally {
    loading.careerNextAction = false
  }
}

const loadApplicationOutcomes = async () => {
  loading.applicationOutcomes = true
  try {
    applicationOutcomes.value = (await growthCoachApi.getApplicationOutcomes()) || null
  } catch (error) {
    console.error('加载投递进展失败', error)
    applicationOutcomes.value = null
  } finally {
    loading.applicationOutcomes = false
  }
}

const loadJobBattleGap = async () => {
  loading.jobBattleGap = true
  try {
    jobBattleGap.value = (await growthCoachApi.getJobBattleGap()) || null
  } catch (error) {
    console.error('加载岗位差距摘要失败', error)
    jobBattleGap.value = null
  } finally {
    loading.jobBattleGap = false
  }
}

const loadJobPreparationLoop = async () => {
  loading.jobPreparationLoop = true
  try {
    jobPreparationLoop.value = (await growthCoachApi.getJobPreparationLoop()) || null
  } catch (error) {
    console.error('加载岗位准备闭环失败', error)
    jobPreparationLoop.value = null
  } finally {
    loading.jobPreparationLoop = false
  }
}

const loadJobMarketSignal = async () => {
  loading.jobMarketSignal = true
  try {
    jobMarketSignal.value = (await growthCoachApi.getJobMarketSignal()) || null
  } catch (error) {
    console.error('加载岗位样本信号失败', error)
    jobMarketSignal.value = null
  } finally {
    loading.jobMarketSignal = false
  }
}

const loadWeeklyReview = async () => {
  loading.weeklyReview = true
  try {
    weeklyReview.value = (await growthCoachApi.getWeeklyReview()) || null
  } catch (error) {
    console.error('加载本周节奏复盘失败', error)
    weeklyReview.value = null
  } finally {
    loading.weeklyReview = false
  }
}

const refreshDashboard = async () => {
  await Promise.all([
    loadDashboard(),
    loadBriefing(),
    loadGithubConnection(),
    loadCodeArtifacts(),
    loadEvidenceViews(),
    loadCareerNextAction(),
    loadApplicationOutcomes(),
    loadJobBattleGap(),
    loadJobPreparationLoop(),
    loadJobMarketSignal(),
    loadWeeklyReview()
  ])
}

const openWeeklyReviewAdjustment = () => {
  const message = weeklyReview.value?.suggestedAdjustmentMessage
  if (!message || !hasPlan.value) return
  openAdjustmentDialog()
  adjustmentMessage.value = message
}

const openPrefilledAdjustment = (message) => {
  if (!hasPlan.value) {
    openGenerateDialog()
    return
  }
  if (!message) {
    return
  }
  openAdjustmentDialog()
  adjustmentMessage.value = message
}

const handleBriefingAction = () => {
  const action = briefing.value?.primaryAction
  if (!action) return
  if (action.actionType === 'PLAN_SETUP') {
    openGenerateDialog()
    return
  }
  if (action.actionType === 'PLAN_ADJUSTMENT') {
    openPrefilledAdjustment(action.prefillMessage)
    return
  }
  goRoute(action.routePath)
}

const handleMarketSignalAction = () => {
  openPrefilledAdjustment(jobMarketSignal.value?.nextAction?.prefillMessage)
}

const handleAdjustmentConfirm = async () => {
  if (!adjustmentRun.value?.runId || !adjustmentRun.value?.previewHash) return
  loading.adjustConfirm = true
  try {
    const data = await growthCoachApi.confirmPlanAdjustment(adjustmentRun.value.runId, {
      previewHash: adjustmentRun.value.previewHash
    })
    adjustmentRun.value = data
    if (data?.status === 'EXECUTED') {
      dashboard.value = data.dashboard || dashboard.value
      adjustmentDialogVisible.value = false
      ElMessage.success('本周计划已调整')
      await refreshDashboard()
    }
  } catch (error) {
    console.error('确认计划调整失败', error)
  } finally {
    loading.adjustConfirm = false
  }
}

const handleAdjustmentCancel = async () => {
  if (!adjustmentRun.value?.runId) {
    adjustmentDialogVisible.value = false
    return
  }
  loading.adjustCancel = true
  try {
    await growthCoachApi.cancelPlanAdjustment(adjustmentRun.value.runId)
    adjustmentDialogVisible.value = false
    adjustmentRun.value = null
    ElMessage.success('计划预览已取消')
  } catch (error) {
    console.error('取消计划调整预览失败', error)
  } finally {
    loading.adjustCancel = false
  }
}

const handleGenerate = async () => {
  loading.generate = true
  try {
    const data = await growthAutopilotApi.generate({
      targetRole: generateForm.targetRole,
      weeklyHours: clampWeeklyHours(generateForm.weeklyHours),
      weekStart: generateForm.weekStart || undefined
    })
    dashboard.value = data || dashboard.value
    generateDialogVisible.value = false
    ElMessage.success('自动驾驶周计划生成成功')
    await refreshDashboard()
  } catch (error) {
    console.error('生成自动驾驶计划失败', error)
    ElMessage.error('生成失败，请稍后重试')
  } finally {
    loading.generate = false
  }
}

const handleReplan = async () => {
  if (!hasPlan.value) {
    ElMessage.warning('请先生成本周计划')
    return
  }
  loading.replan = true
  try {
    const data = await growthAutopilotApi.replan({
      weekStart: dashboard.value.weekStart || undefined
    })
    dashboard.value = data || dashboard.value
    ElMessage.success('任务已按当前进度完成重排')
    await refreshDashboard()
  } catch (error) {
    console.error('任务重排失败', error)
    ElMessage.error('任务重排失败，请稍后重试')
  } finally {
    loading.replan = false
  }
}

const handleCompleteToday = async () => {
  if (!hasPlan.value) {
    ElMessage.warning('请先生成本周计划')
    return
  }
  loading.completeToday = true
  try {
    const data = await growthAutopilotApi.completeToday(dashboard.value.weekStart || undefined)
    dashboard.value = data || dashboard.value
    ElMessage.success('今日待办已批量完成')
    await refreshDashboard()
  } catch (error) {
    console.error('批量完成今日任务失败', error)
    ElMessage.error('批量完成失败，请稍后重试')
  } finally {
    loading.completeToday = false
  }
}

const handleCompleteTask = async (task) => {
  if (!task?.taskId || task.status === 'done') return
  loading.completeTaskId = task.taskId
  try {
    const data = await growthAutopilotApi.completeTask(task.taskId)
    dashboard.value = data || dashboard.value
    ElMessage.success('任务已完成')
    await refreshDashboard()
  } catch (error) {
    console.error('任务完成失败', error)
    ElMessage.error('任务完成失败，请稍后重试')
  } finally {
    loading.completeTaskId = null
  }
}

const handlePostponeTask = async (task) => {
  if (!task?.taskId || !canPostpone(task) || task.status !== 'todo') return
  loading.postponeTaskId = task.taskId
  try {
    const data = await growthAutopilotApi.postponeTask(task.taskId)
    dashboard.value = data || dashboard.value
    ElMessage.success('任务已顺延一天')
    await refreshDashboard()
  } catch (error) {
    console.error('任务顺延失败', error)
    ElMessage.error('任务顺延失败，请稍后重试')
  } finally {
    loading.postponeTaskId = null
  }
}

const handleCompleteCareerAction = async () => {
  const actionId = careerNextAction.value?.actionId
  if (!actionId) return
  loading.completeCareerAction = true
  try {
    await growthCoachApi.completeCareerNextAction(actionId)
    ElMessage.success('求职动作已标记完成')
    await loadCareerNextAction()
  } catch (error) {
    console.error('标记求职动作完成失败', error)
  } finally {
    loading.completeCareerAction = false
  }
}

const goRoute = (path) => {
  if (!path) return
  router.push(path)
}

onMounted(async () => {
  await refreshDashboard()
  const requestedAction = Array.isArray(route.query.growthAction)
    ? route.query.growthAction[0]
    : route.query.growthAction
  if (requestedAction === 'PLAN_SETUP') {
    openGenerateDialog()
  } else if (requestedAction === 'PLAN_ADJUSTMENT') {
    openPrefilledAdjustment(briefing.value?.primaryAction?.prefillMessage)
  }
})
</script>

<style scoped>
.growth-autopilot-panel {
  display: grid;
  gap: var(--cn-space-5);
  width: 100%;
  min-width: 0;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-3);
}

.coach-briefing-content,
.job-market-signal-content,
.job-preparation-loop-content,
.github-connection-content,
.code-artifact-content {
  min-height: 72px;
}

.coach-briefing-meta,
.coach-briefing-actions,
.job-market-signal-meta,
.job-market-signal-actions,
.job-market-skill-list,
.job-preparation-loop-meta,
.job-preparation-loop-actions,
.job-preparation-focus-list,
.github-connection-meta,
.github-connection-actions,
.code-artifact-head,
.code-artifact-preview-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.coach-briefing-meta span,
.job-market-signal-meta span,
.job-preparation-loop-meta span,
.job-preparation-loop-content > small,
.github-connection-actions span,
.code-artifact-meta,
.code-artifact-item small,
.code-artifact-preview span,
.code-artifact-preview p {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.coach-briefing-content h2,
.job-market-signal-content h3,
.job-preparation-loop-content h3 {
  margin: var(--cn-space-2) 0 var(--cn-space-1);
  color: var(--cn-color-text-primary);
  font-size: 16px;
}

.coach-briefing-content > p,
.job-market-signal-content > p,
.job-preparation-loop-content > p {
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.coach-briefing-details {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cn-space-3);
  margin: var(--cn-space-3) 0 0;
}

.coach-briefing-details > div {
  min-width: 0;
  padding-left: var(--cn-space-3);
  border-left: 2px solid var(--cn-color-border-subtle);
}

.coach-briefing-details dt {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.coach-briefing-details dd {
  margin: var(--cn-space-1) 0 0;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.55;
  overflow-wrap: anywhere;
}

.coach-briefing-actions,
.job-market-signal-actions,
.job-market-skill-list,
.job-preparation-loop-actions,
.job-preparation-focus-list {
  margin-top: var(--cn-space-3);
}

.job-preparation-step-list {
  display: grid;
  gap: var(--cn-space-2);
  margin-top: var(--cn-space-3);
  padding-top: var(--cn-space-3);
  border-top: 1px solid var(--cn-color-border-subtle);
}

.job-preparation-step {
  display: grid;
  grid-template-columns: auto minmax(104px, 0.38fr) minmax(0, 1fr);
  align-items: start;
  gap: var(--cn-space-2);
  min-width: 0;
  padding-bottom: var(--cn-space-2);
  border-bottom: 1px solid var(--cn-color-border-subtle);
}

.job-preparation-step:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.job-preparation-step strong {
  color: var(--cn-color-text-primary);
  font-size: 13px;
  line-height: 1.5;
}

.job-preparation-step span,
.job-preparation-source-text {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.job-preparation-source-text {
  display: block;
  margin-top: var(--cn-space-3);
}

.code-artifact-list {
  display: flex;
  flex-direction: column;
  gap: var(--cn-space-2);
}

.code-artifact-item,
.code-artifact-preview {
  padding: var(--cn-space-3);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-control);
  background: var(--cn-color-bg-surface-muted);
}

.code-artifact-head {
  justify-content: space-between;
}

.code-artifact-head > div {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
  min-width: 0;
}

.code-artifact-head strong,
.code-artifact-preview strong {
  color: var(--cn-color-text-primary);
  font-size: 13px;
  overflow-wrap: anywhere;
}

.code-artifact-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
  margin-top: var(--cn-space-2);
}

.code-artifact-item small {
  display: block;
  margin-top: var(--cn-space-2);
  line-height: 1.5;
}

.code-artifact-dialog {
  display: flex;
  flex-direction: column;
  gap: var(--cn-space-3);
}

.github-connection-content {
  display: flex;
  flex-direction: column;
  gap: var(--cn-space-2);
}

.github-connection-meta,
.github-connection-actions {
  justify-content: space-between;
}

.github-connection-meta > span,
.github-connection-actions > span {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.code-artifact-preview {
  display: flex;
  flex-direction: column;
  gap: var(--cn-space-2);
}

.code-artifact-preview p {
  margin: 0;
  line-height: 1.55;
}

.empty-wrap {
  min-height: 280px;
}

.filter-wrap {
  display: flex;
  align-items: center;
  gap: var(--cn-space-3);
  flex-wrap: wrap;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: var(--cn-space-2);
}

.module-filter-select {
  width: 150px;
}

.status-filter-select {
  width: 140px;
}

.full-width-control {
  width: 100%;
}

.filter-label {
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  font-weight: 700;
}

.keyword-item {
  flex: 1;
  min-width: 180px;
}

.switch-item {
  margin-left: auto;
}

.main-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: var(--cn-space-4);
}

.main-left,
.main-right {
  display: flex;
  flex-direction: column;
  gap: var(--cn-space-4);
}

.day-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--cn-space-3);
}

.day-card {
  min-width: 0;
  min-height: 180px;
  padding: var(--cn-space-3);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
}

.day-card.today {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 38%, var(--cn-color-border-subtle));
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--cn-color-brand-primary) 18%, transparent);
  background: color-mix(in srgb, var(--cn-color-brand-soft) 52%, var(--cn-color-bg-surface));
}

.day-head {
  display: flex;
  justify-content: space-between;
  gap: var(--cn-space-2);
  margin-bottom: var(--cn-space-3);
}

.day-head .date {
  font-weight: 700;
  color: var(--cn-color-text-primary);
}

.day-head .weekday {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.day-empty,
.empty-inline {
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: var(--cn-space-2);
}

.task-item {
  min-width: 0;
  padding: var(--cn-space-3);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-control);
  background: var(--cn-color-bg-surface);
}

.task-top {
  display: flex;
  justify-content: space-between;
  gap: var(--cn-space-2);
  min-width: 0;
}

.task-item h4 {
  margin: var(--cn-space-2) 0 var(--cn-space-1);
  font-size: 13px;
  color: var(--cn-color-text-primary);
  line-height: 1.5;
}

.task-item p {
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.task-meta {
  margin-top: var(--cn-space-2);
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.task-actions {
  margin-top: var(--cn-space-2);
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.module-progress-list {
  display: flex;
  flex-direction: column;
  gap: var(--cn-space-3);
}

.module-progress-item {
  padding: var(--cn-space-3);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-control);
  background: var(--cn-color-bg-surface-muted);
}

.module-row {
  display: flex;
  justify-content: space-between;
  gap: var(--cn-space-3);
  margin-bottom: var(--cn-space-2);
}

.module-row strong {
  color: var(--cn-color-text-primary);
}

.module-row span {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.module-action {
  margin-top: var(--cn-space-1);
}

.skill-insight-list {
  display: flex;
  flex-direction: column;
  gap: var(--cn-space-3);
}

.weekly-review-content {
  min-height: 72px;
}

.evidence-profile-content {
  min-height: 72px;
}

.evidence-profile-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.evidence-profile-meta span,
.evidence-highlight-item span {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.evidence-profile-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: var(--cn-space-3);
  margin-top: var(--cn-space-3);
}

.evidence-profile-stats > div {
  min-width: 0;
  padding-left: var(--cn-space-3);
  border-left: 2px solid var(--cn-color-border-subtle);
}

.evidence-profile-stats strong,
.evidence-profile-stats span {
  display: block;
}

.evidence-profile-stats strong {
  color: var(--cn-color-text-primary);
  font-size: 18px;
  line-height: 1.3;
}

.evidence-profile-stats span {
  margin-top: var(--cn-space-1);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.evidence-highlight-list {
  display: flex;
  flex-direction: column;
  gap: var(--cn-space-2);
  margin-top: var(--cn-space-3);
  padding-top: var(--cn-space-3);
  border-top: 1px solid var(--cn-color-border-subtle);
}

.evidence-highlight-item {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.evidence-highlight-item strong {
  color: var(--cn-color-text-primary);
  font-size: 13px;
}

.evidence-highlight-item span {
  overflow-wrap: anywhere;
}

.weekly-review-meta,
.weekly-review-capacity,
.weekly-review-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.weekly-review-meta span,
.weekly-review-capacity,
.weekly-review-signal span {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.weekly-review-content h3 {
  margin: var(--cn-space-2) 0 var(--cn-space-1);
  color: var(--cn-color-text-primary);
  font-size: 15px;
}

.weekly-review-content > p {
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.weekly-review-signal-list {
  display: flex;
  flex-direction: column;
  gap: var(--cn-space-2);
  margin-top: var(--cn-space-3);
  padding-top: var(--cn-space-3);
  border-top: 1px solid var(--cn-color-border-subtle);
}

.weekly-review-signal {
  display: flex;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.weekly-review-signal span {
  flex: 1;
  min-width: 180px;
  line-height: 1.5;
}

.weekly-review-capacity {
  margin-top: var(--cn-space-3);
}

.weekly-review-actions {
  margin-top: var(--cn-space-2);
}

.skill-insight-item {
  padding: var(--cn-space-3);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-control);
  background: var(--cn-color-bg-surface-muted);
}

.skill-insight-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-2);
}

.skill-evidence-count,
.skill-insight-item small,
.skill-practice-meta {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.skill-insight-item h4 {
  margin: var(--cn-space-2) 0 var(--cn-space-1);
  color: var(--cn-color-text-primary);
  font-size: 14px;
}

.skill-insight-item > p,
.skill-practice p {
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.skill-practice {
  margin-top: var(--cn-space-3);
  padding-top: var(--cn-space-3);
  border-top: 1px solid var(--cn-color-border-subtle);
}

.skill-practice strong {
  display: block;
  margin-bottom: var(--cn-space-1);
  color: var(--cn-color-text-primary);
  font-size: 13px;
}

.skill-practice-meta {
  display: flex;
  flex-direction: column;
  gap: var(--cn-space-1);
  margin-top: var(--cn-space-2);
}

.career-next-content {
  min-height: 72px;
}

.career-next-meta,
.career-next-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.career-next-meta span,
.career-next-content small {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.career-next-content h3 {
  margin: var(--cn-space-2) 0 var(--cn-space-1);
  color: var(--cn-color-text-primary);
  font-size: 15px;
}

.career-next-content p {
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.career-next-content small {
  display: block;
  margin-top: var(--cn-space-2);
}

.career-next-actions {
  margin-top: var(--cn-space-2);
}

.application-outcome-content {
  min-height: 72px;
}

.application-outcome-meta,
.application-outcome-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.application-outcome-meta span,
.application-outcome-content small {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.application-outcome-content h3 {
  margin: var(--cn-space-2) 0 var(--cn-space-1);
  color: var(--cn-color-text-primary);
  font-size: 15px;
}

.application-outcome-content p {
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.application-outcome-content small {
  display: block;
  margin-top: var(--cn-space-2);
}

.application-outcome-actions {
  margin-top: var(--cn-space-2);
}

.job-battle-gap-content {
  min-height: 72px;
}

.job-battle-gap-meta,
.job-battle-action-meta,
.job-battle-gap-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.job-battle-gap-meta span,
.job-battle-action-meta,
.job-battle-source-text {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.job-battle-gap-content h3 {
  margin: var(--cn-space-2) 0 var(--cn-space-1);
  color: var(--cn-color-text-primary);
  font-size: 15px;
}

.job-battle-gap-content > p,
.job-battle-gap-item p {
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.job-battle-action-meta {
  margin-top: var(--cn-space-2);
}

.job-battle-gap-list {
  display: flex;
  flex-direction: column;
  gap: var(--cn-space-2);
  margin-top: var(--cn-space-3);
  padding-top: var(--cn-space-3);
  border-top: 1px solid var(--cn-color-border-subtle);
}

.job-battle-gap-item {
  display: flex;
  flex-direction: column;
  gap: var(--cn-space-1);
}

.job-battle-gap-item > div {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.job-battle-gap-item strong {
  color: var(--cn-color-text-primary);
  font-size: 13px;
}

.job-battle-source-text {
  display: block;
  margin-top: var(--cn-space-3);
}

.job-battle-gap-actions {
  margin-top: var(--cn-space-2);
}

.trend-chart {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: var(--cn-space-2);
  margin-bottom: var(--cn-space-3);
}

.trend-bar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--cn-space-1);
  min-width: 0;
}

.trend-bar-bg {
  width: 100%;
  height: 90px;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-control);
  background: var(--cn-color-bg-surface-muted);
  display: flex;
  align-items: flex-end;
  padding: var(--cn-space-1);
}

.trend-bar-value {
  width: 100%;
  border-radius: calc(var(--cn-radius-control) - 2px);
  background: var(--cn-color-brand-primary);
  transition: height var(--cn-motion-base) var(--cn-ease-out);
}

.trend-bar-item small {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.trend-bar-item span {
  color: var(--cn-color-text-secondary);
  font-size: 12px;
}

.status-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
  margin-bottom: var(--cn-space-2);
}

.score-row {
  color: var(--cn-color-text-secondary);
  font-size: 12px;
}

.action-list {
  display: flex;
  flex-direction: column;
  gap: var(--cn-space-3);
}

.action-item {
  display: flex;
  gap: var(--cn-space-3);
  padding: var(--cn-space-3);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-control);
  background: var(--cn-color-bg-surface-muted);
}

.action-order {
  width: 26px;
  height: 26px;
  border-radius: var(--cn-radius-control);
  background: var(--cn-color-brand-primary);
  color: var(--cn-button-primary-color);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 12px;
}

.action-main h4 {
  margin: 0 0 var(--cn-space-1);
  color: var(--cn-color-text-primary);
  font-size: 14px;
}

.action-main p {
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.event-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--cn-space-2);
}

.event-list li {
  padding: var(--cn-space-3);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-control);
  background: var(--cn-color-bg-surface-muted);
}

.event-list p {
  margin: var(--cn-space-2) 0 var(--cn-space-1);
  color: var(--cn-color-text-secondary);
  font-size: 13px;
}

.event-list small {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.generate-form {
  padding: 4px 6px 0;
}

.adjustment-form {
  padding: 4px 6px 0;
}

.adjustment-preview {
  display: flex;
  flex-direction: column;
  gap: var(--cn-space-4);
}

.adjustment-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--cn-space-3);
}

.adjustment-summary > div {
  min-width: 0;
  padding: var(--cn-space-3);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-control);
  background: var(--cn-color-bg-surface-muted);
}

.adjustment-summary span,
.adjustment-summary strong {
  display: block;
}

.adjustment-summary span {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.adjustment-summary strong {
  margin-top: var(--cn-space-1);
  color: var(--cn-color-text-primary);
  font-size: 14px;
  overflow-wrap: anywhere;
}

.adjustment-change-list {
  display: flex;
  flex-direction: column;
  gap: var(--cn-space-2);
  max-height: 300px;
  overflow-y: auto;
}

.adjustment-change-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: start;
  gap: var(--cn-space-3);
  padding: var(--cn-space-3);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-control);
  background: var(--cn-color-bg-surface-muted);
}

.adjustment-change-item strong {
  color: var(--cn-color-text-primary);
  font-size: 13px;
}

.adjustment-change-item p {
  margin: var(--cn-space-1) 0 0;
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.adjustment-change-item small {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  white-space: nowrap;
}

.slider-wrap {
  width: 100%;
}

.slider-wrap span {
  color: var(--cn-color-text-secondary);
  font-size: 13px;
}

@media (max-width: 1200px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .main-grid {
    grid-template-columns: 1fr;
  }

  .day-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .filter-wrap {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-item {
    width: 100%;
  }

  .switch-item {
    margin-left: 0;
    justify-content: flex-start;
  }

  .summary-grid,
  .day-grid,
  .adjustment-summary,
  .evidence-profile-stats,
  .coach-briefing-details {
    grid-template-columns: 1fr;
  }

  .adjustment-change-item {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .adjustment-change-item small {
    grid-column: 2;
  }

  .trend-chart {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .job-preparation-step {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .job-preparation-step span {
    grid-column: 2;
  }
}
</style>
