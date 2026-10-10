import { LoginForm, useAuth } from '@paideia/auth'
import { useQuery } from '@tanstack/react-query'
import {
  AppShell,
  Badge,
  Button,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  Empty,
  EmptyDescription,
  EmptyHeader,
  EmptyTitle,
  PageHeader,
  Spinner,
} from '@paideia/ui'

import { api } from '../api'
import { AdminDenied, canEnterAdmin } from '../guard'

interface HealthResponse {
  status: string
  groups?: string[]
}

/**
 * 后端状态徽标。放在应用外壳里，因此**在所有状态下都会渲染** ——
 * 未登录或权限不足时同样能证明接口链路是通的，而不是"什么都连不上"。
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

/** 页头的账号区。 */
function AccountBar() {
  const { user, logout } = useAuth()
  if (user === null) {
    return null
  }
  return (
    <div className="flex items-center gap-2">
      <span className="text-muted-foreground text-xs" data-testid="admin-current-user">
        {user.username}
      </span>
      <Button variant="ghost" size="sm" data-testid="admin-logout" onClick={() => void logout()}>
        登出
      </Button>
    </div>
  )
}

/**
 * 管理端。它只有一屏，所以**不引路由**：登录视图是这一屏的一个状态。
 *
 * <p>四种状态依次是：确认登录状态 → 未登录（显示登录表单）→ 已登录但非管理员（明确告知被拒）
 * → 管理员（显示管理区）。第三种刻意不是"跳走"而是"说清楚"，授权缺失必须看得见。
 */
export function AdminHomePage() {
  const { status, user } = useAuth()

  return (
    <AppShell
      title="Paideia 管理端"
      subtitle="仅 Web"
      actions={
        <div className="flex items-center gap-2">
          <BackendStatus />
          <AccountBar />
        </div>
      }
    >
      {status === 'loading' ? (
        <div className="text-muted-foreground flex items-center justify-center gap-2 py-16 text-sm">
          <Spinner />
          正在确认登录状态…
        </div>
      ) : status === 'signed-out' ? (
        <div className="mx-auto max-w-sm py-8" data-testid="admin-sign-in">
          <PageHeader title="管理端登录" description="管理端与学习者端共用一套账号。登录后仍需具备管理员角色。" />
          <Card>
            <CardHeader>
              <CardTitle>账号登录</CardTitle>
            </CardHeader>
            <CardContent>
              <LoginForm />
            </CardContent>
          </Card>
        </div>
      ) : canEnterAdmin(user) ? (
        <AdminConsole />
      ) : (
        <>
          <PageHeader title="管理端" description="当前账号没有管理权限。" />
          <AdminDenied user={user} />
        </>
      )}
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
      <PageHeader title="管理端" description="已确认管理员身份。" />
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
