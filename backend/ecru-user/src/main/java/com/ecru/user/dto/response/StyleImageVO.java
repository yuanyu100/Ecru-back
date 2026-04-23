package com.ecru.user.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 风格图片响应VO
 * 用于返回风格图片信息
 */
@Data
public class StyleImageVO {
    private Long id;
    private String imageUrl;
    private String title;
    private String source;
    private String sourceUrl;
    private BigDecimal price;
    private String styleCategory;
    private List<StyleTagVO> tags;
    private LocalDateTime createdAt;
}
