package com.ecru.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * 风格图片标签关联表实体类
 * 存储风格图片与风格标签的关联关系
 */
@Data
@TableName("style_image_tags")
public class StyleImageTag {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long imageId;
    
    private Long styleTagId;
    
    private BigDecimal confidence; // AI识别置信度
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
