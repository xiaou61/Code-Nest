import { Alert, AlertDescription, Button, Input, Label, Spinner } from '@paideia/ui'
import { useState, type FormEvent } from 'react'

import { messageOf, useAuth } from './auth-context'

/**
 * 登录表单。标识可以是用户名或邮箱——后端两种都认。
 *
 * <p>失败文案由后端决定（它刻意不区分"账号不存在"与"密码错误"），这里只负责展示，
 * 不在前端再猜一遍原因。
 */
export function LoginForm({ onSuccess }: { onSuccess?: () => void }) {
  const { login } = useAuth()
  const [identifier, setIdentifier] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function submit(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await login(identifier, password)
      onSuccess?.()
    } catch (failure) {
      setError(messageOf(failure))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="space-y-5" onSubmit={submit} data-testid="login-form">
      <div className="space-y-2">
        <Label htmlFor="login-identifier">用户名或邮箱</Label>
        <Input
          id="login-identifier"
          data-testid="login-identifier"
          autoComplete="username"
          value={identifier}
          onChange={(event) => setIdentifier(event.target.value)}
          className="h-10"
          required
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="login-password">密码</Label>
        <Input
          id="login-password"
          data-testid="login-password"
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          className="h-10"
          required
        />
      </div>

      {error === null ? null : (
        <Alert variant="destructive" data-testid="login-error">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      <Button type="submit" disabled={busy} data-testid="login-submit" className="h-10 w-full">
        {busy ? <Spinner /> : null}
        登录
      </Button>
    </form>
  )
}
