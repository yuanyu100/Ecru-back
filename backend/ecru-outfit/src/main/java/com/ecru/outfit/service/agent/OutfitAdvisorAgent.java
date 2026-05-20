package com.ecru.outfit.service.agent;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ecru.common.service.ai.AiImageAnalyzerService;
import com.ecru.outfit.service.mcp.McpWeatherService;
import com.ecru.outfit.service.rag.RagService;
import com.ecru.outfit.service.rag.VectorSearchServiceV3;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OutfitAdvisorAgent {

    @Autowired
    private McpWeatherService weatherService;

    @Autowired
    @Qualifier("aiImageAnalyzerService")
    private AiImageAnalyzerService imageAnalyzerService;

    @Autowired
    private RagService ragService;

    @Autowired
    private OpenAiChatModel openAiChatModel;

    @Autowired
    private OutfitAdvisorTools outfitAdvisorTools;

    public OutfitAdvice adviseOutfit(
            Long userId,
            InputStream imageStream,
            String description,
            String location,
            String occasion
    ) {
        String safeDescription = normalize(description);
        String safeLocation = normalize(location);
        String safeOccasion = normalize(occasion);
        String imageAnalysisSummary = summarizeImage(imageStream);
        String wardrobeQuery = buildWardrobeQuery(safeDescription, safeOccasion, imageAnalysisSummary);
        String weatherInfo = loadWeatherInfo(safeLocation);
        List<VectorSearchServiceV3.VectorSearchResult> candidateResults = loadWardrobeCandidates(userId, wardrobeQuery);

        try {
            LangChain4jOutfitAdvisor advisor = AiServices.builder(LangChain4jOutfitAdvisor.class)
                    .chatModel(openAiChatModel)
                    .tools(outfitAdvisorTools)
                    .build();

            String rawResponse = advisor.advise(
                    userId,
                    safeDescription,
                    safeLocation,
                    safeOccasion,
                    wardrobeQuery,
                    imageAnalysisSummary
            );

            return mapToAdvice(rawResponse, candidateResults, weatherInfo, safeOccasion, safeDescription);
        } catch (Exception e) {
            log.error("Failed to generate outfit advice with LangChain4j: {}", e.getMessage(), e);
            return buildFallbackAdvice(candidateResults, weatherInfo, safeOccasion, safeDescription, imageAnalysisSummary);
        }
    }

    private String summarizeImage(InputStream imageStream) {
        if (imageStream == null) {
            return "";
        }

        try {
            AiImageAnalyzerService.ImageAnalysisResult result = imageAnalyzerService.analyzeOutfit(imageStream);
            if (result == null) {
                return "";
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("style", result.getStyle());
            payload.put("colorAnalysis", result.getColorAnalysis());
            payload.put("occasion", result.getOccasion());
            payload.put("season", result.getSeason());
            payload.put("suggestions", result.getSuggestions());
            payload.put("detectedItems", result.getDetectedItems());
            return JSON.toJSONString(payload);
        } catch (Exception e) {
            log.warn("Failed to analyze outfit image: {}", e.getMessage());
            return "";
        }
    }

    private String loadWeatherInfo(String location) {
        if (location.isBlank()) {
            return "";
        }

        McpWeatherService.WeatherInfo weatherInfo = weatherService.getWeatherByLocation(location);
        if (weatherInfo == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        if (weatherInfo.getLocation() != null) {
            builder.append(weatherInfo.getLocation()).append(' ');
        }
        if (weatherInfo.getTemperature() != null) {
            builder.append(weatherInfo.getTemperature()).append("C ");
        }
        if (weatherInfo.getWeatherCondition() != null) {
            builder.append(weatherInfo.getWeatherCondition()).append(' ');
        }
        if (weatherInfo.getHumidity() != null) {
            builder.append("humidity ").append(weatherInfo.getHumidity()).append(' ');
        }
        return builder.toString().trim();
    }

    private List<VectorSearchServiceV3.VectorSearchResult> loadWardrobeCandidates(Long userId, String wardrobeQuery) {
        if (userId == null || wardrobeQuery.isBlank()) {
            return List.of();
        }
        return ragService.searchClothes(userId, wardrobeQuery, 10);
    }

    private String buildWardrobeQuery(String description, String occasion, String imageAnalysisSummary) {
        StringBuilder builder = new StringBuilder();
        appendIfPresent(builder, description);
        appendIfPresent(builder, occasion);
        if (!imageAnalysisSummary.isBlank()) {
            JSONObject imageJson = JSON.parseObject(imageAnalysisSummary);
            appendIfPresent(builder, imageJson.getString("style"));
            appendIfPresent(builder, imageJson.getString("occasion"));
            appendIfPresent(builder, imageJson.getString("season"));
            appendIfPresent(builder, imageJson.getString("colorAnalysis"));
        }
        if (builder.length() == 0) {
            builder.append("daily outfit");
        }
        return builder.toString().trim();
    }

    private void appendIfPresent(StringBuilder builder, String value) {
        if (value != null && !value.isBlank()) {
            builder.append(value).append(' ');
        }
    }

    private OutfitAdvice mapToAdvice(
            String rawResponse,
            List<VectorSearchServiceV3.VectorSearchResult> candidateResults,
            String fallbackWeatherInfo,
            String occasion,
            String description
    ) {
        try {
            JSONObject json = extractJson(rawResponse);
            if (json == null) {
                return buildFallbackAdvice(candidateResults, fallbackWeatherInfo, occasion, description, "");
            }

            OutfitAdvice advice = new OutfitAdvice();
            advice.setOutfitName(defaultIfBlank(json.getString("outfitName"), "AI Outfit Suggestion"));
            advice.setOutfitDescription(defaultIfBlank(json.getString("outfitDescription"), "A practical outfit generated from the user's wardrobe."));
            advice.setReasoning(defaultIfBlank(json.getString("reasoning"), "The recommendation balances the user's wardrobe, occasion and practicality."));
            advice.setFashionSuggestions(defaultIfBlank(json.getString("fashionSuggestions"), "Keep the overall look coordinated and comfortable."));
            advice.setStyleAnalysis(defaultIfBlank(json.getString("styleAnalysis"), "The selected wardrobe leans toward a balanced, wearable style."));
            advice.setWeatherInfo(defaultIfBlank(json.getString("weatherInfo"), fallbackWeatherInfo));
            advice.setItems(resolveRecommendedItems(json.getJSONArray("recommendedItems"), candidateResults));
            advice.setPurchaseRecommendations(resolvePurchaseRecommendations(json.getJSONArray("purchaseRecommendations")));

            if (advice.getItems().isEmpty()) {
                advice.setItems(buildFallbackItems(candidateResults));
            }

            return advice;
        } catch (Exception e) {
            log.warn("Failed to parse outfit advice response: {}", e.getMessage());
            return buildFallbackAdvice(candidateResults, fallbackWeatherInfo, occasion, description, "");
        }
    }

    private JSONObject extractJson(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return null;
        }

        String content = rawResponse.trim();
        if (content.startsWith("```")) {
            int firstLineBreak = content.indexOf('\n');
            if (firstLineBreak >= 0) {
                content = content.substring(firstLineBreak + 1);
            }
            if (content.endsWith("```")) {
                content = content.substring(0, content.length() - 3);
            }
            content = content.trim();
        }

        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            content = content.substring(start, end + 1);
        }

        return JSON.parseObject(content);
    }

    private List<OutfitAdvice.OutfitItem> resolveRecommendedItems(
            JSONArray recommendedItems,
            List<VectorSearchServiceV3.VectorSearchResult> candidateResults
    ) {
        Map<Long, VectorSearchServiceV3.VectorSearchResult> candidateMap = candidateResults.stream()
                .filter(item -> item.getClothingId() != null)
                .collect(Collectors.toMap(
                        VectorSearchServiceV3.VectorSearchResult::getClothingId,
                        item -> item,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        List<OutfitAdvice.OutfitItem> items = new ArrayList<>();
        if (recommendedItems == null) {
            return items;
        }

        for (int i = 0; i < recommendedItems.size() && items.size() < 5; i++) {
            JSONObject itemJson = recommendedItems.getJSONObject(i);
            if (itemJson == null) {
                continue;
            }

            Long clothingId = itemJson.getLong("clothingId");
            VectorSearchServiceV3.VectorSearchResult candidate = candidateMap.get(clothingId);
            if (candidate == null) {
                continue;
            }

            OutfitAdvice.OutfitItem item = new OutfitAdvice.OutfitItem();
            item.setClothingId(candidate.getClothingId());
            item.setName(defaultIfBlank(itemJson.getString("name"), candidate.getName()));
            item.setCategory(defaultIfBlank(itemJson.getString("category"), candidate.getCategory()));
            item.setColor(defaultIfBlank(itemJson.getString("color"), candidate.getPrimaryColor()));
            item.setImageUrl(defaultIfBlank(itemJson.getString("imageUrl"), candidate.getImageUrl()));
            Boolean isRecommended = itemJson.getBoolean("isRecommended");
            item.setIsRecommended(isRecommended == null || isRecommended);
            item.setReason(defaultIfBlank(itemJson.getString("reason"), "Matches the recommended outfit direction."));
            items.add(item);
        }

        return items;
    }

    private List<OutfitAdvice.PurchaseRecommendation> resolvePurchaseRecommendations(JSONArray recommendations) {
        List<OutfitAdvice.PurchaseRecommendation> result = new ArrayList<>();
        if (recommendations == null) {
            return result;
        }

        for (int i = 0; i < recommendations.size() && result.size() < 3; i++) {
            JSONObject json = recommendations.getJSONObject(i);
            if (json == null) {
                continue;
            }

            String name = json.getString("name");
            String reason = json.getString("reason");
            if ((name == null || name.isBlank()) && (reason == null || reason.isBlank())) {
                continue;
            }

            OutfitAdvice.PurchaseRecommendation recommendation = new OutfitAdvice.PurchaseRecommendation();
            recommendation.setName(defaultIfBlank(name, "Optional accessory"));
            recommendation.setReason(defaultIfBlank(reason, "Can complement the current wardrobe."));
            recommendation.setLink(defaultIfBlank(json.getString("link"), "#"));
            recommendation.setEstimatedPrice(defaultIfBlank(json.getString("estimatedPrice"), ""));
            result.add(recommendation);
        }
        return result;
    }

    private OutfitAdvice buildFallbackAdvice(
            List<VectorSearchServiceV3.VectorSearchResult> candidateResults,
            String weatherInfo,
            String occasion,
            String description,
            String imageAnalysisSummary
    ) {
        OutfitAdvice advice = new OutfitAdvice();
        advice.setOutfitName("Smart Wardrobe Recommendation");
        advice.setOutfitDescription("This outfit is assembled from the best matching items currently available in the user's wardrobe.");
        advice.setReasoning(buildFallbackReasoning(weatherInfo, occasion, description, imageAnalysisSummary));
        advice.setFashionSuggestions("Use a coordinated color palette and keep one visual focus in the outfit.");
        advice.setWeatherInfo(weatherInfo);
        advice.setStyleAnalysis(imageAnalysisSummary.isBlank()
                ? "The recommendation focuses on versatility and ease of wear."
                : "The recommendation is aligned with the uploaded image style cues.");
        advice.setItems(buildFallbackItems(candidateResults));
        advice.setPurchaseRecommendations(new ArrayList<>());
        return advice;
    }

    private String buildFallbackReasoning(String weatherInfo, String occasion, String description, String imageAnalysisSummary) {
        List<String> parts = new ArrayList<>();
        if (!weatherInfo.isBlank()) {
            parts.add("Weather considered: " + weatherInfo);
        }
        if (!occasion.isBlank()) {
            parts.add("Occasion considered: " + occasion);
        }
        if (!description.isBlank()) {
            parts.add("User request considered: " + description);
        }
        if (!imageAnalysisSummary.isBlank()) {
            parts.add("Image style signals were incorporated.");
        }
        if (parts.isEmpty()) {
            parts.add("The suggestion is based on the best available wardrobe matches.");
        }
        return String.join(" ", parts);
    }

    private List<OutfitAdvice.OutfitItem> buildFallbackItems(List<VectorSearchServiceV3.VectorSearchResult> candidateResults) {
        List<OutfitAdvice.OutfitItem> items = new ArrayList<>();
        for (int i = 0; i < candidateResults.size() && i < 5; i++) {
            VectorSearchServiceV3.VectorSearchResult candidate = candidateResults.get(i);
            OutfitAdvice.OutfitItem item = new OutfitAdvice.OutfitItem();
            item.setClothingId(candidate.getClothingId());
            item.setName(candidate.getName());
            item.setCategory(candidate.getCategory());
            item.setColor(candidate.getPrimaryColor());
            item.setImageUrl(candidate.getImageUrl());
            item.setIsRecommended(true);
            item.setReason("Selected from the user's wardrobe based on similarity and practicality.");
            items.add(item);
        }
        return items;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    @Data
    public static class OutfitAdvice {
        private String outfitName;
        private String outfitDescription;
        private List<OutfitItem> items;
        private String reasoning;
        private String fashionSuggestions;
        private List<PurchaseRecommendation> purchaseRecommendations;
        private String weatherInfo;
        private String styleAnalysis;

        @Data
        public static class OutfitItem {
            private Long clothingId;
            private String name;
            private String category;
            private String color;
            private String imageUrl;
            private Boolean isRecommended;
            private String reason;
        }

        @Data
        public static class PurchaseRecommendation {
            private String name;
            private String reason;
            private String link;
            private String estimatedPrice;
        }
    }
}
