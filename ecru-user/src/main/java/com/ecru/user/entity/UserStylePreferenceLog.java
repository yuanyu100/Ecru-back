package com.ecru.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户风格偏好标记表实体类
 * 存储用户对风格图片的偏好标记（like/dislike/skip）
 */
@Data
@TableName("user_style_preference_logs")
public class UserStylePreferenceLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private Long imageId;
    
    private Integer preferenceType; // 1-like, 2-dislike, 0-skip
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
