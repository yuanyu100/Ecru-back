package com.ecru.user.dto.response;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 用户风格画像响应VO
 * 用于返回用户风格偏好信息
 */
@Data
public class UserStyleProfileVO {
    private StyleTagVO styleTag;
    private BigDecimal preferenceScore;
    private Integer interactionCount;
}
