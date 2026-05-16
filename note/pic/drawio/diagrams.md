# 第三章配套图表 PlantUML 代码

使用方式：复制各代码块到 https://www.plantuml.com/plantuml/uml/ 或 VS Code PlantUML 插件渲染后截图插入论文。

---

## 图3-1 系统角色与核心用例图（总览）

```plantuml
@startuml usecase_overview
!theme plain
skinparam defaultFontName "SimHei"
skinparam defaultFontSize 12
skinparam usecase {
    BackgroundColor #ebf8ff
    BorderColor #4299e1
    FontColor #1a365d
}
skinparam actor {
    BackgroundColor #e6fffa
    BorderColor #38b2ac
    FontColor #1d4044
    FontStyle bold
}
skinparam rectangle {
    BackgroundColor #f7fafc
    BorderColor #718096
}

left to right direction

actor "普通用户" as U
actor "管理员"   as A

rectangle "Ecru 智能穿搭系统" {
    package "用户端功能" {
        usecase "注册 / 登录"       as UC1
        usecase "维护个人资料"       as UC2
        usecase "衣物建档与管理"     as UC3
        usecase "AI 识别衣物属性"    as UC4
        usecase "风格偏好学习"       as UC5
        usecase "查看首页穿搭推荐"   as UC6
        usecase "对推荐进行反馈"     as UC7
        usecase "AI 多轮对话咨询"    as UC8
        usecase "查看历史对话"       as UC9
    }
    package "管理端功能" {
        usecase "查看用户数据"       as UC10
        usecase "查看衣物台账"       as UC11
        usecase "知识库条目管理"     as UC12
        usecase "配置 AI 提示词"     as UC13
        usecase "AI 运行监控"        as UC14
    }
}

U --> UC1
U --> UC2
U --> UC3
UC3 ..> UC4 : <<include>>
U --> UC5
U --> UC6
UC6 ..> UC7 : <<extend>>
U --> UC8
UC8 ..> UC9 : <<extend>>

A --> UC10
A --> UC11
A --> UC12
A --> UC13
A --> UC14
@enduml
```

---

## 图3-2 账号管理用例图

```plantuml
@startuml usecase_account
!theme plain
skinparam defaultFontName "SimHei"
skinparam defaultFontSize 12
skinparam usecase { BackgroundColor #ebf8ff; BorderColor #4299e1 }
skinparam actor { BackgroundColor #e6fffa; BorderColor #38b2ac; FontStyle bold }

left to right direction

actor "普通用户" as U

rectangle "账号管理" {
    usecase "用户注册"         as UC1
    usecase "用户登录"         as UC2
    usecase "刷新 Token"       as UC3
    usecase "查看个人资料"     as UC4
    usecase "修改个人资料"     as UC5
    usecase "修改头像"         as UC6
}

U --> UC1
U --> UC2
UC2 ..> UC3 : <<extend>>
U --> UC4
U --> UC5
UC5 ..> UC6 : <<extend>>
@enduml
```

---

## 图3-3 衣物管理用例图

```plantuml
@startuml usecase_clothing
!theme plain
skinparam defaultFontName "SimHei"
skinparam defaultFontSize 12
skinparam usecase { BackgroundColor #ebf8ff; BorderColor #4299e1 }
skinparam actor { BackgroundColor #e6fffa; BorderColor #38b2ac; FontStyle bold }

left to right direction

actor "普通用户" as U
actor "AI 服务"  as AI

rectangle "衣物管理" {
    usecase "上传衣物图片"       as UC1
    usecase "AI 自动识别属性"    as UC2
    usecase "手动填写属性"       as UC3
    usecase "保存衣物信息"       as UC4
    usecase "编辑衣物信息"       as UC5
    usecase "删除衣物"           as UC6
    usecase "查看衣物列表"       as UC7
    usecase "查看衣物详情"       as UC8
}

U  --> UC1
UC1 ..> UC2 : <<extend>>
UC1 ..> UC3 : <<extend>>
UC2 --> AI
UC2 ..> UC4 : <<include>>
UC3 ..> UC4 : <<include>>
U  --> UC5
U  --> UC6
U  --> UC7
UC7 ..> UC8 : <<extend>>
@enduml
```

---

## 图3-4 风格偏好用例图

```plantuml
@startuml usecase_style
!theme plain
skinparam defaultFontName "SimHei"
skinparam defaultFontSize 12
skinparam usecase { BackgroundColor #ebf8ff; BorderColor #4299e1 }
skinparam actor { BackgroundColor #e6fffa; BorderColor #38b2ac; FontStyle bold }

left to right direction

actor "普通用户" as U

rectangle "风格偏好" {
    usecase "浏览风格图片库"       as UC1
    usecase "标记喜欢 / 不喜欢"    as UC2
    usecase "跳过图片"             as UC3
    usecase "查看个人风格画像"     as UC4
    usecase "系统更新偏好模型"     as UC5
}

U  --> UC1
UC1 ..> UC2 : <<extend>>
UC1 ..> UC3 : <<extend>>
UC2 ..> UC5 : <<include>>
U  --> UC4
@enduml
```

---

## 图3-5 穿搭推荐用例图

```plantuml
@startuml usecase_outfit
!theme plain
skinparam defaultFontName "SimHei"
skinparam defaultFontSize 12
skinparam usecase { BackgroundColor #ebf8ff; BorderColor #4299e1 }
skinparam actor { BackgroundColor #e6fffa; BorderColor #38b2ac; FontStyle bold }

left to right direction

actor "普通用户" as U
actor "AI 服务"  as AI

rectangle "穿搭推荐" {
    usecase "查看首页推荐"         as UC1
    usecase "查看推荐详情"         as UC2
    usecase "对推荐评分 / 反馈"    as UC3
    usecase "AI 生成穿搭方案"      as UC4
    usecase "获取天气信息"         as UC5
}

U  --> UC1
UC1 ..> UC4 : <<include>>
UC4 --> AI
UC4 ..> UC5 : <<include>>
UC1 ..> UC2 : <<extend>>
UC2 ..> UC3 : <<extend>>
@enduml
```

