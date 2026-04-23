# 穿搭系统分离结构设计文档（方案2）

## 1. 项目概述

本项目是一个穿搭系统，采用方案2（完全分离结构），将前端和Java后端完全分离为两个独立的仓库，通过API进行通信。这种结构可以充分利用各自技术栈的优势，提高开发效率和代码维护性。

## 2. 仓库结构设计

### 2.1 前端仓库结构

```
├── fashion-frontend/          # 前端仓库根目录
│   ├── .pnpm-workspace.yaml   # pnpm workspace 配置
│   ├── package.json           # 根项目配置
│   ├── docs/                  # 前端文档目录
│   ├── packages/              # 工作区包目录
│   │   ├── tools/             # 工具类模块
│   │   │   ├── log/           # 统一日志库
│   │   │   ├── mock/          # 业务数据 mock 封装
│   │   │   ├── request/       # 网络请求封装
│   │   │   ├── env-config/    # 不同运行环境封装
│   │   │   └── pdf/           # PDF 相关功能
│   │   ├── components/        # 前端组件库
│   │   ├── app/               # 前端应用
│   │   └── shared/            # 共享代码
│   ├── tests/                 # 测试目录
│   └── scripts/               # 脚本目录
```

### 2.2 Java后端仓库结构

```
├── fashion-backend/           # Java后端仓库根目录
│   ├── pom.xml                # Maven 父项目配置
│   ├── .mvn/                  # Maven 配置目录
│   ├── docs/                  # 后端文档目录
│   ├── fashion-core/          # 核心业务逻辑模块
│   │   ├── pom.xml
│   │   └── src/main/java/com/fashion/core/
│   ├── fashion-api/           # API 接口模块
│   │   ├── pom.xml
│   │   └── src/main/java/com/fashion/api/
│   ├── fashion-service/       # 业务服务模块
│   │   ├── pom.xml
│   │   └── src/main/java/com/fashion/service/
│   ├── fashion-repository/    # 数据访问模块
│   │   ├── pom.xml
│   │   └── src/main/java/com/fashion/repository/
│   ├── fashion-model/         # 数据模型模块
│   │   ├── pom.xml
│   │   └── src/main/java/com/fashion/model/
│   ├── fashion-config/        # 配置管理模块
│   │   ├── pom.xml
│   │   └── src/main/java/com/fashion/config/
│   ├── fashion-common/        # 公共工具模块
│   │   ├── pom.xml
│   │   └── src/main/java/com/fashion/common/
│   └── fashion-test/          # 测试模块
│       ├── pom.xml
│       └── src/test/java/com/fashion/test/
```

## 3. 模块职责说明

### 3.1 前端仓库模块职责

#### 3.1.1 工具类模块 (packages/tools/)

##### 3.1.1.1 log (统一日志库)
- **职责**：提供统一的日志记录接口，支持不同执行环境的日志输出
- **功能**：
  - 支持多种日志级别（debug, info, warn, error）
  - 可配置的日志输出格式
  - 支持不同环境的日志策略
  - 支持日志上下文信息的注入

##### 3.1.1.2 mock (业务数据 mock 封装)
- **职责**：针对业务数据进行 mock 封装，提供模拟数据
- **功能**：
  - 基于业务场景的 mock 数据生成
  - 可配置的 mock 规则
  - 支持延迟响应，模拟网络请求耗时
  - 支持动态 mock 数据

##### 3.1.1.3 request (网络请求封装)
- **职责**：封装网络请求，为前端组件提供统一的请求接口
- **功能**：
  - 基于 axios 或 fetch 的封装
  - 统一的请求/响应拦截器
  - 错误处理机制
  - 请求参数序列化
  - 支持请求取消

##### 3.1.1.4 env-config (运行环境封装)
- **职责**：封装不同运行环境的配置
- **功能**：
  - 根据环境变量加载不同配置
  - 支持开发、测试、生产环境的配置切换
  - 配置的集中管理
  - 敏感信息的安全处理

