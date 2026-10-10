import { useAuth } from '@paideia/auth'
import { Badge, Button } from '@paideia/ui'

/**
 * 页头的账号区：当前用户、角色与登出。
 *
 * <p>从 `HomePage` 里提出来是因为知识库页也要它——两个页面各写一份的话，
 * 迟早出现"一个页面有登出、另一个没有"这种不一致。
 */
export function AccountBar() {
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
