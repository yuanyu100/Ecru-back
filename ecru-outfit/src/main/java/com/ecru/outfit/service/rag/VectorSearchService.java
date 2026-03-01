package com.ecru.outfit.service.rag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Autowired
    private PgVectorService pgVectorService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

            // 执行向量搜索
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
            // 使用pgvector执行向量检索
            List<Map<String, Object>> vectorResults = pgVectorService.searchVectors(userId, queryEmbedding, limit);
            
            // 构建结果列表
            List<VectorSearchResult> results = new ArrayList<>();
            for (Map<String, Object> vectorResult : vectorResults) {
                Long clothingId = ((Number) vectorResult.get("clothing_id")).longValue();
                Double similarity = (Double) vectorResult.get("similarity");
                
                // 查询衣物详细信息
                VectorSearchResult result = getClothingDetails(clothingId);
                if (result != null) {
                    result.setSimilarity(similarity);
                    results.add(result);
                }
            }

            return results;
        } catch (Exception e) {
            System.err.println("执行向量搜索失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 获取衣物详细信息
     * @param clothingId 衣物ID
     * @return 衣物详情
     */
    private VectorSearchResult getClothingDetails(Long clothingId) {
        try {
            String sql = """
            SELECT 
                c.id, c.name, c.category, c.sub_category, 
                c.primary_color, c.secondary_color, c.material, 
                c.style_tags, c.occasion_tags, c.season_tags, 
                c.image_url, c.frequency_level
            FROM 
                clothing c
            WHERE 
                c.id = ? AND c.is_deleted = 0
            """;
            
            Map<String, Object> clothingMap = jdbcTemplate.queryForMap(sql, clothingId);
            
            VectorSearchResult result = new VectorSearchResult();
            result.setClothingId((Long) clothingMap.get("id"));
            result.setName((String) clothingMap.get("name"));
            result.setCategory((String) clothingMap.get("category"));
            result.setPrimaryColor((String) clothingMap.get("primary_color"));
            result.setSecondaryColor((String) clothingMap.get("secondary_color"));
            result.setMaterial((String) clothingMap.get("material"));
            result.setStyleTags((String) clothingMap.get("style_tags"));
            result.setOccasionTags((String) clothingMap.get("occasion_tags"));
            result.setSeasonTags((String) clothingMap.get("season_tags"));
            result.setImageUrl((String) clothingMap.get("image_url"));
            result.setFrequencyLevel((Integer) clothingMap.get("frequency_level"));
            
            return result;
        } catch (Exception e) {
            System.err.println("获取衣物详情失败: " + e.getMessage());
            return null;
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

            // 查询衣物信息以获取用户ID和其他元数据
            String sql = "SELECT user_id, name, category, primary_color FROM clothing WHERE id = ?";
            Map<String, Object> clothingMap = jdbcTemplate.queryForMap(sql, clothingId);
            Long userId = (Long) clothingMap.get("user_id");
            
            // 构建元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("name", clothingMap.get("name"));
            metadata.put("category", clothingMap.get("category"));
            metadata.put("primary_color", clothingMap.get("primary_color"));
            
            // 存储到pgvector
            return pgVectorService.storeVector(clothingId, userId, embedding, metadata);
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
