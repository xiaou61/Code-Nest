import { Panel } from '@paideia/ui'

/**
 * 公开页。只放不需要登录就能看的内容，用于验证 SEO surface 与主体应用可以共存。
 *
 * <p>刻意复用 packages/ui：公开页与登录后的应用共享同一套组件，不必维护两份外观。
 * 注意这里**不能**引用 @paideia/core 的 API 客户端——它依赖 fetch 与令牌，属于登录后的场景。
 */
export function PublicPage() {
  return (
    <main
      style={{
        fontFamily: 'system-ui, -apple-system, sans-serif',
        maxWidth: 720,
        margin: '0 auto',
        padding: 24,
      }}
    >
      <h1 style={{ fontSize: 22 }}>Paideia</h1>
      <p style={{ color: '#57606a' }}>
        千人千面的 AI 个性化学习平台。每个学习者拥有自适应的学习路径、内容与反馈，
        而不是所有人看同一套课程、只在前端显示不同的进度条。
      </p>

      <Panel title="这个页面为什么单独存在">
        <ul style={{ margin: 0, paddingLeft: 18, fontSize: 13, lineHeight: 1.8 }}>
          <li>公开页需要被抓取与索引，因此构建时就产出完整 HTML，不依赖客户端渲染。</li>
          <li>登录后的应用是纯客户端 SPA，桌面端还要把它打进安装包，两者目标不同。</li>
          <li>两个 surface 共享 packages/ui 的组件，只有构建与部署方式不同。</li>
        </ul>
      </Panel>

      <p style={{ fontSize: 13, color: '#57606a' }}>
        正文由服务端渲染生成；页面加载后由客户端接管，便于后续加交互。
      </p>
    </main>
  )
}
