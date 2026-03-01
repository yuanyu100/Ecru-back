package com.ecru.outfit.service.rag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 向量检索服务
 */
@Service
public class VectorSearchService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private EmbeddingService embeddingService;

    /**
     * 语义检索衣物
     * @param userId 用户ID
     * @param query 查询文本
     * @param limit 限制数量
     * @return 检索结果
     */
    public List<VectorSearchResult> searchClothes(Long userId, String query, Integer limit) {
        try {
            // 生成查询向量
            float[] queryEmbedding = embeddingService.generateEmbedding(query);
            if (queryEmbedding == null) {
                return new ArrayList<>();
            }

            // 构建缓存键
            String cacheKey = "vector_search:" + userId + ":" + query + ":" + limit;

            // 尝试从缓存获取
            boolean enableCache = true;
            if (enableCache) {
                String cachedData = redisTemplate.opsForValue().get(cacheKey);
                if (cachedData != null) {
                    return parseSearchResults(cachedData);
                }
            }

            // 执行向量搜索（模拟）
            List<VectorSearchResult> results = executeVectorSearch(userId, queryEmbedding, limit);

            // 缓存结果
            if (!results.isEmpty() && enableCache) {
                redisTemplate.opsForValue().set(
                        cacheKey,
                        formatSearchResults(results),
                        3600,
                        TimeUnit.SECONDS
                );
            }

            return results;
        } catch (Exception e) {
            System.err.println("语义检索衣物失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 执行向量搜索
     * @param userId 用户ID
     * @param queryEmbedding 查询向量
     * @param limit 限制数量
     * @return 检索结果
     */
    private List<VectorSearchResult> executeVectorSearch(Long userId, float[] queryEmbedding, Integer limit) {
        try {
            // 模拟向量搜索结果
            List<VectorSearchResult> results = new ArrayList<>();
            for (int i = 0; i < Math.min(limit, 5); i++) {
                VectorSearchResult result = new VectorSearchResult();
                result.setClothingId((long) (i + 1));
                result.setName("示例衣物 " + (i + 1));
                result.setCategory("上装");
                result.setPrimaryColor("白色");
                result.setSecondaryColor("蓝色");
                result.setMaterial("棉");
                result.setStyleTags("简约,休闲");
                result.setOccasionTags("日常,通勤");
                result.setSeasonTags("春,秋");
                result.setImageUrl("https://example.com/clothing" + (i + 1) + ".jpg");
                result.setFrequencyLevel(3);
                result.setSimilarity(0.85 - i * 0.05);
                results.add(result);
            }

            return results;
        } catch (Exception e) {
            System.err.println("执行向量搜索失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 为衣物生成并存储嵌入
     * @param clothingId 衣物ID
     * @param clothingText 衣物文本描述
     * @return 是否成功
     */
    public boolean generateAndStoreEmbedding(Long clothingId, String clothingText) {
        try {
            // 生成嵌入
            float[] embedding = embeddingService.generateEmbedding(clothingText);
            if (embedding == null) {
                return false;
            }

            // 模拟存储到数据库
            System.out.println("生成并存储嵌入成功: " + clothingId);
            return true;
        } catch (Exception e) {
            System.err.println("生成并存储嵌入失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 解析搜索结果
     * @param resultsJson 结果JSON
     * @return 搜索结果列表
     */
    private List<VectorSearchResult> parseSearchResults(String resultsJson) {
        try {
            return com.alibaba.fastjson2.JSON.parseArray(resultsJson, VectorSearchResult.class);
        } catch (Exception e) {
            System.err.println("解析搜索结果失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 格式化搜索结果
     * @param results 搜索结果列表
     * @return 格式化的JSON
     */
    private String formatSearchResults(List<VectorSearchResult> results) {
        try {
            return com.alibaba.fastjson2.JSON.toJSONString(results);
        } catch (Exception e) {
            System.err.println("格式化搜索结果失败: " + e.getMessage());
            return "[]";
        }
    }

    /**
     * 向量搜索结果
     */
    public static class VectorSearchResult {
        private Long clothingId;
        private String name;
        private String category;
        private String primaryColor;
        private String secondaryColor;
        private String material;
        private String styleTags;
        private String occasionTags;
        private String seasonTags;
        private String imageUrl;
        private Integer frequencyLevel;
        private Double similarity;

        // Getters and Setters
        public Long getClothingId() { return clothingId; }
        public void setClothingId(Long clothingId) { this.clothingId = clothingId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getPrimaryColor() { return primaryColor; }
        public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }
        public String getSecondaryColor() { return secondaryColor; }
        public void setSecondaryColor(String secondaryColor) { this.secondaryColor = secondaryColor; }
        public String getMaterial() { return material; }
        public void setMaterial(String material) { this.material = material; }
        public String getStyleTags() { return styleTags; }
        public void setStyleTags(String styleTags) { this.styleTags = styleTags; }
        public String getOccasionTags() { return occasionTags; }
        public void setOccasionTags(String occasionTags) { this.occasionTags = occasionTags; }
        public String getSeasonTags() { return seasonTags; }
        public void setSeasonTags(String seasonTags) { this.seasonTags = seasonTags; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public Integer getFrequencyLevel() { return frequencyLevel; }
        public void setFrequencyLevel(Integer frequencyLevel) { this.frequencyLevel = frequencyLevel; }
        public Double getSimilarity() { return similarity; }
        public void setSimilarity(Double similarity) { this.similarity = similarity; }
    }

}