##### 3.1.1.5 pdf (PDF 相关功能)
- **职责**：提供 PDF 生成、解析等相关功能
- **功能**：
  - PDF 生成（如穿搭报告、搭配方案）
  - PDF 解析（如从 PDF 中提取信息）
  - 支持不同模板的 PDF 生成

#### 3.1.2 前端组件库 (packages/components/)
- **职责**：提供可复用的前端组件
- **功能**：
  - 基础 UI 组件（按钮、输入框、卡片等）
  - 业务组件（穿搭展示、搭配推荐等）
  - 组件文档和示例
  - 主题配置

#### 3.1.3 前端应用 (packages/app/)
- **职责**：前端用户界面和交互逻辑
- **功能**：
  - 页面路由管理
  - 状态管理
  - 用户交互处理
  - 与后端 API 交互
  - 响应式设计

#### 3.1.4 共享代码 (packages/shared/)
- **职责**：提供前端内部共享的代码
- **功能**：
  - 类型定义
  - 常量定义
  - 工具函数
  - 共享配置

### 3.2 Java后端仓库模块职责

#### 3.2.1 核心业务逻辑模块 (fashion-core/)
- **职责**：实现核心业务逻辑，与具体技术实现无关
- **功能**：
  - 穿搭推荐算法
  - 搭配规则引擎
  - 业务规则处理
  - 核心业务流程

#### 3.2.2 API 接口模块 (fashion-api/)
- **职责**：提供 RESTful API 接口
- **功能**：
  - 控制器实现
  - 请求参数验证
  - 响应格式统一
  - API 文档生成

#### 3.2.3 业务服务模块 (fashion-service/)
- **职责**：实现业务服务逻辑
- **功能**：
  - 服务接口定义
  - 业务逻辑实现
  - 事务管理
  - 服务组合

#### 3.2.4 数据访问模块 (fashion-repository/)
- **职责**：提供数据访问功能
- **功能**：
  - 数据库交互
  - ORM 映射
  - 数据查询
  - 数据持久化

#### 3.2.5 数据模型模块 (fashion-model/)
- **职责**：定义数据模型
- **功能**：
  - 实体类定义
  - DTO (Data Transfer Object) 定义
  - VO (Value Object) 定义
  - 数据校验规则

#### 3.2.6 配置管理模块 (fashion-config/)
- **职责**：管理应用配置
- **功能**：
  - 配置加载
  - 配置解析
  - 配置验证
  - 环境特定配置

#### 3.2.7 公共工具模块 (fashion-common/)
- **职责**：提供公共工具类和方法
- **功能**：
  - 日志工具
  - 日期工具
  - 字符串工具
  - 加密工具
  - 异常处理

#### 3.2.8 测试模块 (fashion-test/)
- **职责**：提供测试相关功能
- **功能**：
  - 单元测试
  - 集成测试
  - 测试工具类
  - 测试数据准备

## 4. 仓库间通信和集成方式

### 4.1 API 设计规范

#### 4.1.1 RESTful API 规范
- **URL 设计**：使用名词复数，如 `/api/users`、`/api/outfits`
- **HTTP 方法**：使用标准 HTTP 方法（GET、POST、PUT、DELETE、PATCH）
- **状态码**：使用标准 HTTP 状态码
- **请求/响应格式**：使用 JSON 格式
- **版本控制**：在 URL 中包含版本号，如 `/api/v1/users`

#### 4.1.2 API 文档
- 使用 OpenAPI 3.0/Swagger 生成 API 文档
- 前端通过 API 文档了解后端接口
- 后端维护 API 文档的准确性

### 4.2 前后端通信方式

#### 4.2.1 网络请求
- 前端使用 `request` 模块发起 HTTP 请求
- 后端通过 `fashion-api` 模块处理请求
- 使用 HTTPS 协议确保通信安全

#### 4.2.2 认证机制
- 使用 JWT (JSON Web Token) 进行身份认证
- 前端在请求头中携带 token
- 后端验证 token 的有效性

