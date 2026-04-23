# 穿搭系统项目

## 项目概述

这是一个穿搭系统项目，采用完全分离的架构（方案2），将前端和Java后端分为两个独立的仓库，通过API进行通信。

## 仓库结构

```
├── fashion-frontend/          # 前端仓库
│   ├── packages/              # 工作区包目录
│   │   ├── tools/             # 工具类模块
│   │   ├── components/        # 前端组件库
│   │   ├── app/               # 前端应用
│   │   └── shared/            # 共享代码
│   ├── tests/                 # 测试目录
│   └── scripts/               # 脚本目录
├── fashion-backend/           # Java后端仓库
│   ├── fashion-core/          # 核心业务逻辑模块
│   ├── fashion-api/           # API 接口模块
│   ├── fashion-service/       # 业务服务模块
│   ├── fashion-repository/    # 数据访问模块
│   ├── fashion-model/         # 数据模型模块
│   ├── fashion-config/        # 配置管理模块
│   ├── fashion-common/        # 公共工具模块
│   └── fashion-test/          # 测试模块
└── docs/                      # 文档目录
    ├── product/               # 产品相关文档
    └── technical/             # 技术相关文档
```

## 技术栈

### 前端
- **框架**：React / Vue.js
- **状态管理**：Redux / Vuex
- **网络请求**：Axios
- **样式方案**：Tailwind CSS
- **构建工具**：Vite
- **包管理**：pnpm

### Java后端
- **框架**：Spring Boot 3.x
- **数据访问**：Spring Data JPA
- **API 设计**：Spring Web
- **认证**：Spring Security + JWT
- **数据库**：PostgreSQL
- **构建工具**：Maven

## 开发流程

### 前端开发
1. 进入前端仓库：`cd fashion-frontend`
2. 安装依赖：`pnpm install`
3. 启动开发服务器：`pnpm dev`
4. 构建生产版本：`pnpm build`
5. 运行测试：`pnpm test`

### 后端开发
1. 进入后端仓库：`cd fashion-backend`
2. 安装依赖：`mvn install`
3. 启动应用服务器：`mvn spring-boot:run -pl fashion-api`
4. 构建生产版本：`mvn package`
5. 运行测试：`mvn test`

## API 文档

后端API文档可通过以下地址访问：
- 开发环境：`http://localhost:8080/swagger-ui.html`
- 生产环境：`https://your-domain.com/swagger-ui.html`

## 部署策略

### 前端部署
- **静态网站托管**：Vercel / Netlify
- **CDN**：使用 CDN 加速静态资源

### 后端部署
- **云服务**：AWS EC2 / Azure VM
- **容器化**：Docker + Kubernetes
- **PaaS**：Heroku / AWS Elastic Beanstalk

## 监控与维护

### 前端监控
- **性能监控**：Lighthouse
- **错误监控**：Sentry
- **用户行为**：Google Analytics

### 后端监控
- **应用监控**：Spring Boot Actuator
- **性能监控**：Prometheus + Grafana
- **日志管理**：ELK Stack

## 贡献指南

1. **分支管理**：使用 Git Flow 工作流
2. **代码规范**：
   - 前端：ESLint + Prettier
   - 后端：Checkstyle
3. **提交信息**：使用 Conventional Commits
4. **PR 流程**：提交 PR 前确保所有测试通过

## 许可证

本项目采用 MIT 许可证。

## 联系信息

如有问题，请联系项目维护者。