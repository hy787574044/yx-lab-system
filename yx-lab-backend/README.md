# yx-lab-backend

## 技术栈

- JDK 1.8
- Spring Boot 2.7.18
- MyBatis-Plus
- MySQL 8
- Redis

## 独立性说明

该项目为全新独立工程，不依赖 `swt-bs` 现有任何模块、包名或 Maven 聚合结构。

## 启动前准备

1. 创建数据库 `yx_lab`，字符集建议 `utf8mb4`
2. 执行 `../sql/init.sql`
3. 修改 `src/main/resources/application.yml` 中的数据库、Redis、文件存储路径

## 启动命令

```bash
mvn spring-boot:run
```

默认账号：

- 用户名：`admin`
- 密码：`Admin@123`

接口文档：

- `http://localhost:8080/swagger-ui/index.html`
