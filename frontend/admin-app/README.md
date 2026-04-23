# Ecru Admin App

后台应用骨架，当前主要包含：

- 管理员登录
- 用户列表与启用/禁用
- 当前账号衣物列表与删除
- AI 监控面板

开发启动：

```powershell
npm install
npm run dev
```

默认通过 Vite 代理把 `/api/v1` 转发到 `http://localhost:8081`。
