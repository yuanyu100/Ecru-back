package com.ecru.clothing.dto.request;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

@Data
public class AdminClothingQueryRequest {

    @Parameter(description = "页码")
    private Integer page = 1;

    @Parameter(description = "每页数量")
    private Integer size = 20;

    @Parameter(description = "衣物所属用户 ID")
    private Long userId;

    @Parameter(description = "衣物关键字，搜索名称/分类/颜色")
    private String keyword;

    @Parameter(description = "用户关键字，搜索用户名/昵称/邮箱")
    private String ownerKeyword;

    @Parameter(description = "衣物分类")
    private String category;

    @Parameter(description = "主色")
    private String primaryColor;

    @Parameter(description = "来源类型")
    private String sourceType;
}
