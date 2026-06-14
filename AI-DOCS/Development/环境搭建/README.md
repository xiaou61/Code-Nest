# 环境搭建指南

## 目录说明

本目录存放开发环境搭建相关文档。

## 环境要求

### 必需软件

| 软件 | 版本 | 用途 | 下载地址 |
|------|------|------|---------|
| JDK | 17+ | Java开发 | https://adoptium.net/ |
| Maven | 3.6+ | 构建工具 | https://maven.apache.org/ |
| Node.js | 16+ | 前端开发 | https://nodejs.org/ |
| MySQL | 8.0+ | 数据库 | https://dev.mysql.com/ |
| Redis | 6.0+ | 缓存 | https://redis.io/ |
| Git | 2.30+ | 版本控制 | https://git-scm.com/ |
| IDE | IntelliJ IDEA | 开发工具 | https://www.jetbrains.com/idea/ |

### 可选软件

| 软件 | 版本 | 用途 | 下载地址 |
|------|------|------|---------|
| Docker | 20.10+ | 容器化 | https://www.docker.com/ |
| Navicat | 16+ | 数据库管理 | https://www.navicat.com/ |
| Postman | 最新版 | API测试 | https://www.postman.com/ |
| Redis Desktop Manager | 最新版 | Redis管理 | https://resp.app/ |

## Windows 环境搭建

### 1. 安装 JDK

```bash
# 下载 JDK 17
https://adoptium.net/

# 配置环境变量
JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.x.x
PATH=%JAVA_HOME%\bin;%PATH%

# 验证安装
java -version
javac -version
```

### 2. 安装 Maven

```bash
# 下载 Maven
https://maven.apache.org/download.cgi

# 解压到目录
C:\Program Files\apache-maven-3.8.x

# 配置环境变量
MAVEN_HOME=C:\Program Files\apache-maven-3.8.x
PATH=%MAVEN_HOME%\bin;%PATH%

# 验证安装
mvn -version
```

### 3. 安装 Node.js

```bash
# 下载 Node.js
https://nodejs.org/

# 安装时勾选"Add to PATH"

# 验证安装
node -v
npm -v
```

### 4. 安装 MySQL

```bash
# 下载 MySQL
https://dev.mysql.com/downloads/mysql/

# 安装后配置
# 1. 设置 root 密码
# 2. 创建数据库
mysql -u root -p
CREATE DATABASE code_nest CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 导入数据
mysql -u root -p code_nest < sql/MySql/code_nest.sql
mysql -u root -p code_nest < sql/MySql/code_nest_data.sql
```

### 5. 安装 Redis

```bash
# 下载 Redis
https://github.com/microsoftarchive/redis/releases

# 启动 Redis
redis-server

# 验证安装
redis-cli ping
```

### 6. 安装 IDE

```bash
# 下载 IntelliJ IDEA
https://www.jetbrains.com/idea/

# 安装插件
- Lombok
- MyBatisX
- Spring Boot Helper
```

## macOS 环境搭建

### 1. 安装 Homebrew

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

### 2. 安装软件

```bash
# 安装 JDK
brew install openjdk@17

# 安装 Maven
brew install maven

# 安装 Node.js
brew install node

# 安装 MySQL
brew install mysql
brew services start mysql

# 安装 Redis
brew install redis
brew services start redis

# 安装 Git
brew install git
```

## Linux 环境搭建

### Ubuntu/Debian

```bash
# 更新软件源
sudo apt update

# 安装 JDK
sudo apt install openjdk-17-jdk

# 安装 Maven
sudo apt install maven

# 安装 Node.js
curl -fsSL https://deb.nodesource.com/setup_16.x | sudo -E bash -
sudo apt install nodejs

# 安装 MySQL
sudo apt install mysql-server
sudo mysql_secure_installation

# 安装 Redis
sudo apt install redis-server
```

### CentOS/RHEL

```bash
# 安装 JDK
sudo yum install java-17-openjdk-devel

# 安装 Maven
sudo yum install maven

# 安装 Node.js
curl -fsSL https://rpm.nodesource.com/setup_16.x | sudo bash -
sudo yum install nodejs

# 安装 MySQL
sudo yum install mysql-server
sudo systemctl start mysqld
sudo mysql_secure_installation

# 安装 Redis
sudo yum install redis
sudo systemctl start redis
```

## 项目配置

### 1. 克隆项目

```bash
git clone https://github.com/xiaou/Code-Nest.git
cd Code-Nest
```

### 2. 后端配置

```bash
# 修改数据库配置
vim xiaou-application/src/main/resources/application-dev.yml

# 安装依赖
mvn clean install

# 启动项目
mvn spring-boot:run -pl xiaou-application
```

### 3. 前端配置

```bash
# 用户端
cd vue3-user-front
npm install
npm run dev

# 管理端
cd vue3-admin-front
npm install
npm run dev
```

## 环境验证

### 1. 后端验证

```bash
# 访问接口
curl http://localhost:8080/api/actuator/health

# 查看日志
tail -f logs/app.log
```

### 2. 前端验证

```bash
# 用户端
http://localhost:3001

# 管理端
http://localhost:3000
```

### 3. 数据库验证

```bash
# 连接数据库
mysql -u root -p code_nest

# 查看表
SHOW TABLES;
```

### 4. Redis 验证

```bash
# 连接 Redis
redis-cli

# 测试命令
PING
SET test hello
GET test
```

## 常见问题

### Q: Maven 下载依赖慢？

A: 配置阿里云镜像源

```xml
<!-- settings.xml -->
<mirrors>
  <mirror>
    <id>aliyun</id>
    <mirrorOf>central</mirrorOf>
    <name>Aliyun Mirror</name>
    <url>https://maven.aliyun.com/repository/public</url>
  </mirror>
</mirrors>
```

### Q: npm 安装依赖慢？

A: 使用淘宝镜像

```bash
npm config set registry https://registry.npmmirror.com
```

### Q: MySQL 连接失败？

A: 检查 MySQL 服务是否启动，用户名密码是否正确。

### Q: Redis 连接失败？

A: 检查 Redis 服务是否启动，端口是否正确。

## 版本历史

| 版本 | 日期 | 更新内容 | 作者 |
|------|------|---------|------|
| v1.0.0 | 2026-05-29 | 初始版本 | AI Assistant |