---

## 图3-6 AI 对话用例图

```plantuml
@startuml usecase_chat
!theme plain
skinparam defaultFontName "SimHei"
skinparam defaultFontSize 12
skinparam usecase { BackgroundColor #ebf8ff; BorderColor #4299e1 }
skinparam actor { BackgroundColor #e6fffa; BorderColor #38b2ac; FontStyle bold }

left to right direction

actor "普通用户" as U
actor "AI 服务"  as AI

rectangle "AI 对话" {
    usecase "发起新对话"           as UC1
    usecase "发送消息"             as UC2
    usecase "穿搭咨询"             as UC3
    usecase "面料 / 知识检索"      as UC4
    usecase "查看历史对话列表"     as UC5
    usecase "查看对话详情"         as UC6
    usecase "检索知识库"           as UC7
}

U  --> UC1
UC1 ..> UC2 : <<include>>
UC2 ..> UC3 : <<extend>>
UC2 ..> UC4 : <<extend>>
UC3 --> AI
UC4 --> AI
AI ..> UC7 : <<include>>
U  --> UC5
UC5 ..> UC6 : <<extend>>
@enduml
```

---

## 图3-7 管理端用例图

```plantuml
@startuml usecase_admin
!theme plain
skinparam defaultFontName "SimHei"
skinparam defaultFontSize 12
skinparam usecase { BackgroundColor #fff5f5; BorderColor #fc8181 }
skinparam actor { BackgroundColor #fff5f5; BorderColor #e53e3e; FontStyle bold }

left to right direction

actor "管理员" as A

rectangle "管理端" {
    package "数据查看" {
        usecase "查看用户列表"       as UC1
        usecase "查看用户详情"       as UC2
        usecase "查看衣物台账"       as UC3
        usecase "查看对话日志"       as UC4
        usecase "查看穿搭记录"       as UC5
    }
    package "内容维护" {
        usecase "新增知识条目"       as UC6
        usecase "编辑知识条目"       as UC7
        usecase "删除知识条目"       as UC8
        usecase "配置 AI 提示词"     as UC9
    }
    package "运行监控" {
        usecase "查看 API 调用统计"  as UC10
        usecase "查看调用记录详情"   as UC11
    }
}

A --> UC1
UC1 ..> UC2 : <<extend>>
A --> UC3
A --> UC4
A --> UC5
A --> UC6
A --> UC7
A --> UC8
A --> UC9
A --> UC10
UC10 ..> UC11 : <<extend>>
@enduml
```

---

## 图3-8 系统整体 ER 图

