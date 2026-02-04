# 用户模块详细设计文档

## 1. 数据库设计

### 1.1 用户表 (users)

#### 表字段说明

| 字段名 | 类型 | 长度 | 是否为空 | 默认值 | 说明 |
|--------|------|------|----------|--------|------|
| id | BIGINT | - | 否 | AUTO_INCREMENT | 主键，用户唯一标识 |
| username | VARCHAR | 50 | 否 | - | 用户名，唯一 |
| password | VARCHAR | 255 | 否 | - | 加密后的密码(BCrypt) |
| email | VARCHAR | 100 | 是 | NULL | 邮箱地址 |
| phone | VARCHAR | 20 | 是 | NULL | 手机号 |
| avatar_url | VARCHAR | 255 | 是 | NULL | 头像URL |
| nickname | VARCHAR | 50 | 是 | NULL | 昵称 |
| gender | TINYINT | - | 是 | NULL | 性别：0-未知，1-男，2-女 |
| birthday | DATE | - | 是 | NULL | 生日 |
| status | TINYINT | - | 否 | 1 | 状态：0-禁用，1-正常 |
| last_login_at | TIMESTAMP | - | 是 | NULL | 最后登录时间 |
| last_login_ip | VARCHAR | 50 | 是 | NULL | 最后登录IP |
| created_at | TIMESTAMP | - | 否 | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | - | 否 | CURRENT_TIMESTAMP | 更新时间 |

#### SQL语句

```sql
-- 创建用户表
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '加密密码',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    avatar_url VARCHAR(255) COMMENT '头像URL',
    nickname VARCHAR(50) COMMENT '昵称',
    gender TINYINT COMMENT '性别：0-未知，1-男，2-女',
    birthday DATE COMMENT '生日',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    last_login_at TIMESTAMP COMMENT '最后登录时间',
    last_login_ip VARCHAR(50) COMMENT '最后登录IP',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_phone (phone),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

### 1.2 用户偏好设置表 (user_settings)

#### 表字段说明

| 字段名 | 类型 | 长度 | 是否为空 | 默认值 | 说明 |
|--------|------|------|----------|--------|------|
| id | BIGINT | - | 否 | AUTO_INCREMENT | 主键 |
| user_id | BIGINT | - | 否 | - | 用户ID，外键 |
| setting_key | VARCHAR | 50 | 否 | - | 设置项键名 |
| setting_value | VARCHAR | 500 | 是 | NULL | 设置项值 |
| created_at | TIMESTAMP | - | 否 | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | - | 否 | CURRENT_TIMESTAMP | 更新时间 |

#### SQL语句

```sql
-- 创建用户偏好设置表
CREATE TABLE user_settings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '设置ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    setting_key VARCHAR(50) NOT NULL COMMENT '设置项键名',
    setting_value VARCHAR(500) COMMENT '设置项值',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_setting (user_id, setting_key),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户偏好设置表';
```

### 1.3 用户登录日志表 (user_login_logs)

#### 表字段说明

| 字段名 | 类型 | 长度 | 是否为空 | 默认值 | 说明 |
|--------|------|------|----------|--------|------|
| id | BIGINT | - | 否 | AUTO_INCREMENT | 主键 |
| user_id | BIGINT | - | 否 | - | 用户ID，外键 |
| login_type | TINYINT | - | 否 | 1 | 登录类型：1-密码，2-短信，3-第三方 |
| login_ip | VARCHAR | 50 | 是 | NULL | 登录IP |
| login_device | VARCHAR | 200 | 是 | NULL | 登录设备信息 |
| login_location | VARCHAR | 100 | 是 | NULL | 登录地点 |
| login_status | TINYINT | - | 否 | 1 | 登录状态：0-失败，1-成功 |
| fail_reason | VARCHAR | 200 | 是 | NULL | 失败原因 |
| created_at | TIMESTAMP | - | 否 | CURRENT_TIMESTAMP | 创建时间 |

#### SQL语句

```sql
-- 创建用户登录日志表
CREATE TABLE user_login_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    login_type TINYINT NOT NULL DEFAULT 1 COMMENT '登录类型：1-密码，2-短信，3-第三方',
    login_ip VARCHAR(50) COMMENT '登录IP',
    login_device VARCHAR(200) COMMENT '登录设备信息',
    login_location VARCHAR(100) COMMENT '登录地点',
    login_status TINYINT NOT NULL DEFAULT 1 COMMENT '登录状态：0-失败，1-成功',
    fail_reason VARCHAR(200) COMMENT '失败原因',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户登录日志表';
