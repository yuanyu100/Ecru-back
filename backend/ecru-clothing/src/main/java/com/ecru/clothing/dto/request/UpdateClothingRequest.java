package com.ecru.clothing.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class UpdateClothingRequest {

    private String name;
    private String category;
    private String subCategory;
    private String primaryColor;
    private String secondaryColor;
    private String material;
    private String materialDetails;
    private String pattern;
    private String fit;
    private String size;
    private List<String> styleTags;
    private List<String> occasionTags;
    private List<String> seasonTags;
    private List<String> imageUrls;
    private Double purchasePrice;
    private String purchaseDate;
    private String purchaseLink;
    private String brand;
    private Integer frequencyLevel;

}
