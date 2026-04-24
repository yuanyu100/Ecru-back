package com.ecru.web.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class FabricKnowledgeBatchImportRequest {

    private Boolean updateExisting = true;

    private List<FabricKnowledgeUpsertRequest> items;
}
