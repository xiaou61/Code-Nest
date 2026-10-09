import { useQuery } from '@tanstack/react-query'
import { AppShell, Badge, Card, CardContent, CardHeader, CardTitle, Empty, EmptyDescription, EmptyHeader, EmptyTitle, PageHeader } from '@paideia/ui'

import { api } from '../api'
import { RequireAdmin } from '../guard'
import { sessionReader } from '../session'

interface HealthResponse {
  status: string
  groups?: string[]
}

/**
 * 后端状态徽标。放在应用外壳里，因此**在有守卫的情况下也会渲染** ——
 * 被拒的页面上因此同样能证明接口链路是通的，而不是"因为没权限所以什么都看不到"。
 */
function BackendStatus() {
  const health = useQuery({
    queryKey: ['backend-health'],
    queryFn: () => api.request<HealthResponse>('/actuator/health'),
  })

  if (health.isPending) {
    return <Badge variant="secondary">后端查询中</Badge>
  }
  if (health.isError) {
    return <Badge variant="destructive">后端不可达</Badge>
  }
  return (
    <Badge variant={health.data.status === 'UP' ? 'outline' : 'destructive'}>
      后端 <span data-testid="admin-health-status">{health.data.status}</span>
    </Badge>
  )
}

export function AdminHomePage() {
  const session = sessionReader.current()

  return (
    <AppShell title="Paideia 管理端" subtitle="仅 Web" actions={<BackendStatus />}>
      <PageHeader
        title="管理端"
        description="这个应用与学习者端同仓共享组件，但不进入桌面安装包。"
      />

      <RequireAdmin session={session}>
        <AdminConsole />
      </RequireAdmin>
    </AppShell>
  )
}

/**
 * 管理区。本期还没有任何管理功能，因此这里是一个明确的空态，
 * 而不是摆几个假装能用的入口——空态比假入口诚实。
 */
function AdminConsole() {
  return (
    <div data-testid="admin-console">
      <Card>
        <CardHeader>
          <CardTitle>管理区</CardTitle>
        </CardHeader>
        <CardContent>
          <Empty>
            <EmptyHeader>
              <EmptyTitle>还没有管理功能</EmptyTitle>
              <EmptyDescription>
                用户、课程与内容管理的入口会随对应需求加入，这里不提前摆假入口。
              </EmptyDescription>
            </EmptyHeader>
          </Empty>
        </CardContent>
      </Card>
    </div>
  )
}
