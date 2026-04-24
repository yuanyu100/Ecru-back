# Ecru Workspace

Ecru 是一个 AI 穿搭毕业设计工作区，当前仓库由一个 Spring Boot 多模块后端和两个 Vue 3 前端应用组成：

- `frontend/user-app`：用户端
- `frontend/admin-app`：管理端
- `backend`：统一后端入口与业务模块

如果你的目标是本地快速跑通项目，优先看下面两部分：

- 启动说明
- [最短联调与演示流程](./docs/technical/联调与演示流程.md)

## 目录结构

```text
bishe/
├── backend/
│   ├── ecru-common/
│   ├── ecru-user/
│   ├── ecru-clothing/
│   ├── ecru-outfit/
│   ├── ecru-web/
│   └── manual-tests/
├── frontend/
│   ├── user-app/
│   └── admin-app/
├── scripts/
└── docs/
    ├── product/
    ├── technical/
    └── backend-trae/
```

## 运行前准备

后端默认依赖以下本地服务，配置位于 `backend/ecru-web/src/main/resources/application.yml`：

- MySQL：`localhost:3306/ecru`
- PostgreSQL：`localhost:5432/ecru-pg`
- MinIO：`http://localhost:9000`
- Redis：可选，当前配置被注释，不是启动硬依赖

AI 相关能力还依赖环境变量：

- `AI_API_KEY`
- `SILICONFLOW_API_KEY`
- `MCP_WEATHER_API_KEY`

如果这些 key 没有配置好，项目仍可启动，但 AI 对话、图片识别、天气增强、部分向量检索能力可能不可用或不完整。

## 本地依赖脚本

仓库已经提供了部分依赖服务的启停脚本：

### MinIO

```powershell
.\scripts\start-minio.ps1
.\scripts\stop-minio.ps1
```

如果当前 PowerShell 执行策略禁止直接运行 `.ps1`，改用：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-minio.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\stop-minio.ps1
```

启动后默认地址：

- API：`http://127.0.0.1:9000`
- Console：`http://127.0.0.1:9001`
- 用户名：`minioadmin`
- 密码：`minioadmin`

注意：

- 脚本当前依赖本机路径 `D:/Tools/minIO`
- 对应配置见 [start-minio.ps1](D:/Code/TRAE/bishe/scripts/start-minio.ps1)

### Demo Nginx

如果你需要本地打一个接近演示环境的静态入口，可以使用：

```powershell
.\scripts\start-demo-nginx.ps1
.\scripts\stop-demo-nginx.ps1
```

启动后默认地址：

- 用户端：`http://127.0.0.1:8090/`
- 管理端：`http://127.0.0.1:8090/admin/`
- API 代理：`http://127.0.0.1:8090/api/v1/`

注意：

- 脚本会先构建两个前端
- 脚本当前依赖本机路径 `D:/Tools/nginx-1.20.2`

### 环境自检

在正式启动前，建议先跑一次环境检查脚本：

```powershell
.\scripts\check-dev-env.ps1
```

如果直接执行被系统策略拦截，改用：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\check-dev-env.ps1
```

这个脚本会检查：

- Java、Node、Maven、MySQL、PostgreSQL 客户端是否在 PATH 中
- `scripts/` 和 `backend/manual-tests/` 里的关键脚本是否存在
- MySQL、PostgreSQL、MinIO、后端、Demo Nginx 常用端口是否监听
- `AI_API_KEY`、`SILICONFLOW_API_KEY`、`MCP_WEATHER_API_KEY` 是否已配置

### 一键开发启动

如果你希望把 MinIO、后端、两个前端 dev 服务一起拉起，可以使用：

```powershell
.\scripts\start-local-dev.ps1
```

如果本机 PowerShell 提示“禁止运行脚本”，改用：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-local-dev.ps1
```

查看状态：

```powershell
.\scripts\status-local-dev.ps1
```

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\status-local-dev.ps1
```

停止：

```powershell
.\scripts\stop-local-dev.ps1
```

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\stop-local-dev.ps1
```

说明：

- 运行日志和 pid 文件会写入 `logs/dev/`
- 默认启动 MinIO、后端、用户端、管理端
- 也支持按需跳过，例如：

```powershell
.\scripts\start-local-dev.ps1 -SkipAdminApp
```

## 启动方式

### 1. 启动后端

```powershell
cd backend
mvn spring-boot:run -pl ecru-web
```

默认地址：

- API 基础路径：`http://localhost:8081/api/v1`

### 2. 启动用户端

```powershell
cd frontend/user-app
npm install
npm run dev
```

### 3. 启动管理端

```powershell
cd frontend/admin-app
npm install
npm run dev
```

说明：

- 两个前端都已配置 Vite 代理，开发环境下 `/api/v1` 会转发到 `http://localhost:8081`
- 用户端和管理端同时运行时，Vite 会自动分配不同端口
- 本地前后端联调不依赖 Nginx，但演示时可以使用 `scripts/start-demo-nginx.ps1`

## 常用联调素材

仓库里已经放了若干手工联调文件：

- `backend/manual-tests/register-new.json`
- `backend/manual-tests/login-testuser.json`
- `backend/manual-tests/login-zhangsan.json`
- `backend/manual-tests/seed-demo-data.sql`
- `backend/manual-tests/prepare-demo-data.ps1`
- `backend/manual-tests/upgrade-user-role-and-admin-clothing.sql`
- `backend/manual-tests/upload-clothing.html`
- `backend/manual-tests/login.html`

如果你要准备答辩演示，建议直接按 [最短联调与演示流程](./docs/technical/联调与演示流程.md) 走一遍。

## 协作约定

- git 提交描述使用中文
- README 只保留启动和联调入口，详细演示路径放在 `docs/technical/`
- 如果脚本依赖本机固定路径，文档里需要显式写出

## 当前主线

- 主后端：`backend`
- 主用户端：`frontend/user-app`
- 主管理端：`frontend/admin-app`

## 说明

- 旧仓库中的文档已经迁移到 `docs/`
- 手工测试文件统一收敛在 `backend/manual-tests/`
