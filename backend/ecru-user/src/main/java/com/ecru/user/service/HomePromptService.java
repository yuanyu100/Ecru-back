package com.ecru.user.service;

import com.ecru.user.dto.HomePromptPdfPreviewDTO;
import com.ecru.user.dto.HomePromptSettingsDTO;
import org.springframework.web.multipart.MultipartFile;

public interface HomePromptService {

    HomePromptSettingsDTO getHomePromptSettings(Long userId);

    HomePromptSettingsDTO updateHomePromptSettings(Long userId, HomePromptSettingsDTO request);

    HomePromptPdfPreviewDTO previewHomePromptsFromPdf(Long userId, MultipartFile file);
}