```plantuml
@startuml er_full
!theme plain
skinparam defaultFontName "SimHei"
skinparam defaultFontSize 11
skinparam linetype ortho

entity "users\n用户" as users {
    * id : BIGINT <<PK>>
    --
    username : VARCHAR
    password : VARCHAR
    email : VARCHAR
    phone : VARCHAR
    avatar_url : VARCHAR
    nickname : VARCHAR
    role : VARCHAR
    gender : INT
    birthday : DATE
    status : INT
    last_login_at : DATETIME
    created_at : DATETIME
    updated_at : DATETIME
}

entity "user_settings\n用户设置" as user_settings {
    * id : BIGINT <<PK>>
    --
    * user_id : BIGINT <<FK>>
    setting_key : VARCHAR
    setting_value : VARCHAR
    created_at : DATETIME
    updated_at : DATETIME
}

entity "user_login_logs\n登录日志" as user_login_logs {
    * id : BIGINT <<PK>>
    --
    user_id : BIGINT <<FK>>
    login_type : INT
    login_ip : VARCHAR
    login_device : VARCHAR
    login_status : INT
    fail_reason : VARCHAR
    created_at : DATETIME
}

entity "user_style_archives\n风格档案" as user_style_archives {
    * id : BIGINT <<PK>>
    --
    * user_id : BIGINT <<FK>>
    temperament_type : VARCHAR
    height_cm : INT
    weight_kg : INT
    body_type : VARCHAR
    skin_tone : VARCHAR
    preferred_styles : JSON
    avoided_styles : JSON
    preferred_colors : JSON
    is_test_completed : BOOL
    created_at : DATETIME
    updated_at : DATETIME
}

entity "style_tags\n风格标签" as style_tags {
    * id : BIGINT <<PK>>
    --
    name : VARCHAR
    category : VARCHAR
    is_preset : BOOL
    description : VARCHAR
    usage_count : INT
    created_at : DATETIME
    updated_at : DATETIME
}

entity "style_images\n风格图片" as style_images {
    * id : BIGINT <<PK>>
    --
    image_url : VARCHAR
    title : VARCHAR
    style_category : VARCHAR
    is_active : BOOL
    created_at : DATETIME
    updated_at : DATETIME
}

entity "style_image_tags\n图片标签关联" as style_image_tags {
    * id : BIGINT <<PK>>
    --
    * image_id : BIGINT <<FK>>
    * style_tag_id : BIGINT <<FK>>
    confidence : DECIMAL
    created_at : DATETIME
}

entity "user_style_profiles\n用户偏好分数" as user_style_profiles {
    * id : BIGINT <<PK>>
    --
    * user_id : BIGINT <<FK>>
    * style_tag_id : BIGINT <<FK>>
    preference_score : DECIMAL
    interaction_count : INT
    updated_at : DATETIME
}

entity "user_style_preference_logs\n偏好操作日志" as user_style_preference_logs {
    * id : BIGINT <<PK>>
    --
    * user_id : BIGINT <<FK>>
    * image_id : BIGINT <<FK>>
    preference_type : INT
    created_at : DATETIME
}

entity "clothings\n衣物" as clothings {
    * id : BIGINT <<PK>>
    --
    * user_id : BIGINT <<FK>>
    name : VARCHAR
    category : VARCHAR
    sub_category : VARCHAR
    primary_color : VARCHAR
    material : VARCHAR
    style_tags : VARCHAR
    occasion_tags : VARCHAR
    season_tags : VARCHAR
    frequency_level : INT
    wear_count : INT
    image_url : VARCHAR
    purchase_price : DECIMAL
    purchase_date : DATE
    brand : VARCHAR
    source_type : VARCHAR
    ai_confidence : DECIMAL
    is_deleted : BOOL
    created_at : DATETIME
    updated_at : DATETIME
}

entity "clothing_wear_logs\n穿着日志" as clothing_wear_logs {
    * id : BIGINT <<PK>>
    --
    * clothing_id : BIGINT <<FK>>
    * user_id : BIGINT <<FK>>
    worn_at : DATE
    outfit_id : BIGINT
    weather_condition : VARCHAR
    temperature : DECIMAL
    notes : VARCHAR
    created_at : DATETIME
}

entity "outfit_advice_records\n穿搭建议记录" as outfit_advice_records {
    * id : BIGINT <<PK>>
    --
    * user_id : BIGINT <<FK>>
    input_type : INT
    location : VARCHAR
    temperature : DECIMAL
    weather_condition : VARCHAR
    season : VARCHAR
    outfit_name : VARCHAR
    outfit_description : VARCHAR
    reasoning : VARCHAR
    occasion : VARCHAR
    suitability_score : DECIMAL
    is_favorite : BOOL
    is_deleted : BOOL
    created_at : DATETIME
    updated_at : DATETIME
}

entity "outfit_items\n搭配单品" as outfit_items {
    * id : BIGINT <<PK>>
    --
    * outfit_advice_id : BIGINT <<FK>>
    clothing_id : BIGINT <<FK>>
    item_name : VARCHAR
    item_category : VARCHAR
    item_color : VARCHAR
    is_recommended : BOOL
    reason : VARCHAR
    sort_order : INT
    created_at : DATETIME
}

entity "outfit_feedback\n搭配反馈" as outfit_feedback {
    * id : BIGINT <<PK>>
    --
    * outfit_advice_id : BIGINT <<FK>>
    * user_id : BIGINT <<FK>>
    overall_rating : INT
    style_rating : INT
    practicality_rating : INT
    weather_rating : INT
    is_worn : BOOL
    feedback_text : VARCHAR
    created_at : DATETIME
    updated_at : DATETIME
}

entity "ai_conversations\nAI 会话" as ai_conversations {
    * id : BIGINT <<PK>>
    --
    * user_id : BIGINT <<FK>>
    session_id : VARCHAR
    title : VARCHAR
    context : VARCHAR
    is_active : BOOL
    message_count : INT
    created_at : DATETIME
    updated_at : DATETIME
}

entity "ai_chat_messages\nAI 消息" as ai_chat_messages {
    * id : BIGINT <<PK>>
    --
    * conversation_id : BIGINT <<FK>>
    * user_id : BIGINT <<FK>>
    role : VARCHAR
    content : TEXT
    message_type : VARCHAR
    recommendations : TEXT
    context_snapshot : TEXT
    created_at : DATETIME
}

entity "ai_api_call_record\nAI 调用记录" as ai_api_call_record {
    * id : BIGINT <<PK>>
    --
    scene : VARCHAR
    model : VARCHAR
    request_id : VARCHAR
    user_id : BIGINT <<FK>>
    status : TINYINT
    response_time : BIGINT
    input_tokens : INT
    output_tokens : INT
    total_tokens : INT
    created_at : DATETIME
    create_date : VARCHAR
}

entity "ai_prompt_settings\nAI 提示词配置" as ai_prompt_settings {
    * id : BIGINT <<PK>>
    --
    setting_key : VARCHAR
    setting_value : LONGTEXT
    description : VARCHAR
    updated_by : BIGINT <<FK>>
    created_at : DATETIME
    updated_at : DATETIME
}

' ---- 关系 ----
users           ||--o{ user_settings              : "1..N"
users           ||--o{ user_login_logs            : "1..N"
users           ||--o|  user_style_archives       : "1..1"
users           ||--o{ user_style_profiles        : "1..N"
users           ||--o{ user_style_preference_logs : "1..N"
users           ||--o{ clothings                  : "1..N"
users           ||--o{ clothing_wear_logs         : "1..N"
users           ||--o{ outfit_advice_records      : "1..N"
users           ||--o{ outfit_feedback            : "1..N"
users           ||--o{ ai_conversations           : "1..N"
users           ||--o{ ai_chat_messages           : "1..N"

style_images    ||--o{ style_image_tags           : "1..N"
style_tags      ||--o{ style_image_tags           : "1..N"
style_tags      ||--o{ user_style_profiles        : "1..N"
style_images    ||--o{ user_style_preference_logs : "1..N"

clothings       ||--o{ clothing_wear_logs         : "1..N"
clothings       ||--o{ outfit_items               : "0..N"

outfit_advice_records ||--o{ outfit_items         : "1..N"
outfit_advice_records ||--o{ outfit_feedback      : "1..N"

ai_conversations ||--o{ ai_chat_messages          : "1..N"
@enduml
```

---

## 图3-9 用户模块局部 ER 图

