# 快速开始

Code Nest 文档站是一个独立的 VitePress 子项目，目录在 `docs-site/`。它不参与根目录 Maven 构建，也不依赖两个业务前端的构建流程。

## 环境要求

| 工具 | 建议版本 | 说明 |
| --- | --- | --- |
| Node.js | 18+ | VitePress 运行环境 |
| npm | 9+ | 与现有前端项目保持一致 |
| Git | 2.30+ | 用于分支与部署流水线 |

## 启动文档站

```bash
cd docs-site
npm install
npm run dev
```

默认会启动 VitePress 开发服务。终端会打印本地访问地址。

## 构建文档站

```bash
cd docs-site
npm run build
```

构建产物位于：

```text
docs-site/.vitepress/dist
```

这个目录可以直接交给 Nginx、静态对象存储、GitHub Pages、Vercel 或其他静态站点平台托管。

## 预览生产产物

```bash
cd docs-site
npm run preview
```

`preview` 读取构建后的静态产物，适合在发布前确认路由、导航和静态资源路径。

## 与旧文档的关系

现有 `docs/` 目录继续作为资料库保留，包括 PRD、截图手册、技术方案、升级计划和归档文档。

`docs-site/` 负责对这些资料重新组织成“读者能顺着看”的文档站。后续迁移或引用旧资料时，优先保持原始文件不动，再在文档站里写结构化说明。

## 验证清单

第一次启动文档站时，确认：

1. `npm install` 能在 `docs-site/` 内完成，不需要回到项目根目录安装。
2. `npm run dev` 能打印本地访问地址，并能打开首页。
3. `npm run build` 能生成 `.vitepress/dist`。
4. `npm run preview` 能预览构建产物。
5. 修改任意 Markdown 后，开发服务能热更新页面内容。