#### 4.2.3 数据传输格式
- 请求和响应数据使用 JSON 格式
- 前端定义与后端一致的数据模型
- 使用 DTO 模式传输数据，避免直接暴露数据库模型

### 4.3 开发环境集成

#### 4.3.1 本地开发
- 前端启动本地开发服务器（如 `pnpm dev`）
- 后端启动本地应用服务器（如 `mvn spring-boot:run`）
- 前端通过配置的 API 地址访问后端服务

#### 4.3.2 Mock 数据
- 前端在开发初期使用 `mock` 模块模拟后端 API 响应
- 当后端 API 就绪后，切换到真实 API

#### 4.3.3 跨域处理
- 后端配置 CORS (Cross-Origin Resource Sharing)
- 允许前端开发服务器的跨域请求

### 4.4 测试集成

#### 4.4.1 单元测试
- 前端使用 Jest、Vitest 等测试框架
- 后端使用 JUnit 5 等测试框架
- 各自独立运行单元测试

#### 4.4.2 集成测试
- 前端使用 Cypress、Playwright 等进行端到端测试
- 后端使用 Spring Boot Test 进行集成测试
- 可以使用测试环境的 API 进行集成测试

## 5. 开发流程建议

### 5.1 前端开发流程

1. **环境搭建**：安装 Node.js、pnpm，克隆前端仓库
2. **依赖安装**：运行 `pnpm install` 安装依赖
3. **模块开发**：
   - 工具模块开发（如 log、request 等）
   - 组件库开发
   - 应用开发
4. **本地运行**：运行 `pnpm dev` 启动开发服务器
5. **测试**：运行 `pnpm test` 执行测试
6. **构建**：运行 `pnpm build` 构建生产版本
7. **代码提交**：遵循 Git 工作流，提交代码到版本控制

### 5.2 Java后端开发流程

1. **环境搭建**：安装 JDK、Maven，克隆后端仓库
2. **依赖安装**：运行 `mvn install` 安装依赖
3. **模块开发**：
   - 数据模型定义
   - 数据访问层开发
   - 业务服务层开发
   - API 接口开发
4. **本地运行**：运行 `mvn spring-boot:run` 启动应用服务器
5. **测试**：运行 `mvn test` 执行测试
6. **构建**：运行 `mvn package` 构建生产版本
7. **代码提交**：遵循 Git 工作流，提交代码到版本控制

### 5.3 协作流程

1. **需求分析**：共同分析产品需求
2. **API 设计**：前后端共同设计 API 接口
3. **并行开发**：前后端并行开发各自功能
4. **联调测试**：使用真实 API 进行联调测试
5. **问题修复**：解决联调过程中发现的问题
6. **集成测试**：执行完整的集成测试
7. **部署上线**：部署到生产环境

## 6. 部署策略

### 6.1 前端部署

#### 6.1.1 构建
- 运行 `pnpm build` 生成静态资源
- 构建产物位于 `packages/app/dist` 目录

#### 6.1.2 部署目标
- **静态网站托管**：如 Vercel、Netlify、GitHub Pages
- **CDN**：使用 CDN 加速静态资源访问
- **容器化**：使用 Docker 容器部署到云服务

#### 6.1.3 环境配置
- 生产环境：配置真实的后端 API 地址
- 使用 `env-config` 模块管理不同环境的配置

### 6.2 Java后端部署

#### 6.2.1 构建
- 运行 `mvn package` 生成可执行 JAR 文件
- 构建产物位于 `fashion-api/target` 目录

#### 6.2.2 部署目标
- **云服务**：如 AWS EC2、Azure VM、阿里云 ECS
- **容器化**：使用 Docker 容器部署到 Kubernetes 集群
- **PaaS**：如 Heroku、AWS Elastic Beanstalk

#### 6.2.3 环境配置
- 生产环境：配置数据库连接、API 密钥等
- 使用 Spring Boot 的配置管理（`application-prod.yml`）

