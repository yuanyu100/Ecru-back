package com.ecru.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 风格图片向量实体类
 * 存储在 PostgreSQL 中
 */
@Data
@TableName("style_image_embeddings")
public class StyleImageEmbedding {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long imageId;
    
    private String embedding;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
