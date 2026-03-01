package com.ecru.clothing.dto.request;


import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class CreateClothingRequest {

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
    private String imageUrl;
    private List<String> imageUrls;
    private Double purchasePrice;
    private String purchaseDate;
    private String purchaseLink;
    private String brand;
    private Boolean autoRecognize;

}