```

---

## 2. RESTful API 接口文档

### 基础信息

- **Base URL**: `/api/v1`
- **Content-Type**: `application/json`
- **认证方式**: JWT Token (Header: `Authorization: Bearer {token}`)

### 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1706963200000
}
```

### 错误码定义

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权/Token无效 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 409 | 资源冲突（如用户名已存在）|
| 500 | 服务器内部错误 |

---

## 3. 接口详情

### 3.1 认证相关接口

#### 3.1.1 用户注册

- **接口**: `POST /auth/register`
- **描述**: 新用户注册账号
- **请求头**: 无
- **请求体**:

```json
{
  "username": "zhangsan",
  "password": "123456",
  "email": "zhangsan@example.com",
  "phone": "13800138000"
}
```

- **请求参数说明**:

| 参数名 | 类型 | 是否必填 | 说明 |
|--------|------|----------|------|
| username | String | 是 | 用户名，4-20位字母数字下划线 |
| password | String | 是 | 密码，6-20位 |
| email | String | 否 | 邮箱地址 |
| phone | String | 否 | 手机号 |

- **响应示例(成功)**:

```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "userId": 10001,
    "username": "zhangsan",
    "email": "zhangsan@example.com",
    "createdAt": "2026-02-03T10:30:00"
  },
  "timestamp": 1706963200000
}
```

- **响应示例(失败-用户名已存在)**:

```json
{
  "code": 409,
  "message": "用户名已存在",
  "data": null,
  "timestamp": 1706963200000
}
```

---

#### 3.1.2 用户登录

- **接口**: `POST /auth/login`
- **描述**: 用户登录获取JWT Token
- **请求头**: 无
- **请求体**:

```json
{
  "username": "zhangsan",
  "password": "123456"
}
```

- **请求参数说明**:

| 参数名 | 类型 | 是否必填 | 说明 |
|--------|------|----------|------|
| username | String | 是 | 用户名 |
| password | String | 是 | 密码 |

