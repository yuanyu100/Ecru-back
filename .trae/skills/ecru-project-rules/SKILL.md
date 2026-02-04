---
name: "ecru-project-rules"
description: "Ecru AI Wardrobe 后端项目开发规范。编写任何代码前必须遵循此规范，包括包结构、命名约定、DTO/VO使用、MapStruct转换、异常处理等。"
---

# Ecru 项目开发规范

## 1. 项目结构规范

### 1.1 模块划分
```
ecru-back/
├── ecru-common/          # 公共模块：工具类、常量、异常、结果封装
├── ecru-user/            # 用户模块：用户相关所有代码
├── ecru-web/             # Web模块：配置、拦截器、启动类
└── pom.xml               # 父POM
```

### 1.2 包结构规范
每个模块必须遵循以下包结构：
```
com.ecru.{module}/
├── controller/           # 控制器层（必须放在业务模块，如ecru-user）
├── service/              # 服务层
│   └── impl/             # 实现类
├── mapper/               # MyBatis Mapper接口
├── entity/               # 数据库实体类
├── dto/                  # 数据传输对象（入参）
├── vo/                   # 视图对象（出参）
├── converter/            # MapStruct转换器
├── config/               # 模块配置
└── constants/            # 模块常量
```

## 2. 命名规范

### 2.1 类命名
| 类型 | 后缀 | 示例 |
|------|------|------|
| 实体类 | 无 | `User`, `Clothing` |
| DTO | DTO | `LoginDTO`, `UpdateUserDTO` |
| VO | VO | `LoginVO`, `UserVO` |
| Mapper | Mapper | `UserMapper` |
| Service | Service | `UserService` |
| Service实现 | ServiceImpl | `UserServiceImpl` |
| Controller | Controller | `AuthController`, `UserController` |
| 转换器 | Converter | `UserConverter` |
| 工具类 | Util/Utils | `JwtUtil`, `DateUtils` |
| 常量类 | Constants | `UserConstants` |
| 异常类 | Exception | `BusinessException` |

### 2.2 方法命名
- **查询单个**: `getById`, `getByUsername`
- **查询列表**: `list`, `listByUserId`
- **分页查询**: `page`, `pageByCondition`
- **新增**: `save`, `insert`
- **修改**: `update`, `updateById`
- **删除**: `remove`, `removeById`
- **业务操作**: `login`, `register`, `uploadAvatar`

### 2.3 变量命名
- 使用驼峰命名法
- 布尔值避免使用 `is` 开头（防止序列化问题）
- 常量使用全大写下划线分隔

## 3. DTO/VO 使用规范

### 3.1 DTO（数据传输对象）
- **用途**: 接收前端传入的参数
- **位置**: `dto` 包
- **命名**: `XxxDTO`
- **必须**: 添加 `@Valid` 校验注解

```java
@Data
@Schema(description = "用户登录请求")
public class LoginDTO {
    @Schema(description = "用户名", example = "zhangsan", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    @Schema(description = "密码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度6-20位")
    private String password;
}
```

### 3.2 VO（视图对象）
- **用途**: 返回给前端的数据
- **位置**: `vo` 包
- **命名**: `XxxVO`
- **建议**: 使用 `@Builder` 构建

```java
@Data
@Builder
@Schema(description = "登录响应")
public class LoginVO {
    @Schema(description = "访问令牌")
    private String accessToken;
    
    @Schema(description = "用户信息")
    private UserInfo user;
    
    @Data
    @Builder
    @Schema(description = "用户信息")
    public static class UserInfo {
        @Schema(description = "用户ID")
        private Long userId;
    }
}
```

### 3.3 校验注解使用
| 类型 | 注解 | 说明 |
|------|------|------|
| String | `@NotBlank`, `@Size`, `@Pattern` | 字符串校验 |
| Integer | `@NotNull`, `@Min`, `@Max` | 数值范围 |
| 集合 | `@NotEmpty`, `@Size` | 集合非空 |
| 通用 | `@NotNull` | 非空校验 |

**⚠️ 重要**: `@Pattern` 只能用于 String 类型，Integer 用 `@Min`/`@Max`

## 4. MapStruct 转换规范

### 4.1 必须使用 MapStruct
**禁止**使用 `BeanUtils.copyProperties`，必须使用 MapStruct。

### 4.2 转换器定义
```java
@Mapper
public interface UserConverter {
    UserConverter INSTANCE = Mappers.getMapper(UserConverter.class);
    
    // DTO -> Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    User toEntity(RegisterDTO dto);
    
    // Entity -> VO
    @Mapping(source = "id", target = "userId")
    RegisterVO toRegisterVO(User user);
}
```

### 4.3 使用方式
```java
// 在Service中使用
RegisterVO vo = UserConverter.INSTANCE.toRegisterVO(user);
```

## 5. Controller 规范

### 5.1 位置要求
- **必须**放在业务模块（如 `ecru-user`），不要放在 `ecru-web`
- 每个模块独立管理自己的 Controller

