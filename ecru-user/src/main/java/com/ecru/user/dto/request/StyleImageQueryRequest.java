package com.ecru.user.dto.request;

import lombok.Data;

/**
 * 风格图片查询请求DTO
 * 用于获取风格图片列表的查询参数
 */
@Data
public class StyleImageQueryRequest {
    private String styleCategory;
    private Integer limit = 20;
    private Long excludeImageId;
}
