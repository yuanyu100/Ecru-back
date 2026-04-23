package com.ecru.user.dto.response;

import lombok.Data;

/**
 * 风格标签响应VO
 * 用于返回风格标签信息
 */
@Data
public class StyleTagVO {
    private Long id;
    private String name;
    private String category;
    private String description;
    private Integer usageCount;
}
