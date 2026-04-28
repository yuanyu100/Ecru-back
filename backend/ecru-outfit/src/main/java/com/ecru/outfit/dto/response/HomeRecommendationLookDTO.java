package com.ecru.outfit.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class HomeRecommendationLookDTO {

    private Long id;

    private String mood;

    private String title;

    private String note;

    private List<String> tags;

    private List<String> palette;

    private List<LookItem> items;

    private LocalDateTime createdAt;

    @Data
    public static class LookItem {
        private Long clothingId;
        private String name;
        private String category;
        private String color;
        private String imageUrl;
        private String reason;
        private Integer frequencyLevel;
        private Integer wearCount;
        private String sourceType;
        private String sourcePlatform;
        private Boolean fromWardrobe;
    }
}
