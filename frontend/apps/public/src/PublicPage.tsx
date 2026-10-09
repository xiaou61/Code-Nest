import { Card, CardContent, CardHeader, CardTitle } from '@paideia/ui'

/**
 * 公开页。只放不需要登录就能看的内容，用于验证 SEO surface 与主体应用可以共存。
 *
 * <p>刻意复用 packages/ui：公开页与登录后的应用共享同一套组件，不必维护两份外观。
 *
 * <p>两条约束：这里**不能**引用 `@paideia/core` 的 API 客户端（它依赖 fetch 与令牌，
 * 属于登录后的场景）；也**不用 `AppShell`**——外壳带主题切换，而主题切换需要
 * `Platform` 端口来持久化，那会绕道把 core 拖进来。公开页是静态营销页，跟随系统偏好即可。
 */
export function PublicPage() {
  return (
    <main className="mx-auto max-w-2xl px-6 py-16">
      <h1 className="text-3xl">Paideia</h1>
      <p className="text-muted-foreground mt-4">
        千人千面的 AI 个性化学习平台。每个学习者拥有自适应的学习路径、内容与反馈，
        而不是所有人看同一套课程、只在前端显示不同的进度条。
      </p>

      <Card className="mt-10">
        <CardHeader>
          <CardTitle>这个页面为什么单独存在</CardTitle>
        </CardHeader>
        <CardContent>
          <ul className="text-muted-foreground list-disc space-y-2 pl-5 text-sm">
            <li>公开页需要被抓取与索引，因此构建时就产出完整 HTML，不依赖客户端渲染。</li>
            <li>登录后的应用是纯客户端 SPA，桌面端还要把它打进安装包，两者目标不同。</li>
            <li>两个 surface 共享 packages/ui 的组件，只有构建与部署方式不同。</li>
          </ul>
        </CardContent>
      </Card>

      <p className="text-muted-foreground mt-8 text-sm">
        正文由服务端渲染生成；页面加载后由客户端接管，便于后续加交互。
      </p>
    </main>
  )
}
