import { AuthLayout, LoginForm, redirectTarget, RequireGuest } from '@paideia/auth'
import { useLocation, useNavigate } from 'react-router'

/**
 * 登录页。
 *
 * <p>用 `AuthLayout` 而不是应用外壳：外壳的横条与页脚在未登录时读起来像设置页，
 * 而那些导航本来就不可用。登录后回到原本要去的地址（由登录门记在 `redirect` 参数里）。
 */
export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()

  return (
    <RequireGuest>
      <AuthLayout
        description="登录以继续你的学习。"
        footer={
          <>
            还没有账号？
            <a href="#/register" className="text-foreground ml-1 underline underline-offset-4">
              立即注册
            </a>
          </>
        }
      >
        <LoginForm onSuccess={() => navigate(redirectTarget(location.search), { replace: true })} />
      </AuthLayout>
    </RequireGuest>
  )
}
