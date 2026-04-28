package com.ecru.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * 用户风格画像表实体类
 * 存储用户对各种风格标签的偏好分数
 */
@Data
@TableName("user_style_profiles")
public class UserStyleProfile {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private Long styleTagId;
    
    private BigDecimal preferenceScore; // 偏好分数（-1.0 ~ 1.0）
    
    private Integer interactionCount; // 交互次数
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
