package com.ecru.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AdminStyleImageUpsertRequest {

    @NotBlank(message = "图片地址不能为空")
    private String imageUrl;

    private String title;

    private String source;

    private String sourceUrl;

    private BigDecimal price;

    @NotBlank(message = "风格分类不能为空")
    private String styleCategory;

    private List<Long> styleTagIds;

    private Boolean isActive;
}