```plantuml
@startuml er_user
!theme plain
skinparam defaultFontName "SimHei"
skinparam defaultFontSize 11
skinparam linetype ortho

entity "users\n用户" as users {
    * id : BIGINT <<PK>>
    --
    username : VARCHAR
    password : VARCHAR
    email / phone : VARCHAR
    nickname / avatar_url : VARCHAR
    role : VARCHAR
    gender / birthday : -
    status : INT
    created_at / updated_at : DATETIME
}

entity "user_settings\n用户设置" as user_settings {
    * id : BIGINT <<PK>>
    --
    * user_id : BIGINT <<FK>>
    setting_key : VARCHAR
    setting_value : VARCHAR
}

entity "user_login_logs\n登录日志" as user_login_logs {
    * id : BIGINT <<PK>>
    --
    user_id : BIGINT <<FK>>
    login_type / login_ip : -
    login_device / login_status : -
    created_at : DATETIME
}

entity "user_style_archives\n风格档案" as user_style_archives {
    * id : BIGINT <<PK>>
    --
    * user_id : BIGINT <<FK>>
    temperament_type : VARCHAR
    height_cm / weight_kg : INT
    body_type / skin_tone : VARCHAR
    preferred_styles : JSON
    avoided_styles : JSON
    is_test_completed : BOOL
}

entity "user_style_profiles\n偏好分数" as user_style_profiles {
    * id : BIGINT <<PK>>
    --
    * user_id : BIGINT <<FK>>
    * style_tag_id : BIGINT <<FK>>
    preference_score : DECIMAL
    interaction_count : INT
}

entity "style_tags\n风格标签" as style_tags {
    * id : BIGINT <<PK>>
    --
    name : VARCHAR
    category : VARCHAR
    is_preset : BOOL
}

entity "style_images\n风格图片" as style_images {
    * id : BIGINT <<PK>>
    --
    image_url : VARCHAR
    title : VARCHAR
    style_category : VARCHAR
    is_active : BOOL
}

entity "style_image_tags\n图片标签关联" as style_image_tags {
    * id : BIGINT <<PK>>
    --
    * image_id : BIGINT <<FK>>
    * style_tag_id : BIGINT <<FK>>
    confidence : DECIMAL
}

entity "user_style_preference_logs\n偏好操作日志" as user_style_preference_logs {
    * id : BIGINT <<PK>>
    --
    * user_id : BIGINT <<FK>>
    * image_id : BIGINT <<FK>>
    preference_type : INT
}

users ||--o{ user_settings              : ""
users ||--o{ user_login_logs            : ""
users ||--o|  user_style_archives       : ""
users ||--o{ user_style_profiles        : ""
users ||--o{ user_style_preference_logs : ""
style_tags   ||--o{ user_style_profiles        : ""
style_tags   ||--o{ style_image_tags           : ""
style_images ||--o{ style_image_tags           : ""
style_images ||--o{ user_style_preference_logs : ""
@enduml
```

---

## 图3-10 衣物与穿搭模块局部 ER 图

```plantuml
@startuml er_clothing_outfit
!theme plain
skinparam defaultFontName "SimHei"
skinparam defaultFontSize 11
skinparam linetype ortho

entity "users\n用户" as users {
    * id : BIGINT <<PK>>
    --
    username : VARCHAR
}

entity "clothings\n衣物" as clothings {
    * id : BIGINT <<PK>>
    --
    * user_id : BIGINT <<FK>>
    name / category : VARCHAR
    primary_color / material : VARCHAR
    style_tags / season_tags : VARCHAR
    wear_count : INT
    image_url : VARCHAR
    ai_confidence : DECIMAL
    is_deleted : BOOL
    created_at / updated_at : DATETIME
}

entity "clothing_wear_logs\n穿着日志" as clothing_wear_logs {
    * id : BIGINT <<PK>>
    --
    * clothing_id : BIGINT <<FK>>
    * user_id : BIGINT <<FK>>
    worn_at : DATE
    weather_condition : VARCHAR
    temperature : DECIMAL
}

entity "outfit_advice_records\n穿搭建议" as outfit_advice_records {
    * id : BIGINT <<PK>>
    --
    * user_id : BIGINT <<FK>>
    weather_condition / season : VARCHAR
    outfit_name : VARCHAR
    outfit_description : TEXT
    reasoning : TEXT
    occasion : VARCHAR
    suitability_score : DECIMAL
    is_favorite / is_deleted : BOOL
}

entity "outfit_items\n搭配单品" as outfit_items {
    * id : BIGINT <<PK>>
    --
    * outfit_advice_id : BIGINT <<FK>>
    clothing_id : BIGINT <<FK>>
    item_name / item_category : VARCHAR
    is_recommended : BOOL
    reason : VARCHAR
    sort_order : INT
}

entity "outfit_feedback\n搭配反馈" as outfit_feedback {
    * id : BIGINT <<PK>>
    --
    * outfit_advice_id : BIGINT <<FK>>
    * user_id : BIGINT <<FK>>
    overall_rating : INT
    style_rating : INT
    is_worn : BOOL
    feedback_text : VARCHAR
}

users                 ||--o{ clothings              : ""
users                 ||--o{ clothing_wear_logs      : ""
users                 ||--o{ outfit_advice_records   : ""
users                 ||--o{ outfit_feedback         : ""
clothings             ||--o{ clothing_wear_logs      : ""
clothings             ||--o{ outfit_items            : ""
outfit_advice_records ||--o{ outfit_items            : ""
outfit_advice_records ||--o{ outfit_feedback         : ""
@enduml
```

---

## 图3-11 AI 对话与监控模块局部 ER 图

