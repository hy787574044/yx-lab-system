# yx-lab-frontend

## 启动

```bash
npm install
npm run dev
```

默认联调地址：

- 前端：`http://localhost:5173`
- 后端：`http://localhost:9010`

该前端为独立 Vite 工程，不依赖当前仓库已有前端代码。

默认账号：

- `admin / Admin@123`

## 地图配置

采样计划手工点位和采样任务导航使用天地图。启动或打包前请在前端环境变量中配置：

```bash
VITE_TIANDITU_TK=你的天地图应用密钥
```
