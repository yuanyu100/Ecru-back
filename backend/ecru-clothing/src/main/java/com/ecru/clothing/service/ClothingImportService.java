package com.ecru.clothing.service;

import com.ecru.clothing.dto.request.PinduoduoImportCommitRequest;
import com.ecru.clothing.dto.request.PinduoduoImportPreviewRequest;
import com.ecru.clothing.dto.response.PinduoduoImportPreviewVO;
import com.ecru.clothing.dto.response.PinduoduoImportResultVO;

public interface ClothingImportService {

    PinduoduoImportPreviewVO previewPinduoduoImport(Long userId, PinduoduoImportPreviewRequest request);

    PinduoduoImportResultVO importPinduoduoItems(Long userId, PinduoduoImportCommitRequest request);
}