```plantuml
@startuml er_ai
!theme plain
skinparam defaultFontName "SimHei"
skinparam defaultFontSize 11
skinparam linetype ortho

entity "users\n用户" as users {
    * id : BIGINT <<PK>>
    --
    username : VARCHAR
}

entity "ai_conversations\nAI 会话" as ai_conversations {
    * id : BIGINT <<PK>>
    --
    * user_id : BIGINT <<FK>>
    session_id : VARCHAR
    title : VARCHAR
    context : VARCHAR
    is_active : BOOL
    message_count : INT
    created_at / updated_at : DATETIME
}

entity "ai_chat_messages\nAI 消息" as ai_chat_messages {
    * id : BIGINT <<PK>>
    --
    * conversation_id : BIGINT <<FK>>
    * user_id : BIGINT <<FK>>
    role : VARCHAR
    content : TEXT
    message_type : VARCHAR
    recommendations : TEXT
    created_at : DATETIME
}

entity "ai_api_call_record\nAI 调用记录" as ai_api_call_record {
    * id : BIGINT <<PK>>
    --
    scene : VARCHAR
    model : VARCHAR
    user_id : BIGINT <<FK>>
    status : TINYINT
    response_time : BIGINT
    input_tokens / output_tokens : INT
    total_tokens : INT
    created_at : DATETIME
}

entity "ai_api_stats_daily\n日级统计" as ai_api_stats_daily {
    * id : BIGINT <<PK>>
    --
    stats_date : VARCHAR
    scene / model : VARCHAR
    total_calls / success_calls : INT
    success_rate : DECIMAL
    avg_response_time : DECIMAL
    total_tokens : INT
}

entity "ai_prompt_settings\nAI 提示词配置" as ai_prompt_settings {
    * id : BIGINT <<PK>>
    --
    setting_key : VARCHAR
    setting_value : LONGTEXT
    description : VARCHAR
    updated_by : BIGINT <<FK>>
}

users              ||--o{ ai_conversations    : ""
users              ||--o{ ai_chat_messages    : ""
users              ||--o{ ai_api_call_record  : ""
ai_conversations   ||--o{ ai_chat_messages    : ""
ai_api_call_record }o--|| ai_api_stats_daily  : "聚合"
@enduml
```

---

## 图5-1 用户注册时序图

```plantuml
@startuml seq_register
!theme plain
skinparam defaultFontName "SimHei"
skinparam defaultFontSize 12
skinparam sequenceArrowThickness 2
skinparam sequence {
    ArrowColor #4299e1
    LifeLineBorderColor #718096
    LifeLineBackgroundColor #f7fafc
    ParticipantBorderColor #4299e1
    ParticipantBackgroundColor #ebf8ff
    ParticipantFontColor #1a365d
    ParticipantFontStyle bold
    ActorBackgroundColor #e6fffa
    ActorBorderColor #38b2ac
    ActorFontColor #1d4044
    ActorFontStyle bold
}

actor "用户" as U
participant "前端\n(Vue)" as FE
participant "后端\n(Spring Boot)" as BE
database "MySQL" as DB

U -> FE : 填写注册表单\n（用户名/密码）
activate FE
FE -> FE : 前端字段校验
alt 校验不通过
    FE --> U : 提示字段格式错误
else 校验通过
    FE -> BE : POST /api/v1/auth/register
    activate BE
    BE -> DB : 查询用户名是否存在
    activate DB
    DB --> BE : 返回查询结果
    deactivate DB
    alt 用户名已存在
        BE --> FE : 返回错误：用户名重复
        FE --> U : 提示用户名已被使用
    else 用户名可用
        BE -> BE : BCrypt 加密密码
        BE -> DB : INSERT INTO users
        activate DB
        DB --> BE : 插入成功
        deactivate DB
        BE -> DB : 初始化 user_style_archives
        activate DB
        DB --> BE : 初始化成功
        deactivate DB
        BE --> FE : 返回注册成功
        deactivate BE
        FE --> U : 跳转至登录页面
    end
end
deactivate FE
@enduml
```

---

## 图5-3 用户登录时序图

```plantuml
@startuml seq_login
!theme plain
skinparam defaultFontName "SimHei"
skinparam defaultFontSize 12
skinparam sequenceArrowThickness 2
skinparam sequence {
    ArrowColor #4299e1
    LifeLineBorderColor #718096
    LifeLineBackgroundColor #f7fafc
    ParticipantBorderColor #4299e1
    ParticipantBackgroundColor #ebf8ff
    ParticipantFontColor #1a365d
    ParticipantFontStyle bold
    ActorBackgroundColor #e6fffa
    ActorBorderColor #38b2ac
    ActorFontColor #1d4044
    ActorFontStyle bold
}

actor "用户" as U
participant "前端\n(Vue)" as FE
participant "后端\n(Spring Boot)" as BE
database "MySQL" as DB

U -> FE : 输入用户名和密码
activate FE
FE -> FE : 非空校验
FE -> BE : POST /api/v1/auth/login
activate BE
BE -> DB : 查询用户记录
activate DB
DB --> BE : 返回用户信息
deactivate DB
alt 用户不存在
    BE --> FE : 返回错误：账号不存在
    FE --> U : 提示账号不存在
else 用户存在
    BE -> BE : BCrypt 验证密码
    alt 密码错误
        BE --> FE : 返回错误：密码错误
        FE --> U : 提示密码错误
    else 密码正确
        BE -> BE : 签发 JWT 令牌（7天有效）
        BE -> DB : 记录登录日志
        activate DB
        DB --> BE : 记录成功
        deactivate DB
        BE --> FE : 返回 JWT 令牌
        deactivate BE
        FE -> FE : 存储令牌至 localStorage
        FE --> U : 跳转至首页
    end
end
deactivate FE
@enduml
```

---

## 图5-6 衣物录入时序图

