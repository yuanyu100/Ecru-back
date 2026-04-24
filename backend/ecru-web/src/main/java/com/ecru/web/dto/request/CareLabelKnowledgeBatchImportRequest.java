package com.ecru.web.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class CareLabelKnowledgeBatchImportRequest {

    private Boolean updateExisting = true;

    private List<CareLabelKnowledgeUpsertRequest> items;
}