- **响应示例(成功)**:

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 7200,
    "tokenType": "Bearer",
    "user": {
      "userId": 10001,
      "username": "zhangsan",
      "nickname": "张三",
      "avatarUrl": "https://example.com/avatar.jpg"
    }
  },
  "timestamp": 1706963200000
}
```

- **响应示例(失败-密码错误)**:

```json
{
  "code": 401,
  "message": "用户名或密码错误",
  "data": null,
  "timestamp": 1706963200000
}
```

---

#### 3.1.3 刷新Token

- **接口**: `POST /auth/refresh`
- **描述**: 使用Refresh Token获取新的Access Token
- **请求头**: 无
- **请求体**:

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

- **请求参数说明**:

| 参数名 | 类型 | 是否必填 | 说明 |
|--------|------|----------|------|
| refreshToken | String | 是 | 刷新令牌 |

- **响应示例(成功)**:

```json
{
  "code": 200,
  "message": "刷新成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 7200,
    "tokenType": "Bearer"
  },
  "timestamp": 1706963200000
}
```

---

#### 3.1.4 用户登出

- **接口**: `POST /auth/logout`
- **描述**: 用户登出，使当前Token失效
- **请求头**: `Authorization: Bearer {token}`
- **请求体**: 无
- **响应示例**:

```json
{
  "code": 200,
  "message": "登出成功",
  "data": null,
  "timestamp": 1706963200000
}
```

---

#### 3.1.5 修改密码

- **接口**: `PUT /auth/password`
- **描述**: 修改当前用户密码
- **请求头**: `Authorization: Bearer {token}`
- **请求体**:

```json
{
  "oldPassword": "123456",
  "newPassword": "newpassword123"
}
```

- **请求参数说明**:

| 参数名 | 类型 | 是否必填 | 说明 |
|--------|------|----------|------|
| oldPassword | String | 是 | 旧密码 |
| newPassword | String | 是 | 新密码，6-20位 |

- **响应示例**:

```json
{
  "code": 200,
  "message": "密码修改成功",
  "data": null,
  "timestamp": 1706963200000
}
```

---

### 3.2 用户信息接口

#### 3.2.1 获取当前用户信息

- **接口**: `GET /user/profile`
- **描述**: 获取当前登录用户的详细信息
- **请求头**: `Authorization: Bearer {token}`
- **响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 10001,
    "username": "zhangsan",
    "email": "zhangsan@example.com",
    "phone": "13800138000",
    "avatarUrl": "https://example.com/avatar.jpg",
    "nickname": "张三",
    "gender": 1,
    "birthday": "1995-05-20",
    "status": 1,
    "lastLoginAt": "2026-02-03T10:30:00",
    "createdAt": "2026-01-15T08:00:00"
  },
  "timestamp": 1706963200000
}
```

---

#### 3.2.2 更新用户信息

- **接口**: `PUT /user/profile`
- **描述**: 更新当前用户的个人信息
- **请求头**: `Authorization: Bearer {token}`
- **请求体**:

```json
{
  "nickname": "张三三",
  "email": "zhangsan_new@example.com",
  "phone": "13900139000",
  "gender": 1,
  "birthday": "1995-05-20",
  "avatarUrl": "https://example.com/new_avatar.jpg"
}
```

- **请求参数说明**:

| 参数名 | 类型 | 是否必填 | 说明 |
|--------|------|----------|------|
| nickname | String | 否 | 昵称 |
| email | String | 否 | 邮箱 |
| phone | String | 否 | 手机号 |
| gender | Integer | 否 | 性别：0-未知，1-男，2-女 |
| birthday | String | 否 | 生日，格式：yyyy-MM-dd |
| avatarUrl | String | 否 | 头像URL |

- **响应示例**:

```json
{
  "code": 200,
  "message": "更新成功",
  "data": {
    "userId": 10001,
    "nickname": "张三三",
    "email": "zhangsan_new@example.com",
    "updatedAt": "2026-02-03T11:00:00"
  },
  "timestamp": 1706963200000
}
```

---

#### 3.2.3 上传头像

- **接口**: `POST /user/avatar`
- **描述**: 上传用户头像
- **请求头**: 
  - `Authorization: Bearer {token}`
  - `Content-Type: multipart/form-data`
- **请求参数**:

| 参数名 | 类型 | 是否必填 | 说明 |
|--------|------|----------|------|
| file | File | 是 | 头像图片文件，支持jpg/png，最大2MB |

- **响应示例**:

```json
{
  "code": 200,
  "message": "上传成功",
  "data": {
    "avatarUrl": "https://example.com/avatars/10001_1706963200.jpg"
  },
  "timestamp": 1706963200000
}
```

---

#### 3.2.4 获取用户统计信息

- **接口**: `GET /user/statistics`
- **描述**: 获取当前用户的统计数据（衣物数量、搭配次数等）
- **请求头**: `Authorization: Bearer {token}`
- **响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "clothingCount": 45,
    "outfitCount": 12,
    "favoriteStyle": "日系简约",
    "joinDays": 20
  },
  "timestamp": 1706963200000
}
```

---

### 3.3 用户偏好设置接口

#### 3.3.1 获取用户设置

- **接口**: `GET /user/settings`
- **描述**: 获取用户的所有偏好设置
- **请求头**: `Authorization: Bearer {token}`
- **响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "notification_enabled": "true",
    "theme": "light",
    "language": "zh-CN",
    "privacy_mode": "false"
  },
  "timestamp": 1706963200000
}
```

