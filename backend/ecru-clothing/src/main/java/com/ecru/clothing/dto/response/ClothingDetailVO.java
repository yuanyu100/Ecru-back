package com.ecru.clothing.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ClothingDetailVO {

    private Long id;
    private Long userId;
    private String name;
    private String category;
    private String subCategory;
    private String primaryColor;
    private String primaryColorHex;
    private String secondaryColor;
    private String secondaryColorHex;
    private String material;
    private String materialDetails;
    private String pattern;
    private String fit;
    private String size;
    private List<String> styleTags;
    private List<String> occasionTags;
    private List<String> seasonTags;
    private Integer frequencyLevel;
    private Integer wearCount;
    private LocalDateTime lastWornAt;
    private String imageUrl;
    private List<String> imageUrls;
    private String thumbnailUrl;
    private Double purchasePrice;
    private String purchaseDate;
    private String purchaseLink;
    private String brand;
    private String sourceType;
    private Double aiConfidence;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private WearHistory wearHistory;

    @Data
    public static class WearHistory {
        private Integer total;
        private List<WearRecord> recent;

        @Data
        public static class WearRecord {
            private String date;
            private Long outfitId;
        }
    }

}