### 6.3 CI/CD 流程

#### 6.3.1 前端 CI/CD
- **持续集成**：使用 GitHub Actions、Jenkins 等
- **自动化测试**：每次代码提交运行测试
- **自动化构建**：测试通过后构建生产版本
- **自动化部署**：构建成功后部署到测试/生产环境

#### 6.3.2 后端 CI/CD
- **持续集成**：使用 GitHub Actions、Jenkins 等
- **自动化测试**：每次代码提交运行测试
- **自动化构建**：测试通过后构建生产版本
- **自动化部署**：构建成功后部署到测试/生产环境

## 7. 版本管理

### 7.1 版本号规范
- 使用语义化版本号（Semantic Versioning）：MAJOR.MINOR.PATCH
- **MAJOR**：不兼容的 API 变更
- **MINOR**：向后兼容的功能添加
- **PATCH**：向后兼容的 bug 修复

### 7.2 版本同步
- 前端和后端可以独立版本化
- 但需要在发布说明中记录版本兼容性
- 建议使用相同的 MAJOR 版本号，确保 API 兼容性

## 8. 监控和维护

### 8.1 前端监控
- **性能监控**：使用 Lighthouse、Web Vitals 等
- **错误监控**：使用 Sentry、LogRocket 等
- **用户行为分析**：使用 Google Analytics 等

### 8.2 后端监控
- **应用监控**：使用 Spring Boot Actuator、Micrometer
- **性能监控**：使用 Prometheus、Grafana 等
- **日志管理**：使用 ELK Stack、Splunk 等
- **健康检查**：实现健康检查端点

### 8.3 故障排查
- 前端错误日志和网络请求日志
- 后端应用日志和数据库日志
- API 调用记录和响应时间
- 服务器资源使用情况

## 9. 总结

采用方案2（完全分离结构）的穿搭系统具有以下优势：

1. **技术栈独立性**：前端和后端可以使用各自最适合的技术栈
2. **开发效率**：前后端可以并行开发，减少相互依赖
3. **代码维护性**：每个仓库的代码结构清晰，职责分明
4. **部署灵活性**：前端和后端可以独立部署，根据需要扩展
5. **团队协作**：不同技术背景的团队可以专注于各自领域

通过合理的 API 设计和集成方式，完全分离的结构可以实现前后端的高效协作，为用户提供更好的穿搭推荐服务。

## 10. 附录

### 10.1 前端技术栈建议
- **框架**：React、Vue.js 或 Angular
- **状态管理**：Redux、Vuex 或 NgRx
- **网络请求**：Axios、Fetch API
- **样式方案**：Tailwind CSS、Styled Components
- **构建工具**：Vite、Webpack
- **测试框架**：Jest、Vitest、Cypress

### 10.2 Java后端技术栈建议
- **框架**：Spring Boot 3.x
- **数据访问**：Spring Data JPA、Hibernate
- **API 设计**：Spring Web、OpenAPI 3.0
- **认证**：Spring Security、JWT
- **数据库**：PostgreSQL、MySQL
- **缓存**：Redis
- **测试**：JUnit 5、Mockito、Spring Boot Test
- **构建工具**：Maven、Gradle

### 10.3 开发工具建议
- **IDE**：VS Code（前端）、IntelliJ IDEA（后端）
- **版本控制**：Git、GitHub/GitLab
- **项目管理**：Jira、Trello
- **协作工具**：Slack、Microsoft Teams
- **文档工具**：Confluence、Notion

### 10.4 最佳实践
- **代码规范**：前端使用 ESLint、Prettier；后端使用 Checkstyle、Spotless
- **Git 工作流**：使用 Git Flow 或 GitHub Flow
- **安全实践**：遵循 OWASP Top 10 安全建议
- **性能优化**：前端使用代码分割、懒加载；后端使用缓存、异步处理
- **可访问性**：遵循 WCAG 2.1 标准，确保应用对所有人可用
