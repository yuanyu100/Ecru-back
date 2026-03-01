# AI建议模块 - 详细实现方案

## 1. 模块架构

### 1.1 模块结构

```
ecru-ai-advice/
├── pom.xml
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── ecru/
        │           └── aiadvice/
        │               ├── config/
        │               │   └── LangChain4jConfig.java       # LangChain4j配置
        │               ├── controller/
        │               │   └── OutfitAdviceController.java  # 搭配建议接口
        │               ├── service/
        │               │   ├── impl/
        │               │   │   ├── OutfitAdviceServiceImpl.java
        │               │   │   └── OutfitAnalysisServiceImpl.java
        │               │   ├── OutfitAdviceService.java
        │               │   └── OutfitAnalysisService.java
        │               ├── agent/
        │               │   ├── OutfitAdvisorAgent.java      # 搭配顾问Agent
        │               │   ├── ClothingAnalyzerAgent.java   # 衣物分析Agent
        │               │   └── tools/
        │               │       ├── WeatherTool.java         # MCP天气工具
        │               │       ├── WardrobeTool.java        # 衣橱检索工具
        │               │       └── ImageAnalysisTool.java   # 图像分析工具
        │               ├── dto/
        │               │   ├── request/
        │               │   │   ├── OutfitAdviceRequest.java
        │               │   │   ├── OutfitFeedbackRequest.java
        │               │   │   └── StyleProfileRequest.java
        │               │   └── response/
        │               │       ├── OutfitAdviceVO.java
        │               │       ├── OutfitAnalysisVO.java
        │               │       └── StyleProfileVO.java
        │               ├── entity/
        │               │   ├── OutfitAdviceRecord.java
        │               │   ├── OutfitItem.java
        │               │   ├── OutfitFeedback.java
        │               │   └── UserStyleProfile.java
        │               ├── mapper/
        │               │   ├── OutfitAdviceRecordMapper.java
        │               │   ├── OutfitItemMapper.java
        │               │   ├── OutfitFeedbackMapper.java
        │               │   └── UserStyleProfileMapper.java
        │               └── converter/
        │                   └── OutfitAdviceConverter.java
        └── resources/
            └── mapper/
                ├── OutfitAdviceRecordMapper.xml
                ├── OutfitItemMapper.xml
                ├── OutfitFeedbackMapper.xml
                └── UserStyleProfileMapper.xml
```

---

## 2. Maven依赖配置

