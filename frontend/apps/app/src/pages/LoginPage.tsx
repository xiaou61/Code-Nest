import { AppShell, Card, CardContent, CardHeader, CardTitle, PageHeader } from '@paideia/ui'
import { useAuth } from '@paideia/auth'

import { LoginForm } from '@paideia/auth'
import { redirectTarget, RequireGuest } from '@paideia/auth'
import { useLocation, useNavigate } from 'react-router'

/** 登录页。登录后回到原本要去的地址（由登录门记在 `redirect` 参数里）。 */
export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()

  return (
    <RequireGuest>
      <AppShell title="Paideia" subtitle="登录">
        <div className="mx-auto max-w-sm py-8">
          <PageHeader title="登录" description="用用户名或邮箱登录。" />
          <Card>
            <CardHeader>
              <CardTitle>账号登录</CardTitle>
            </CardHeader>
            <CardContent>
              <LoginForm onSuccess={() => navigate(redirectTarget(location.search), { replace: true })} />
              <p className="text-muted-foreground mt-4 text-sm">
                还没有账号？
                <LinkToRegister />
              </p>
            </CardContent>
          </Card>
        </div>
      </AppShell>
    </RequireGuest>
  )
}

function LinkToRegister() {
  const { status } = useAuth()
  // 已登录时不该看到注册入口：登录门已经在跳转了
  return status === 'signed-in' ? null : (
    <a href="#/register" className="text-foreground ml-1 underline underline-offset-4">
      去注册
    </a>
  )
}