---

#### 3.3.2 更新用户设置

- **接口**: `PUT /user/settings`
- **描述**: 更新用户的偏好设置
- **请求头**: `Authorization: Bearer {token}`
- **请求体**:

```json
{
  "notification_enabled": "true",
  "theme": "dark"
}
```

- **响应示例**:

```json
{
  "code": 200,
  "message": "设置更新成功",
  "data": {
    "notification_enabled": "true",
    "theme": "dark",
    "language": "zh-CN",
    "privacy_mode": "false"
  },
  "timestamp": 1706963200000
}
```

---

### 3.4 管理员接口（可选）

#### 3.4.1 获取用户列表

- **接口**: `GET /admin/users`
- **描述**: 获取所有用户列表（管理员权限）
- **请求头**: `Authorization: Bearer {admin_token}`
- **查询参数**:

| 参数名 | 类型 | 是否必填 | 说明 |
|--------|------|----------|------|
| page | Integer | 否 | 页码，默认1 |
| size | Integer | 否 | 每页大小，默认10 |
| keyword | String | 否 | 搜索关键词（用户名/邮箱）|
| status | Integer | 否 | 状态筛选：0-禁用，1-正常 |

- **响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 100,
    "page": 1,
    "size": 10,
    "list": [
      {
        "userId": 10001,
        "username": "zhangsan",
        "email": "zhangsan@example.com",
        "status": 1,
        "createdAt": "2026-01-15T08:00:00"
      }
    ]
  },
  "timestamp": 1706963200000
}
```

---

#### 3.4.2 更新用户状态

- **接口**: `PUT /admin/users/{userId}/status`
- **描述**: 启用/禁用用户账号（管理员权限）
- **请求头**: `Authorization: Bearer {admin_token}`
- **请求体**:

```json
{
  "status": 0
}
```

- **响应示例**:

```json
{
  "code": 200,
  "message": "状态更新成功",
  "data": null,
  "timestamp": 1706963200000
}
```

---

## 4. 接口路由汇总

| 方法 | 路径 | 描述 | 认证要求 |
|------|------|------|----------|
| POST | /auth/register | 用户注册 | 无 |
| POST | /auth/login | 用户登录 | 无 |
| POST | /auth/refresh | 刷新Token | 无 |
| POST | /auth/logout | 用户登出 | 需要 |
| PUT | /auth/password | 修改密码 | 需要 |
| GET | /user/profile | 获取用户信息 | 需要 |
| PUT | /user/profile | 更新用户信息 | 需要 |
| POST | /user/avatar | 上传头像 | 需要 |
| GET | /user/statistics | 获取用户统计 | 需要 |
| GET | /user/settings | 获取用户设置 | 需要 |
| PUT | /user/settings | 更新用户设置 | 需要 |
| GET | /admin/users | 获取用户列表 | 管理员 |
| PUT | /admin/users/{userId}/status | 更新用户状态 | 管理员 |

---

## 5. 数据模型定义

### 5.1 User 实体类参考

```java
@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String username;
    private String password;
    private String email;
    private String phone;
    private String avatarUrl;
    private String nickname;
    private Integer gender;
    private LocalDate birthday;
    private Integer status;
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 5.2 DTO 定义

```java
// 注册请求
@Data
public class RegisterRequest {
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "用户名格式不正确")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度6-20位")
    private String password;
    
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}

// 登录请求
@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    private String password;
}

// 登录响应
@Data
@Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private String tokenType;
    private UserInfo user;
}

// 用户信息
@Data
@Builder
public class UserInfo {
    private Long userId;
    private String username;
    private String nickname;
    private String avatarUrl;
}
```

---

**文档版本**: v1.0  
**创建日期**: 2026-02-03  
**最后更新**: 2026-02-03
