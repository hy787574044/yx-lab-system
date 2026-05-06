# 阳新化验室管理系统

基于需求文档与功能清单整理的一套前后端分离首版工程，目录如下：

说明：该目录是完全独立的新项目，不引用当前仓库已有模块、私有 Jar、公共包或 Maven 子工程。

- `yx-lab-backend`：Spring Boot 2.7 + MyBatis-Plus + Redis + MySQL 8 后端
- `yx-lab-frontend`：Vue 3 + Vite + Element Plus 前端
- `sql`：初始化表结构与演示数据

首版聚焦化验室主流程闭环：

1. 监测点位、采样计划、采样任务
2. 样品登录与样品台账
3. 检测参数配置、检测分析、结果审核
4. 报告台账、仪器设备、设备维修、文档台账
5. 统计看板与移动端复用接口

详细启动说明见后端与前端目录下 README。

补充说明：

- 后端已新增第三方统一平台鉴权接入模块，包含授权码、Access-Token、用户信息、角色用户、菜单权限等代理接口
- 详细功能描述、配置项与调用示例见 [yx-lab-backend/README.md](</e:/share/yx-lab-system/yx-lab-backend/README.md>)