```plantuml
@startuml seq_clothing
!theme plain
skinparam defaultFontName "SimHei"
skinparam defaultFontSize 12
skinparam sequenceArrowThickness 2
skinparam sequence {
    ArrowColor #4299e1
    LifeLineBorderColor #718096
    LifeLineBackgroundColor #f7fafc
    ParticipantBorderColor #4299e1
    ParticipantBackgroundColor #ebf8ff
    ParticipantFontColor #1a365d
    ParticipantFontStyle bold
    ActorBackgroundColor #e6fffa
    ActorBorderColor #38b2ac
    ActorFontColor #1d4044
    ActorFontStyle bold
}

actor "用户" as U
participant "前端\n(Vue)" as FE
participant "后端\n(Spring Boot)" as BE
participant "Qwen3-VL\n(视觉大模型)" as AI
participant "MinIO\n(对象存储)" as MINIO
database "MySQL" as DB

U -> FE : 选择衣物图片
activate FE
FE -> BE : POST /api/v1/clothing/recognize\n（上传图片）
activate BE
BE -> AI : 调用视觉识别接口\n（图片 Base64）
activate AI
AI --> BE : 返回识别结果\n（颜色/类型/风格标签）
deactivate AI
BE --> FE : 返回识别结果
deactivate BE
FE --> U : 将识别结果填入表单
U -> FE : 确认或修改表单，点击保存
FE -> BE : POST /api/v1/clothing/save\n（表单数据 + 图片）
activate BE
BE -> MINIO : 上传图片文件
activate MINIO
MINIO --> BE : 返回图片访问 URL
deactivate MINIO
BE -> DB : INSERT INTO clothings\n（含 image_url / ai_confidence）
activate DB
DB --> BE : 插入成功
deactivate DB
BE --> FE : 返回保存成功
deactivate BE
FE --> U : 刷新衣物列表
deactivate FE
@enduml
```

---

## 图5-10 风格偏好选择时序图

```plantuml
@startuml seq_style
!theme plain
skinparam defaultFontName "SimHei"
skinparam defaultFontSize 12
skinparam sequenceArrowThickness 2
skinparam sequence {
    ArrowColor #4299e1
    LifeLineBorderColor #718096
    LifeLineBackgroundColor #f7fafc
    ParticipantBorderColor #4299e1
    ParticipantBackgroundColor #ebf8ff
    ParticipantFontColor #1a365d
    ParticipantFontStyle bold
    ActorBackgroundColor #e6fffa
    ActorBorderColor #38b2ac
    ActorFontColor #1d4044
    ActorFontStyle bold
}

actor "用户" as U
participant "前端\n(Vue)" as FE
participant "后端\n(Spring Boot)" as BE
database "MySQL" as DB

U -> FE : 进入风格偏好页面
activate FE
FE -> BE : GET /api/v1/style/images
activate BE
BE -> DB : 查询风格图片列表
activate DB
DB --> BE : 返回图片及关联标签
deactivate DB
BE --> FE : 返回图片列表
deactivate BE
FE --> U : 展示风格图片

loop 逐张浏览图片
    U -> FE : 点击喜欢 / 不喜欢 / 跳过
    FE -> BE : POST /api/v1/style/preference\n（image_id, preference_type）
    activate BE
    BE -> DB : INSERT user_style_preference_logs
    activate DB
    DB --> BE : 记录成功
    deactivate DB
    BE -> DB : UPDATE user_style_profiles\n（按标签权重更新偏好分数）
    activate DB
    DB --> BE : 更新成功
    deactivate DB
    BE --> FE : 返回操作成功
    deactivate BE
    FE --> U : 加载下一张图片
end
deactivate FE
@enduml
```

---

## 图5-13 穿搭推荐生成时序图

```plantuml
@startuml seq_outfit
!theme plain
skinparam defaultFontName "SimHei"
skinparam defaultFontSize 12
skinparam sequenceArrowThickness 2
skinparam sequence {
    ArrowColor #4299e1
    LifeLineBorderColor #718096
    LifeLineBackgroundColor #f7fafc
    ParticipantBorderColor #4299e1
    ParticipantBackgroundColor #ebf8ff
    ParticipantFontColor #1a365d
    ParticipantFontStyle bold
    ActorBackgroundColor #e6fffa
    ActorBorderColor #38b2ac
    ActorFontColor #1d4044
    ActorFontStyle bold
}

actor "用户" as U
participant "前端\n(Vue)" as FE
participant "后端\n(Spring Boot)" as BE
participant "AI 代理\n(LangChain4j)" as AGENT
participant "天气 API\n(高德)" as WEATHER
participant "Qwen3\n(大模型)" as LLM
database "MySQL" as DB

U -> FE : 访问首页
activate FE
FE -> BE : GET /api/v1/outfit/recommend
activate BE
BE -> AGENT : 启动穿搭推荐代理
activate AGENT
AGENT -> WEATHER : 获取当前天气信息
activate WEATHER
WEATHER --> AGENT : 返回天气/温度/季节
deactivate WEATHER
AGENT -> DB : 查询用户衣柜数据
activate DB
DB --> AGENT : 返回衣物列表
deactivate DB
AGENT -> DB : 查询用户风格偏好分数
activate DB
DB --> AGENT : 返回偏好标签权重
deactivate DB
AGENT -> LLM : 组装提示词\n（天气+衣物+偏好）\n调用 Qwen3 生成方案
activate LLM
LLM --> AGENT : 返回穿搭方案\n（名称/描述/单品列表/理由）
deactivate LLM
AGENT --> BE : 返回生成结果
deactivate AGENT
BE -> DB : INSERT outfit_advice_records\n+ outfit_items
activate DB
DB --> BE : 存储成功
deactivate DB
BE --> FE : 返回推荐结果
deactivate BE
FE --> U : 展示穿搭方案卡片
deactivate FE
@enduml
```

---

## 图5-17 AI 对话时序图

