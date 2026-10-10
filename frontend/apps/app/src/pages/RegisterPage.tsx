import { AppShell, Card, CardContent, CardHeader, CardTitle, PageHeader } from '@paideia/ui'
import { RegisterForm, RequireGuest } from '@paideia/auth'
import { useNavigate } from 'react-router'

/** 注册页。注册成功即已登录，直接进首页。 */
export function RegisterPage() {
  const navigate = useNavigate()

  return (
    <RequireGuest>
      <AppShell title="Paideia" subtitle="注册">
        <div className="mx-auto max-w-sm py-8">
          <PageHeader
            title="注册"
            description="注册需要邮箱验证码；发验证码之前要先通过图形验证码。"
          />
          <Card>
            <CardHeader>
              <CardTitle>创建账号</CardTitle>
            </CardHeader>
            <CardContent>
              <RegisterForm onSuccess={() => navigate('/', { replace: true })} />
              <p className="text-muted-foreground mt-4 text-sm">
                已有账号？{' '}
                <a href="#/login" className="text-foreground underline underline-offset-4">
                  去登录
                </a>
              </p>
            </CardContent>
          </Card>
        </div>
      </AppShell>
    </RequireGuest>
  )
}
