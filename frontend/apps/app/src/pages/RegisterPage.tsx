import { AuthLayout, RegisterForm, RequireGuest } from '@paideia/auth'
import { useNavigate } from 'react-router'

/** 注册页。注册成功即已登录，直接进首页。字段比登录多，所以卡片宽一档。 */
export function RegisterPage() {
  const navigate = useNavigate()

  return (
    <RequireGuest>
      <AuthLayout
        wide
        description="创建一个账号，开始你的个性化学习。"
        footer={
          <>
            已有账号？
            <a href="#/login" className="text-foreground ml-1 underline underline-offset-4">
              去登录
            </a>
          </>
        }
      >
        <RegisterForm onSuccess={() => navigate('/', { replace: true })} />
      </AuthLayout>
    </RequireGuest>
  )
}
