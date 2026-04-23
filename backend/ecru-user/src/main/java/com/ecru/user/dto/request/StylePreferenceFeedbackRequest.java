package com.ecru.user.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 风格偏好反馈请求DTO
 * 用于用户提交对风格图片的偏好反馈
 */
@Data
public class StylePreferenceFeedbackRequest {
    @NotNull(message = "图片ID不能为空")
    private Long imageId;
    
    @NotNull(message = "偏好类型不能为空")
    private Integer preferenceType; // 1-like, 2-dislike, 0-skip
}
