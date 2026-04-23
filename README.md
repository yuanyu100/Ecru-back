# Ecru Workspace

当前工作区已经收敛成一个主后端和一个前端工作区。前端工作区里按用途拆分用户端与后台端，便于后续继续扩展 H5、小程序或管理后台。

## 目录结构

```text
bishe/
├─ backend/                  # 当前主后端（Spring Boot 多模块）
│  ├─ ecru-common/
│  ├─ ecru-user/
│  ├─ ecru-clothing/
│  ├─ ecru-outfit/
│  ├─ ecru-web/
│  └─ manual-tests/          # 手工联调脚本、HTML、SQL 样例
├─ frontend/                 # 前端工作区
│  ├─ user-app/              # 当前主用户端（Vue 3 + Vite）
│  └─ admin-prototype/       # 后台原型，含较多 mock/占位内容
├─ docs/
│  ├─ product/
│  ├─ technical/
│  ├─ backend-trae/
│  └─ legacy-ecru-readme.md
```

## 当前主线

- 主后端：`backend`
- 主用户端：`frontend/user-app`
- `frontend/admin-prototype` 目前仅作后台原型参考，不作为主联调入口

## 启动方式

### 后端

```powershell
cd backend
mvn spring-boot:run -pl ecru-web
```

默认地址：

- `http://localhost:8081/api/v1`

### 前端

```powershell
cd frontend
cd user-app
npm install
npm run dev
```

说明：

- 前端已配置 Vite 代理，开发环境下 `/api/v1` 会自动转发到 `http://localhost:8081`
- 本地前后端联调不需要额外安装 Nginx
- 如果后面要做管理后台和 H5，建议继续保留“一个前端工作区，多个前端应用”的结构，不建议强行塞进同一个单页应用

## 说明

- 旧总仓 `Ecru` 中的产品/技术文档已迁到 `docs/`
- 后端隐藏目录 `.trae/doc` 中有价值的设计文档已迁到 `docs/backend-trae/`
- 手工测试文件已集中到 `backend/manual-tests/`
