# 生产 CI/CD 自动发布

本页说明 Code Nest `v2.3.1` 起启用的生产 CI/CD 流程。目标是每次准备发布版本后，由 GitHub Actions 自动构建后端、用户端、管理端和文档站，并在触发生产部署时同步更新服务器。

## 流水线总览

| Workflow | 触发方式 | 作用 |
| --- | --- | --- |
| `CI` | PR、`master` push、`v*` 分支 push、手动触发 | 后端打包、双前端构建、文档站构建、脚本语法检查 |
| `Deploy Production` | `v*` 分支/tag push、手动触发 | 构建 release bundle，上传服务器，执行生产发布 |
| `Docs Site` | 文档路径变更、手动触发 | 单独验证文档站 |
| `AI Regression` | AI 相关路径变更、手动触发 | 单独验证 AI 回归样例 |

生产部署不会读取仓库里的真实密码、SSH 私钥或 token，所有敏感信息必须放在 GitHub Secrets 中。

## GitHub Secrets

在仓库的 `Settings -> Secrets and variables -> Actions` 中配置：

| Secret | 必填 | 示例 | 说明 |
| --- | --- | --- | --- |
| `CODE_NEST_DEPLOY_HOST` | 是 | `your-server.example.com` | 生产服务器 IP 或域名 |
| `CODE_NEST_DEPLOY_USER` | 是 | `root` 或专用部署用户 | SSH 登录用户 |
| `CODE_NEST_DEPLOY_SSH_KEY` | 是 | OpenSSH 私钥内容 | 建议使用专用部署 key，不要提交到仓库 |
| `CODE_NEST_DEPLOY_PORT` | 否 | `22` | SSH 端口，不填时默认 `22` |

建议额外在 GitHub Environments 中创建 `production` 环境。后续如果需要人工审批，可以直接在该环境上增加 protection rules。

## 服务器目录约定

默认部署脚本使用以下路径：

| 用途 | 默认路径 |
| --- | --- |
| 应用根目录 | `/opt/code-nest` |
| 后端 Jar | `/opt/code-nest/app/app.jar` |
| 发布脚本副本 | `/opt/code-nest/bin/deploy-release.sh` |
| 发布备份目录 | `/opt/code-nest/backups/releases` |
| 用户端静态目录 | `/var/www/code-nest-user` |
| 管理端静态目录 | `/var/www/code-nest-admin` |
| systemd 服务 | `code-nest.service` |
| 健康检查 | `http://127.0.0.1:9999/api/actuator/health` |

如服务器目录不同，可以在部署用户的 shell 环境、systemd 环境或 workflow 远端命令中覆盖：

```bash
CODE_NEST_APP_ROOT=/opt/code-nest
CODE_NEST_APP_DIR=/opt/code-nest/app
CODE_NEST_USER_WEB_DIR=/var/www/code-nest-user
CODE_NEST_ADMIN_WEB_DIR=/var/www/code-nest-admin
CODE_NEST_SERVICE_NAME=code-nest.service
CODE_NEST_HEALTH_URL=http://127.0.0.1:9999/api/actuator/health
```

部署脚本会拒绝修改不在 `/opt/code-nest/*` 或 `/var/www/code-nest-*` 下的目标目录，避免路径配置错误导致误删上级目录。

## 发布触发方式

### 版本分支或 tag 自动部署

推送 `v*` 分支或 tag 会触发 `Deploy Production`：

```bash
git checkout -b v2.3.1
git push -u origin v2.3.1
```

也可以在合并后创建 tag：

```bash
git tag -a v2.3.1 -m "Code Nest v2.3.1"
git push origin v2.3.1
```

### 手动部署指定 ref

在 GitHub Actions 页面打开 `Deploy Production`，点击 `Run workflow`：

| 输入项 | 说明 |
| --- | --- |
| `ref` | 可填写分支、tag 或 commit；留空时使用当前 workflow ref |
| `reload_nginx` | 是否在替换静态资源后执行 `nginx -t && systemctl reload nginx` |

手动触发适合补发某个 commit、重跑失败部署或验证一个版本分支。

## 发布包内容

`Deploy Production` 会构建一个 tar.gz release bundle，内容包括：

```text
backend/app.jar
admin/*
user/*
scripts/deploy-release.sh
RELEASE
```

服务器端脚本会执行：

1. 解压 release bundle 到临时 stage。
2. 校验 Jar、用户端 `index.html` 和管理端 `index.html` 是否存在。
3. 备份当前 Jar、用户端静态目录和管理端静态目录。
4. 替换 `/opt/code-nest/app/app.jar`。
5. 替换 `/var/www/code-nest-user` 和 `/var/www/code-nest-admin`。
6. 更新 `/opt/code-nest/bin/deploy-release.sh`。
7. reload Nginx。
8. 重启 `code-nest.service`。
9. 访问 Actuator health，失败则自动回滚到本次发布前备份。

## 手动回滚

查看最近备份：

```bash
ls -lt /opt/code-nest/backups/releases
```

回滚到某个备份目录：

```bash
sudo CODE_NEST_RELEASE_VERSION=manual-rollback \
  /opt/code-nest/bin/deploy-release.sh rollback /opt/code-nest/backups/releases/<backup-dir>
```

回滚会恢复备份中的 `app.jar`、用户端静态资源和管理端静态资源，然后 reload Nginx 并重启后端服务。

## 排障入口

| 问题 | 检查 |
| --- | --- |
| GitHub 无法连接服务器 | Secrets 中 host、user、port、SSH key 是否正确；服务器安全组是否允许 GitHub Actions 出口连接 |
| 上传成功但部署失败 | 查看 Actions 中 `Deploy on server` 日志，重点看脚本输出的备份目录和健康检查错误 |
| 健康检查失败 | 在服务器执行 `systemctl status code-nest.service` 和 `journalctl -u code-nest.service -n 200 --no-pager` |
| 前端资源未更新 | 检查 Nginx root 是否指向 `/var/www/code-nest-user` 或 `/var/www/code-nest-admin` |
| WebSocket 异常 | 确认 Nginx `/api/ws/` 配置包含 `Upgrade` 和 `Connection` 头 |
| 需要跳过 Nginx reload | 手动触发 workflow 时将 `reload_nginx` 设为 `false` |

生产部署只负责应用产物同步，不自动执行数据库迁移。涉及 SQL 的版本仍需在发布前单独评审、备份和执行迁移脚本。
