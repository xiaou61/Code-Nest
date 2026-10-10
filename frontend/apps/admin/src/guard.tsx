import { canAccessAdmin, type Session } from '@paideia/core'
import { Alert, AlertDescription, AlertTitle, Card, CardContent, CardHeader, CardTitle } from '@paideia/ui'
import type { AuthUser } from '@paideia/auth'

/**
 * 管理入口的唯一判定点。
 *
 * <p>它只调 `canAccessAdmin`，不自己比较角色字符串——角色逻辑只有 core 里那一处。
 * 这里只做界面分流，**不是授权边界**；真正的拦截必须在后端每个管理员接口上
 * （本期后端还没有管理员接口，这一点在验证报告里记为缺口）。
 */
export function canEnterAdmin(user: AuthUser | null): boolean {
  const session: Session | null = user === null ? null : { role: user.role, signedIn: true }
  return canAccessAdmin(session)
}

/**
 * 被拒时展示的界面。
 *
 * <p>刻意把"当前是谁、为什么被拒"写在页面上，而不是静默跳走：授权缺失必须是**可见**的。
 * 也刻意不提供任何"以管理员身份预览"的开关——那会把授权缺失伪装成功能正常。
 */
export function AdminDenied({ user }: { user: AuthUser | null }) {
  const identity = user === null ? '未登录' : user.role === 'admin' ? '管理员' : '学习者'

  return (
    <div data-testid="admin-denied">
      <Alert variant="destructive">
        <AlertTitle>无管理权限</AlertTitle>
        <AlertDescription>管理端只对管理员开放，当前身份被拒绝进入。</AlertDescription>
      </Alert>

      <Card className="mt-6">
        <CardHeader>
          <CardTitle>为什么进不去</CardTitle>
        </CardHeader>
        <CardContent className="text-muted-foreground space-y-3 text-sm">
          <p className="m-0">
            当前身份：
            <span className="text-foreground font-medium" data-testid="admin-identity">
              {identity}
            </span>
          </p>
          <p className="m-0">
            管理端只放行管理员。注册产生的账号一律是学习者，管理员只能由种子迁移或运维手段产生——
            否则任何人都能通过改一个字段把自己变成管理员。
          </p>
          {user === null ? (
            <p className="m-0">请先用管理员账号登录。</p>
          ) : (
            <p className="m-0">如需管理权限，请联系运维为这个账号提权。</p>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
