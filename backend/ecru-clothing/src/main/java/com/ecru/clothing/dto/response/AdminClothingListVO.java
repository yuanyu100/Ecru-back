package com.ecru.clothing.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminClothingListVO {

    private Long id;
    private Long userId;
    private String ownerUsername;
    private String ownerNickname;
    private String ownerEmail;
    private String name;
    private String category;
    private String subCategory;
    private String primaryColor;
    private String material;
    private String styleTags;
    private String imageUrl;
    private String sourceType;
    private Integer wearCount;
    private Integer frequencyLevel;
    private LocalDateTime createdAt;
}
