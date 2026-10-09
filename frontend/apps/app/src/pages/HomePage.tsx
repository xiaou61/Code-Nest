import { Panel } from '@paideia/ui'
import { useQuery } from '@tanstack/react-query'

import { api } from '../api'
import { API_BASE_URL } from '../env'
import { usePlatform } from '../platform'

interface HealthResponse {
  status: string
  groups?: string[]
}

interface SubjectResponse {
  subject: string
}

export function HomePage() {
  const platform = usePlatform()
  const health = useQuery({
    queryKey: ['backend-health'],
    queryFn: () => api.request<HealthResponse>('/actuator/health'),
  })
  // 受保护接口：当前还没有登录流程，因此正常情况下这里会拿到 401。
  // 这正是要展示的事实——该接口确实受保护，不是浏览器拿到就能读。
  const identity = useQuery({
    queryKey: ['backend-identity'],
    queryFn: () => api.requestEnvelope<SubjectResponse>('/api/v1/me').then((data) => data.subject),
  })

  return (
    <main
      style={{
        fontFamily: 'system-ui, -apple-system, sans-serif',
        maxWidth: 720,
        margin: '0 auto',
        padding: 24,
      }}
    >
      <h1 style={{ fontSize: 20, marginBottom: 4 }}>Paideia 骨架</h1>
      <p style={{ color: '#57606a', fontSize: 13, marginTop: 0 }}>
        本页展示的是后端接口的真实返回，不是硬编码数据。
      </p>

      <Panel title="运行环境">
        <dl style={{ margin: 0, fontSize: 13, display: 'grid', gridTemplateColumns: 'auto 1fr', gap: 4 }}>
          <dt>平台</dt>
          <dd style={{ margin: 0 }} data-testid="platform-kind">
            {platform.kind}
          </dd>
          <dt>后端基址</dt>
          <dd style={{ margin: 0 }} data-testid="api-base">
            {API_BASE_URL === '' ? '同源（开发期由 Vite 代理转发）' : API_BASE_URL}
          </dd>
        </dl>
      </Panel>

      <Panel title="后端健康检查 · GET /actuator/health">
        {health.isPending && <p data-testid="health-state">查询中…</p>}
        {health.isError && (
          <p data-testid="health-state" style={{ color: '#cf222e', margin: 0 }}>
            请求失败：{(health.error as Error).message}
          </p>
        )}
        {health.isSuccess && (
          <>
            <p style={{ margin: '0 0 8px' }} data-testid="health-state">
              状态：<strong data-testid="health-status">{health.data.status}</strong>
            </p>
            <pre
              data-testid="health-raw"
              style={{ background: '#f6f8fa', padding: 12, fontSize: 12, overflowX: 'auto', margin: 0 }}
            >
              {JSON.stringify(health.data, null, 2)}
            </pre>
          </>
        )}
        <button
          type="button"
          onClick={() => void health.refetch()}
          style={{ marginTop: 12, fontSize: 13, padding: '4px 10px' }}
        >
          重新查询
        </button>
      </Panel>

      <Panel title="当前身份 · GET /api/v1/me（受保护接口）">
        {identity.isPending && <p data-testid="identity-state">查询中…</p>}
        {identity.isSuccess && (
          <p style={{ margin: 0 }} data-testid="identity-state">
            已认证：<strong data-testid="identity-subject">{identity.data}</strong>
          </p>
        )}
        {identity.isError && (
          <p style={{ margin: 0 }} data-testid="identity-state">
            <span data-testid="identity-subject">未登录</span>
            <span style={{ color: '#57606a' }}>（{(identity.error as Error).message}）</span>
          </p>
        )}
      </Panel>
    </main>
  )
}