```plantuml
@startuml seq_chat
!theme plain
skinparam defaultFontName "SimHei"
skinparam defaultFontSize 12
skinparam sequenceArrowThickness 2
skinparam sequence {
    ArrowColor #4299e1
    LifeLineBorderColor #718096
    LifeLineBackgroundColor #f7fafc
    ParticipantBorderColor #4299e1
    ParticipantBackgroundColor #ebf8ff
    ParticipantFontColor #1a365d
    ParticipantFontStyle bold
    ActorBackgroundColor #e6fffa
    ActorBorderColor #38b2ac
    ActorFontColor #1d4044
    ActorFontStyle bold
}

actor "用户" as U
participant "前端\n(Vue)" as FE
participant "后端\n(Spring Boot)" as BE
participant "对话代理\n(LangChain4j)" as AGENT
participant "SiliconFlow\n(嵌入模型)" as EMBED
database "pgvector\n(知识库)" as PG
participant "Qwen3\n(大模型)" as LLM
database "MySQL" as DB

U -> FE : 输入消息，点击发送
activate FE
FE -> BE : POST /api/v1/chat/send\n（session_id + content）
activate BE
BE -> DB : INSERT ai_chat_messages\n（role=user）
activate DB
DB --> BE : 存储成功
deactivate DB
BE -> AGENT : 传入对话历史 + 用户画像
activate AGENT
AGENT -> EMBED : 将查询文本向量化
activate EMBED
EMBED --> AGENT : 返回 1024 维向量
deactivate EMBED
AGENT -> PG : 向量相似度检索\n（余弦距离 Top-K）
activate PG
PG --> AGENT : 返回相关知识条目
deactivate PG
AGENT -> LLM : 组装提示词\n（知识条目+对话历史）\n调用 Qwen3
activate LLM
LLM --> AGENT : 流式返回回答内容
deactivate LLM
AGENT --> BE : 返回完整回答
deactivate AGENT
BE -> DB : INSERT ai_chat_messages\n（role=assistant）
activate DB
DB --> BE : 存储成功
deactivate DB
BE --> FE : 流式返回回答
deactivate BE
FE --> U : 实时展示回答内容
deactivate FE
@enduml
```

---

## 图4-2 系统逻辑架构图

```plantuml
@startuml arch_logic
!theme plain
skinparam defaultFontName "SimHei"
skinparam defaultFontSize 12
skinparam linetype ortho
skinparam rectangle {
    BackgroundColor #ebf8ff
    BorderColor #4299e1
    FontColor #1a365d
    FontStyle bold
    RoundCorner 8
}
skinparam component {
    BackgroundColor #e6fffa
    BorderColor #38b2ac
    FontColor #1d4044
}
skinparam arrow {
    Color #718096
    Thickness 1.5
}

title 系统逻辑架构图

rectangle "表示层" as L1 #dbeafe {
    component "用户端\nVue 3 应用\n(user-app)" as UA #bfdbfe
    component "管理端\nVue 3 应用\n(admin-app)" as AA #bfdbfe
}

rectangle "业务逻辑层" as L2 #dcfce7 {
    component "ecru-web\n统一入口 / Spring Security / JWT" as WEB #bbf7d0
    component "ecru-user\n用户认证 / 个人资料 / 风格偏好" as USER #bbf7d0
    component "ecru-clothing\n衣物管理 / 图片上传 / AI 识别" as CLOTH #bbf7d0
    component "ecru-outfit\n穿搭推荐 / AI 代理 / 对话管理" as OUTFIT #bbf7d0
    component "ecru-common\n公共工具 / 异常处理 / DTO" as COMMON #bbf7d0
}

rectangle "数据访问层" as L3 #fef9c3 {
    component "MySQL\n主业务数据库" as MYSQL #fef08a
    component "PostgreSQL\n+ pgvector\n向量知识库" as PG #fef08a
    component "MinIO\n图片对象存储" as MINIO #fef08a
}

rectangle "外部服务层" as L4 #fce7f3 {
    component "Qwen3-VL\n图像识别 / 多轮对话" as QWEN #fbcfe8
    component "SiliconFlow\n文本嵌入（1024维）" as SF #fbcfe8
    component "高德天气 API\n实时天气信息" as AMAP #fbcfe8
}

UA -down-> WEB : HTTP / REST
AA -down-> WEB : HTTP / REST
WEB -down-> USER
WEB -down-> CLOTH
WEB -down-> OUTFIT
USER -down-> MYSQL
CLOTH -down-> MYSQL
CLOTH -down-> MINIO
OUTFIT -down-> MYSQL
OUTFIT -down-> PG
OUTFIT -right-> QWEN
OUTFIT -right-> SF
OUTFIT -right-> AMAP
CLOTH -right-> QWEN
USER .down. COMMON
CLOTH .down. COMMON
OUTFIT .down. COMMON
@enduml
```

---

## 图4-3 系统技术架构图

```plantuml
@startuml arch_tech
!theme plain
skinparam defaultFontName "SimHei"
skinparam defaultFontSize 11
skinparam rectangle {
    RoundCorner 10
    BorderThickness 1.5
}
skinparam component {
    RoundCorner 6
    BorderThickness 1
}
skinparam arrow {
    Color #4a5568
    Thickness 1.5
}

title 系统技术架构图

rectangle "前端层  Frontend" as FE_LAYER #ebf8ff {
    rectangle "用户端 (user-app)" as FE1 #bee3f8 {
        component "Vue 3 + Vite" as VUE1
        component "Axios / Pinia" as AX1
    }
    rectangle "管理端 (admin-app)" as FE2 #bee3f8 {
        component "Vue 3 + Vite" as VUE2
        component "Axios / Element Plus" as AX2
    }
}

rectangle "接入层  Gateway" as GW_LAYER #e6fffa {
    component "Spring Security\nJWT Filter" as SEC #b2f5ea
    component "Knife4j\nSwagger UI" as KNIFE #b2f5ea
}

rectangle "业务层  Business" as BIZ_LAYER #f0fff4 {
    component "用户模块\necru-user" as M_USER #c6f6d5
    component "衣物模块\necru-clothing" as M_CLOTH #c6f6d5
    component "推荐模块\necru-outfit" as M_OUTFIT #c6f6d5
    component "LangChain4j\nAI Agent / RAG" as LC4J #c6f6d5
}

rectangle "存储层  Storage" as STORE_LAYER #fffff0 {
    database "MySQL 8\n主业务数据" as MYSQL #fefcbf
    database "PostgreSQL\n+ pgvector\n向量检索" as PG #fefcbf
    component "MinIO\n对象存储" as MINIO #fefcbf
}

rectangle "AI 服务层  AI Services" as AI_LAYER #fff5f5 {
    component "Qwen3-VL\n图像识别 / 对话生成" as QWEN #fed7d7
    component "SiliconFlow\n文本嵌入 1024维" as SF #fed7d7
    component "高德天气 API\n实时天气" as AMAP #fed7d7
}

FE1 -down-> SEC : HTTPS /api/v1
FE2 -down-> SEC : HTTPS /api/v1
SEC -down-> M_USER
SEC -down-> M_CLOTH
SEC -down-> M_OUTFIT
M_USER -down-> MYSQL : MyBatis Plus
M_CLOTH -down-> MYSQL : MyBatis Plus
M_CLOTH -down-> MINIO : MinIO SDK
M_OUTFIT -down-> MYSQL : MyBatis Plus
M_OUTFIT -down-> LC4J
LC4J -down-> PG : pgvector 检索
LC4J -right-> QWEN : 对话 / 识别
LC4J -right-> SF : 向量化
LC4J -right-> AMAP : 天气查询
M_CLOTH -right-> QWEN : 图像识别
@enduml
```

