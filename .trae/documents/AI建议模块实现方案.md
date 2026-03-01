# AI建议模块实现方案

## 1. 模块结构设计

### 1.1 新增模块
- **ecru-outfit**：搭配建议核心模块

### 1.2 目录结构
```
ecru-outfit/
├── src/main/java/com/ecru/outfit/
│   ├── controller/       # 控制器
│   ├── service/          # 业务逻辑
│   │   ├── agent/        # Agent工作流
│   │   ├── analyzer/     # 图像分析
│   │   ├── mcp/          # MCP工具
│   │   └── rag/          # RAG检索
│   ├── dto/              # 数据传输对象
│   ├── entity/           # 实体类
│   ├── mapper/           # 数据访问
│   ├── config/           # 配置
│   └── utils/            # 工具类
├── src/main/resources/
│   ├── mapper/           # XML映射文件
│   └── prompts/          # AI提示模板
└── pom.xml
```

## 2. 核心功能实现

### 2.1 Agent工作流
- **OutfitAdvisorAgent**：核心智能体，编排多工具调用
- **MCP天气工具**：集成高德天气API
- **图像分析工具**：调用Qwen3-VL分析穿搭照片
- **RAG检索工具**：基于用户衣橱的语义检索

### 2.2 主要服务
- **OutfitAdviceService**：搭配建议生成
- **ImageAnalyzerService**：图像分析
- **StyleProfileService**：用户风格档案管理
- **OutfitFeedbackService**：反馈管理

### 2.3 数据模型
- **OutfitAdviceRecord**：搭配建议记录
- **OutfitItem**：搭配单品关联
- **OutfitFeedback**：搭配反馈
- **UserStyleProfile**：用户风格档案

## 3. 技术实现细节

### 3.1 AI集成
- **LangChain4j**：构建Agent工作流
- **Qwen3-VL**：多模态图像分析
- **硅基流动API**：大模型调用

### 3.2 MCP集成
- **高德天气**：获取地理位置和天气数据
- **接口封装**：统一MCP工具调用

### 3.3 RAG实现
- **PostgreSQL+pgvector**：向量存储
- **语义检索**：基于自然语言查询用户衣橱

### 3.4 图像分析
- **Qwen3-VL Prompt**：结构化图像分析
- **结果解析**：JSON格式解析

## 4. API接口设计

### 4.1 核心接口
- `POST /outfit/advice`：获取搭配建议
- `POST /outfit/analyze`：分析穿搭照片
- `GET /outfit/history`：获取历史搭配
- `POST /outfit/{id}/feedback`：提交反馈
- `GET /outfit/style-profile`：获取风格档案

### 4.2 请求/响应格式
- 支持multipart/form-data上传图片
- 标准RESTful JSON响应
- 完整的错误处理和状态码

## 5. 数据库设计

### 5.1 新增表结构
- **outfit_advice_records**：搭配建议记录
- **outfit_items**：搭配单品关联
- **outfit_feedback**：搭配反馈
- **user_style_profiles**：用户风格档案

### 5.2 数据关联
- 与用户表(users)关联
- 与衣物表(clothings)关联

## 6. 集成与部署

### 6.1 模块集成
- 在父pom.xml中添加ecru-outfit模块
- 依赖关系：
  - 依赖ecru-common
  - 依赖ecru-user
  - 依赖ecru-clothing

### 6.2 配置文件
- 在application.yml中添加AI配置
- 添加硅基流动API密钥
- 添加高德MCP配置

## 7. 关键技术点

### 7.1 Agent编排
- 使用LangChain4j的@Agent注解
- 多工具并行调用
- 智能决策流程

### 7.2 图像分析
- Qwen3-VL多模态能力
- 结构化Prompt设计
- 结果解析和验证

### 7.3 MCP集成
- 高德天气API调用
- 地理位置解析
- 天气数据缓存

### 7.4 RAG检索
- 衣物向量生成
- 语义相似度计算
- 多条件混合检索

## 8. 性能优化

### 8.1 缓存策略
- Redis缓存天气数据
- 缓存AI分析结果
- 缓存用户风格档案

### 8.2 异步处理
- 图像分析异步执行
- 搭配生成后台处理
- 反馈处理异步保存

### 8.3 批处理
- 批量向量检索
- 批量数据存储
- 批量API调用

## 9. 测试计划

### 9.1 单元测试
- Agent工作流测试
- 图像分析测试
- RAG检索测试

### 9.2 集成测试
- MCP工具集成测试
- 数据库集成测试
- API接口测试

### 9.3 性能测试
- 响应时间测试
- 并发测试
- 大数据量测试

## 10. 风险应对

### 10.1 潜在风险
- AI服务不稳定
- MCP调用失败
- 图像分析超时
- 向量检索性能

### 10.2 应对策略
- 服务降级机制
- 重试和容错
- 异步处理
- 缓存预热

---

## 预期交付物

1. **ecru-outfit模块**：完整的搭配建议功能
2. **数据库脚本**：新增表结构和索引
3. **API文档**：完整的接口文档
4. **测试用例**：单元测试和集成测试
5. **配置示例**：AI和MCP配置示例

该方案基于现有的项目结构，充分利用LangChain4j和Qwen3-VL的能力，实现智能搭配建议功能。