<template>
  <div class="ai-config-page">
    <el-card class="header-card" shadow="never">
      <div class="header-content">
        <div>
          <h2>AI 配置与观测</h2>
          <p>面向 OpenAI 兼容中转站的运行配置、连通性测试与 AI 运行观测，不会在后台落库存储输入的 Key。</p>
        </div>
        <div class="header-actions">
          <el-button :loading="runtimeLoading" @click="loadRuntimeConfig">
            <el-icon><Refresh /></el-icon>
            刷新运行时配置
          </el-button>
          <el-button :loading="metricsLoading" @click="loadRuntimeMetrics">
            <el-icon><Histogram /></el-icon>
            刷新运行观测
          </el-button>
        </div>
      </div>
    </el-card>

    <el-row :gutter="20">
      <el-col :xs="24" :lg="10">
        <el-card class="summary-card" shadow="never" v-loading="runtimeLoading">
          <template #header>
            <div class="card-title">
              <span>当前后台运行配置</span>
              <el-tag :type="runtimeConfig.configured ? 'success' : 'warning'">
                {{ runtimeConfig.configured ? '已可用' : '待完善' }}
              </el-tag>
            </div>
          </template>

          <div class="summary-list">
            <div class="summary-item">
              <span class="label">Provider</span>
              <span class="value">{{ runtimeConfig.provider || 'openai-compatible' }}</span>
            </div>
            <div class="summary-item">
              <span class="label">Base URL</span>
              <span class="value break-all">{{ runtimeConfig.baseUrl || '未配置' }}</span>
            </div>
            <div class="summary-item">
              <span class="label">模型</span>
              <span class="value">{{ runtimeConfig.model || '未配置' }}</span>
            </div>
            <div class="summary-item">
              <span class="label">API Key</span>
              <span class="value">
                {{ runtimeConfig.apiKeyConfigured ? runtimeConfig.apiKeyMasked : '未配置' }}
              </span>
            </div>
            <div class="summary-item">
              <span class="label">RAG 检索</span>
              <span class="value">{{ runtimeConfig.ragEnabled ? '已启用' : '未启用' }}</span>
            </div>
            <div class="summary-item">
              <span class="label">RAG Endpoint</span>
              <span class="value break-all">{{ runtimeConfig.ragEndpoint || '未配置' }}</span>
            </div>
            <div class="summary-item">
              <span class="label">RAG API Key</span>
              <span class="value">
                {{ runtimeConfig.ragApiKeyConfigured ? runtimeConfig.ragApiKeyMasked : '未配置' }}
              </span>
            </div>
            <div class="summary-item">
              <span class="label">RAG 默认 TopK</span>
              <span class="value">{{ runtimeConfig.ragDefaultTopK || '-' }}</span>
            </div>
            <div class="summary-item">
              <span class="label">计价币种</span>
              <span class="value">{{ runtimeConfig.pricingCurrency || 'USD' }}</span>
            </div>
            <div class="summary-item">
              <span class="label">输入单价 / 百万</span>
              <span class="value">
                {{ runtimeConfig.pricingConfigured ? formatCost(runtimeConfig.inputPricePerMillion, runtimeConfig.pricingCurrency) : '未配置' }}
              </span>
            </div>
            <div class="summary-item">
              <span class="label">输出单价 / 百万</span>
              <span class="value">
                {{ runtimeConfig.pricingConfigured ? formatCost(runtimeConfig.outputPricePerMillion, runtimeConfig.pricingCurrency) : '未配置' }}
              </span>
            </div>
            <div class="summary-item">
              <span class="label">观测持久化</span>
              <span class="value">
                {{ runtimeConfig.metricsPersistenceEnabled ? `已启用（${runtimeConfig.metricsPersistenceMode || 'redis'}）` : '仅内存模式' }}
              </span>
            </div>
          </div>

          <el-alert
            :title="runtimeConfig.metricsPersistenceEnabled
              ? '建议把真实中转站密钥放在 application-sec.yml 或环境变量里；当前 AI 运行观测会持久化到 Redis，服务重启后仍可恢复。'
              : '建议把真实中转站密钥放在 application-sec.yml 或环境变量里；当前 AI 运行观测仍为内存模式，服务重启后会清空。'"
            type="info"
            :closable="false"
            show-icon
          />
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="14">
        <el-card class="form-card" shadow="never">
          <template #header>
            <div class="card-title">
              <span>中转站连通性测试</span>
              <el-tag type="primary">OpenAI 兼容</el-tag>
            </div>
          </template>

          <el-form :model="form" label-width="120px">
            <el-form-item label="接入类型">
              <el-input value="OpenAI 兼容中转站" disabled />
            </el-form-item>

            <el-form-item label="Base URL">
              <el-input
                v-model="form.baseUrl"
                placeholder="请输入中转站地址，例如 https://xxx.example.com/v1"
                clearable
              />
            </el-form-item>

            <el-form-item label="API Key">
              <el-input
                v-model="form.apiKey"
                type="password"
                show-password
                placeholder="可留空，留空时优先使用当前后台已配置的 Key"
                clearable
              />
              <div class="form-tip">
                当前后台 Key 状态：
                <span :class="runtimeConfig.apiKeyConfigured ? 'success-text' : 'warning-text'">
                  {{ runtimeConfig.apiKeyConfigured ? runtimeConfig.apiKeyMasked : '未配置' }}
                </span>
              </div>
            </el-form-item>

            <el-form-item label="模型名称">
                <el-input
                  v-model="form.model"
                placeholder="请输入模型名称，例如 gpt-5.4"
                clearable
              />
            </el-form-item>

            <el-form-item>
              <div class="form-actions">
                <el-button @click="resetToRuntimeConfig">
                  <el-icon><RefreshLeft /></el-icon>
                  恢复运行时配置
                </el-button>
                <el-button @click="clearInputApiKey">
                  <el-icon><Delete /></el-icon>
                  清空输入 Key
                </el-button>
                <el-button type="primary" :loading="testing" @click="handleTest">
                  <el-icon><Connection /></el-icon>
                  测试连接
                </el-button>
              </div>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <el-card v-if="testResult" class="result-card" shadow="never">
      <template #header>
        <div class="card-title">
          <span>最近一次测试结果</span>
          <el-tag :type="testResult.available ? 'success' : 'danger'">
            {{ testResult.available ? '可用' : '失败' }}
          </el-tag>
        </div>
      </template>

      <div class="result-grid">
        <div class="result-item">
          <span class="label">结果</span>
          <span class="value">
            {{ testResult.available ? '连通成功' : '连通失败' }}
          </span>
        </div>
        <div class="result-item">
          <span class="label">耗时</span>
          <span class="value">{{ formatLatency(testResult.latencyMs) }}</span>
        </div>
        <div class="result-item">
          <span class="label">Provider</span>
          <span class="value">{{ testResult.provider || 'openai-compatible' }}</span>
        </div>
        <div class="result-item">
          <span class="label">模型</span>
          <span class="value">{{ testResult.model || '-' }}</span>
        </div>
        <div class="result-item full-width">
          <span class="label">Base URL</span>
          <span class="value break-all">{{ testResult.baseUrl || '-' }}</span>
        </div>
        <div class="result-item">
          <span class="label">Key 来源</span>
          <span class="value">
            {{ testResult.usedConfiguredApiKey ? '后台已配置 Key' : '本次输入 Key' }}
          </span>
        </div>
        <div class="result-item">
          <span class="label">Key 脱敏</span>
          <span class="value">{{ testResult.apiKeyMasked || '未使用' }}</span>
        </div>
        <div class="result-item full-width">
          <span class="label">说明</span>
          <span class="value">{{ testResult.message || '-' }}</span>
        </div>
        <div class="result-item full-width" v-if="testResult.preview">
          <span class="label">返回预览</span>
          <span class="value preview">{{ testResult.preview }}</span>
        </div>
      </div>
    </el-card>

    <el-card class="debug-card" shadow="never" v-loading="debugLoading">
      <template #header>
        <div class="card-title">
          <div class="title-group">
            <span>Prompt 在线试跑</span>
            <el-tag v-if="selectedPromptMeta" type="primary">
              {{ selectedPromptMeta.promptId }}
            </el-tag>
            <el-tag v-if="selectedStructuredSchema" type="success">
              已绑定 Structured Schema
            </el-tag>
          </div>
        </div>
      </template>

      <el-row :gutter="20" class="debug-layout">
        <el-col :xs="24" :xl="10">
          <el-form :model="debugForm" label-width="100px">
            <el-form-item label="Prompt">
              <el-select
                v-model="debugForm.promptId"
                filterable
                clearable
                placeholder="请选择要调试的 Prompt"
                style="width: 100%"
              >
                <el-option
                  v-for="item in promptOptions"
                  :key="item.promptId"
                  :label="item.optionLabel"
                  :value="item.promptId"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="变量 JSON">
              <el-input
                v-model="debugForm.variablesJson"
                type="textarea"
                :rows="16"
                placeholder="请输入 JSON 对象，例如 {&quot;title&quot;:&quot;示例&quot;}"
              />
            </el-form-item>

            <el-form-item>
              <div class="form-actions">
                <el-button @click="fillPromptDebugVariables">
                  生成变量样例
                </el-button>
                <el-button :loading="debugLoading" @click="handlePromptDebug(false)">
                  仅渲染 Prompt
                </el-button>
                <el-button type="primary" :loading="debugLoading" @click="handlePromptDebug(true)">
                  试跑模型
                </el-button>
              </div>
            </el-form-item>
          </el-form>

          <el-alert
            v-if="selectedPromptMeta"
            :title="`当前模板变量：${selectedPromptMeta.templateVariables?.length ? selectedPromptMeta.templateVariables.join('、') : '无'}`"
            type="info"
            :closable="false"
            show-icon
          />
        </el-col>

        <el-col :xs="24" :xl="14">
          <el-empty v-if="!debugResult" description="先选择 Prompt，再渲染或试跑模型" />

          <div v-else class="expand-panel">
            <div class="debug-summary-grid">
              <div class="result-item">
                <span class="label">Prompt</span>
                <span class="value">{{ debugResult.promptId || '-' }}</span>
              </div>
              <div class="result-item">
                <span class="label">执行状态</span>
                <span class="value">{{ debugResult.executed ? '已试跑模型' : '仅完成渲染' }}</span>
              </div>
              <div class="result-item">
                <span class="label">模型</span>
                <span class="value">{{ debugResult.modelName || '-' }}</span>
              </div>
              <div class="result-item">
                <span class="label">结构化校验</span>
                <span class="value">
                  <template v-if="!debugResult.structuredOutputBound">未绑定</template>
                  <template v-else-if="debugResult.structuredValid === true">通过</template>
                  <template v-else-if="debugResult.structuredValid === false">失败</template>
                  <template v-else>待执行</template>
                </span>
              </div>
            </div>

            <div>
              <div class="expand-label">规范化变量 JSON</div>
              <pre class="code-block">{{ debugResult.variablesJson || '{}' }}</pre>
            </div>

            <div>
              <div class="expand-label">System Prompt</div>
              <pre class="code-block">{{ debugResult.systemPrompt }}</pre>
            </div>

            <div>
              <div class="expand-label">Rendered User Prompt</div>
              <pre class="code-block">{{ debugResult.renderedUserPrompt }}</pre>
            </div>

            <div v-if="debugResult.structuredOutputBound">
              <div class="expand-label">Structured Schema</div>
              <pre class="code-block">{{ debugResult.schemaJson }}</pre>
            </div>

            <div v-if="debugResult.rawResponse">
              <div class="expand-label">Raw Response</div>
              <pre class="code-block">{{ debugResult.rawResponse }}</pre>
            </div>

            <div v-if="debugResult.parsedResponseJson">
              <div class="expand-label">Parsed Response JSON</div>
              <pre class="code-block">{{ debugResult.parsedResponseJson }}</pre>
            </div>

            <div v-if="debugResult.structuredOutputBound && debugResult.structuredValid === false">
              <el-alert
                :title="`Structured Output 校验失败：${debugResult.structuredValidationReason || '未知原因'}`"
                type="warning"
                :closable="false"
                show-icon
              />
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-card class="debug-card" shadow="never" v-loading="ragDebugLoading">
      <template #header>
        <div class="card-title">
          <div class="title-group">
            <span>LlamaIndex 检索调试</span>
            <el-tag :type="runtimeConfig.ragEnabled ? 'success' : 'warning'">
              {{ runtimeConfig.ragEnabled ? 'RAG 已启用' : 'RAG 未启用' }}
            </el-tag>
            <el-tag v-if="selectedRetrievalProfile" type="warning">
              {{ selectedRetrievalProfile.profileId }}
            </el-tag>
          </div>
        </div>
      </template>

      <el-row :gutter="20" class="debug-layout">
        <el-col :xs="24" :xl="10">
          <el-form :model="ragDebugForm" label-width="110px">
            <el-form-item label="Retrieval Profile">
              <el-select
                v-model="ragDebugForm.profileId"
                filterable
                clearable
                placeholder="可选，不选时走默认检索配置"
                style="width: 100%"
              >
                <el-option
                  v-for="item in retrievalProfileOptions"
                  :key="item.profileId"
                  :label="item.optionLabel"
                  :value="item.profileId"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="Query">
              <el-input
                v-model="ragDebugForm.query"
                type="textarea"
                :rows="6"
                placeholder="请输入要检索的知识库问题"
              />
            </el-form-item>

            <el-form-item label="Scene 覆盖">
              <el-input
                v-model="ragDebugForm.scene"
                clearable
                placeholder="留空时优先使用画像默认 scene"
              />
            </el-form-item>

            <el-form-item label="TopK 覆盖">
              <el-input
                v-model="ragDebugForm.topK"
                clearable
                placeholder="留空时优先使用画像默认 TopK"
              />
            </el-form-item>

            <el-form-item label="Metadata JSON">
              <el-input
                v-model="ragDebugForm.metadataFiltersJson"
                type="textarea"
                :rows="10"
                placeholder="留空时优先使用画像默认 metadataFilters；如需清空过滤条件可输入 {}"
              />
            </el-form-item>

            <el-form-item>
              <div class="form-actions">
                <el-button @click="fillRagDebugDefaults">
                  使用画像默认值
                </el-button>
                <el-button @click="clearRagDebugOverrides">
                  清空覆盖项
                </el-button>
                <el-button type="primary" :loading="ragDebugLoading" @click="handleRagDebug">
                  开始检索
                </el-button>
              </div>
            </el-form-item>
          </el-form>

          <el-alert
            v-if="selectedRetrievalProfile"
            :title="`当前画像默认值：queryId=${selectedRetrievalProfile.queryId || '-'}，scene=${selectedRetrievalProfile.scene || '-'}，topK=${selectedRetrievalProfile.topK || '-'}`"
            type="info"
            :closable="false"
            show-icon
          />
        </el-col>

        <el-col :xs="24" :xl="14">
          <el-empty v-if="!ragDebugResult" description="先输入 query，再执行一次 LlamaIndex 检索调试" />

          <div v-else class="expand-panel">
            <div class="debug-summary-grid">
              <div class="result-item">
                <span class="label">Profile</span>
                <span class="value">{{ ragDebugResult.profileId || '未使用画像' }}</span>
              </div>
              <div class="result-item">
                <span class="label">耗时</span>
                <span class="value">{{ formatLatency(ragDebugResult.latencyMs) }}</span>
              </div>
              <div class="result-item">
                <span class="label">Scene</span>
                <span class="value">{{ ragDebugResult.scene || '-' }}</span>
              </div>
              <div class="result-item">
                <span class="label">TopK</span>
                <span class="value">{{ ragDebugResult.topK || '-' }}</span>
              </div>
              <div class="result-item">
                <span class="label">命中节点数</span>
                <span class="value">{{ formatNumber(ragDebugResult.nodeCount) }}</span>
              </div>
              <div class="result-item">
                <span class="label">Fallback</span>
                <span class="value">{{ ragDebugResult.fallback ? '是' : '否' }}</span>
              </div>
              <div class="result-item full-width">
                <span class="label">RAG Endpoint</span>
                <span class="value break-all">{{ ragDebugResult.endpoint || '未配置' }}</span>
              </div>
              <div class="result-item">
                <span class="label">RAG 开关</span>
                <span class="value">{{ ragDebugResult.ragEnabled ? '已启用' : '未启用' }}</span>
              </div>
              <div class="result-item">
                <span class="label">RAG API Key</span>
                <span class="value">
                  {{ ragDebugResult.apiKeyConfigured ? ragDebugResult.apiKeyMasked : '未配置' }}
                </span>
              </div>
            </div>

            <div>
              <div class="expand-label">Metadata Filters</div>
              <pre class="code-block">{{ ragDebugResult.metadataFiltersJson || '{}' }}</pre>
            </div>

            <div>
              <div class="expand-label">Context Snippet</div>
              <div
                class="code-block highlight-block"
                v-html="renderHighlightedText(ragDebugResult.contextSnippet || '当前没有可展示的上下文片段', ragDebugForm.query)"
              ></div>
            </div>

            <div>
              <div class="expand-label">命中节点</div>
              <el-empty v-if="!ragDebugResult.nodes?.length" description="本次检索未命中文档节点" />
              <el-table v-else :data="ragDebugResult.nodes" stripe>
                <el-table-column type="expand">
                  <template #default="{ row }">
                    <div class="expand-panel">
                      <div>
                        <div class="expand-label">命中片段</div>
                        <div
                          class="code-block highlight-block"
                          v-html="renderHighlightedText(row.bestSnippet || '当前节点没有返回命中片段', row.matchedTerms)"
                        ></div>
                      </div>
                      <div>
                        <div class="expand-label">文本内容</div>
                        <div
                          class="code-block highlight-block"
                          v-html="renderHighlightedText(row.text, row.matchedTerms)"
                        ></div>
                      </div>
                      <div>
                        <div class="expand-label">主命中字段</div>
                        <div v-if="row.bestMatchField" class="tag-list">
                          <el-tag size="small" type="warning">
                            {{ formatMatchField(row.bestMatchField) }}
                          </el-tag>
                        </div>
                        <span v-else class="empty-text">当前节点没有返回主命中字段</span>
                      </div>
                      <div>
                        <div class="expand-label">命中词</div>
                        <div v-if="row.matchedTerms?.length" class="tag-list">
                          <el-tag
                            v-for="term in row.matchedTerms"
                            :key="`${row.id}-${term}`"
                            size="small"
                            type="success"
                          >
                            {{ term }}
                          </el-tag>
                        </div>
                        <span v-else class="empty-text">当前节点没有返回命中词解释</span>
                      </div>
                      <div>
                        <div class="expand-label">Metadata</div>
                        <pre class="code-block">{{ formatJson(row.metadata) }}</pre>
                      </div>
                      <div>
                        <div class="expand-label">打分拆解</div>
                        <div v-if="scoreBreakdownItems(row.scoreBreakdown).length" class="score-breakdown-list">
                          <div
                            v-for="item in scoreBreakdownItems(row.scoreBreakdown)"
                            :key="`${row.id}-${item.key}`"
                            class="score-breakdown-item"
                          >
                            <span class="score-breakdown-label">{{ item.label }}</span>
                            <strong class="score-breakdown-value">{{ formatScore(item.value) }}</strong>
                          </div>
                        </div>
                        <span v-else class="empty-text">当前节点没有返回打分拆解</span>
                      </div>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="id" label="Node ID" min-width="200" />
                <el-table-column label="Score" width="100">
                  <template #default="{ row }">
                    {{ formatScore(row.score) }}
                  </template>
                </el-table-column>
                <el-table-column label="主命中字段" min-width="120">
                  <template #default="{ row }">
                    <el-tag v-if="row.bestMatchField" size="small" type="warning">
                      {{ formatMatchField(row.bestMatchField) }}
                    </el-tag>
                    <span v-else class="empty-text">-</span>
                  </template>
                </el-table-column>
                <el-table-column label="文本预览" min-width="260">
                  <template #default="{ row }">
                    <div
                      class="node-text-preview"
                      v-html="renderHighlightedText(previewText(row.text), row.matchedTerms)"
                    ></div>
                  </template>
                </el-table-column>
                <el-table-column label="命中片段" min-width="260">
                  <template #default="{ row }">
                    <div
                      class="node-text-preview"
                      v-html="renderHighlightedText(row.bestSnippet, row.matchedTerms)"
                    ></div>
                  </template>
                </el-table-column>
                <el-table-column label="命中词" min-width="200">
                  <template #default="{ row }">
                    <div v-if="row.matchedTerms?.length" class="tag-list">
                      <el-tag
                        v-for="term in row.matchedTerms.slice(0, 4)"
                        :key="`${row.id}-${term}`"
                        size="small"
                        type="success"
                      >
                        {{ term }}
                      </el-tag>
                    </div>
                    <span v-else class="empty-text">-</span>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-card class="debug-card" shadow="never" v-loading="ragServiceLoading || ragSampleImporting || ragCustomImporting || ragExporting || ragBatchDeleting">
      <template #header>
        <div class="card-title">
          <div class="title-group">
            <span>RAG 服务管理</span>
            <el-tag v-if="ragServiceHealth" :type="ragServiceHealth.reachable ? 'success' : 'warning'">
              {{ ragServiceHealth.reachable ? '服务可达' : '待排查' }}
            </el-tag>
            <el-tag v-if="ragServiceHealth?.status" type="info">
              {{ ragServiceHealth.status }}
            </el-tag>
          </div>
          <div class="filter-actions">
            <el-button :loading="ragServiceLoading" @click="loadRagServiceHealth">
              <el-icon><Refresh /></el-icon>
              刷新服务状态
            </el-button>
            <el-button :loading="ragServiceLoading" @click="loadRagServiceDocuments">
              <el-icon><Refresh /></el-icon>
              刷新文档列表
            </el-button>
          </div>
        </div>
      </template>

      <el-row :gutter="20" class="debug-layout">
        <el-col :xs="24" :xl="10">
          <el-empty v-if="!ragServiceHealth" description="点击刷新后获取 RAG 服务状态" />

          <div v-else class="expand-panel">
            <div class="debug-summary-grid">
              <div class="result-item">
                <span class="label">RAG 开关</span>
                <span class="value">{{ ragServiceHealth.ragEnabled ? '已启用' : '未启用' }}</span>
              </div>
              <div class="result-item">
                <span class="label">服务可达</span>
                <span class="value">{{ ragServiceHealth.reachable ? '是' : '否' }}</span>
              </div>
              <div class="result-item full-width">
                <span class="label">Endpoint</span>
                <span class="value break-all">{{ ragServiceHealth.endpoint || '未配置' }}</span>
              </div>
              <div class="result-item">
                <span class="label">鉴权模式</span>
                <span class="value">{{ ragServiceHealth.authEnabled ? 'Bearer' : '关闭' }}</span>
              </div>
              <div class="result-item">
                <span class="label">API Key</span>
                <span class="value">
                  {{ ragServiceHealth.apiKeyConfigured ? ragServiceHealth.apiKeyMasked : '未配置' }}
                </span>
              </div>
              <div class="result-item">
                <span class="label">文档数</span>
                <span class="value">{{ formatNumber(ragServiceHealth.documentCount) }}</span>
              </div>
              <div class="result-item">
                <span class="label">场景数</span>
                <span class="value">{{ formatNumber(ragServiceHealth.sceneCount) }}</span>
              </div>
              <div class="result-item">
                <span class="label">耗时</span>
                <span class="value">{{ formatLatency(ragServiceHealth.latencyMs) }}</span>
              </div>
            </div>

            <div>
              <div class="expand-label">数据文件</div>
              <pre class="code-block">{{ ragServiceHealth.dataFile || '未返回数据文件路径' }}</pre>
            </div>

            <el-alert
              :title="ragServiceHealth.message || (ragServiceHealth.reachable ? 'RAG 服务当前可用于后台联调' : 'RAG 服务当前不可达，请检查 sidecar 是否启动')"
              :type="ragServiceHealth.reachable ? 'success' : 'warning'"
              :closable="false"
              show-icon
            />

            <el-form :inline="true" :model="ragServiceFilters" class="catalog-filter-form">
              <el-form-item label="Scene">
                <el-select
                  v-model="ragServiceFilters.scene"
                  clearable
                  placeholder="全部场景"
                  style="width: 220px"
                >
                  <el-option label="全部场景" value="" />
                  <el-option
                    v-for="scene in ragServiceSceneOptions"
                    :key="scene"
                    :label="scene"
                    :value="scene"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="返回条数">
                <el-select v-model="ragServiceFilters.limit" style="width: 140px">
                  <el-option :value="10" label="10 条" />
                  <el-option :value="20" label="20 条" />
                  <el-option :value="50" label="50 条" />
                  <el-option :value="100" label="100 条" />
                </el-select>
              </el-form-item>
              <el-form-item label="关键词">
                <el-input
                  v-model="ragServiceFilters.keyword"
                  clearable
                  placeholder="支持 ID / Scene / 文本 / Metadata"
                  style="width: 260px"
                />
              </el-form-item>
              <el-form-item label="导入策略">
                <el-switch
                  v-model="ragSampleOptions.replace"
                  inline-prompt
                  active-text="覆盖"
                  inactive-text="追加"
                />
              </el-form-item>
              <el-form-item>
                <div class="filter-actions">
                  <el-button type="primary" :loading="ragServiceLoading" @click="loadRagServiceDocuments">
                    查询文档
                  </el-button>
                  <el-button :disabled="ragServiceLoading" @click="resetRagServiceFilters">
                    重置筛选
                  </el-button>
                  <el-button :loading="ragExporting" @click="handleExportRagDocuments">
                    导出当前知识
                  </el-button>
                  <el-button
                    type="danger"
                    plain
                    :disabled="!ragSelectedDocumentIds.length"
                    :loading="ragBatchDeleting"
                    @click="handleBatchDeleteRagDocuments"
                  >
                    批量删除已选
                  </el-button>
                  <el-button type="warning" :loading="ragSampleImporting" @click="handleImportRagSample">
                    导入样例知识
                  </el-button>
                </div>
              </el-form-item>
            </el-form>

            <div>
              <div class="expand-label">自定义文档导入</div>
              <el-form :model="ragImportForm" label-width="96px">
                <el-form-item label="默认 Scene">
                  <el-input
                    v-model="ragImportForm.defaultScene"
                    clearable
                    placeholder="可选，未填写 scene 的文档将回退到这里"
                  />
                </el-form-item>
                <el-form-item label="文档 JSON">
                  <el-input
                    v-model="ragImportForm.documentsJson"
                    type="textarea"
                    :rows="14"
                    placeholder="支持 JSON 数组或 {&quot;documents&quot;:[...]}"
                  />
                </el-form-item>
                <el-form-item>
                  <div class="form-actions">
                    <el-button @click="fillRagImportExample">
                      填充示例
                    </el-button>
                    <el-button type="primary" :loading="ragCustomImporting" @click="handleImportRagDocuments">
                      导入自定义知识
                    </el-button>
                  </div>
                </el-form-item>
              </el-form>

              <el-alert
                :title="`当前导入策略：${ragSampleOptions.replace ? '覆盖现有 sidecar 文档' : '保留现有文档并追加'}；支持 JSON 数组或 documents 包装对象；未传 id 时后台会自动生成稳定 ID。`"
                type="info"
                :closable="false"
                show-icon
              />
            </div>
          </div>
        </el-col>

        <el-col :xs="24" :xl="14">
          <div class="expand-panel">
            <div class="debug-summary-grid">
              <div class="result-item">
                <span class="label">当前 Scene</span>
                <span class="value">{{ ragServiceDocuments.scene || '全部场景' }}</span>
              </div>
              <div class="result-item">
                <span class="label">返回条数</span>
                <span class="value">{{ formatNumber(ragServiceDocuments.totalCount) }}</span>
              </div>
              <div class="result-item">
                <span class="label">当前展示</span>
                <span class="value">{{ formatNumber(filteredRagServiceDocuments.length) }}</span>
              </div>
              <div class="result-item">
                <span class="label">查询上限</span>
                <span class="value">{{ formatNumber(ragServiceDocuments.limit) }}</span>
              </div>
              <div class="result-item">
                <span class="label">耗时</span>
                <span class="value">{{ formatLatency(ragServiceDocuments.latencyMs) }}</span>
              </div>
            </div>

            <el-empty v-if="!filteredRagServiceDocuments.length" description="当前没有可展示的 RAG 文档" />

            <el-table
              v-else
              ref="ragDocumentTableRef"
              :data="filteredRagServiceDocuments"
              stripe
              @selection-change="handleRagDocumentSelectionChange"
            >
              <el-table-column type="selection" width="48" />
              <el-table-column type="expand">
                <template #default="{ row }">
                  <div class="expand-panel">
                    <div>
                      <div class="expand-label">文本预览</div>
                      <div
                        class="code-block highlight-block"
                        v-html="renderHighlightedText(row.textPreview, ragServiceFilters.keyword)"
                      ></div>
                    </div>
                    <div>
                      <div class="expand-label">Metadata</div>
                      <pre class="code-block">{{ formatJson(row.metadata) }}</pre>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="id" label="Document ID" min-width="220" />
              <el-table-column prop="scene" label="Scene" min-width="140" />
              <el-table-column label="文本预览" min-width="260">
                <template #default="{ row }">
                  <div class="node-text-preview" v-html="renderHighlightedText(row.textPreview, [ragServiceFilters.keyword])"></div>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="120" fixed="right">
                <template #default="{ row }">
                  <el-button
                    type="danger"
                    text
                    :loading="ragDeletingDocumentId === row.id"
                    @click="handleDeleteRagDocument(row)"
                  >
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-card class="debug-card" shadow="never" v-loading="regressionCasesLoading">
      <template #header>
        <div class="card-title">
          <div class="title-group">
            <span>AI 黄金样例回归</span>
            <el-tag type="primary">{{ formatNumber(regressionCatalog.totalCount) }} 条用例</el-tag>
            <el-tag type="warning">{{ formatNumber(regressionScenarioOptions.length) }} 个场景</el-tag>
            <el-tag
              v-if="regressionRunResult"
              :type="regressionRunResult.failedCount ? 'danger' : 'success'"
            >
              {{ regressionRunResult.failedCount ? '最近一次存在失败' : '最近一次全通过' }}
            </el-tag>
          </div>
          <div class="filter-actions">
            <el-button :loading="regressionCasesLoading" @click="refreshRegressionPanel">
              <el-icon><Refresh /></el-icon>
              刷新用例
            </el-button>
            <el-button
              type="warning"
              :loading="regressionRunning && regressionRunTarget === `scenario:${regressionFilters.scenario}`"
              :disabled="!regressionFilters.scenario || regressionRunning"
              @click="handleRunRegression({ scenario: regressionFilters.scenario, requireScenario: true })"
            >
              跑当前场景
            </el-button>
            <el-button
              type="primary"
              :loading="regressionRunning && regressionRunTarget === 'all'"
              :disabled="!regressionCatalog.totalCount || regressionRunning"
              @click="handleRunRegression()"
            >
              跑全部
            </el-button>
          </div>
        </div>
      </template>

      <div class="overview-grid">
        <div class="overview-item">
          <strong>{{ formatNumber(regressionCatalog.totalCount) }}</strong>
          <span class="overview-label">黄金样例总数</span>
        </div>
        <div class="overview-item">
          <strong>{{ formatNumber(regressionScenarioOptions.length) }}</strong>
          <span class="overview-label">覆盖场景数</span>
        </div>
        <div class="overview-item">
          <strong>{{ regressionRunResult ? formatNumber(regressionRunResult.passedCount) : '-' }}</strong>
          <span class="overview-label">最近通过数</span>
        </div>
        <div class="overview-item">
          <strong>{{ regressionRunResult ? formatNumber(regressionRunResult.failedCount) : '-' }}</strong>
          <span class="overview-label">最近失败数</span>
        </div>
      </div>

      <el-alert
        v-if="regressionRunResult"
        :title="`最近一次执行：${regressionRunResult.caseId || regressionRunResult.scenario || '全量回归'}，时间 ${formatTime(regressionRunResult.executedAt)}，共 ${regressionRunResult.totalCount || 0} 条，用时 ${formatLatency(regressionRunResult.durationMs)}，通过 ${regressionRunResult.passedCount || 0} 条，失败 ${regressionRunResult.failedCount || 0} 条`"
        :type="regressionRunResult.failedCount ? 'warning' : 'success'"
        :closable="false"
        show-icon
        class="result-alert"
      />

      <el-alert
        title="这里会直接走当前 LangChain4j / LangGraph4j / LlamaIndex 运行链路，可用于上线前检查黄金样例是否稳定通过。"
        type="info"
        :closable="false"
        show-icon
        class="result-alert"
      />

      <div class="section-header">
        <div class="title-group">
          <span class="section-title">最近执行历史</span>
          <el-tag type="info">当前展示 {{ formatNumber(regressionHistory.runs.length) }} 条</el-tag>
        </div>
        <span class="empty-text">支持刷新后直接回看某次回归详情</span>
      </div>

      <el-table
        v-if="regressionHistory.runs.length"
        v-loading="regressionHistoryLoading"
        :data="regressionHistory.runs"
        stripe
        size="small"
      >
        <el-table-column label="执行时间" min-width="180">
          <template #default="{ row }">
            {{ formatTime(row.executedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="执行目标" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            {{ resolveRegressionRunTarget(row) }}
          </template>
        </el-table-column>
        <el-table-column prop="totalCount" label="总数" width="80" />
        <el-table-column prop="passedCount" label="通过" width="80" />
        <el-table-column prop="failedCount" label="失败" width="80" />
        <el-table-column label="耗时" width="100">
          <template #default="{ row }">
            {{ formatLatency(row.durationMs) }}
          </template>
        </el-table-column>
        <el-table-column label="结果" width="100">
          <template #default="{ row }">
            <el-tag :type="row.failedCount ? 'danger' : 'success'" size="small">
              {{ row.failedCount ? '有失败' : '通过' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              text
              :disabled="isActiveRegressionHistory(row)"
              @click="handleViewRegressionHistory(row)"
            >
              {{ isActiveRegressionHistory(row) ? '当前详情' : '查看详情' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty
        v-else-if="!regressionHistoryLoading"
        description="当前还没有可回看的回归历史记录"
      />

      <div class="section-header">
        <div class="title-group">
          <span class="section-title">场景健康聚合</span>
          <el-tag type="warning">聚合最近 {{ formatNumber(regressionScenarioHealth.limit) }} 次回归</el-tag>
        </div>
        <span class="empty-text">优先把最近状态异常的场景排到前面，便于快速定位退化链路</span>
      </div>

      <el-form :inline="true" :model="regressionScenarioFilters" class="catalog-filter-form">
        <el-form-item label="状态">
          <el-select
            v-model="regressionScenarioFilters.status"
            placeholder="请选择状态"
            clearable
            style="width: 140px"
          >
            <el-option label="全部" value="all" />
            <el-option label="仅退化" value="degraded" />
            <el-option label="仅稳定" value="stable" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="regressionScenarioFilters.keyword"
            clearable
            placeholder="支持搜索场景、失败用例、失败原因"
            style="width: 300px"
          />
        </el-form-item>
        <el-form-item>
          <el-tag type="info">
            当前命中 {{ formatNumber(filteredRegressionScenarioHealth.length) }} / {{ formatNumber(regressionScenarioHealth.totalCount) }}
          </el-tag>
        </el-form-item>
      </el-form>

      <el-table
        v-if="filteredRegressionScenarioHealth.length"
        v-loading="regressionScenarioHealthLoading"
        :data="filteredRegressionScenarioHealth"
        stripe
        size="small"
      >
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-panel">
              <div class="debug-summary-grid">
                <div class="result-item">
                  <span class="label">最近运行次数</span>
                  <span class="value">{{ formatNumber(row.runCount) }}</span>
                </div>
                <div class="result-item">
                  <span class="label">失败运行次数</span>
                  <span class="value">{{ formatNumber(row.failedRunCount) }}</span>
                </div>
                <div class="result-item">
                  <span class="label">累计通过用例</span>
                  <span class="value">{{ formatNumber(row.passedCaseCount) }}</span>
                </div>
                <div class="result-item">
                  <span class="label">累计失败用例</span>
                  <span class="value">{{ formatNumber(row.failedCaseCount) }}</span>
                </div>
              </div>
              <div>
                <div class="expand-label">最近一次失败用例</div>
                <div v-if="row.latestFailedCaseIds?.length" class="tag-list">
                  <el-tag
                    v-for="item in row.latestFailedCaseIds"
                    :key="`${row.scenario}-${item}`"
                    size="small"
                    type="danger"
                  >
                    {{ item }}
                  </el-tag>
                </div>
                <span v-else class="empty-text">最近一次运行没有失败用例</span>
              </div>
              <div>
                <div class="expand-label">高频失败用例 Top5</div>
                <div v-if="row.topFailedCases?.length" class="insight-list">
                  <div
                    v-for="item in row.topFailedCases"
                    :key="`${row.scenario}-failed-case-${item.label}`"
                    class="insight-item"
                  >
                    <span class="insight-label">{{ item.label }}</span>
                    <el-tag size="small" type="danger">
                      {{ formatNumber(item.count) }} 次
                    </el-tag>
                  </div>
                </div>
                <span v-else class="empty-text">最近窗口没有失败用例热点</span>
              </div>
              <div>
                <div class="expand-label">高频失败原因 Top5</div>
                <div v-if="row.topFailureReasons?.length" class="insight-list">
                  <div
                    v-for="item in row.topFailureReasons"
                    :key="`${row.scenario}-failure-reason-${item.label}`"
                    class="insight-item insight-item--reason"
                  >
                    <span class="insight-label">{{ item.label }}</span>
                    <el-tag size="small" type="warning">
                      {{ formatNumber(item.count) }} 次
                    </el-tag>
                  </div>
                </div>
                <span v-else class="empty-text">最近窗口没有可聚类的失败原因</span>
              </div>
              <div>
                <div class="expand-label">退化模型 Top5</div>
                <div v-if="row.topModelNames?.length" class="insight-list">
                  <div
                    v-for="item in row.topModelNames"
                    :key="`${row.scenario}-model-${item.label}`"
                    class="insight-item"
                  >
                    <span class="insight-label">{{ item.label }}</span>
                    <el-tag size="small" type="primary">
                      {{ formatNumber(item.count) }} 次
                    </el-tag>
                  </div>
                </div>
                <span v-else class="empty-text">最近窗口没有模型维度的失败热点</span>
              </div>
              <div>
                <div class="expand-label">高影响图编排 Top5</div>
                <div v-if="row.topGraphNames?.length" class="insight-list">
                  <div
                    v-for="item in row.topGraphNames"
                    :key="`${row.scenario}-graph-${item.label}`"
                    class="insight-item"
                  >
                    <span class="insight-label">{{ item.label }}</span>
                    <el-tag size="small" type="success">
                      {{ formatNumber(item.count) }} 次
                    </el-tag>
                  </div>
                </div>
                <span v-else class="empty-text">最近窗口没有图编排维度的失败热点</span>
              </div>
              <div>
                <div class="expand-label">高影响 Prompt Top5</div>
                <div v-if="row.topPromptIds?.length" class="insight-list">
                  <div
                    v-for="item in row.topPromptIds"
                    :key="`${row.scenario}-prompt-${item.label}`"
                    class="insight-item insight-item--prompt"
                  >
                    <span class="insight-label">{{ item.label }}</span>
                    <el-tag size="small" type="info">
                      {{ formatNumber(item.count) }} 次
                    </el-tag>
                  </div>
                </div>
                <span v-else class="empty-text">最近窗口没有 Prompt 维度的失败热点</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="scenario" label="Scene" min-width="180" />
        <el-table-column label="最近状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.latestPassed ? 'success' : 'danger'" size="small">
              {{ row.latestPassed ? '稳定' : '退化' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="窗口通过率" width="120">
          <template #default="{ row }">
            {{ formatRate(row.passedCaseCount, row.totalCaseCount) }}
          </template>
        </el-table-column>
        <el-table-column label="失败运行" width="100">
          <template #default="{ row }">
            {{ formatNumber(row.failedRunCount) }}
          </template>
        </el-table-column>
        <el-table-column label="最近失败时间" min-width="180">
          <template #default="{ row }">
            {{ formatTime(row.lastFailedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="最近失败用例" width="120">
          <template #default="{ row }">
            {{ formatNumber(row.latestFailedCaseCount) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              text
              @click="handleViewRegressionScenario(row)"
            >
              查看最近结果
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty
        v-else-if="!regressionScenarioHealthLoading && regressionScenarioHealth.scenarios.length"
        description="当前筛选条件下没有命中的场景健康数据"
      />

      <el-empty
        v-else-if="!regressionScenarioHealthLoading"
        description="当前还没有可聚合的场景健康数据"
      />

      <el-form :inline="true" :model="regressionFilters" class="catalog-filter-form">
        <el-form-item label="Scene">
          <el-select
            v-model="regressionFilters.scenario"
            clearable
            placeholder="全部场景"
            style="width: 220px"
          >
            <el-option label="全部场景" value="" />
            <el-option
              v-for="scene in regressionScenarioOptions"
              :key="scene"
              :label="scene"
              :value="scene"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="regressionFilters.keyword"
            clearable
            placeholder="支持 caseId / scene / 描述 / 输入字段搜索"
            style="width: 320px"
          />
        </el-form-item>
        <el-form-item>
          <div class="filter-actions">
            <el-button :disabled="regressionRunning" @click="resetRegressionFilters">
              重置筛选
            </el-button>
          </div>
        </el-form-item>
      </el-form>

      <el-empty
        v-if="!filteredRegressionCases.length"
        :description="regressionCatalog.totalCount ? '当前筛选条件下没有回归用例' : '当前还没有可执行的黄金样例回归用例'"
      />

      <el-table v-else :data="filteredRegressionCases" stripe>
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-panel">
              <div>
                <div class="expand-label">输入字段</div>
                <div v-if="row.inputKeys?.length" class="tag-list">
                  <el-tag
                    v-for="item in row.inputKeys"
                    :key="`${row.caseId}-${item}`"
                    size="small"
                    type="info"
                  >
                    {{ item }}
                  </el-tag>
                </div>
                <span v-else class="empty-text">当前用例没有声明输入字段</span>
              </div>

              <div>
                <div class="expand-label">最近失败原因</div>
                <ul
                  v-if="regressionResultMap[row.caseId] && regressionResultMap[row.caseId].failureReasons?.length"
                  class="failure-list"
                >
                  <li
                    v-for="(reason, index) in regressionResultMap[row.caseId].failureReasons"
                    :key="`${row.caseId}-failure-${index}`"
                  >
                    {{ reason }}
                  </li>
                </ul>
                <span v-else class="empty-text">
                  {{ regressionResultMap[row.caseId] ? '最近一次执行没有失败原因' : '当前用例还没有执行结果' }}
                </span>
              </div>

              <div>
                <div class="expand-label">执行链路</div>
                <div v-if="regressionResultMap[row.caseId]" class="expand-panel">
                  <div class="tag-list">
                    <el-tag v-if="regressionResultMap[row.caseId].modelName" size="small" type="primary">
                      模型：{{ regressionResultMap[row.caseId].modelName }}
                    </el-tag>
                    <el-tag v-if="regressionResultMap[row.caseId].graphName" size="small" type="success">
                      图编排：{{ regressionResultMap[row.caseId].graphName }}
                    </el-tag>
                  </div>
                  <div v-if="regressionResultMap[row.caseId].promptIds?.length" class="tag-list">
                    <el-tag
                      v-for="item in regressionResultMap[row.caseId].promptIds"
                      :key="`${row.caseId}-prompt-${item}`"
                      size="small"
                      type="info"
                    >
                      {{ item }}
                    </el-tag>
                  </div>
                  <span v-else class="empty-text">当前用例未记录 Prompt 元信息</span>
                </div>
                <span v-else class="empty-text">当前用例还没有执行结果</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="caseId" label="Case ID" min-width="220" />
        <el-table-column label="Scene" min-width="150">
          <template #default="{ row }">
            <el-tag size="small" type="warning">
              {{ row.scenario || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="280" show-overflow-tooltip />
        <el-table-column label="预期降级" width="110">
          <template #default="{ row }">
            <el-tag :type="row.expectedFallback ? 'warning' : 'info'" size="small">
              {{ row.expectedFallback ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="输入字段" min-width="220">
          <template #default="{ row }">
            <div v-if="row.inputKeys?.length" class="tag-list">
              <el-tag
                v-for="item in row.inputKeys.slice(0, 3)"
                :key="`${row.caseId}-input-${item}`"
                size="small"
                type="info"
              >
                {{ item }}
              </el-tag>
              <span v-if="row.inputKeys.length > 3" class="empty-text">
                +{{ row.inputKeys.length - 3 }}
              </span>
            </div>
            <span v-else class="empty-text">-</span>
          </template>
        </el-table-column>
        <el-table-column label="最近结果" min-width="240">
          <template #default="{ row }">
            <div v-if="regressionResultMap[row.caseId]" class="regression-result-cell">
              <div class="tag-list">
                <el-tag
                  :type="regressionResultMap[row.caseId].passed ? 'success' : 'danger'"
                  size="small"
                >
                  {{ regressionResultMap[row.caseId].passed ? '通过' : '失败' }}
                </el-tag>
                <el-tag
                  :type="regressionResultMap[row.caseId].actualFallback ? 'warning' : 'info'"
                  size="small"
                >
                  实际降级：{{ resolveRegressionFallbackText(regressionResultMap[row.caseId].actualFallback) }}
                </el-tag>
              </div>
              <span class="empty-text">耗时 {{ formatLatency(regressionResultMap[row.caseId].durationMs) }}</span>
              <span
                v-if="!regressionResultMap[row.caseId].passed && regressionResultMap[row.caseId].failureReasons?.length"
                class="muted-text"
              >
                {{ previewText(regressionResultMap[row.caseId].failureReasons.join('；'), 72) }}
              </span>
              <span
                v-if="regressionResultMap[row.caseId].modelName || regressionResultMap[row.caseId].graphName"
                class="muted-text"
              >
                {{ previewText([regressionResultMap[row.caseId].modelName, regressionResultMap[row.caseId].graphName].filter((item) => item).join(' / '), 72) }}
              </span>
            </div>
            <span v-else class="empty-text">尚未执行</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              text
              :loading="regressionRunning && regressionRunTarget === `case:${row.caseId}`"
              :disabled="regressionRunning"
              @click="handleRunRegressionCase(row)"
            >
              运行
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="catalog-card" shadow="never" v-loading="catalogLoading">
      <template #header>
        <div class="card-title">
          <div class="title-group">
            <span>Prompt / RAG / Schema 调试</span>
            <el-tag type="primary">{{ schemaCatalog.prompts.length }} Prompt</el-tag>
            <el-tag type="success">{{ schemaCatalog.ragQueries.length }} Query</el-tag>
            <el-tag type="warning">{{ schemaCatalog.retrievalProfiles.length }} Profile</el-tag>
            <el-tag type="info">{{ schemaCatalog.structuredSchemas.length }} Schema</el-tag>
          </div>
          <el-button :loading="catalogLoading" @click="loadSchemaCatalog">
            <el-icon><Refresh /></el-icon>
            刷新调试清单
          </el-button>
        </div>
      </template>

      <el-form :inline="true" :model="catalogFilters" class="catalog-filter-form">
        <el-form-item label="业务域">
          <el-select v-model="catalogFilters.domain" placeholder="全部业务域" style="width: 220px">
            <el-option label="全部业务域" value="all" />
            <el-option
              v-for="domain in schemaCatalog.domains"
              :key="domain"
              :label="formatDomainLabel(domain)"
              :value="domain"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="catalogFilters.keyword"
            clearable
            placeholder="支持 key / version / scene / 变量 / schema 搜索"
          />
        </el-form-item>
        <el-form-item>
          <div class="filter-actions">
            <el-button @click="resetCatalogFilters">
              重置筛选
            </el-button>
          </div>
        </el-form-item>
      </el-form>

      <el-tabs v-model="catalogFilters.activeTab" class="catalog-tabs">
        <el-tab-pane :label="`Prompt (${filteredPrompts.length})`" name="prompts">
          <el-empty v-if="!filteredPrompts.length" description="当前筛选条件下没有 Prompt" />
          <el-table v-else :data="filteredPrompts" stripe>
            <el-table-column type="expand">
              <template #default="{ row }">
                <div class="expand-panel">
                  <div>
                    <div class="expand-label">System Prompt</div>
                    <pre class="code-block">{{ row.systemPrompt }}</pre>
                  </div>
                  <div>
                    <div class="expand-label">User Template</div>
                    <pre class="code-block">{{ row.userTemplate }}</pre>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="业务域" width="150">
              <template #default="{ row }">
                <el-tag size="small">{{ formatDomainLabel(row.domain, false) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="key" label="Prompt Key" min-width="220" />
            <el-table-column prop="version" label="版本" width="90" />
            <el-table-column prop="promptId" label="Prompt ID" min-width="220" />
            <el-table-column label="模板变量" min-width="240">
              <template #default="{ row }">
                <div class="tag-list">
                  <el-tag v-for="item in row.templateVariables" :key="item" size="small" type="info">
                    {{ item }}
                  </el-tag>
                  <span v-if="!row.templateVariables?.length" class="empty-text">无变量</span>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="`RAG Query (${filteredRagQueries.length})`" name="ragQueries">
          <el-empty v-if="!filteredRagQueries.length" description="当前筛选条件下没有 RAG Query" />
          <el-table v-else :data="filteredRagQueries" stripe>
            <el-table-column type="expand">
              <template #default="{ row }">
                <div class="expand-panel">
                  <div>
                    <div class="expand-label">Query Template</div>
                    <pre class="code-block">{{ row.template }}</pre>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="业务域" width="150">
              <template #default="{ row }">
                <el-tag size="small" type="success">{{ formatDomainLabel(row.domain, false) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="key" label="Query Key" min-width="240" />
            <el-table-column prop="version" label="版本" width="90" />
            <el-table-column prop="queryId" label="Query ID" min-width="260" />
            <el-table-column label="模板变量" min-width="240">
              <template #default="{ row }">
                <div class="tag-list">
                  <el-tag v-for="item in row.templateVariables" :key="item" size="small" type="info">
                    {{ item }}
                  </el-tag>
                  <span v-if="!row.templateVariables?.length" class="empty-text">无变量</span>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="`Retrieval Profile (${filteredRetrievalProfiles.length})`" name="retrievalProfiles">
          <el-empty v-if="!filteredRetrievalProfiles.length" description="当前筛选条件下没有 Retrieval Profile" />
          <el-table v-else :data="filteredRetrievalProfiles" stripe>
            <el-table-column type="expand">
              <template #default="{ row }">
                <div class="expand-panel">
                  <div>
                    <div class="expand-label">Metadata Filters</div>
                    <pre class="code-block">{{ formatJson(row.metadataFilters) }}</pre>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="业务域" width="150">
              <template #default="{ row }">
                <el-tag size="small" type="warning">{{ formatDomainLabel(row.domain, false) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="queryKey" label="Query Key" min-width="240" />
            <el-table-column prop="scene" label="Scene" min-width="160" />
            <el-table-column prop="topK" label="TopK" width="90" />
            <el-table-column prop="profileId" label="Profile ID" min-width="260" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="`Structured Schema (${filteredStructuredSchemas.length})`" name="structuredSchemas">
          <el-empty v-if="!filteredStructuredSchemas.length" description="当前筛选条件下没有 Structured Schema" />
          <el-table v-else :data="filteredStructuredSchemas" stripe>
            <el-table-column type="expand">
              <template #default="{ row }">
                <div class="expand-panel">
                  <div>
                    <div class="expand-label">JSON Schema</div>
                    <pre class="code-block">{{ row.schemaJson }}</pre>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="业务域" width="150">
              <template #default="{ row }">
                <el-tag size="small" type="info">{{ formatDomainLabel(row.domain, false) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="promptKey" label="Prompt Key" min-width="220" />
            <el-table-column prop="promptVersion" label="版本" width="90" />
            <el-table-column prop="rootType" label="Root Type" width="120" />
            <el-table-column prop="schemaFileName" label="Schema File" min-width="220" />
            <el-table-column prop="schemaId" label="Schema ID" min-width="300" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-card class="metrics-card" shadow="never">
      <template #header>
        <div class="card-title">
          <span>运行观测筛选</span>
          <el-tag type="primary">scene / outcome / model / recent</el-tag>
        </div>
      </template>

      <el-form :inline="true" :model="metricsFilters" class="metrics-filter-form">
        <el-form-item label="场景关键词">
          <el-input
            v-model="metricsFilters.scene"
            clearable
            placeholder="支持 scene / prompt 模糊匹配"
            @keyup.enter="handleSearchMetrics"
          />
        </el-form-item>
        <el-form-item label="模型关键词">
          <el-input
            v-model="metricsFilters.model"
            clearable
            placeholder="支持 model 模糊匹配，如 gpt-5.4"
            @keyup.enter="handleSearchMetrics"
          />
        </el-form-item>
        <el-form-item label="调用结果">
          <el-select v-model="metricsFilters.outcome" clearable placeholder="全部结果">
            <el-option label="全部结果" value="" />
            <el-option label="成功" value="success" />
            <el-option label="错误" value="error" />
          </el-select>
        </el-form-item>
        <el-form-item label="最近调用条数">
          <el-select v-model="metricsFilters.recentLimit">
            <el-option :value="10" label="10 条" />
            <el-option :value="20" label="20 条" />
            <el-option :value="50" label="50 条" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <div class="filter-actions">
            <el-button type="primary" :loading="metricsLoading" @click="handleSearchMetrics">
              查询观测
            </el-button>
            <el-button :disabled="metricsLoading" @click="resetMetricsFilters">
              重置筛选
            </el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="metrics-card" shadow="never" v-loading="metricsLoading">
      <template #header>
        <div class="card-title">
          <div class="title-group">
            <span>AI 运行观测总览</span>
            <el-tag :type="runtimeMetrics.overview.totalInvocations > 0 ? 'success' : 'info'">
              {{ runtimeMetrics.overview.totalInvocations > 0 ? '已有调用数据' : '暂无调用数据' }}
            </el-tag>
          </div>
          <el-button
            type="danger"
            plain
            size="small"
            :disabled="!hasMetricsData || metricsLoading"
            @click="handleClearMetrics"
          >
            <el-icon><Delete /></el-icon>
            清空观测
          </el-button>
        </div>
      </template>

      <div class="overview-grid">
        <div class="overview-item">
          <span class="overview-label">总调用次数</span>
          <strong>{{ formatNumber(runtimeMetrics.overview.totalInvocations) }}</strong>
        </div>
        <div class="overview-item">
          <span class="overview-label">成功率</span>
          <strong>{{ formatRate(runtimeMetrics.overview.successCount, runtimeMetrics.overview.totalInvocations) }}</strong>
        </div>
        <div class="overview-item">
          <span class="overview-label">累计 Token</span>
          <strong>{{ formatNumber(runtimeMetrics.overview.totalTokens) }}</strong>
        </div>
        <div class="overview-item">
          <span class="overview-label">估算成本</span>
          <strong>{{ formatCost(runtimeMetrics.overview.estimatedCost, runtimeMetrics.overview.currency) }}</strong>
        </div>
        <div class="overview-item">
          <span class="overview-label">平均耗时</span>
          <strong>{{ formatLatency(runtimeMetrics.overview.averageLatencyMs) }}</strong>
        </div>
        <div class="overview-item">
          <span class="overview-label">Fallback 次数</span>
          <strong>{{ formatNumber(runtimeMetrics.overview.fallbackCount) }}</strong>
        </div>
        <div class="overview-item">
          <span class="overview-label">解析失败次数</span>
          <strong>{{ formatNumber(runtimeMetrics.overview.structuredParseFailureCount) }}</strong>
        </div>
        <div class="overview-item">
          <span class="overview-label">最近调用</span>
          <strong>{{ formatTime(runtimeMetrics.overview.lastInvocationAt) }}</strong>
        </div>
      </div>
    </el-card>

    <el-card class="metrics-card" shadow="never">
      <template #header>
        <div class="card-title">
          <span>分场景聚合</span>
          <el-tag type="primary">{{ runtimeMetrics.sceneStats.length }} 个场景</el-tag>
        </div>
      </template>

      <el-empty v-if="!runtimeMetrics.sceneStats.length" description="当前还没有 AI 调用数据" />
      <el-table v-else :data="runtimeMetrics.sceneStats" stripe>
        <el-table-column prop="scene" label="Scene" min-width="220" />
        <el-table-column prop="promptKey" label="Prompt Key" min-width="180" />
        <el-table-column prop="promptVersion" label="版本" width="90" />
        <el-table-column prop="lastModelName" label="最近模型" min-width="120" />
        <el-table-column prop="invocations" label="调用" width="90" />
        <el-table-column prop="successCount" label="成功" width="90" />
        <el-table-column prop="errorCount" label="错误" width="90" />
        <el-table-column prop="fallbackCount" label="Fallback" width="100" />
        <el-table-column prop="structuredParseFailureCount" label="解析失败" width="100" />
        <el-table-column label="累计 Token" width="120">
          <template #default="{ row }">
            {{ formatNumber(row.totalTokens) }}
          </template>
        </el-table-column>
        <el-table-column label="平均耗时" width="120">
          <template #default="{ row }">
            {{ formatLatency(row.averageLatencyMs) }}
          </template>
        </el-table-column>
        <el-table-column label="估算成本" min-width="140">
          <template #default="{ row }">
            {{ formatCost(row.estimatedCost, runtimeMetrics.overview.currency) }}
          </template>
        </el-table-column>
        <el-table-column label="最近调用时间" min-width="170">
          <template #default="{ row }">
            {{ formatTime(row.lastInvocationAt) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="metrics-card" shadow="never">
      <template #header>
        <div class="card-title">
          <span>分模型聚合</span>
          <el-tag type="success">{{ runtimeMetrics.modelStats.length }} 个模型</el-tag>
        </div>
      </template>

      <el-empty v-if="!runtimeMetrics.modelStats.length" description="当前还没有模型维度调用数据" />
      <el-table v-else :data="runtimeMetrics.modelStats" stripe>
        <el-table-column prop="modelName" label="模型" min-width="180" />
        <el-table-column prop="invocations" label="调用" width="90" />
        <el-table-column prop="successCount" label="成功" width="90" />
        <el-table-column prop="errorCount" label="错误" width="90" />
        <el-table-column label="成功率" width="110">
          <template #default="{ row }">
            {{ formatRate(row.successCount, row.invocations) }}
          </template>
        </el-table-column>
        <el-table-column label="累计 Token" width="120">
          <template #default="{ row }">
            {{ formatNumber(row.totalTokens) }}
          </template>
        </el-table-column>
        <el-table-column label="平均耗时" width="120">
          <template #default="{ row }">
            {{ formatLatency(row.averageLatencyMs) }}
          </template>
        </el-table-column>
        <el-table-column label="估算成本" min-width="140">
          <template #default="{ row }">
            {{ formatCost(row.estimatedCost, runtimeMetrics.overview.currency) }}
          </template>
        </el-table-column>
        <el-table-column label="最近调用时间" min-width="170">
          <template #default="{ row }">
            {{ formatTime(row.lastInvocationAt) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="metrics-card" shadow="never">
      <template #header>
        <div class="card-title">
          <span>最近调用</span>
          <el-tag type="info">最多展示 50 条</el-tag>
        </div>
      </template>

      <el-empty v-if="!runtimeMetrics.recentCalls.length" description="最近暂无 AI 调用记录" />
      <el-table v-else :data="runtimeMetrics.recentCalls" stripe>
        <el-table-column label="时间" min-width="170">
          <template #default="{ row }">
            {{ formatTime(row.timestamp) }}
          </template>
        </el-table-column>
        <el-table-column prop="scene" label="Scene" min-width="220" />
        <el-table-column prop="modelName" label="模型" min-width="120" />
        <el-table-column label="结果" width="100">
          <template #default="{ row }">
            <el-tag :type="outcomeTagType(row.outcome)" size="small">
              {{ row.outcome }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="110">
          <template #default="{ row }">
            {{ formatLatency(row.latencyMs) }}
          </template>
        </el-table-column>
        <el-table-column label="输入 Token" width="110">
          <template #default="{ row }">
            {{ formatNumber(row.inputTokens) }}
          </template>
        </el-table-column>
        <el-table-column label="输出 Token" width="110">
          <template #default="{ row }">
            {{ formatNumber(row.outputTokens) }}
          </template>
        </el-table-column>
        <el-table-column label="总 Token" width="110">
          <template #default="{ row }">
            {{ formatNumber(row.totalTokens) }}
          </template>
        </el-table-column>
        <el-table-column label="估算成本" min-width="140">
          <template #default="{ row }">
            {{ formatCost(row.estimatedCost, runtimeMetrics.overview.currency) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Connection, Delete, Histogram, Refresh, RefreshLeft } from '@element-plus/icons-vue'
import {
  batchDeleteAiRagDocuments,
  clearAiRuntimeMetrics,
  deleteAiRagDocument,
  debugAiPrompt,
  debugAiRag,
  exportAiRagDocuments,
  getAiRegressionCases,
  getAiRegressionHistory,
  getAiRegressionLatestRun,
  getAiRegressionScenarioHealth,
  getAiRagServiceDocuments,
  getAiRagServiceHealth,
  getAiRuntimeConfig,
  getAiRuntimeMetrics,
  getAiSchemaCatalog,
  importAiRagDocuments,
  importAiRagSampleDocuments,
  runAiRegression,
  testAiConfig
} from '@/api/aiConfig'

const runtimeLoading = ref(false)
const catalogLoading = ref(false)
const regressionCasesLoading = ref(false)
const regressionHistoryLoading = ref(false)
const regressionScenarioHealthLoading = ref(false)
const regressionRunning = ref(false)
const regressionRunTarget = ref('')
const debugLoading = ref(false)
const ragDebugLoading = ref(false)
const ragServiceLoading = ref(false)
const ragSampleImporting = ref(false)
const ragCustomImporting = ref(false)
const ragExporting = ref(false)
const ragBatchDeleting = ref(false)
const ragDeletingDocumentId = ref('')
const metricsLoading = ref(false)
const testing = ref(false)
const testResult = ref(null)
const debugResult = ref(null)
const ragDebugResult = ref(null)
const ragServiceHealth = ref(null)
const regressionRunResult = ref(null)
const ragDocumentTableRef = ref(null)
const ragSelectedDocumentIds = ref([])

const DOMAIN_LABEL_MAP = Object.freeze({
  community: '社区',
  mock_interview: '模拟面试',
  job_battle: '求职作战台',
  sql_optimize: 'SQL 优化'
})

const runtimeConfig = reactive({
  provider: 'openai-compatible',
  baseUrl: '',
  model: 'gpt-5.4',
  configured: false,
  apiKeyConfigured: false,
  apiKeyMasked: '',
  pricingConfigured: false,
  pricingCurrency: 'USD',
  inputPricePerMillion: 0,
  outputPricePerMillion: 0,
  metricsPersistenceEnabled: false,
  metricsPersistenceMode: 'memory',
  ragEnabled: false,
  ragEndpoint: '',
  ragApiKeyConfigured: false,
  ragApiKeyMasked: '',
  ragDefaultTopK: 0
})

const form = reactive({
  baseUrl: '',
  apiKey: '',
  model: 'gpt-5.4'
})

const debugForm = reactive({
  promptId: '',
  variablesJson: '{}'
})

const ragDebugForm = reactive({
  profileId: '',
  query: '',
  scene: '',
  topK: '',
  metadataFiltersJson: ''
})

const ragImportForm = reactive({
  defaultScene: '',
  documentsJson: ''
})

const schemaCatalog = reactive({
  domains: [],
  prompts: [],
  ragQueries: [],
  retrievalProfiles: [],
  structuredSchemas: []
})

const regressionCatalog = reactive({
  totalCount: 0,
  scenarios: [],
  cases: []
})

const regressionHistory = reactive({
  limit: 10,
  totalCount: 0,
  runs: []
})

const regressionScenarioHealth = reactive({
  limit: 10,
  totalCount: 0,
  scenarios: []
})

const catalogFilters = reactive({
  domain: 'all',
  keyword: '',
  activeTab: 'prompts'
})

const regressionFilters = reactive({
  scenario: '',
  keyword: ''
})

const regressionScenarioFilters = reactive({
  status: 'all',
  keyword: ''
})

const metricsFilters = reactive({
  scene: '',
  model: '',
  outcome: '',
  recentLimit: 50
})

const ragServiceFilters = reactive({
  scene: '',
  limit: 20,
  keyword: ''
})

const ragSampleOptions = reactive({
  replace: true
})

const createEmptyOverview = () => ({
  totalInvocations: 0,
  successCount: 0,
  errorCount: 0,
  fallbackCount: 0,
  structuredParseFailureCount: 0,
  totalInputTokens: 0,
  totalOutputTokens: 0,
  totalTokens: 0,
  estimatedCost: 0,
  averageLatencyMs: 0,
  lastInvocationAt: null,
  currency: 'USD',
  pricingConfigured: false
})

const runtimeMetrics = reactive({
  overview: createEmptyOverview(),
  sceneStats: [],
  modelStats: [],
  recentCalls: []
})

const ragServiceDocuments = reactive({
  endpoint: '',
  scene: '',
  limit: 20,
  totalCount: 0,
  latencyMs: null,
  documents: []
})

const hasMetricsData = computed(() => {
  return runtimeMetrics.overview.totalInvocations > 0
    || runtimeMetrics.sceneStats.length > 0
    || runtimeMetrics.modelStats.length > 0
    || runtimeMetrics.recentCalls.length > 0
})

const promptOptions = computed(() => {
  return [...(schemaCatalog.prompts || [])]
    .sort((left, right) => left.promptId.localeCompare(right.promptId))
    .map((item) => ({
      ...item,
      optionLabel: `${formatDomainLabel(item.domain, false)} / ${item.promptId}`
    }))
})

const selectedPromptMeta = computed(() => {
  return (schemaCatalog.prompts || []).find((item) => item.promptId === debugForm.promptId) || null
})

const selectedStructuredSchema = computed(() => {
  return (schemaCatalog.structuredSchemas || []).find((item) => item.specId === debugForm.promptId) || null
})

const retrievalProfileOptions = computed(() => {
  return [...(schemaCatalog.retrievalProfiles || [])]
    .sort((left, right) => left.profileId.localeCompare(right.profileId))
    .map((item) => ({
      ...item,
      optionLabel: `${formatDomainLabel(item.domain, false)} / ${item.profileId}`
    }))
})

const selectedRetrievalProfile = computed(() => {
  return (schemaCatalog.retrievalProfiles || []).find((item) => item.profileId === ragDebugForm.profileId) || null
})

const ragServiceSceneOptions = computed(() => {
  return [...new Set((schemaCatalog.retrievalProfiles || [])
    .map((item) => item.scene)
    .filter((item) => item))]
})

const regressionScenarioOptions = computed(() => {
  return [...new Set([
    ...(regressionCatalog.scenarios || []),
    ...(regressionCatalog.cases || [])
      .map((item) => item?.scenario)
      .filter((item) => item)
  ])]
    .sort((left, right) => left.localeCompare(right))
})

const filteredRegressionCases = computed(() => {
  const keyword = regressionFilters.keyword?.trim().toLowerCase()
  return [...(regressionCatalog.cases || [])]
    .filter((item) => {
      if (regressionFilters.scenario && item.scenario !== regressionFilters.scenario) {
        return false
      }
      if (!keyword) {
        return true
      }
      return flattenSearchFields([
        item.caseId,
        item.scenario,
        item.description,
        item.inputKeys,
        item.expectedFallback ? 'fallback' : 'normal'
      ]).some((field) => field.toLowerCase().includes(keyword))
    })
    .sort((left, right) => `${left.scenario || ''}/${left.caseId || ''}`.localeCompare(`${right.scenario || ''}/${right.caseId || ''}`))
})

const regressionResultMap = computed(() => {
  return (regressionRunResult.value?.caseResults || []).reduce((accumulator, item) => {
    if (item?.caseId) {
      accumulator[item.caseId] = item
    }
    return accumulator
  }, {})
})

const filteredRegressionScenarioHealth = computed(() => {
  const keyword = regressionScenarioFilters.keyword?.trim().toLowerCase()
  return [...(regressionScenarioHealth.scenarios || [])].filter((item) => {
    if (regressionScenarioFilters.status === 'degraded' && item.latestPassed !== false) {
      return false
    }
    if (regressionScenarioFilters.status === 'stable' && item.latestPassed !== true) {
      return false
    }
    if (!keyword) {
      return true
    }
    return flattenSearchFields([
      item.scenario,
      item.latestFailedCaseIds,
      (item.topFailedCases || []).map((entry) => entry?.label),
      (item.topFailureReasons || []).map((entry) => entry?.label),
      (item.topModelNames || []).map((entry) => entry?.label),
      (item.topGraphNames || []).map((entry) => entry?.label),
      (item.topPromptIds || []).map((entry) => entry?.label)
    ]).some((field) => field.toLowerCase().includes(keyword))
  })
})

const filteredRagServiceDocuments = computed(() => {
  const keyword = ragServiceFilters.keyword?.trim().toLowerCase()
  const documents = ragServiceDocuments.documents || []
  if (!keyword) {
    return documents
  }
  return documents.filter((item) => {
    return flattenSearchFields([
      item.id,
      item.scene,
      item.text,
      item.textPreview,
      item.metadata
    ]).some((field) => field.toLowerCase().includes(keyword))
  })
})

const filteredPrompts = computed(() => filterCatalogItems(schemaCatalog.prompts, (item) => [
  item.domain,
  item.key,
  item.version,
  item.promptId,
  item.systemPrompt,
  item.userTemplate,
  item.templateVariables
]))

const filteredRagQueries = computed(() => filterCatalogItems(schemaCatalog.ragQueries, (item) => [
  item.domain,
  item.key,
  item.version,
  item.queryId,
  item.template,
  item.templateVariables
]))

const filteredRetrievalProfiles = computed(() => filterCatalogItems(schemaCatalog.retrievalProfiles, (item) => [
  item.domain,
  item.queryKey,
  item.queryVersion,
  item.queryId,
  item.profileId,
  item.scene,
  item.topK,
  item.metadataFilters
]))

const filteredStructuredSchemas = computed(() => filterCatalogItems(schemaCatalog.structuredSchemas, (item) => [
  item.domain,
  item.specId,
  item.promptKey,
  item.promptVersion,
  item.rootType,
  item.schemaFileName,
  item.schemaId,
  item.schemaJson
]))

onMounted(() => {
  loadRuntimeConfig()
  loadSchemaCatalog()
  loadRegressionCases()
  loadLatestRegressionRun({ silent: true })
  loadRegressionHistory({ silent: true })
  loadRegressionScenarioHealth({ silent: true })
  loadRuntimeMetrics()
  loadRagServiceHealth()
  loadRagServiceDocuments()
})

watch(
  () => ragServiceFilters.keyword,
  () => {
    resetRagDocumentSelection()
  }
)

const loadRuntimeConfig = async () => {
  runtimeLoading.value = true
  try {
    const data = await getAiRuntimeConfig()
    Object.assign(runtimeConfig, data || {})
    form.baseUrl = data?.baseUrl || ''
    form.model = data?.model || 'gpt-5.4'
  } catch (error) {
    ElMessage.error('获取运行时配置失败：' + error.message)
  } finally {
    runtimeLoading.value = false
  }
}

const loadRuntimeMetrics = async () => {
  metricsLoading.value = true
  try {
    const data = await getAiRuntimeMetrics(buildMetricsQuery())
    applyRuntimeMetrics(data)
  } catch (error) {
    ElMessage.error('获取 AI 运行观测失败：' + error.message)
  } finally {
    metricsLoading.value = false
  }
}

const loadSchemaCatalog = async () => {
  catalogLoading.value = true
  try {
    const data = await getAiSchemaCatalog()
    applySchemaCatalog(data)
  } catch (error) {
    ElMessage.error('获取 AI 调试清单失败：' + error.message)
  } finally {
    catalogLoading.value = false
  }
}

const loadRegressionCases = async () => {
  regressionCasesLoading.value = true
  try {
    const data = await getAiRegressionCases()
    applyRegressionCatalog(data)
  } catch (error) {
    ElMessage.error('获取 AI 黄金样例回归用例失败：' + error.message)
  } finally {
    regressionCasesLoading.value = false
  }
}

const loadLatestRegressionRun = async (options = {}) => {
  try {
    regressionRunResult.value = await getAiRegressionLatestRun()
  } catch (error) {
    if (options.silent) {
      return
    }
    ElMessage.error('获取最近一次 AI 黄金样例回归结果失败：' + error.message)
  }
}

const loadRegressionHistory = async (options = {}) => {
  regressionHistoryLoading.value = true
  try {
    const data = await getAiRegressionHistory({
      limit: regressionHistory.limit || 10
    })
    applyRegressionHistory(data)
  } catch (error) {
    if (options.silent) {
      return
    }
    ElMessage.error('获取 AI 黄金样例回归历史失败：' + error.message)
  } finally {
    regressionHistoryLoading.value = false
  }
}

const loadRegressionScenarioHealth = async (options = {}) => {
  regressionScenarioHealthLoading.value = true
  try {
    const data = await getAiRegressionScenarioHealth({
      limit: regressionScenarioHealth.limit || regressionHistory.limit || 10
    })
    applyRegressionScenarioHealth(data)
  } catch (error) {
    if (options.silent) {
      return
    }
    ElMessage.error('获取 AI 回归场景健康聚合失败：' + error.message)
  } finally {
    regressionScenarioHealthLoading.value = false
  }
}

const refreshRegressionPanel = async () => {
  await Promise.all([
    loadRegressionCases(),
    loadLatestRegressionRun({ silent: true }),
    loadRegressionHistory({ silent: true }),
    loadRegressionScenarioHealth({ silent: true })
  ])
}

const loadRagServiceHealth = async () => {
  ragServiceLoading.value = true
  try {
    ragServiceHealth.value = await getAiRagServiceHealth()
  } catch (error) {
    ElMessage.error('获取 RAG 服务状态失败：' + error.message)
  } finally {
    ragServiceLoading.value = false
  }
}

const loadRagServiceDocuments = async () => {
  ragServiceLoading.value = true
  try {
    const data = await getAiRagServiceDocuments({
      scene: ragServiceFilters.scene?.trim() || undefined,
      limit: ragServiceFilters.limit || 20
    })
    ragServiceDocuments.endpoint = data?.endpoint || ''
    ragServiceDocuments.scene = data?.scene || ''
    ragServiceDocuments.limit = data?.limit || ragServiceFilters.limit || 20
    ragServiceDocuments.totalCount = data?.totalCount || 0
    ragServiceDocuments.latencyMs = data?.latencyMs
    ragServiceDocuments.documents = data?.documents || []
    await resetRagDocumentSelection()
  } catch (error) {
    ElMessage.error('获取 RAG 文档列表失败：' + error.message)
  } finally {
    ragServiceLoading.value = false
  }
}

const handleExportRagDocuments = async () => {
  ragExporting.value = true
  try {
    const data = await exportAiRagDocuments({
      scene: ragServiceFilters.scene?.trim() || undefined
    })
    const fileName = buildRagExportFileName(data?.scene)
    downloadJsonFile(
      {
        documents: data?.documents || []
      },
      fileName
    )
    ElMessage.success(`RAG 文档导出完成，本次导出 ${data?.totalCount || 0} 条`)
  } catch (error) {
    ElMessage.error('导出 RAG 文档失败：' + error.message)
  } finally {
    ragExporting.value = false
  }
}

const resetToRuntimeConfig = () => {
  form.baseUrl = runtimeConfig.baseUrl || ''
  form.model = runtimeConfig.model || 'gpt-5.4'
  form.apiKey = ''
}

const clearInputApiKey = () => {
  form.apiKey = ''
}

const buildMetricsQuery = () => ({
  scene: metricsFilters.scene?.trim() || undefined,
  model: metricsFilters.model?.trim() || undefined,
  outcome: metricsFilters.outcome || undefined,
  recentLimit: metricsFilters.recentLimit || 50
})

const applyRuntimeMetrics = (data) => {
  runtimeMetrics.overview = {
    ...createEmptyOverview(),
    ...(data?.overview || {})
  }
  runtimeMetrics.sceneStats = data?.sceneStats || []
  runtimeMetrics.modelStats = data?.modelStats || []
  runtimeMetrics.recentCalls = data?.recentCalls || []
}

const applySchemaCatalog = (data) => {
  schemaCatalog.domains = data?.domains || []
  schemaCatalog.prompts = data?.prompts || []
  schemaCatalog.ragQueries = data?.ragQueries || []
  schemaCatalog.retrievalProfiles = data?.retrievalProfiles || []
  schemaCatalog.structuredSchemas = data?.structuredSchemas || []
  if (!debugForm.promptId && schemaCatalog.prompts.length) {
    debugForm.promptId = schemaCatalog.prompts[0].promptId
  }
  if (!ragDebugForm.profileId && schemaCatalog.retrievalProfiles.length) {
    ragDebugForm.profileId = schemaCatalog.retrievalProfiles[0].profileId
  }
}

const applyRegressionCatalog = (data) => {
  regressionCatalog.totalCount = data?.totalCount || 0
  regressionCatalog.scenarios = data?.scenarios || []
  regressionCatalog.cases = data?.cases || []
  if (regressionFilters.scenario && !regressionScenarioOptions.value.includes(regressionFilters.scenario)) {
    regressionFilters.scenario = ''
  }
}

const applyRegressionHistory = (data) => {
  regressionHistory.limit = data?.limit || regressionHistory.limit || 10
  regressionHistory.totalCount = data?.totalCount || 0
  regressionHistory.runs = data?.runs || []
}

const applyRegressionScenarioHealth = (data) => {
  regressionScenarioHealth.limit = data?.limit || regressionScenarioHealth.limit || 10
  regressionScenarioHealth.totalCount = data?.totalCount || 0
  regressionScenarioHealth.scenarios = (data?.scenarios || []).map((item) => ({
    ...item,
    latestFailedCaseIds: item?.latestFailedCaseIds || [],
    topFailedCases: item?.topFailedCases || [],
    topFailureReasons: item?.topFailureReasons || [],
    topModelNames: item?.topModelNames || [],
    topGraphNames: item?.topGraphNames || [],
    topPromptIds: item?.topPromptIds || []
  }))
}

const handleSearchMetrics = () => {
  loadRuntimeMetrics()
}

const resetCatalogFilters = () => {
  catalogFilters.domain = 'all'
  catalogFilters.keyword = ''
}

const resetRegressionFilters = () => {
  regressionFilters.scenario = ''
  regressionFilters.keyword = ''
}

const fillPromptDebugVariables = () => {
  if (!selectedPromptMeta.value) {
    ElMessage.warning('请先选择一个 Prompt')
    return
  }
  const variables = {}
  ;(selectedPromptMeta.value.templateVariables || []).forEach((item) => {
    variables[item] = ''
  })
  debugForm.variablesJson = JSON.stringify(variables, null, 2)
}

const fillRagDebugDefaults = () => {
  if (!selectedRetrievalProfile.value) {
    ElMessage.warning('请先选择一个 Retrieval Profile')
    return
  }
  ragDebugForm.scene = selectedRetrievalProfile.value.scene || ''
  ragDebugForm.topK = selectedRetrievalProfile.value.topK ? String(selectedRetrievalProfile.value.topK) : ''
  ragDebugForm.metadataFiltersJson = formatJson(selectedRetrievalProfile.value.metadataFilters || {})
}

const clearRagDebugOverrides = () => {
  ragDebugForm.scene = ''
  ragDebugForm.topK = ''
  ragDebugForm.metadataFiltersJson = ''
}

const resetRagServiceFilters = () => {
  ragServiceFilters.scene = ''
  ragServiceFilters.limit = 20
  ragServiceFilters.keyword = ''
  loadRagServiceDocuments()
}

const resetMetricsFilters = () => {
  metricsFilters.scene = ''
  metricsFilters.model = ''
  metricsFilters.outcome = ''
  metricsFilters.recentLimit = 50
  loadRuntimeMetrics()
}

const handleTest = async () => {
  testing.value = true
  try {
    const payload = {
      baseUrl: form.baseUrl?.trim(),
      apiKey: form.apiKey?.trim(),
      model: form.model?.trim()
    }
    const result = await testAiConfig(payload)
    testResult.value = result

    if (result.available) {
      ElMessage.success(result.message || 'AI 配置测试成功')
      loadRuntimeMetrics()
    } else {
      ElMessage.warning(result.message || 'AI 配置测试失败')
    }
  } catch (error) {
    ElMessage.error('AI 配置测试失败：' + error.message)
  } finally {
    testing.value = false
  }
}

const handlePromptDebug = async (execute) => {
  if (!debugForm.promptId) {
    ElMessage.warning('请先选择一个 Prompt')
    return
  }

  debugLoading.value = true
  try {
    const result = await debugAiPrompt({
      promptId: debugForm.promptId,
      variablesJson: debugForm.variablesJson?.trim() || '{}',
      execute
    })
    debugResult.value = result

    if (!execute) {
      ElMessage.success('Prompt 渲染完成')
      return
    }

    if (result.structuredOutputBound && result.structuredValid === false) {
      ElMessage.warning('模型试跑完成，但 Structured Output 校验未通过')
    } else {
      ElMessage.success('模型试跑完成')
    }
    loadRuntimeMetrics()
  } catch (error) {
    ElMessage.error('Prompt 调试失败：' + error.message)
  } finally {
    debugLoading.value = false
  }
}

const handleRagDebug = async () => {
  if (!ragDebugForm.query?.trim()) {
    ElMessage.warning('请输入检索 query')
    return
  }

  ragDebugLoading.value = true
  try {
    const result = await debugAiRag({
      profileId: ragDebugForm.profileId || undefined,
      query: ragDebugForm.query.trim(),
      scene: ragDebugForm.scene?.trim() || undefined,
      topK: parseOptionalPositiveInteger(ragDebugForm.topK, 'TopK'),
      metadataFiltersJson: ragDebugForm.metadataFiltersJson?.trim() || undefined
    })
    ragDebugResult.value = result
    ElMessage.success('LlamaIndex 检索调试完成')
  } catch (error) {
    ElMessage.error('LlamaIndex 检索调试失败：' + error.message)
  } finally {
    ragDebugLoading.value = false
  }
}

const handleRunRegression = async (options = {}) => {
  const scenario = String(options.scenario || '').trim()
  const caseId = String(options.caseId || '').trim()
  const hasFilters = Boolean(scenario || caseId)

  if (!regressionCatalog.totalCount) {
    ElMessage.warning('当前没有可执行的黄金样例回归用例')
    return
  }

  if (options.requireScenario && !scenario) {
    ElMessage.warning('请先选择一个场景，再执行当前场景回归')
    return
  }

  regressionRunning.value = true
  regressionRunTarget.value = caseId ? `case:${caseId}` : scenario ? `scenario:${scenario}` : 'all'
  try {
    const result = await runAiRegression({
      scenario: scenario || undefined,
      caseId: caseId || undefined
    })
    regressionRunResult.value = result

    if ((result?.failedCount || 0) > 0) {
      ElMessage.warning(`回归执行完成：通过 ${result?.passedCount || 0} 条，失败 ${result?.failedCount || 0} 条`)
    } else if (hasFilters) {
      ElMessage.success(`定向回归执行完成，共 ${result?.totalCount || 0} 条，全部通过`)
    } else {
      ElMessage.success(`全量回归执行完成，共 ${result?.totalCount || 0} 条，全部通过`)
    }
    loadRegressionHistory({ silent: true })
    loadRegressionScenarioHealth({ silent: true })
    loadRuntimeMetrics()
  } catch (error) {
    ElMessage.error('执行 AI 黄金样例回归失败：' + error.message)
  } finally {
    regressionRunning.value = false
    regressionRunTarget.value = ''
  }
}

const handleRunRegressionCase = async (row) => {
  if (!row?.caseId) {
    ElMessage.warning('当前用例缺少有效 caseId，暂时无法执行')
    return
  }
  await handleRunRegression({ caseId: row.caseId })
}

const handleViewRegressionHistory = (row) => {
  if (!row) {
    return
  }
  regressionRunResult.value = row
}

const handleViewRegressionScenario = (row) => {
  const scenario = row?.scenario
  if (!scenario) {
    return
  }
  const targetRun = findLatestRegressionRunByScenario(scenario)
  if (!targetRun) {
    ElMessage.warning('当前场景暂时没有可回看的最近结果')
    return
  }
  regressionRunResult.value = targetRun
}

const handleImportRagSample = async () => {
  const importModeText = ragSampleOptions.replace
    ? '导入后会覆盖现有 sidecar 文档，适合回归前重置样例知识。'
    : '导入后会保留现有 sidecar 文档，并在其基础上追加样例知识。'
  try {
    await ElMessageBox.confirm(
      `导入后可直接用于后台 RAG 调试与本地联调。${importModeText}`,
      '确认导入 RAG 样例知识？',
      {
        confirmButtonText: '导入样例',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch (error) {
    return
  }

  ragSampleImporting.value = true
  try {
    const result = await importAiRagSampleDocuments({ replace: ragSampleOptions.replace })
    ElMessage.success(
      `RAG 样例知识导入完成，本次导入 ${result.importedCount || 0} 条，当前总计 ${result.totalCount || 0} 条`
    )
    await Promise.all([loadRagServiceHealth(), loadRagServiceDocuments()])
  } catch (error) {
    ElMessage.error('导入 RAG 样例知识失败：' + error.message)
  } finally {
    ragSampleImporting.value = false
  }
}

const fillRagImportExample = () => {
  if (!ragImportForm.defaultScene?.trim()) {
    ragImportForm.defaultScene = 'general'
  }
  const scene = ragImportForm.defaultScene.trim()
  ragImportForm.documentsJson = formatJson({
    documents: [
      {
        id: `${scene}-architecture-overview`,
        scene,
        text: 'LangGraph4j 负责多步骤工作流编排，适合把问答、工具调用、知识检索和结果校验串成稳定链路。',
        metadata: {
          source: 'manual',
          category: 'architecture',
          tags: ['langgraph4j', 'workflow']
        }
      },
      {
        text: 'LlamaIndex sidecar 负责知识导入、检索和可解释命中结果输出；如果文档未提供 scene，将回退到 defaultScene。',
        metadata: {
          source: 'manual',
          category: 'rag',
          tags: ['llamaindex', 'retrieval', 'default-scene']
        }
      }
    ]
  })
}

const handleImportRagDocuments = async () => {
  if (!ragImportForm.documentsJson?.trim()) {
    ElMessage.warning('请先输入要导入的文档 JSON')
    return
  }

  const importModeText = ragSampleOptions.replace
    ? '本次会覆盖当前 sidecar 中已有文档，请确认这是你期望的操作。'
    : '本次会在现有 sidecar 文档基础上追加自定义知识。'
  try {
    await ElMessageBox.confirm(
      `导入成功后可立刻用于后台 RAG 调试。${importModeText}`,
      '确认导入自定义知识？',
      {
        confirmButtonText: '确认导入',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch (error) {
    return
  }

  ragCustomImporting.value = true
  try {
    const result = await importAiRagDocuments({
      replace: ragSampleOptions.replace,
      defaultScene: ragImportForm.defaultScene?.trim() || undefined,
      documentsJson: ragImportForm.documentsJson.trim()
    })
    ElMessage.success(
      `自定义知识导入完成，请求解析 ${result.requestedDocumentCount || 0} 条，本次导入 ${result.importedCount || 0} 条，当前总计 ${result.totalCount || 0} 条`
    )
    await Promise.all([loadRagServiceHealth(), loadRagServiceDocuments()])
  } catch (error) {
    ElMessage.error('导入自定义知识失败：' + error.message)
  } finally {
    ragCustomImporting.value = false
  }
}

const handleDeleteRagDocument = async (row) => {
  if (!row?.id) {
    ElMessage.warning('当前文档缺少有效 ID，暂时无法删除')
    return
  }

  try {
    await ElMessageBox.confirm(
      `删除后将立即从 sidecar 知识库移除文档 ${row.id}，后续检索将不再命中这条知识。`,
      '确认删除这条 RAG 文档？',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch (error) {
    return
  }

  ragDeletingDocumentId.value = row.id
  try {
    const result = await deleteAiRagDocument({ documentId: row.id })
    ElMessage.success(`文档删除完成，当前剩余 ${result?.totalCount || 0} 条`)
    await Promise.all([loadRagServiceHealth(), loadRagServiceDocuments()])
  } catch (error) {
    ElMessage.error('删除 RAG 文档失败：' + error.message)
  } finally {
    ragDeletingDocumentId.value = ''
  }
}

const handleRagDocumentSelectionChange = (selection) => {
  ragSelectedDocumentIds.value = (selection || [])
    .map((item) => item?.id)
    .filter((item) => item)
}

const handleBatchDeleteRagDocuments = async () => {
  const documentIds = [...ragSelectedDocumentIds.value]
  if (!documentIds.length) {
    ElMessage.warning('请先勾选要删除的 RAG 文档')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确认批量删除已选中的 ${documentIds.length} 条文档吗？删除后这些知识会立刻从 sidecar 中移除。`,
      '确认批量删除 RAG 文档？',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch (error) {
    return
  }

  ragBatchDeleting.value = true

  try {
    const result = await batchDeleteAiRagDocuments({
      documentIds
    })

    if ((result?.deletedCount || 0) > 0 || (result?.missingCount || 0) > 0) {
      await Promise.all([loadRagServiceHealth(), loadRagServiceDocuments()])
    }

    if (!(result?.missingCount || 0)) {
      ElMessage.success(`批量删除完成，本次删除 ${result?.deletedCount || 0} 条，当前剩余 ${result?.totalCount || 0} 条`)
      return
    }

    if (!(result?.deletedCount || 0)) {
      ElMessage.error(`批量删除未命中文档，本次共 ${result?.missingCount || 0} 条未找到`)
      return
    }

    ElMessage.warning(
      `批量删除已完成，成功 ${result?.deletedCount || 0} 条，未命中 ${result?.missingCount || 0} 条（${(result?.missingDocumentIds || []).slice(0, 3).join('、')}）`
    )
  } catch (error) {
    ElMessage.error('批量删除 RAG 文档失败：' + error.message)
  } finally {
    ragBatchDeleting.value = false
  }
}

const handleClearMetrics = async () => {
  try {
    await ElMessageBox.confirm(
      runtimeConfig.metricsPersistenceEnabled
        ? '清空后会移除当前进程与 Redis 中持久化的 AI 调用统计与最近调用记录，通常用于联调、压测或回归前归零观测数据。'
        : '清空后会移除当前进程内聚合的 AI 调用统计与最近调用记录，通常用于联调、压测或回归前归零观测数据。',
      '确认清空运行观测？',
      {
        confirmButtonText: '确认清空',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch (error) {
    return
  }

  metricsLoading.value = true
  try {
    await clearAiRuntimeMetrics()
    applyRuntimeMetrics()
    ElMessage.success('AI 运行观测已清空')
  } catch (error) {
    ElMessage.error('清空 AI 运行观测失败：' + error.message)
  } finally {
    metricsLoading.value = false
  }
}

const filterCatalogItems = (items, fieldResolver) => {
  const keyword = catalogFilters.keyword?.trim().toLowerCase()
  return (items || []).filter((item) => {
    if (catalogFilters.domain !== 'all' && item.domain !== catalogFilters.domain) {
      return false
    }
    if (!keyword) {
      return true
    }
    return flattenSearchFields(fieldResolver(item))
      .some((field) => field.toLowerCase().includes(keyword))
  })
}

const flattenSearchFields = (values) => {
  const flattened = []
  ;(values || []).forEach((value) => {
    if (Array.isArray(value)) {
      flattened.push(...flattenSearchFields(value))
      return
    }
    if (value === undefined || value === null || value === '') {
      return
    }
    if (typeof value === 'object') {
      flattened.push(JSON.stringify(value))
      return
    }
    flattened.push(String(value))
  })
  return flattened
}

const normalizeHighlightTerms = (terms) => {
  return [...new Set(flattenSearchFields(Array.isArray(terms) ? terms : [terms])
    .flatMap((item) => String(item)
      .split(/\s+/)
      .map((segment) => segment.trim()))
    .filter((item) => item))]
}

const escapeHtml = (value) => {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

const escapeRegExp = (value) => {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

const renderHighlightedText = (text, terms) => {
  const normalizedText = text === undefined || text === null || text === '' ? '-' : String(text)
  const escapedText = escapeHtml(normalizedText)
  const highlightTerms = normalizeHighlightTerms(terms)
    .map((item) => escapeHtml(item))
    .filter((item) => item)
    .sort((left, right) => right.length - left.length)

  if (!highlightTerms.length) {
    return escapedText
  }

  const pattern = highlightTerms.map((item) => escapeRegExp(item)).join('|')
  return escapedText.replace(new RegExp(`(${pattern})`, 'gi'), '<mark>$1</mark>')
}

const resetRagDocumentSelection = async () => {
  ragSelectedDocumentIds.value = []
  await nextTick()
  ragDocumentTableRef.value?.clearSelection?.()
}

const parseOptionalPositiveInteger = (value, fieldLabel) => {
  const normalized = String(value ?? '').trim()
  if (!normalized) {
    return undefined
  }
  const parsed = Number.parseInt(normalized, 10)
  if (!Number.isInteger(parsed) || parsed <= 0) {
    throw new Error(`${fieldLabel} 必须是正整数`)
  }
  return parsed
}

const formatLatency = (latencyMs) => {
  if (latencyMs === undefined || latencyMs === null) {
    return '-'
  }
  return `${latencyMs} ms`
}

const formatNumber = (value) => {
  if (value === undefined || value === null) {
    return '-'
  }
  return Number(value).toLocaleString('zh-CN')
}

const formatRate = (successCount, totalInvocations) => {
  if (!totalInvocations) {
    return '0%'
  }
  return `${((successCount / totalInvocations) * 100).toFixed(1)}%`
}

const formatCost = (value, currency = 'USD') => {
  if (value === undefined || value === null) {
    return '-'
  }
  const amount = Number(value)
  if (Number.isNaN(amount)) {
    return '-'
  }
  return `${currency || 'USD'} ${amount.toFixed(6)}`
}

const formatTime = (timestamp) => {
  if (!timestamp) {
    return '-'
  }
  const date = new Date(timestamp)
  if (Number.isNaN(date.getTime())) {
    return '-'
  }
  return date.toLocaleString('zh-CN', { hour12: false })
}

const formatDomainLabel = (domain, includeCode = true) => {
  if (!domain) {
    return '未知业务域'
  }
  const label = DOMAIN_LABEL_MAP[domain] || domain
  return includeCode ? `${label} (${domain})` : label
}

const formatJson = (value) => {
  if (value === undefined || value === null) {
    return '{}'
  }
  if (typeof value === 'string') {
    return value
  }
  try {
    return JSON.stringify(value, null, 2)
  } catch (error) {
    return String(value)
  }
}

const formatScore = (value) => {
  if (value === undefined || value === null) {
    return '-'
  }
  const score = Number(value)
  if (Number.isNaN(score)) {
    return '-'
  }
  return score.toFixed(4)
}

const formatMatchField = (field) => {
  if (!field) {
    return '-'
  }
  const labels = {
    text: '正文',
    metadata: 'Metadata',
    scene: 'Scene',
    id: 'ID'
  }
  return labels[field] || field
}

const buildRegressionRunIdentity = (run) => {
  return [
    run?.executedAt || '',
    run?.scenario || '',
    run?.caseId || '',
    run?.durationMs || ''
  ].join('::')
}

const isActiveRegressionHistory = (row) => {
  return buildRegressionRunIdentity(row) === buildRegressionRunIdentity(regressionRunResult.value)
}

const resolveRegressionRunTarget = (row) => {
  return row?.caseId || row?.scenario || '全量回归'
}

const findLatestRegressionRunByScenario = (scenario) => {
  if (!scenario) {
    return null
  }
  return (regressionHistory.runs || []).find((run) => {
    if (run?.scenario === scenario) {
      return true
    }
    return (run?.caseResults || []).some((item) => item?.scenario === scenario)
  }) || null
}

const buildRagExportFileName = (scene) => {
  const normalizedScene = String(scene || '').trim() || 'all-scenes'
  return `rag-documents-${normalizedScene}-${formatFileTimestamp(new Date())}.json`
}

const formatFileTimestamp = (date) => {
  const target = date instanceof Date ? date : new Date()
  const year = target.getFullYear()
  const month = String(target.getMonth() + 1).padStart(2, '0')
  const day = String(target.getDate()).padStart(2, '0')
  const hours = String(target.getHours()).padStart(2, '0')
  const minutes = String(target.getMinutes()).padStart(2, '0')
  const seconds = String(target.getSeconds()).padStart(2, '0')
  return `${year}${month}${day}-${hours}${minutes}${seconds}`
}

const downloadJsonFile = (payload, fileName) => {
  const blob = new Blob([JSON.stringify(payload || {}, null, 2)], { type: 'application/json;charset=utf-8' })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

const scoreBreakdownItems = (scoreBreakdown) => {
  const labels = {
    exactTextMatch: '正文精确命中',
    exactMetadataMatch: 'Metadata 精确命中',
    exactSceneMatch: 'Scene 精确命中',
    exactIdMatch: 'ID 精确命中',
    textTermScore: '正文词项得分',
    sceneTermScore: 'Scene 词项得分',
    metadataTermScore: 'Metadata 词项得分',
    metadataTitleScore: '标题词项得分',
    metadataKeywordScore: '关键词词项得分',
    metadataTagScore: '标签词项得分',
    metadataSummaryScore: '摘要词项得分',
    metadataCategoryScore: '分类词项得分',
    idTermScore: 'ID 词项得分',
    termCoverage: '命中覆盖率',
    termDensity: '命中密度',
    metadataFilterMatch: '过滤条件命中'
  }
  return Object.entries(scoreBreakdown || {})
    .map(([key, value]) => ({
      key,
      label: labels[key] || key,
      value: Number(value)
    }))
    .filter((item) => !Number.isNaN(item.value) && item.value > 0)
    .sort((left, right) => right.value - left.value)
}

const previewText = (text, maxLength = 120) => {
  if (!text) {
    return '-'
  }
  const normalized = String(text).replace(/\s+/g, ' ').trim()
  if (normalized.length <= maxLength) {
    return normalized
  }
  return `${normalized.slice(0, maxLength)}...`
}

const outcomeTagType = (outcome) => {
  if (outcome === 'success') {
    return 'success'
  }
  if (outcome === 'error') {
    return 'danger'
  }
  return 'info'
}

const resolveRegressionFallbackText = (value) => {
  if (value === true) {
    return '是'
  }
  if (value === false) {
    return '否'
  }
  return '未返回'
}
</script>

<style scoped>
.ai-config-page {
  padding: 20px;
}

.header-card,
.summary-card,
.form-card,
.result-card,
.debug-card,
.catalog-card,
.metrics-card {
  margin-bottom: 20px;
}

.header-content,
.card-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.header-content h2 {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 600;
}

.header-content p {
  margin: 0;
  color: #666;
}

.header-actions,
.form-actions,
.title-group,
.filter-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.metrics-filter-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}

.catalog-filter-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}

.metrics-filter-form :deep(.el-form-item) {
  margin-bottom: 12px;
}

.catalog-filter-form :deep(.el-form-item) {
  margin-bottom: 12px;
}

.summary-list,
.overview-grid,
.result-grid,
.debug-summary-grid {
  display: grid;
  gap: 14px;
}

.result-alert {
  margin-top: 16px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin: 20px 0 12px;
}

.section-title {
  color: #303133;
  font-size: 14px;
  font-weight: 600;
}

.summary-item,
.overview-item,
.result-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border-radius: 10px;
  background: #f7f9fc;
}

.result-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.debug-summary-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.overview-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.overview-item {
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.overview-item strong {
  font-size: 22px;
  color: #1f2d3d;
}

.overview-label {
  color: #909399;
  font-size: 13px;
}

.result-item.full-width {
  grid-column: 1 / -1;
}

.label {
  color: #909399;
  flex-shrink: 0;
}

.value {
  color: #303133;
  text-align: right;
  word-break: break-word;
}

.break-all {
  word-break: break-all;
}

.preview {
  white-space: pre-wrap;
}

.catalog-tabs {
  margin-top: 8px;
}

.debug-layout {
  align-items: stretch;
}

.expand-panel {
  display: grid;
  gap: 16px;
}

.expand-label {
  margin-bottom: 8px;
  color: #606266;
  font-weight: 600;
}

.code-block {
  margin: 0;
  padding: 14px 16px;
  border-radius: 12px;
  background: #0f172a;
  color: #e5e7eb;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
  line-height: 1.7;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, Courier, monospace;
}

.highlight-block {
  white-space: pre-wrap;
  word-break: break-word;
}

.node-text-preview {
  color: #303133;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.highlight-block :deep(mark),
.node-text-preview :deep(mark) {
  padding: 0 2px;
  border-radius: 4px;
  background: rgba(250, 204, 21, 0.35);
  color: inherit;
  font-weight: 600;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.insight-list {
  display: grid;
  gap: 10px;
}

.insight-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 10px;
  background: #f7f9fc;
}

.insight-item--reason {
  background: #fff9eb;
}

.insight-item--prompt {
  background: #eef6ff;
}

.insight-label {
  color: #303133;
  line-height: 1.6;
  word-break: break-word;
}

.regression-result-cell {
  display: grid;
  gap: 8px;
}

.failure-list {
  margin: 0;
  padding-left: 18px;
  color: #606266;
  line-height: 1.7;
}

.muted-text {
  color: #909399;
  font-size: 12px;
  line-height: 1.6;
}

.score-breakdown-list {
  display: grid;
  gap: 10px;
}

.score-breakdown-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f7f9fc;
}

.score-breakdown-label {
  color: #606266;
}

.score-breakdown-value {
  color: #1f2d3d;
  font-size: 13px;
}

.empty-text {
  color: #909399;
  font-size: 12px;
}

.form-tip {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
  line-height: 1.6;
}

.success-text {
  color: #67c23a;
}

.warning-text {
  color: #e6a23c;
}

@media (max-width: 992px) {
  .overview-grid,
  .result-grid,
  .debug-summary-grid {
    grid-template-columns: 1fr;
  }

  .summary-item,
  .overview-item,
  .result-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .value {
    text-align: left;
  }
}
</style>
