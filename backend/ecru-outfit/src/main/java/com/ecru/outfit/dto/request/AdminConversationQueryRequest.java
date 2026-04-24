package com.ecru.outfit.dto.request;

import lombok.Data;

@Data
public class AdminConversationQueryRequest {

    private Integer page = 1;

    private Integer size = 20;

    private String keyword;

    private String ownerKeyword;

    private String context;

    private Integer active;
}
