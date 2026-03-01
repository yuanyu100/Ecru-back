package com.ecru.clothing.dto.request;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

@Data
public class ClothingQueryRequest {

    @Parameter(description = "页码，默认1")
    private Integer page = 1;

    @Parameter(description = "每页数量，默认20")
    private Integer size = 20;

    @Parameter(description = "类别筛选")
    private String category;

    @Parameter(description = "主色调筛选")
    private String primaryColor;

    @Parameter(description = "材质筛选")
    private String material;

    @Parameter(description = "风格标签筛选")
    private String styleTag;

    @Parameter(description = "场合标签筛选")
    private String occasionTag;

    @Parameter(description = "季节标签筛选")
    private String seasonTag;

    @Parameter(description = "关键词搜索")
    private String keyword;

    @Parameter(description = "排序字段")
    private String sortBy;

    @Parameter(description = "排序方向")
    private String sortOrder;

    @Parameter(description = "最小搭配频率")
    private Integer minFrequency;

    @Parameter(description = "最大搭配频率")
    private Integer maxFrequency;

}
