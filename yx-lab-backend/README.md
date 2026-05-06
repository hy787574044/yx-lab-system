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

## 第三方统一平台鉴权接入

### 功能描述

项目已新增一套独立的第三方统一平台接入代码，位于 `com.yx.lab.modules.unified` 包下，不影响现有系统内部登录逻辑。

当前接入能力包括：

- 获取授权码
- 根据授权码获取 Access-Token
- 根据 Refresh-Token 刷新 Access-Token
- 回收 Access-Token
- 根据 Access-Token 和 `open_id` 获取账号信息
- 查询用户信息
- 根据用户 ID 获取用户信息
- 根据用户工号获取用户信息
- 根据角色 ID 获取用户列表
- 根据用户 ID 获取用户权限菜单

### 配置说明

第三方统一平台相关配置位于 `src/main/resources/application.yml` 的 `lab.unified` 节点：

```yml
lab:
  unified:
    base-url: http://172.168.1.55:8187
    api-token:
    client-id:
    client-secret:
    scope:
    response-type: code
    timeout: 10000
    headers:
      x-ltdno-header:
      x-tenant-schema:
      x-tenantid-header:
      x-userid-header:
      x-username-header:
```

说明：

- `base-url`：第三方统一平台基础地址
- `api-token`：第三方要求的 `HDataApiToken`
- `client-id` / `client-secret`：应用凭证
- `scope`：授权范围
- `response-type`：默认 `code`
- `headers`：第三方接口要求的公共请求头

### 本地代理接口

后端统一对外提供以下本地代理接口，默认前缀为 `http://localhost:8080/api/unified`：

- `GET /oauth/authorize`：获取授权码
- `GET /oauth/token`：根据授权码获取 Access-Token
- `GET /oauth/refresh`：根据 Refresh-Token 刷新 Access-Token
- `GET /oauth/revoke`：回收 Access-Token
- `GET /oauth/userinfo`：根据 `accessToken` 和 `openId` 获取账号信息
- `GET /users/query`：根据 `jobNo` 和 `mobile` 查询用户信息
- `GET /users/by-id`：根据 `id` 获取用户信息
- `GET /users/by-job-no`：根据 `jobNo` 获取用户信息
- `GET /users/by-role-id`：根据 `roleId` 获取用户列表
- `GET /users/menus`：根据 `id` 获取用户权限菜单

### 调用说明

- 当前本地代理接口均为 `GET`
- 请求参数通过 query string 传递，不使用 JSON body
- 第三方所需的公共请求头由后端从 `application.yml` 自动代发
- 如果当前系统开启了自身登录鉴权，调用这些本地代理接口时仍需带本系统的 `Authorization` 头
- 返回结果中保留了 `rawBody` 和 `raw` 字段，便于联调时查看第三方原始响应

### 调用示例

获取授权码：

```bash
curl -G "http://localhost:8080/api/unified/oauth/authorize" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  --data-urlencode "responseType=code" \
  --data-urlencode "clientId=your-client-id" \
  --data-urlencode "scope=your-scope"
```

根据授权码获取 Access-Token：

```bash
curl -G "http://localhost:8080/api/unified/oauth/token" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  --data-urlencode "code=AUTH_CODE"
```

根据 Refresh-Token 刷新 Access-Token：

```bash
curl -G "http://localhost:8080/api/unified/oauth/refresh" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  --data-urlencode "refreshToken=REFRESH_TOKEN"
```

回收 Access-Token：

```bash
curl -G "http://localhost:8080/api/unified/oauth/revoke" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  --data-urlencode "accessToken=ACCESS_TOKEN"
```

根据 Access-Token 和 `openId` 获取账号信息：

```bash
curl -G "http://localhost:8080/api/unified/oauth/userinfo" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  --data-urlencode "accessToken=ACCESS_TOKEN" \
  --data-urlencode "openId=OPEN_ID"
```

查询用户信息：

```bash
curl -G "http://localhost:8080/api/unified/users/query" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  --data-urlencode "jobNo=10001" \
  --data-urlencode "mobile=13800138000"
```

根据用户 ID 获取用户信息：

```bash
curl -G "http://localhost:8080/api/unified/users/by-id" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  --data-urlencode "id=123456"
```

根据用户工号获取用户信息：

```bash
curl -G "http://localhost:8080/api/unified/users/by-job-no" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  --data-urlencode "jobNo=10001"
```

根据角色 ID 获取用户列表：

```bash
curl -G "http://localhost:8080/api/unified/users/by-role-id" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  --data-urlencode "roleId=ROLE_001"
```

根据用户 ID 获取用户权限菜单：

```bash
curl -G "http://localhost:8080/api/unified/users/menus" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  --data-urlencode "id=123456"
```
