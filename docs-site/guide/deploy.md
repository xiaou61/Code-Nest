# 独立部署

`docs-site/` 可以作为一个独立静态站部署。它不需要 Java 运行时，也不需要数据库和 Redis。

## 构建产物

```bash
cd docs-site
npm ci
npm run build
```

产物目录：

```text
docs-site/.vitepress/dist
```

## Nginx 部署

示例：

```nginx
server {
    listen 80;
    server_name docs.example.com;

    root /var/www/code-nest-docs;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

将 `docs-site/.vitepress/dist` 中的文件同步到 `/var/www/code-nest-docs` 即可。

## 子路径部署

如果文档站部署在子路径，例如：

```text
https://example.com/code-nest-docs/
```

构建时设置：

```bash
VITEPRESS_BASE=/code-nest-docs/ npm run build
```

配置文件会读取 `VITEPRESS_BASE`，默认是 `/`。

## GitHub Pages 思路

可以新增单独 workflow，只在 `docs-site/**` 变更时构建：

```yaml
name: Docs Site

on:
  push:
    branches: [v2.2.0]
    paths:
      - "docs-site/**"
      - ".github/workflows/docs-site.yml"

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 20
          cache: npm
          cache-dependency-path: docs-site/package-lock.json
      - run: npm ci
        working-directory: docs-site
      - run: npm run build
        working-directory: docs-site
```

是否发布到 Pages、服务器或对象存储，可以在团队部署策略确定后补充。

