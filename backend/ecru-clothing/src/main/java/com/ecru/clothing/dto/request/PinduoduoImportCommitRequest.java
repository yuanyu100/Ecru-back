package com.ecru.clothing.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class PinduoduoImportCommitRequest {

    private List<PinduoduoImportItemRequest> items;

    private Boolean autoRecognize;

    private Boolean skipExisting;
}
