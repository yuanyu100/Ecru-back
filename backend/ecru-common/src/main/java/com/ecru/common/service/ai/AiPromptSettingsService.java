package com.ecru.common.service.ai;

import com.ecru.common.dto.ai.AiPromptSettingsDTO;

public interface AiPromptSettingsService {

    AiPromptSettingsDTO getChatPromptSettings();

    AiPromptSettingsDTO updateChatPromptSettings(AiPromptSettingsDTO request, Long updatedBy);

    String getChatSystemPrompt();

    String getGreetingReply();

    String getIdentityReply();
}
