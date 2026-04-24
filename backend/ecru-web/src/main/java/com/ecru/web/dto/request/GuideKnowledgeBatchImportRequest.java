package com.ecru.web.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class GuideKnowledgeBatchImportRequest {

    private Boolean updateExisting = true;

    private List<GuideKnowledgeUpsertRequest> items;
}
