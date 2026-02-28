# go-judge 判题沙箱部署

基于 [criyle/go-judge](https://github.com/criyle/go-judge)，额外安装了 Java、GCC、G++、Python3、Go、Node.js 编译器。

## 部署步骤

将 `docker/go-judge` 目录上传到服务器，然后执行：

```bash
# 构建镜像并启动（首次需要几分钟下载编译器）
docker compose up -d --build

# 查看日志确认启动成功
docker compose logs -f

# 验证编译器是否安装成功
docker exec go-judge javac -version
docker exec go-judge gcc --version
docker exec go-judge python3 --version
docker exec go-judge go version
docker exec go-judge node --version
```

## 常用命令

```bash
# 停止
docker compose down

# 重启
docker compose restart

# 重新构建（更新编译器版本后）
docker compose up -d --build
```

## 说明

- 端口：`5050`（go-judge REST API）
- 并发数：4（可在 docker-compose.yml 中调整 `GOJUDGE_PARALLELISM`）
- 内存限制：1G，CPU 限制：2 核
