import { ThemeToggle } from '@paideia/ui'
import type { ReactNode } from 'react'

/**
 * 认证页布局：登录页与注册页共用的"裸页面"。
 *
 * <p><b>刻意不用 `AppShell`</b>：外壳带应用横条与页脚，套在登录页上会读成"设置页"，
 * 而且未登录时那些导航本来就没有意义。通行做法（Vercel、Clerk、Linear 的登录页都是这样）是
 * 一页空白、内容垂直居中、品牌在上、卡片在下。
 *
 * <p>主题切换保留在右上角：登录页也要能切深浅色，但它不该占一条横条。
 *
 * <p>标题固定是品牌名并作为本页唯一的 `h1`；具体在做什么（登录还是注册）由说明文字与
 * 表单里的按钮表达，避免出现"Paideia / 登录 / 账号登录"三行标题叠在一起。
 */
export function AuthLayout({
  description,
  footer,
  wide = false,
  children,
}: {
  description: ReactNode
  /** 卡片下方的次要动作，例如"还没有账号？去注册"。 */
  footer?: ReactNode
  /** 字段多的表单（注册）用更宽一档。 */
  wide?: boolean
  children: ReactNode
}) {
  return (
    <div className="flex min-h-screen flex-col">
      <div className="flex justify-end px-6 py-4">
        <ThemeToggle />
      </div>

      <div className="flex flex-1 items-center justify-center px-6 pb-20">
        <div className={wide ? 'w-full max-w-md' : 'w-full max-w-sm'}>
          <div className="mb-8 text-center">
            <h1 className="text-2xl">Paideia</h1>
            <p className="text-muted-foreground mt-2 text-sm">{description}</p>
          </div>

          <div className="border-border bg-card text-card-foreground shadow-card rounded-lg border p-6">
            {children}
          </div>

          {footer === undefined ? null : (
            <div className="text-muted-foreground mt-6 text-center text-sm">{footer}</div>
          )}
        </div>
      </div>

      <p className="text-muted-foreground px-6 pb-8 text-center text-xs">
        千人千面的 AI 个性化学习平台
      </p>
    </div>
  )
}
