package com.ecru.web.dto.request;

import lombok.Data;

@Data
public class AdminKnowledgeListRequest {

    private String keyword;

    private Integer active;

    private Integer page = 1;

    private Integer size = 20;
}