### 5.2 注解规范
```java
@Tag(name = "用户管理", description = "用户相关接口")           // Swagger标签
@RestController
@RequestMapping("/users")                                      // 统一前缀
@RequiredArgsConstructor                                       // 构造器注入
public class UserController {
    private final UserService userService;
}
```

### 5.3 方法规范
```java
@Operation(summary = "获取用户信息", description = "根据用户ID获取详细信息")
@GetMapping("/{userId}")
public Result<UserVO> getUserInfo(@Parameter(description = "用户ID") @PathVariable Long userId) {
    UserVO vo = userService.getUserInfo(userId);
    return Result.success(vo);
}
```

### 5.4 返回结果
- **统一**使用 `Result<T>` 封装
- **禁止**直接返回实体类或裸数据

## 6. Service 规范

### 6.1 接口定义
```java
public interface UserService {
    LoginVO login(LoginDTO dto);
    User register(RegisterDTO dto);
}
```

### 6.2 实现类
```java
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    
    @Override
    public LoginVO login(LoginDTO dto) {
        // 业务逻辑
    }
}
```

## 7. Mapper 规范

### 7.1 XML 位置
**必须**放在 `resources/mapper/` 目录下，禁止放在 `java` 目录。

```
src/main/resources/
└── mapper/
    └── UserMapper.xml
```

### 7.2 配置
```yaml
mybatis-plus:
  mapper-locations: classpath:/mapper/**/*.xml
```

## 8. 异常处理规范

### 8.1 统一异常处理
使用 `@RestControllerAdvice` 统一处理异常。

### 8.2 业务异常
```java
public class BusinessException extends RuntimeException {
    private final Integer code;
    
    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }
    
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }
}
```

## 9. 统一结果封装

### 9.1 Result 结构
```java
@Data
public class Result<T> {
    private Integer code;       // 200成功，其他失败
    private String message;     // 提示信息
    private T data;            // 数据
    private Long timestamp;    // 时间戳
}
```

### 9.2 使用方式
```java
// 成功
return Result.success(data);
return Result.success("操作成功", data);

// 失败
return Result.error("操作失败");
return Result.error(400, "参数错误");
```

## 10. Swagger 文档规范

### 10.1 必填项
- `@Tag`: 控制器类必须添加
- `@Operation`: 每个方法必须添加
- `@Schema`: DTO/VO字段必须添加

### 10.2 示例
```java
@Schema(description = "用户名", example = "zhangsan", requiredMode = Schema.RequiredMode.REQUIRED)
private String username;
```

## 11. 依赖注入规范

### 11.1 推荐方式
使用构造器注入（配合 `@RequiredArgsConstructor`）：
```java
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
}
```

### 11.2 禁止
- 禁止使用 `@Autowired` 字段注入
- 禁止使用 `BeanUtils.copyProperties`

## 12. 日志规范

### 12.1 使用 Lombok
```java
@Slf4j
public class UserServiceImpl {
    public void method() {
        log.info("用户登录成功: {}", username);
        log.error("用户登录失败", e);
    }
}
```

## 13. 配置文件规范

### 13.1 多环境配置
```
application.yml          # 公共配置
application-dev.yml      # 开发环境
application-prod.yml     # 生产环境
```

### 13.2 敏感信息
数据库密码等敏感信息使用环境变量或配置中心，禁止硬编码。

## 14. 代码检查清单

提交代码前检查：
- [ ] 是否遵循包结构规范
- [ ] DTO/VO 命名是否正确
- [ ] 是否使用 MapStruct 而非 copyProperties
- [ ] Controller 是否在正确的模块
- [ ] Mapper XML 是否在 resources 目录
- [ ] 是否添加了 Swagger 文档注解
- [ ] 校验注解是否使用正确（@Pattern 只用于 String）
- [ ] 返回是否使用 Result 封装
- [ ] 是否使用构造器注入
- [ ] 代码是否格式化

## 15. 示例代码模板

### 15.1 新增一个完整接口

**1. DTO (LoginDTO.java)**
```java
@Data
@Schema(description = "登录请求")
public class LoginDTO {
    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}
```

**2. VO (LoginVO.java)**
```java
@Data
@Builder
@Schema(description = "登录响应")
public class LoginVO {
    @Schema(description = "访问令牌")
    private String accessToken;
    
    @Schema(description = "用户信息")
    private UserInfo user;
}
```

**3. Controller (AuthController.java)**
```java
@Tag(name = "认证管理")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        LoginVO vo = userService.login(dto);
        return Result.success("登录成功", vo);
    }
}
```

**4. Service 接口**
```java
public interface UserService {
    LoginVO login(LoginDTO dto);
}
```

**5. Service 实现**
```java
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;
    
    @Override
    public LoginVO login(LoginDTO dto) {
        // 业务逻辑
        return LoginVO.builder()
            .accessToken(token)
            .build();
    }
}
```
