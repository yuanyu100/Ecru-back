package com.ecru.user.dto;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

@Data
public class UserQueryRequest {

    @Parameter(description = "页码，默认1")
    private Integer page = 1;

    @Parameter(description = "每页大小，默认10")
    private Integer size = 10;

    @Parameter(description = "搜索关键词（用户名/邮箱）")
    private String keyword;

    @Parameter(description = "状态筛选：0-禁用，1-正常")
    private Integer status;

}
