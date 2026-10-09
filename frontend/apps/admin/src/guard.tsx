import { canAccessAdmin, type Session } from '@paideia/core'
import { Alert, AlertDescription, AlertTitle, Card, CardContent, CardHeader, CardTitle } from '@paideia/ui'
import type { ReactNode } from 'react'

/**
 * 管理入口的唯一判定点。
 *
 * <p>它只调 `canAccessAdmin`，不自己比较角色字符串——角色逻辑只有那一处，
 * 将来加角色或换认证机制都不用改这里。
 */
export function RequireAdmin({ session, children }: { session: Session | null; children: ReactNode }) {
  return canAccessAdmin(session) ? <>{children}</> : <AdminDenied session={session} />
}

/**
 * 被拒时展示的界面。
 *
 * <p>刻意把"当前是谁、为什么被拒"写在页面上，而不是静默跳走：授权缺失必须是**可见**的。
 * 也刻意不提供任何"以管理员身份预览"的开关——那会把授权缺失伪装成功能正常。
 */
function AdminDenied({ session }: { session: Session | null }) {
  const identity =
    session === null ? '未登录（没有可用令牌）' : session.role === 'admin' ? '管理员' : '学习者'

  return (
    <div data-testid="admin-denied">
      <Alert variant="destructive">
        <AlertTitle>无管理权限</AlertTitle>
        <AlertDescription>
          管理端只对管理员开放，当前身份被拒绝进入。
        </AlertDescription>
      </Alert>

      <Card className="mt-6">
        <CardHeader>
          <CardTitle>为什么现在一定进不去</CardTitle>
        </CardHeader>
        <CardContent className="text-muted-foreground space-y-3 text-sm">
          <p className="m-0">
            当前身份：
            <span className="text-foreground font-medium" data-testid="admin-identity">
              {identity}
            </span>
          </p>
          <p className="m-0">
            后端还没有签发角色字段，所以会话读取拿不到 admin——按"失败即拒绝"的规则，
            这里只会被拒。这不是没做完，而是刻意的：授权缺失必须看得见，不能用调试开关糊过去。
          </p>
          <p className="m-0">
            待后端的角色契约落地后，只需让令牌带上 `role`，本页无需改动即可放行管理员。
          </p>
        </CardContent>
      </Card>
    </div>
  )
}