---

## 图4-3 系统功能架构图

```plantuml
@startuml arch_func
!theme plain
skinparam defaultFontName "SimHei"
skinparam defaultFontSize 11
skinparam nodesep 20
skinparam ranksep 30
skinparam rectangle {
    RoundCorner 4
    BorderThickness 1.2
}

' ── 第一层 ──
rectangle "Ecru\n智能穿搭\n系统" as SYS #2b6cb0

' ── 第二层 ──
rectangle "普\n通\n用\n户" as U #4299e1
rectangle "管\n理\n员" as A #38b2ac

' ── 第三层 用户侧 ──
rectangle "账\n号\n管\n理" as M1 #bee3f8
rectangle "衣\n物\n管\n理" as M2 #bee3f8
rectangle "风\n格\n偏\n好" as M3 #bee3f8
rectangle "穿\n搭\n推\n荐" as M4 #bee3f8
rectangle "A\nI\n对\n话" as M5 #bee3f8

' ── 第三层 管理侧 ──
rectangle "用\n户\n管\n理" as MA1 #b2f5ea
rectangle "衣\n物\n台\n账" as MA2 #b2f5ea
rectangle "知\n识\n库\n管\n理" as MA3 #b2f5ea
rectangle "系\n统\n配\n置" as MA4 #b2f5ea
rectangle "A\nI\n监\n控" as MA5 #b2f5ea

' ── 第四层 用户侧功能 ──
rectangle "注\n册\n/\n登\n录" as F11 #ebf8ff
rectangle "查\n看\n个\n人\n资\n料" as F12 #ebf8ff
rectangle "修\n改\n个\n人\n资\n料" as F13 #ebf8ff
rectangle "修\n改\n头\n像" as F14 #ebf8ff

rectangle "上\n传\n衣\n物\n图\n片" as F21 #ebf8ff
rectangle "A\nI\n识\n别\n属\n性" as F22 #ebf8ff
rectangle "手\n动\n填\n写\n/\n编\n辑" as F23 #ebf8ff
rectangle "删\n除\n衣\n物" as F24 #ebf8ff
rectangle "查\n看\n衣\n物\n列\n表" as F25 #ebf8ff

rectangle "浏\n览\n风\n格\n图\n片\n库" as F31 #ebf8ff
rectangle "标\n记\n喜\n欢\n/\n不\n喜\n欢" as F32 #ebf8ff
rectangle "查\n看\n风\n格\n画\n像" as F33 #ebf8ff

rectangle "查\n看\n首\n页\n推\n荐" as F41 #ebf8ff
rectangle "查\n看\n推\n荐\n详\n情" as F42 #ebf8ff
rectangle "评\n分\n与\n反\n馈" as F43 #ebf8ff

rectangle "发\n起\n新\n对\n话" as F51 #ebf8ff
rectangle "多\n轮\n穿\n搭\n咨\n询" as F52 #ebf8ff
rectangle "查\n看\n历\n史\n对\n话" as F53 #ebf8ff

' ── 第四层 管理侧功能 ──
rectangle "查\n看\n用\n户\n列\n表" as FA11 #e6fffa
rectangle "查\n看\n用\n户\n详\n情" as FA12 #e6fffa

rectangle "查\n看\n全\n量\n衣\n物" as FA21 #e6fffa
rectangle "按\n用\n户\n筛\n选" as FA22 #e6fffa

rectangle "新\n增\n知\n识\n条\n目" as FA31 #e6fffa
rectangle "编\n辑\n知\n识\n条\n目" as FA32 #e6fffa
rectangle "删\n除\n知\n识\n条\n目" as FA33 #e6fffa

rectangle "配\n置\nA\nI\n提\n示\n词" as FA41 #e6fffa

rectangle "查\n看\n调\n用\n统\n计" as FA51 #e6fffa
rectangle "查\n看\n对\n话\n日\n志" as FA52 #e6fffa

' ── 连线（无箭头） ──
SYS -  U
SYS -  A

U -  M1
U -  M2
U -  M3
U -  M4
U -  M5

M1 -  F11
M1 -  F12
M1 -  F13
M1 -  F14

M2 -  F21
M2 -  F22
M2 -  F23
M2 -  F24
M2 -  F25

M3 -  F31
M3 -  F32
M3 -  F33

M4 -  F41
M4 -  F42
M4 -  F43

M5 -  F51
M5 -  F52
M5 -  F53

A -  MA1
A -  MA2
A -  MA3
A -  MA4
A -  MA5

MA1 -  FA11
MA1 -  FA12

MA2 -  FA21
MA2 -  FA22

MA3 -  FA31
MA3 -  FA32
MA3 -  FA33

MA4 -  FA41

MA5 -  FA51
MA5 -  FA52

@enduml
```
