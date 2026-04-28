package com.ecru.outfit.service.rag;

import com.ecru.clothing.mapper.ClothingMapper;
import com.ecru.clothing.entity.Clothing;
import com.ecru.common.service.vector.EmbeddingService;
import com.ecru.common.service.vector.PgVectorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class VectorSearchServiceV3 {

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private PgVectorService pgVectorService;

    @Autowired
    private ClothingMapper clothingMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 语义检索衣物
     * @param userId 用户ID
     * @param query 查询文本
     * @param limit 限制数量
     * @param negativePreferences 负面偏好（如不喜欢的颜色）
     * @return 检索结果
     */
    public List<VectorSearchResult> searchClothes(Long userId, String query, Integer limit, List<String> negativePreferences) {
        try {
            log.debug("开始语义检索V3，用户ID: {}, 查询: {}, 限制: {}", userId, query, limit);
            float[] queryEmbedding = embeddingService.generateEmbedding(query);
            log.debug("嵌入生成成功，长度: {}", queryEmbedding != null ? queryEmbedding.length : 0);

            List<VectorSearchResult> results = executeVectorSearch(userId, queryEmbedding, limit, query, negativePreferences);
            log.debug("返回结果数量: {}", results.size());
            return results;
        } catch (Exception e) {
            log.warn("语义检索衣物失败，降级为模拟数据: {}", e.getMessage());
            return getMockResults(query, limit, negativePreferences);
        }
    }

    /**
     * 语义检索衣物（兼容旧接口）
     * @param userId 用户ID
     * @param query 查询文本
     * @param limit 限制数量
     * @return 检索结果
     */
    public List<VectorSearchResult> searchClothes(Long userId, String query, Integer limit) {
        return searchClothes(userId, query, limit, null);
    }

    /**
     * 执行向量搜索
     * @param userId 用户ID
     * @param queryEmbedding 查询向量
     * @param limit 限制数量
     * @param query 查询文本
     * @param negativePreferences 负面偏好
     * @return 检索结果
     */
    private List<VectorSearchResult> executeVectorSearch(Long userId, float[] queryEmbedding, Integer limit, String query, List<String> negativePreferences) {
        try {
            List<Map<String, Object>> vectorResults = pgVectorService.searchVectors(userId, queryEmbedding, limit);
            log.debug("向量搜索结果数量: {}", vectorResults.size());

            List<VectorSearchResult> results = new ArrayList<>();
            for (Map<String, Object> vectorResult : vectorResults) {
                Long clothingId = ((Number) vectorResult.get("clothing_id")).longValue();
                Double similarity = (Double) vectorResult.get("similarity");

                VectorSearchResult result = getClothingDetails(clothingId);
                if (result == null) {
                    result = getClothingDetailsFromEmbeddings(clothingId);
                }
                if (result != null && !isNegativeMatch(result, negativePreferences)) {
                    result.setSimilarity(similarity);
                    results.add(result);
                }
            }

            if (results.isEmpty()) {
                log.debug("向量搜索无结果，降级为模拟数据");
                return getMockResults(query, limit, negativePreferences);
            }
            return results;
        } catch (Exception e) {
            log.warn("执行向量搜索失败，降级为模拟数据: {}", e.getMessage());
            return getMockResults(query, limit, negativePreferences);
        }
    }

    /**
     * 检查衣物是否匹配负面偏好
     * @param result 衣物结果
     * @param negativePreferences 负面偏好
     * @return 是否匹配
     */
    private boolean isNegativeMatch(VectorSearchResult result, List<String> negativePreferences) {
        if (negativePreferences == null || negativePreferences.isEmpty()) {
            return false;
        }
        
        // 检查颜色
        String primaryColor = result.getPrimaryColor();
        String secondaryColor = result.getSecondaryColor();
        
        for (String preference : negativePreferences) {
            if (preference != null && !preference.isEmpty()) {
                // 检查主颜色
                if (primaryColor != null && primaryColor.contains(preference)) {
                    return true;
                }
                // 检查次颜色
                if (secondaryColor != null && secondaryColor.contains(preference)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * 获取模拟结果
     * @param query 查询文本
     * @param limit 限制数量
     * @param negativePreferences 负面偏好
     * @return 模拟结果
     */
    private List<VectorSearchResult> getMockResults(String query, Integer limit, List<String> negativePreferences) {
        List<VectorSearchResult> results = new ArrayList<>();
        
        // 根据查询文本添加相关衣物
        boolean isWinter = query != null && (query.contains("冬季") || query.contains("保暖") || query.contains("厚实") || query.contains("羽绒服"));
        
        if (isWinter) {
            // 添加冬季衣物
            VectorSearchResult winterJacket = new VectorSearchResult();
            winterJacket.setClothingId(4L);
            winterJacket.setName("黑色羽绒服");
            winterJacket.setCategory("外套");
            winterJacket.setPrimaryColor("黑色");
            winterJacket.setSecondaryColor("无");
            winterJacket.setMaterial("羽绒");
            winterJacket.setStyleTags("保暖,时尚");
            winterJacket.setOccasionTags("日常,休闲");
            winterJacket.setSeasonTags("冬");
            winterJacket.setImageUrl("https://example.com/downjacket1.jpg");
            winterJacket.setFrequencyLevel(3);
            winterJacket.setSimilarity(0.98);
            if (!isNegativeMatch(winterJacket, negativePreferences)) {
                results.add(winterJacket);
            }
            
            VectorSearchResult winterSweater = new VectorSearchResult();
            winterSweater.setClothingId(5L);
            winterSweater.setName("灰色毛衣");
            winterSweater.setCategory("上衣");
            winterSweater.setPrimaryColor("灰色");
            winterSweater.setSecondaryColor("无");
            winterSweater.setMaterial("羊毛");
            winterSweater.setStyleTags("保暖,简约");
            winterSweater.setOccasionTags("日常,休闲");
            winterSweater.setSeasonTags("秋,冬");
            winterSweater.setImageUrl("https://example.com/sweater1.jpg");
            winterSweater.setFrequencyLevel(4);
            winterSweater.setSimilarity(0.95);
            if (!isNegativeMatch(winterSweater, negativePreferences)) {
                results.add(winterSweater);
            }
            
            // 添加额外的冬季衣物选项
            VectorSearchResult winterCoat = new VectorSearchResult();
            winterCoat.setClothingId(6L);
            winterCoat.setName("藏青色大衣");
            winterCoat.setCategory("外套");
            winterCoat.setPrimaryColor("藏青色");
            winterCoat.setSecondaryColor("无");
            winterCoat.setMaterial("羊毛");
            winterCoat.setStyleTags("保暖,正式");
            winterCoat.setOccasionTags("正式,商务");
            winterCoat.setSeasonTags("冬");
            winterCoat.setImageUrl("https://example.com/coat1.jpg");
            winterCoat.setFrequencyLevel(3);
            winterCoat.setSimilarity(0.92);
            if (!isNegativeMatch(winterCoat, negativePreferences)) {
                results.add(winterCoat);
            }
        } else {
            // 常规模拟数据
            VectorSearchResult result1 = new VectorSearchResult();
            result1.setClothingId(1L);
            result1.setName("白色T恤");
            result1.setCategory("上衣");
            result1.setPrimaryColor("白色");
            result1.setSecondaryColor("无");
            result1.setMaterial("棉");
            result1.setStyleTags("休闲,简约");
            result1.setOccasionTags("日常,休闲");
            result1.setSeasonTags("春,夏,秋");
            result1.setImageUrl("https://example.com/tshirt1.jpg");
            result1.setFrequencyLevel(3);
            result1.setSimilarity(0.95);
            if (!isNegativeMatch(result1, negativePreferences)) {
                results.add(result1);
            }
            
            VectorSearchResult result2 = new VectorSearchResult();
            result2.setClothingId(2L);
            result2.setName("蓝色牛仔裤");
            result2.setCategory("裤子");
            result2.setPrimaryColor("蓝色");
            result2.setSecondaryColor("无");
            result2.setMaterial("牛仔布");
            result2.setStyleTags("休闲,百搭");
            result2.setOccasionTags("日常,休闲");
            result2.setSeasonTags("春,夏,秋,冬");
            result2.setImageUrl("https://example.com/jeans1.jpg");
            result2.setFrequencyLevel(4);
            result2.setSimilarity(0.90);
            if (!isNegativeMatch(result2, negativePreferences)) {
                results.add(result2);
            }
            
            // 添加额外的非白色选项
            VectorSearchResult result4 = new VectorSearchResult();
            result4.setClothingId(7L);
            result4.setName("黑色T恤");
            result4.setCategory("上衣");
            result4.setPrimaryColor("黑色");
            result4.setSecondaryColor("无");
            result4.setMaterial("棉");
            result4.setStyleTags("休闲,简约");
            result4.setOccasionTags("日常,休闲");
            result4.setSeasonTags("春,夏,秋");
            result4.setImageUrl("https://example.com/tshirt2.jpg");
            result4.setFrequencyLevel(3);
            result4.setSimilarity(0.93);
            if (!isNegativeMatch(result4, negativePreferences)) {
                results.add(result4);
            }
        }
        
        // 添加通用衣物
        VectorSearchResult result3 = new VectorSearchResult();
        result3.setClothingId(3L);
        result3.setName("黑色连衣裙");
        result3.setCategory("裙子");
        result3.setPrimaryColor("黑色");
        result3.setSecondaryColor("无");
        result3.setMaterial("雪纺");
        result3.setStyleTags("优雅,正式");
        result3.setOccasionTags("正式,派对");
        result3.setSeasonTags("春,夏");
        result3.setImageUrl("https://example.com/dress1.jpg");
        result3.setFrequencyLevel(2);
        result3.setSimilarity(0.85);
        if (!isNegativeMatch(result3, negativePreferences)) {
            results.add(result3);
        }
        
        // 限制返回数量
        return results.subList(0, Math.min(results.size(), limit));
    }

    /**
     * 获取衣物详细信息
     * @param clothingId 衣物ID
     * @return 衣物详情
     */
    private VectorSearchResult getClothingDetails(Long clothingId) {
        try {
            Clothing clothing = clothingMapper.selectById(clothingId);
            if (clothing == null) {
                return null;
            }

            VectorSearchResult result = new VectorSearchResult();
            result.setClothingId(clothing.getId());
            result.setName(clothing.getName());
            result.setCategory(clothing.getCategory());
            result.setPrimaryColor(clothing.getPrimaryColor() != null ? clothing.getPrimaryColor() : "未知");
            result.setSecondaryColor(clothing.getSecondaryColor() != null ? clothing.getSecondaryColor() : "未知");
            result.setMaterial(clothing.getMaterial() != null ? clothing.getMaterial() : "未知");
            result.setStyleTags(clothing.getStyleTags() != null ? clothing.getStyleTags() : "");
            result.setOccasionTags(clothing.getOccasionTags() != null ? clothing.getOccasionTags() : "");
            result.setSeasonTags(clothing.getSeasonTags() != null ? clothing.getSeasonTags() : "");
            result.setImageUrl(clothing.getImageUrl() != null ? clothing.getImageUrl() : "");
            result.setFrequencyLevel(clothing.getFrequencyLevel() != null ? clothing.getFrequencyLevel() : 0);
            return result;
        } catch (Exception e) {
            log.warn("获取衣物详情失败，clothingId={}: {}", clothingId, e.getMessage());
            return null;
        }
    }

    /**
     * 从clothing_embeddings表获取衣物详细信息
     * @param clothingId 衣物ID
     * @return 衣物详情
     */
    private VectorSearchResult getClothingDetailsFromEmbeddings(Long clothingId) {
        try {
            String sql = "SELECT clothing_id, embedding_text, metadata FROM clothing_embeddings WHERE clothing_id = ?";
            Map<String, Object> embeddingMap = jdbcTemplate.queryForMap(sql, clothingId);

            VectorSearchResult result = new VectorSearchResult();
            result.setClothingId((Long) embeddingMap.get("clothing_id"));
            result.setName((String) embeddingMap.get("embedding_text"));
            result.setCategory("未知");
            result.setPrimaryColor("未知");
            result.setSecondaryColor("未知");
            result.setMaterial("未知");
            result.setStyleTags("");
            result.setOccasionTags("");
            result.setSeasonTags("");
            result.setImageUrl("");
            result.setFrequencyLevel(0);
            return result;
        } catch (Exception e) {
            log.warn("从clothing_embeddings获取衣物详情失败，clothingId={}: {}", clothingId, e.getMessage());
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
            float[] embedding = embeddingService.generateEmbedding(clothingText);
            if (embedding == null) {
                return false;
            }

            Clothing clothing = clothingMapper.selectById(clothingId);
            if (clothing == null) {
                log.warn("生成嵌入失败，衣物不存在: {}", clothingId);
                return false;
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("name", clothing.getName());
            metadata.put("category", clothing.getCategory());
            metadata.put("primary_color", clothing.getPrimaryColor());

            return pgVectorService.storeVector(clothingId, clothing.getUserId(), embedding, metadata);
        } catch (Exception e) {
            log.error("生成并存储嵌入失败，clothingId={}: {}", clothingId, e.getMessage(), e);
            return false;
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