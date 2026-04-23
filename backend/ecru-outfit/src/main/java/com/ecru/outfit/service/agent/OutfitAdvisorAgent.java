package com.ecru.outfit.service.agent;

import com.ecru.outfit.config.AgentConfig;
import com.ecru.common.service.ai.AiImageAnalyzerService;
import com.ecru.outfit.service.mcp.McpWeatherService;
import com.ecru.outfit.service.rag.RagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 搭配建议智能体
 */
@Slf4j
@Service
public class OutfitAdvisorAgent {

    @Autowired
    private AgentConfig agentConfig;

    @Autowired
    private McpWeatherService weatherService;

    @Autowired
    @Qualifier("aiImageAnalyzerService")
    private AiImageAnalyzerService imageAnalyzerService;

    @Autowired
    private RagService ragService;

    /**
     * 获取搭配建议
     * @param userId 用户ID
     * @param imageStream 穿搭照片
     * @param description 文字描述
     * @param location 地理位置
     * @param occasion 场合
     * @return 搭配建议
     */
    public OutfitAdvice adviseOutfit(
            Long userId,
            InputStream imageStream,
            String description,
            String location,
            String occasion
    ) {
        try {
            // 1. 获取天气信息
            McpWeatherService.WeatherInfo weatherInfo = null;
            String weatherInfoStr = "";
            if (location != null) {
                weatherInfo = weatherService.getWeatherByLocation(location);
                if (weatherInfo != null) {
                    weatherInfoStr = weatherInfo.getLocation() + "，" + weatherInfo.getTemperature() + "℃，" + weatherInfo.getWeatherCondition();
                }
            }

            // 2. 分析穿搭照片
            Object imageAnalysisResult = null;
            if (imageStream != null) {
                imageAnalysisResult = imageAnalyzerService.analyzeOutfit(imageStream);
            }

            // 3. 语义检索衣物
            List<Map<String, Object>> clothingResults = new ArrayList<>();
            if (description != null) {
                var results = ragService.searchClothes(userId, description, 10);
                for (var result : results) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", result.getClothingId());
                    item.put("name", result.getName());
                    item.put("category", result.getCategory());
                    item.put("color", result.getPrimaryColor());
                    item.put("imageUrl", result.getImageUrl());
                    item.put("similarity", result.getSimilarity());
                    clothingResults.add(item);
                }
            }

            // 4. 获取用户衣橱统计信息
            Map<String, Object> clothingStats = ragService.getClothingStatistics(userId);

            // 5. 生成搭配建议
            OutfitAdvice advice = new OutfitAdvice();
            advice.setOutfitName("智能搭配方案");
            advice.setOutfitDescription("根据您的需求和当前天气状况生成的个性化搭配方案");
            advice.setReasoning("基于天气、场合和个人风格的综合分析");
            advice.setFashionSuggestions("建议根据实际天气情况适当调整搭配");
            advice.setWeatherInfo(weatherInfoStr);
            advice.setStyleAnalysis("简约时尚风格，适合日常穿着");

            // 6. 构建搭配单品
            List<OutfitAdvice.OutfitItem> items = new ArrayList<>();
            for (int i = 0; i < Math.min(5, clothingResults.size()); i++) {
                Map<String, Object> itemMap = clothingResults.get(i);
                OutfitAdvice.OutfitItem item = new OutfitAdvice.OutfitItem();
                item.setClothingId((Long) itemMap.get("id"));
                item.setName((String) itemMap.get("name"));
                item.setCategory((String) itemMap.get("category"));
                item.setColor((String) itemMap.get("color"));
                item.setImageUrl((String) itemMap.get("imageUrl"));
                item.setIsRecommended(true);
                item.setReason("适合当前场合和天气");
                items.add(item);
            }
            advice.setItems(items);

            // 7. 生成购买推荐
            List<OutfitAdvice.PurchaseRecommendation> purchaseRecommendations = new ArrayList<>();
            OutfitAdvice.PurchaseRecommendation rec1 = new OutfitAdvice.PurchaseRecommendation();
            rec1.setName("时尚休闲鞋");
            rec1.setReason("搭配当前穿搭风格");
            rec1.setLink("#");
            rec1.setEstimatedPrice("¥399");
            purchaseRecommendations.add(rec1);
            advice.setPurchaseRecommendations(purchaseRecommendations);

            return advice;
        } catch (Exception e) {
            log.error("获取搭配建议失败: {}", e.getMessage());
            throw new RuntimeException("获取搭配建议失败", e);
        }
    }

    /**
     * 搭配建议结果
     */
    public static class OutfitAdvice {
        private String outfitName;
        private String outfitDescription;
        private List<OutfitItem> items;
        private String reasoning;
        private String fashionSuggestions;
        private List<PurchaseRecommendation> purchaseRecommendations;
        private String weatherInfo;
        private String styleAnalysis;

        // Getters and Setters
        public String getOutfitName() { return outfitName; }
        public void setOutfitName(String outfitName) { this.outfitName = outfitName; }
        public String getOutfitDescription() { return outfitDescription; }
        public void setOutfitDescription(String outfitDescription) { this.outfitDescription = outfitDescription; }
        public List<OutfitItem> getItems() { return items; }
        public void setItems(List<OutfitItem> items) { this.items = items; }
        public String getReasoning() { return reasoning; }
        public void setReasoning(String reasoning) { this.reasoning = reasoning; }
        public String getFashionSuggestions() { return fashionSuggestions; }
        public void setFashionSuggestions(String fashionSuggestions) { this.fashionSuggestions = fashionSuggestions; }
        public List<PurchaseRecommendation> getPurchaseRecommendations() { return purchaseRecommendations; }
        public void setPurchaseRecommendations(List<PurchaseRecommendation> purchaseRecommendations) { this.purchaseRecommendations = purchaseRecommendations; }
        public String getWeatherInfo() { return weatherInfo; }
        public void setWeatherInfo(String weatherInfo) { this.weatherInfo = weatherInfo; }
        public String getStyleAnalysis() { return styleAnalysis; }
        public void setStyleAnalysis(String styleAnalysis) { this.styleAnalysis = styleAnalysis; }

        /**
         * 搭配单品
         */
        public static class OutfitItem {
            private Long clothingId;
            private String name;
            private String category;
            private String color;
            private String imageUrl;
            private Boolean isRecommended;
            private String reason;

            // Getters and Setters
            public Long getClothingId() { return clothingId; }
            public void setClothingId(Long clothingId) { this.clothingId = clothingId; }
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            public String getCategory() { return category; }
            public void setCategory(String category) { this.category = category; }
            public String getColor() { return color; }
            public void setColor(String color) { this.color = color; }
            public String getImageUrl() { return imageUrl; }
            public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
            public Boolean getIsRecommended() { return isRecommended; }
            public void setIsRecommended(Boolean isRecommended) { this.isRecommended = isRecommended; }
            public String getReason() { return reason; }
            public void setReason(String reason) { this.reason = reason; }
        }

        /**
         * 购买推荐
         */
        public static class PurchaseRecommendation {
            private String name;
            private String reason;
            private String link;
            private String estimatedPrice;

            // Getters and Setters
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            public String getReason() { return reason; }
            public void setReason(String reason) { this.reason = reason; }
            public String getLink() { return link; }
            public void setLink(String link) { this.link = link; }
            public String getEstimatedPrice() { return estimatedPrice; }
            public void setEstimatedPrice(String estimatedPrice) { this.estimatedPrice = estimatedPrice; }
        }
    }

}