### 2.1 ecru-ai-advice/pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.ecru</groupId>
        <artifactId>ecru-parent</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>ecru-ai-advice</artifactId>
    <name>Ecru AI Advice Module</name>
    <description>AI搭配建议模块</description>

    <dependencies>
        <!-- 内部模块依赖 -->
        <dependency>
            <groupId>com.ecru</groupId>
            <artifactId>ecru-common</artifactId>
        </dependency>
        <dependency>
            <groupId>com.ecru</groupId>
            <artifactId>ecru-user</artifactId>
        </dependency>
        <dependency>
            <groupId>com.ecru</groupId>
            <artifactId>ecru-clothing</artifactId>
        </dependency>

        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- LangChain4j 核心 -->
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-spring-boot-starter</artifactId>
        </dependency>

        <!-- LangChain4j 千问模型 -->
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-dashscope</artifactId>
            <version>${langchain4j.version}</version>
        </dependency>

        <!-- LangChain4j 向量存储 -->
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-pgvector</artifactId>
            <version>${langchain4j.version}</version>
        </dependency>

        <!-- MyBatis Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        </dependency>

        <!-- MapStruct -->
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
        </dependency>

        <!-- JSON处理 -->
        <dependency>
            <groupId>com.alibaba.fastjson2</groupId>
            <artifactId>fastjson2</artifactId>
        </dependency>

        <!-- 图片处理 -->
        <dependency>
            <groupId>javax.media</groupId>
            <artifactId>jai-core</artifactId>
            <version>1.1.3</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct-processor</artifactId>
                            <version>${mapstruct.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## 3. LangChain4j 配置

### 3.1 配置文件 application-ai.yml

```yaml
langchain4j:
  # 千问模型配置 (硅基流动)
  dashscope:
    api-key: ${SILICON_FLOW_API_KEY:sk-rejkwjvqwerqgfmolygzjxqqoaejovbkeieyuwxtogokvgqp}
    model-name: Qwen/Qwen3-VL-32B-Instruct
    base-url: https://api.siliconflow.cn/v1
    temperature: 0.7
    timeout: 60s

  # Embedding模型配置
  embedding-model:
    api-key: ${SILICON_FLOW_API_KEY}
    model-name: BAAI/bge-large-zh-v1.5
    base-url: https://api.siliconflow.cn/v1

# MCP配置
mcp:
  amap:
    enabled: true
    api-key: ${AMAP_API_KEY}

# AI建议模块配置
ai-advice:
  # 搭配建议配置
  outfit:
    max-items-per-outfit: 6
    min-suitability-score: 0.6
    default-recommendations: 3
  
  # RAG检索配置
  rag:
    max-results: 10
    min-similarity-score: 0.7
```

### 3.2 LangChain4jConfig.java

```java
package com.ecru.aiadvice.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.dashscope.QwenChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class LangChain4jConfig {

    @Value("${langchain4j.dashscope.api-key}")
    private String apiKey;

    @Value("${langchain4j.dashscope.model-name:Qwen/Qwen3-VL-32B-Instruct}")
    private String modelName;

    @Value("${langchain4j.dashscope.base-url:https://api.siliconflow.cn/v1}")
    private String baseUrl;

    @Value("${langchain4j.embedding-model.model-name:BAAI/bge-large-zh-v1.5}")
    private String embeddingModelName;

    /**
     * 配置聊天模型 (Qwen3-VL-32B)
     */
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return QwenChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .temperature(0.7)
                .maxRetries(3)
                .build();
    }

    /**
     * 配置Embedding模型
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return QwenEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(embeddingModelName)
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * 配置PgVector向量存储
     */
    @Bean
    public EmbeddingStore embeddingStore(DataSource dataSource) {
        return PgVectorEmbeddingStore.builder()
                .dataSource(dataSource)
                .table("clothing_embeddings")
                .dimension(1536)
                .build();
    }
}
```

---

## 4. Agent 设计

### 4.1 OutfitAdvisorAgent - 搭配顾问Agent

```java
package com.ecru.aiadvice.agent;

import com.ecru.aiadvice.agent.tools.WeatherTool;
import com.ecru.aiadvice.agent.tools.WardrobeTool;
import com.ecru.aiadvice.agent.tools.ImageAnalysisTool;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.V;

@AiService
public interface OutfitAdvisorAgent {

    @SystemMessage("""
        你是一位专业的时尚搭配顾问，拥有丰富的服装搭配知识和时尚敏感度。
        
        你的任务是：
        1. 分析用户上传的穿搭照片或描述
        2. 结合当前天气和季节信息
        3. 从用户衣橱中检索合适的单品
        4. 生成完整的搭配方案
        5. 提供专业的时尚建议
        
        搭配原则：
        - 考虑场合适配性（通勤、约会、休闲等）
        - 注重色彩协调性
        - 根据天气温度推荐合适厚度
        - 尊重用户个人风格偏好
        - 优先使用用户已有衣物
        
        输出格式要求：
        {
          "analysis": {
            "detectedItems": ["识别到的单品"],
            "style": "风格分析",
            "colors": "色彩分析"
          },
          "weather": {
            "location": "位置",
            "temperature": "温度",
            "condition": "天气状况",
            "season": "季节"
          },
          "recommendation": {
            "outfitName": "搭配方案名称",
            "items": [
              {
                "clothingId": "衣物ID或null",
                "name": "单品名称",
                "category": "类别",
                "color": "颜色",
                "reason": "推荐理由"
              }
            ],
            "reasoning": "搭配思路",
            "suggestions": "时尚建议"
          },
          "purchaseRecommendations": [
            {
              "name": "推荐商品名",
              "reason": "推荐原因",
              "link": "购买链接"
            }
          ]
        }
        """)
    @UserMessage("""
        请为用户生成搭配建议：
        
        用户ID: {{userId}}
        输入类型: {{inputType}}
        场合: {{occasion}}
        是否考虑天气: {{considerWeather}}
        是否优先使用已有衣物: {{useExistingClothes}}
        
        {{#imageUrl}}
        穿搭照片URL: {{imageUrl}}
        {{/imageUrl}}
        
        {{#description}}
        用户描述: {{description}}
        {{/description}}
        
        请调用相关工具获取天气信息和衣橱数据，然后生成搭配方案。
        """)
    String generateOutfitAdvice(
            @V("userId") Long userId,
            @V("inputType") String inputType,
            @V("occasion") String occasion,
            @V("considerWeather") Boolean considerWeather,
            @V("useExistingClothes") Boolean useExistingClothes,
            @V("imageUrl") String imageUrl,
            @V("description") String description
    );
}
```

### 4.2 ClothingAnalyzerAgent - 衣物分析Agent

```java
package com.ecru.aiadvice.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.V;

@AiService
public interface ClothingAnalyzerAgent {

    @SystemMessage("""
        你是一位专业的服装分析师，擅长从图片中识别服装特征。
        
        分析维度：
        1. 类别：上装、下装、外套、连衣裙、鞋履、包袋、配饰
        2. 颜色：主色调、辅色调
        3. 风格：日系、韩系、欧美、通勤、运动、复古等
        4. 材质：棉、麻、丝、羊毛、化纤、混纺、皮革等
        5. 图案：纯色、条纹、格子、印花等
        6. 版型：修身、宽松、直筒、A字等
        7. 适用场合：日常、通勤、约会、运动、正式、休闲
        8. 适用季节：春、夏、秋、冬
        
        输出严格的JSON格式：
        {
          "category": "类别",
          "primaryColor": "主色调",
          "secondaryColor": "辅色调",
          "style": ["风格1", "风格2"],
          "material": "材质",
          "pattern": "图案",
          "fit": "版型",
          "occasion": ["场合1", "场合2"],
          "season": ["季节1", "季节2"],
          "description": "详细描述"
        }
        """)
    @UserMessage("请分析这张服装图片: {{imageUrl}}")
    String analyzeClothingImage(@V("imageUrl") String imageUrl);

    @SystemMessage("""
        你是一位专业的穿搭分析师，擅长分析整体穿搭效果。
        
        分析维度：
        1. 单品识别：上衣、下装、外套、鞋履等
        2. 风格判断：整体风格定位
        3. 色彩分析：配色方案和协调性
        4. 场合适配：适合的场合
        5. 季节判断：适合的季节
        6. 改进建议：如何提升搭配效果
        
        输出严格的JSON格式：
        {
          "detectedItems": [
            {"item": "单品名", "category": "类别", "color": "颜色"}
          ],
          "style": "风格",
          "colors": "色彩分析",
          "occasion": "适用场合",
          "season": "适用季节",
          "suggestions": "改进建议"
        }
        """)
    @UserMessage("请分析这张穿搭照片: {{imageUrl}}")
    String analyzeOutfitImage(@V("imageUrl") String imageUrl);
}
```

---

## 5. MCP Tools 实现

### 5.1 WeatherTool - 天气工具

```java
package com.ecru.aiadvice.agent.tools;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
public class WeatherTool {

    @Value("${mcp.amap.api-key}")
    private String amapApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String AMAP_GEO_URL = 
        "https://restapi.amap.com/v3/ip?key={key}";
    private static final String AMAP_WEATHER_URL = 
        "https://restapi.amap.com/v3/weather/weatherInfo?key={key}&city={city}";

    /**
     * 获取当前位置和天气信息
     */
    @Tool("获取用户当前位置和天气信息")
    public WeatherInfo getCurrentWeather(
            @P("用户IP地址，可为空") String ipAddress) {
        
        try {
            // 1. 获取地理位置
            String cityCode = getCityCode(ipAddress);
            
            // 2. 获取天气信息
            return getWeatherByCity(cityCode);
            
        } catch (Exception e) {
            log.error("获取天气信息失败", e);
            // 返回默认天气信息
            return WeatherInfo.builder()
                    .location("北京市")
                    .temperature(22.0)
                    .condition("晴")
                    .season(getCurrentSeason())
                    .timeOfDay(getCurrentTimeOfDay())
                    .build();
        }
    }

    /**
     * 获取城市编码
     */
    private String getCityCode(String ipAddress) {
        String url = AMAP_GEO_URL.replace("{key}", amapApiKey);
        
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        
        if (response != null && "1".equals(response.get("status"))) {
            return (String) response.get("adcode");
        }
        
        // 默认返回北京
        return "110000";
    }

    /**
     * 根据城市编码获取天气
     */
    private WeatherInfo getWeatherByCity(String cityCode) {
        String url = AMAP_WEATHER_URL
                .replace("{key}", amapApiKey)
                .replace("{city}", cityCode);
        
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        
        if (response != null && "1".equals(response.get("status"))) {
            Map<String, Object> lives = (Map<String, Object>) 
                ((java.util.List<?>) response.get("lives")).get(0);
            
            return WeatherInfo.builder()
                    .location((String) lives.get("city"))
                    .temperature(Double.parseDouble((String) lives.get("temperature")))
                    .condition((String) lives.get("weather"))
                    .humidity((String) lives.get("humidity"))
                    .windDirection((String) lives.get("winddirection"))
                    .windPower((String) lives.get("windpower"))
                    .season(getCurrentSeason())
                    .timeOfDay(getCurrentTimeOfDay())
                    .build();
        }
        
        throw new RuntimeException("获取天气信息失败");
    }

    private String getCurrentSeason() {
        int month = java.time.LocalDate.now().getMonthValue();
        if (month >= 3 && month <= 5) return "春";
        if (month >= 6 && month <= 8) return "夏";
        if (month >= 9 && month <= 11) return "秋";
        return "冬";
    }

    private String getCurrentTimeOfDay() {
        int hour = java.time.LocalTime.now().getHour();
        if (hour >= 6 && hour < 9) return "早晨";
        if (hour >= 9 && hour < 12) return "上午";
        if (hour >= 12 && hour < 18) return "下午";
        if (hour >= 18 && hour < 24) return "晚上";
        return "深夜";
    }
}
```

### 5.2 WeatherInfo DTO

```java
package com.ecru.aiadvice.agent.tools;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WeatherInfo {
    private String location;
    private Double temperature;
    private String condition;
    private String humidity;
    private String windDirection;
    private String windPower;
    private String season;
    private String timeOfDay;
}
```

### 5.3 WardrobeTool - 衣橱检索工具

```java
package com.ecru.aiadvice.agent.tools;

import com.ecru.clothing.entity.Clothing;
import com.ecru.clothing.service.ClothingService;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class WardrobeTool {

    private final ClothingService clothingService;

    /**
     * 根据类别检索用户衣物
     */
    @Tool("根据类别检索用户的衣物")
    public List<ClothingInfo> searchClothesByCategory(
            @P("用户ID") Long userId,
            @P("衣物类别: 上装/下装/外套/连衣裙/鞋履/配饰") String category) {
        
        log.info("检索用户[{}]的[{}]类别衣物", userId, category);
        
        List<Clothing> clothes = clothingService.getByUserIdAndCategory(userId, category);
        
        return clothes.stream()
                .map(this::convertToClothingInfo)
                .collect(Collectors.toList());
    }

    /**
     * 根据场合和季节检索衣物
     */
    @Tool("根据场合和季节检索合适的衣物")
    public List<ClothingInfo> searchClothesByOccasionAndSeason(
            @P("用户ID") Long userId,
            @P("场合: 日常/通勤/约会/运动/正式/休闲") String occasion,
            @P("季节: 春/夏/秋/冬") String season) {
        
        log.info("检索用户[{}]适合[{}]场合[{}]季节的衣物", userId, occasion, season);
        
        List<Clothing> clothes = clothingService
                .getByUserIdAndOccasionAndSeason(userId, occasion, season);
        
        return clothes.stream()
                .map(this::convertToClothingInfo)
                .collect(Collectors.toList());
    }

    /**
     * 根据颜色检索衣物
     */
    @Tool("根据颜色检索衣物")
    public List<ClothingInfo> searchClothesByColor(
            @P("用户ID") Long userId,
            @P("颜色") String color) {
        
        log.info("检索用户[{}]的[{}]颜色衣物", userId, color);
        
        List<Clothing> clothes = clothingService.getByUserIdAndColor(userId, color);
        
        return clothes.stream()
                .map(this::convertToClothingInfo)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户高频穿搭单品
     */
    @Tool("获取用户最常穿的衣物")
    public List<ClothingInfo> getFrequentlyWornClothes(
            @P("用户ID") Long userId,
            @P("返回数量限制") Integer limit) {
        
        log.info("获取用户[{}]的高频穿搭单品", userId);
        
        List<Clothing> clothes = clothingService
                .getTopByFrequency(userId, limit);
        
        return clothes.stream()
                .map(this::convertToClothingInfo)
                .collect(Collectors.toList());
    }

    private ClothingInfo convertToClothingInfo(Clothing clothing) {
        return ClothingInfo.builder()
                .id(clothing.getId())
                .name(clothing.getName())
                .category(clothing.getCategory())
                .primaryColor(clothing.getPrimaryColor())
                .secondaryColor(clothing.getSecondaryColor())
                .material(clothing.getMaterial())
                .styleTags(clothing.getStyleTags())
                .occasionTags(clothing.getOccasionTags())
                .seasonTags(clothing.getSeasonTags())
                .frequencyLevel(clothing.getFrequencyLevel())
                .imageUrl(clothing.getImageUrl())
                .build();
    }
}
```

### 5.4 ClothingInfo DTO

```java
package com.ecru.aiadvice.agent.tools;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ClothingInfo {
    private Long id;
    private String name;
    private String category;
    private String primaryColor;
    private String secondaryColor;
    private String material;
    private List<String> styleTags;
    private List<String> occasionTags;
    private List<String> seasonTags;
    private Integer frequencyLevel;
    private String imageUrl;
}
```

### 5.5 ImageAnalysisTool - 图像分析工具

```java
package com.ecru.aiadvice.agent.tools;

import com.alibaba.fastjson2.JSON;
import com.ecru.aiadvice.agent.ClothingAnalyzerAgent;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageAnalysisTool {

    private final ClothingAnalyzerAgent clothingAnalyzerAgent;

    /**
     * 分析服装单品图片
     */
    @Tool("分析服装单品图片，识别类别、颜色、风格等信息")
    public ClothingAnalysisResult analyzeClothingImage(
            @P("图片URL") String imageUrl) {
        
        log.info("分析服装图片: {}", imageUrl);
        
        try {
            String result = clothingAnalyzerAgent.analyzeClothingImage(imageUrl);
            return JSON.parseObject(result, ClothingAnalysisResult.class);
        } catch (Exception e) {
            log.error("分析服装图片失败", e);
            return null;
        }
    }

    /**
     * 分析穿搭照片
     */
    @Tool("分析穿搭照片，识别整体搭配效果")
    public OutfitAnalysisResult analyzeOutfitImage(
            @P("图片URL") String imageUrl) {
        
        log.info("分析穿搭照片: {}", imageUrl);
        
        try {
            String result = clothingAnalyzerAgent.analyzeOutfitImage(imageUrl);
            return JSON.parseObject(result, OutfitAnalysisResult.class);
        } catch (Exception e) {
            log.error("分析穿搭照片失败", e);
            return null;
        }
    }
}
```

### 5.6 分析结果DTO

```java
package com.ecru.aiadvice.agent.tools;

import lombok.Data;
import java.util.List;

@Data
public class ClothingAnalysisResult {
    private String category;
    private String primaryColor;
    private String secondaryColor;
    private List<String> style;
    private String material;
    private String pattern;
    private String fit;
    private List<String> occasion;
    private List<String> season;
    private String description;
}

@Data
public class OutfitAnalysisResult {
    private List<DetectedItem> detectedItems;
    private String style;
    private String colors;
    private String occasion;
    private String season;
    private String suggestions;

    @Data
    public static class DetectedItem {
        private String item;
        private String category;
        private String color;
    }
}
```

---

## 6. Service 层实现

### 6.1 OutfitAdviceService

```java
package com.ecru.aiadvice.service;

import com.ecru.aiadvice.dto.request.OutfitAdviceRequest;
import com.ecru.aiadvice.dto.request.OutfitFeedbackRequest;
import com.ecru.aiadvice.dto.response.OutfitAdviceVO;
import com.ecru.common.result.PageResult;

public interface OutfitAdviceService {

    /**
     * 获取搭配建议 (Agent工作流)
     */
    OutfitAdviceVO getOutfitAdvice(Long userId, OutfitAdviceRequest request);

    /**
     * 分析穿搭照片
     */
    OutfitAdviceVO analyzeOutfit(Long userId, String imageUrl);

    /**
     * 获取历史搭配记录
     */
    PageResult<OutfitAdviceVO> getHistory(Long userId, Integer page, Integer size, 
                                          String occasion, Boolean isFavorite);

    /**
     * 获取搭配详情
     */
    OutfitAdviceVO getDetail(Long userId, Long adviceId);

    /**
     * 删除搭配记录
     */
    void deleteAdvice(Long userId, Long adviceId);

    /**
     * 收藏/取消收藏
     */
    void toggleFavorite(Long userId, Long adviceId, Boolean isFavorite);

    /**
     * 提交反馈
     */
    void submitFeedback(Long userId, Long adviceId, OutfitFeedbackRequest request);

    /**
     * 获取智能推荐
     */
    List<OutfitAdviceVO> getRecommendations(Long userId, Integer limit, String occasion);
}
```

### 6.2 OutfitAdviceServiceImpl

```java
package com.ecru.aiadvice.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ecru.aiadvice.agent.OutfitAdvisorAgent;
import com.ecru.aiadvice.agent.tools.WeatherInfo;
import com.ecru.aiadvice.agent.tools.WeatherTool;
import com.ecru.aiadvice.converter.OutfitAdviceConverter;
import com.ecru.aiadvice.dto.request.OutfitAdviceRequest;
import com.ecru.aiadvice.dto.request.OutfitFeedbackRequest;
import com.ecru.aiadvice.dto.response.OutfitAdviceVO;
import com.ecru.aiadvice.entity.OutfitAdviceRecord;
import com.ecru.aiadvice.entity.OutfitFeedback;
import com.ecru.aiadvice.entity.OutfitItem;
import com.ecru.aiadvice.mapper.OutfitAdviceRecordMapper;
import com.ecru.aiadvice.mapper.OutfitFeedbackMapper;
import com.ecru.aiadvice.mapper.OutfitItemMapper;
import com.ecru.aiadvice.service.OutfitAdviceService;
import com.ecru.common.exception.BusinessException;
import com.ecru.common.result.ErrorCode;
import com.ecru.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutfitAdviceServiceImpl implements OutfitAdviceService {

    private final OutfitAdvisorAgent outfitAdvisorAgent;
    private final WeatherTool weatherTool;
    private final OutfitAdviceRecordMapper adviceRecordMapper;
    private final OutfitItemMapper outfitItemMapper;
    private final OutfitFeedbackMapper feedbackMapper;
    private final OutfitAdviceConverter converter;

    @Override
    @Transactional
    public OutfitAdviceVO getOutfitAdvice(Long userId, OutfitAdviceRequest request) {
        log.info("用户[{}]请求搭配建议", userId);

        // 1. 保存上传的图片（如果有）
        String imageUrl = null;
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            imageUrl = saveImage(request.getImage());
        }

        // 2. 获取天气信息
        WeatherInfo weather = null;
        if (Boolean.TRUE.equals(request.getConsiderWeather())) {
            weather = weatherTool.getCurrentWeather(null);
        }

        // 3. 调用Agent生成搭配建议
        String inputType = imageUrl != null ? "UPLOAD_PHOTO" : 
                          (request.getDescription() != null ? "TEXT_DESCRIPTION" : "SMART_RECOMMEND");
        
        String agentResponse = outfitAdvisorAgent.generateOutfitAdvice(
                userId,
                inputType,
                request.getOccasion(),
                request.getConsiderWeather(),
                request.getUseExistingClothes(),
                imageUrl,
                request.getDescription()
        );

        // 4. 解析Agent响应
        OutfitAdviceVO adviceVO = parseAgentResponse(agentResponse);
        
        // 5. 补充天气信息
        if (weather != null) {
            adviceVO.setWeather(converter.toWeatherVO(weather));
        }

        // 6. 保存到数据库
        saveAdviceRecord(userId, imageUrl, request, adviceVO);

        return adviceVO;
    }

    @Override
    public OutfitAdviceVO analyzeOutfit(Long userId, String imageUrl) {
        log.info("用户[{}]分析穿搭照片", userId);
        
        // 调用Agent分析
        String agentResponse = outfitAdvisorAgent.generateOutfitAdvice(
                userId,
                "UPLOAD_PHOTO",
                null,
                false,
                true,
                imageUrl,
                null
        );

        return parseAgentResponse(agentResponse);
    }

    @Override
    public PageResult<OutfitAdviceVO> getHistory(Long userId, Integer page, Integer size, 
                                                  String occasion, Boolean isFavorite) {
        // 实现分页查询逻辑
        return null;
    }

    @Override
    public OutfitAdviceVO getDetail(Long userId, Long adviceId) {
        OutfitAdviceRecord record = adviceRecordMapper.selectById(adviceId);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "搭配记录不存在");
        }
        return converter.toVO(record);
    }

    @Override
    @Transactional
    public void deleteAdvice(Long userId, Long adviceId) {
        OutfitAdviceRecord record = adviceRecordMapper.selectById(adviceId);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "搭配记录不存在");
        }
        record.setIsDeleted(true);
        adviceRecordMapper.updateById(record);
    }

    @Override
    @Transactional
    public void toggleFavorite(Long userId, Long adviceId, Boolean isFavorite) {
        OutfitAdviceRecord record = adviceRecordMapper.selectById(adviceId);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "搭配记录不存在");
        }
        record.setIsFavorite(isFavorite);
        adviceRecordMapper.updateById(record);
    }

    @Override
    @Transactional
    public void submitFeedback(Long userId, Long adviceId, OutfitFeedbackRequest request) {
        OutfitAdviceRecord record = adviceRecordMapper.selectById(adviceId);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "搭配记录不存在");
        }

        OutfitFeedback feedback = converter.toEntity(request);
        feedback.setOutfitAdviceId(adviceId);
        feedback.setUserId(userId);
        feedbackMapper.insert(feedback);
    }

    @Override
    public List<OutfitAdviceVO> getRecommendations(Long userId, Integer limit, String occasion) {
        // 实现智能推荐逻辑
        return null;
    }

    // ========== 私有方法 ==========

    private String saveImage(org.springframework.web.multipart.MultipartFile image) {
        // 实现图片保存逻辑
        return "https://cdn.example.com/" + image.getOriginalFilename();
    }

    private OutfitAdviceVO parseAgentResponse(String agentResponse) {
        try {
            // 提取JSON部分
            String jsonStr = extractJson(agentResponse);
            return JSON.parseObject(jsonStr, OutfitAdviceVO.class);
        } catch (Exception e) {
            log.error("解析Agent响应失败: {}", agentResponse, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI解析失败");
        }
    }

    private String extractJson(String text) {
        // 从Agent响应中提取JSON
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private void saveAdviceRecord(Long userId, String imageUrl, 
                                   OutfitAdviceRequest request, 
                                   OutfitAdviceVO adviceVO) {
        // 保存主记录
        OutfitAdviceRecord record = new OutfitAdviceRecord();
        record.setUserId(userId);
        record.setInputType(imageUrl != null ? 1 : 2);
        record.setInputImageUrl(imageUrl);
        record.setInputDescription(request.getDescription());
        
        // 天气信息
        if (adviceVO.getWeather() != null) {
            record.setLocation(adviceVO.getWeather().getLocation());
            record.setTemperature(adviceVO.getWeather().getTemperature());
            record.setWeatherCondition(adviceVO.getWeather().getCondition());
            record.setSeason(adviceVO.getWeather().getSeason());
        }
        
        // AI分析结果
        if (adviceVO.getAnalysis() != null) {
            record.setDetectedItems(JSON.toJSONString(adviceVO.getAnalysis().getDetectedItems()));
            record.setDetectedStyle(adviceVO.getAnalysis().getStyle());
            record.setColorAnalysis(adviceVO.getAnalysis().getColors());
        }
        
        // 搭配方案
        record.setOutfitName(adviceVO.getRecommendation().getOutfitName());
        record.setOutfitDescription(adviceVO.getRecommendation().getOutfitDescription());
        record.setReasoning(adviceVO.getRecommendation().getReasoning());
        record.setFashionSuggestions(adviceVO.getRecommendation().getSuggestions());
        record.setPurchaseRecommendations(JSON.toJSONString(
                adviceVO.getPurchaseRecommendations()));
        
        record.setOccasion(request.getOccasion());
        record.setIsDeleted(false);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        
        adviceRecordMapper.insert(record);
        
        // 保存单品关联
        if (adviceVO.getRecommendation().getItems() != null) {
            List<OutfitItem> items = adviceVO.getRecommendation().getItems().stream()
                    .map(item -> {
                        OutfitItem outfitItem = new OutfitItem();
                        outfitItem.setOutfitAdviceId(record.getId());
                        outfitItem.setClothingId(item.getClothingId());
                        outfitItem.setItemName(item.getName());
                        outfitItem.setItemCategory(item.getCategory());
                        outfitItem.setItemColor(item.getColor());
                        outfitItem.setItemImageUrl(item.getImageUrl());
                        outfitItem.setIsRecommended(item.getIsRecommended());
                        outfitItem.setReason(item.getReason());
                        return outfitItem;
                    })
                    .collect(Collectors.toList());
            
            for (OutfitItem item : items) {
                outfitItemMapper.insert(item);
            }
        }
        
        // 设置返回的ID
        adviceVO.setId(record.getId());
    }
}
```

---

## 7. Controller 层

```java
package com.ecru.aiadvice.controller;

import com.ecru.aiadvice.dto.request.OutfitAdviceRequest;
import com.ecru.aiadvice.dto.request.OutfitFeedbackRequest;
import com.ecru.aiadvice.dto.response.OutfitAdviceVO;
import com.ecru.aiadvice.service.OutfitAdviceService;
import com.ecru.common.result.PageResult;
import com.ecru.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/outfit")
@RequiredArgsConstructor
public class OutfitAdviceController {

    private final OutfitAdviceService outfitAdviceService;

    /**
     * 获取搭配建议 (Agent工作流)
     */
    @PostMapping("/advice")
    public Result<OutfitAdviceVO> getOutfitAdvice(
            @Valid @ModelAttribute OutfitAdviceRequest request,
            HttpServletRequest httpRequest) {
        
        Long userId = getCurrentUserId(httpRequest);
        OutfitAdviceVO advice = outfitAdviceService.getOutfitAdvice(userId, request);
        return Result.success(advice);
    }

    /**
     * 分析穿搭照片
     */
    @PostMapping("/analyze")
    public Result<OutfitAdviceVO> analyzeOutfit(
            @RequestParam("image") String imageUrl,
            HttpServletRequest httpRequest) {
        
        Long userId = getCurrentUserId(httpRequest);
        OutfitAdviceVO analysis = outfitAdviceService.analyzeOutfit(userId, imageUrl);
        return Result.success(analysis);
    }

    /**
     * 获取历史搭配记录
     */
    @GetMapping("/history")
    public Result<PageResult<OutfitAdviceVO>> getHistory(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String occasion,
            @RequestParam(required = false) Boolean isFavorite,
            HttpServletRequest httpRequest) {
        
        Long userId = getCurrentUserId(httpRequest);
        PageResult<OutfitAdviceVO> history = outfitAdviceService
                .getHistory(userId, page, size, occasion, isFavorite);
        return Result.success(history);
    }

    /**
     * 获取搭配详情
     */
    @GetMapping("/history/{id}")
    public Result<OutfitAdviceVO> getDetail(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        
        Long userId = getCurrentUserId(httpRequest);
        OutfitAdviceVO detail = outfitAdviceService.getDetail(userId, id);
        return Result.success(detail);
    }

    /**
     * 删除搭配记录
     */
    @DeleteMapping("/history/{id}")
    public Result<Void> deleteAdvice(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        
        Long userId = getCurrentUserId(httpRequest);
        outfitAdviceService.deleteAdvice(userId, id);
        return Result.success();
    }

    /**
     * 收藏/取消收藏搭配
     */
    @PostMapping("/history/{id}/favorite")
    public Result<Void> toggleFavorite(
            @PathVariable Long id,
            @RequestParam Boolean isFavorite,
            HttpServletRequest httpRequest) {
        
        Long userId = getCurrentUserId(httpRequest);
        outfitAdviceService.toggleFavorite(userId, id, isFavorite);
        return Result.success();
    }

    /**
     * 提交搭配反馈
     */
    @PostMapping("/{id}/feedback")
    public Result<Void> submitFeedback(
            @PathVariable Long id,
            @Valid @RequestBody OutfitFeedbackRequest request,
            HttpServletRequest httpRequest) {
        
        Long userId = getCurrentUserId(httpRequest);
        outfitAdviceService.submitFeedback(userId, id, request);
        return Result.success();
    }

    /**
     * 获取智能推荐搭配
     */
    @GetMapping("/recommendations")
    public Result<List<OutfitAdviceVO>> getRecommendations(
            @RequestParam(defaultValue = "3") Integer limit,
            @RequestParam(required = false) String occasion,
            HttpServletRequest httpRequest) {
        
        Long userId = getCurrentUserId(httpRequest);
        List<OutfitAdviceVO> recommendations = outfitAdviceService
                .getRecommendations(userId, limit, occasion);
        return Result.success(recommendations);
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        // 从JWT Token中获取用户ID
        return (Long) request.getAttribute("userId");
    }
}
```

---

## 8. 数据库Mapper

### 8.1 OutfitAdviceRecordMapper.java

```java
package com.ecru.aiadvice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecru.aiadvice.entity.OutfitAdviceRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OutfitAdviceRecordMapper extends BaseMapper<OutfitAdviceRecord> {

    @Select("SELECT * FROM outfit_advice_records " +
            "WHERE user_id = #{userId} AND is_deleted = false " +
            "ORDER BY created_at DESC " +
            "LIMIT #{offset}, #{limit}")
    List<OutfitAdviceRecord> selectByUserId(@Param("userId") Long userId,
                                            @Param("offset") Integer offset,
                                            @Param("limit") Integer limit);
}
```

### 8.2 OutfitAdviceRecordMapper.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" 
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.ecru.aiadvice.mapper.OutfitAdviceRecordMapper">

    <resultMap id="BaseResultMap" type="com.ecru.aiadvice.entity.OutfitAdviceRecord">
        <id column="id"