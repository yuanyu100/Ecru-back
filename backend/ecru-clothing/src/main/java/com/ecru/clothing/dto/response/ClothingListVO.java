package com.ecru.clothing.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ClothingListVO {

    private Long id;
    private String name;
    private String category;
    private String subCategory;
    private String primaryColor;
    private String primaryColorHex;
    private String material;
    private String pattern;
    private List<String> styleTags;
    private Integer frequencyLevel;
    private Integer wearCount;
    private String imageUrl;
    private String thumbnailUrl;
    private LocalDateTime createdAt;

}
