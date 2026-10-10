import { useAuth } from '@paideia/auth'
import {
  Alert,
  AlertDescription,
  AlertTitle,
  AppShell,
  Badge,
  Button,
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
  PageHeader,
  Spinner,
} from '@paideia/ui'
import { useQuery } from '@tanstack/react-query'

import { api } from '../api'
import { API_BASE_URL } from '../env'
import { usePlatform } from '../platform'

/** 页头的账号区：显示当前用户与登出入口。 */
function AccountBar() {
  const { user, logout } = useAuth()
  if (user === null) {
    return null
  }
  return (
    <div className="flex items-center gap-2">
      <span className="text-muted-foreground text-xs" data-testid="current-user">
        {user.username}
      </span>
      <Badge variant="outline">{user.role === 'admin' ? '管理员' : '学习者'}</Badge>
      <Button variant="ghost" size="sm" data-testid="logout" onClick={() => void logout()}>
        登出
      </Button>
    </div>
  )
}

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
  // 受保护接口：身份只从令牌解析。登录之后这里会返回当前主体的标识，
  // 而在没有令牌时后端会拒为 401（e2e 用不带令牌的直连请求断言这一点）。
  const identity = useQuery({
    queryKey: ['backend-identity'],
    queryFn: () => api.requestEnvelope<SubjectResponse>('/api/v1/me').then((data) => data.subject),
  })

  return (
    <AppShell title="Paideia" subtitle="骨架" actions={<AccountBar />}>
      <PageHeader
        title="Paideia 骨架"
        description="本页展示的是后端接口的真实返回，不是硬编码数据。"
      />

      <div className="space-y-6">
        <Card>
          <CardHeader>
            <CardTitle>运行环境</CardTitle>
            <CardDescription>平台能力来自 Platform 端口，界面代码不认识宿主。</CardDescription>
          </CardHeader>
          <CardContent>
            <dl className="grid grid-cols-[auto_1fr] gap-x-6 gap-y-2 text-sm">
              <dt className="text-muted-foreground">平台</dt>
              <dd className="m-0">
                <Badge variant="secondary" data-testid="platform-kind">
                  {platform.kind}
                </Badge>
              </dd>
              <dt className="text-muted-foreground">后端基址</dt>
              <dd className="m-0 font-mono text-xs" data-testid="api-base">
                {API_BASE_URL === '' ? '同源（开发期由 Vite 代理转发）' : API_BASE_URL}
              </dd>
            </dl>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>后端健康检查 · GET /actuator/health</CardTitle>
            <CardDescription>这个端点是公开的，不需要令牌。</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {health.isPending && (
              <p className="text-muted-foreground flex items-center gap-2 text-sm" data-testid="health-state">
                <Spinner />
                查询中…
              </p>
            )}
            {health.isError && (
              <Alert variant="destructive" data-testid="health-state">
                <AlertTitle>请求失败</AlertTitle>
                <AlertDescription>{(health.error as Error).message}</AlertDescription>
              </Alert>
            )}
            {health.isSuccess && (
              <>
                <p className="m-0 text-sm" data-testid="health-state">
                  状态：
                  <strong data-testid="health-status">{health.data.status}</strong>
                </p>
                <pre
                  data-testid="health-raw"
                  className="bg-muted text-muted-foreground overflow-x-auto rounded-md p-3 font-mono text-xs"
                >
                  {JSON.stringify(health.data, null, 2)}
                </pre>
              </>
            )}
            <Button variant="outline" size="sm" onClick={() => void health.refetch()}>
              重新查询
            </Button>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>当前身份 · GET /api/v1/me</CardTitle>
            <CardDescription>
              受保护接口。身份只从请求令牌解析，不接受客户端传入的用户标识。
            </CardDescription>
          </CardHeader>
          <CardContent>
            {identity.isPending && (
              <p className="text-muted-foreground flex items-center gap-2 text-sm" data-testid="identity-state">
                <Spinner />
                查询中…
              </p>
            )}
            {identity.isSuccess && (
              <p className="m-0 text-sm" data-testid="identity-state">
                已认证：<strong data-testid="identity-subject">{identity.data}</strong>
              </p>
            )}
            {identity.isError && (
              <p className="m-0 text-sm" data-testid="identity-state">
                <span data-testid="identity-subject">未登录</span>
                <span className="text-muted-foreground">（{(identity.error as Error).message}）</span>
              </p>
            )}
          </CardContent>
        </Card>
      </div>
    </AppShell>
  )
}
