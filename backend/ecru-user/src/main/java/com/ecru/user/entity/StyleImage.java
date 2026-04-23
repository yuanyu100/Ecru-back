package com.ecru.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * 风格图片实体类
 * 存储在 PostgreSQL 中
 */
@Data
@TableName("style_images")
public class StyleImage {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String imageUrl;
    
    private String title;
    
    private String source;
    
    private String sourceUrl;
    
    private BigDecimal price;
    
    private String styleCategory;
    
    private Boolean isActive;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
