package com.ecru.outfit.service.agent;

import com.alibaba.fastjson2.JSON;
import com.ecru.outfit.service.mcp.McpWeatherService;
import com.ecru.outfit.service.rag.RagService;
import com.ecru.outfit.service.rag.VectorSearchServiceV3;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OutfitAdvisorTools {

    @Autowired
    private McpWeatherService weatherService;

    @Autowired
    private RagService ragService;

    @Tool("Get current weather context for a city. Use this when the user provides a location.")
    public String getWeatherContext(@P("city or location name") String location) {
        McpWeatherService.WeatherInfo weatherInfo = weatherService.getWeatherByLocation(location);
        if (weatherInfo == null) {
            return "{\"available\":false}";
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("available", true);
        payload.put("location", weatherInfo.getLocation());
        payload.put("temperature", weatherInfo.getTemperature());
        payload.put("weatherCondition", weatherInfo.getWeatherCondition());
        payload.put("humidity", weatherInfo.getHumidity());
        payload.put("windDirection", weatherInfo.getWindDirection());
        payload.put("windPower", weatherInfo.getWindPower());
        payload.put("season", weatherInfo.getSeason());
        payload.put("timeOfDay", weatherInfo.getTimeOfDay());
        payload.put("reportTime", weatherInfo.getReportTime());
        return JSON.toJSONString(payload);
    }

    @Tool("Search the user's wardrobe and return matching clothes. Always use this before recommending concrete outfit items.")
    public String searchWardrobe(
            @P("user id") Long userId,
            @P("search query describing style, weather and occasion") String query,
            @P("maximum number of clothing items to return") Integer limit
    ) {
        int actualLimit = limit == null || limit <= 0 ? 8 : Math.min(limit, 12);
        List<VectorSearchServiceV3.VectorSearchResult> results = ragService.searchClothes(userId, query, actualLimit);
        List<Map<String, Object>> payload = new ArrayList<>();
        for (VectorSearchServiceV3.VectorSearchResult result : results) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("clothingId", result.getClothingId());
            item.put("name", result.getName());
            item.put("category", result.getCategory());
            item.put("color", result.getPrimaryColor());
            item.put("material", result.getMaterial());
            item.put("styleTags", result.getStyleTags());
            item.put("occasionTags", result.getOccasionTags());
            item.put("seasonTags", result.getSeasonTags());
            item.put("imageUrl", result.getImageUrl());
            item.put("similarity", result.getSimilarity());
            payload.add(item);
        }
        return JSON.toJSONString(payload);
    }

    @Tool("Get a concise overview of the user's wardrobe structure, including category and color distribution.")
    public String getWardrobeStatistics(@P("user id") Long userId) {
        Map<String, Object> statistics = ragService.getClothingStatistics(userId);
        return JSON.toJSONString(statistics);
    }
}
