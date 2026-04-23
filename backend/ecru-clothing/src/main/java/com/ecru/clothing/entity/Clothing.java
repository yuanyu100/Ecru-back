package com.ecru.clothing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("clothings")
public class Clothing implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
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

    private String styleTags;

    private String occasionTags;

    private String seasonTags;

    private Integer frequencyLevel;

    private Integer wearCount;

    private LocalDateTime lastWornAt;

    private String imageUrl;

    private String imageUrls;

    private String thumbnailUrl;

    private BigDecimal purchasePrice;

    private LocalDate purchaseDate;

    private String purchaseLink;

    private String brand;

    private Boolean isDeleted;

    private String sourceType;

    private BigDecimal aiConfidence;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
