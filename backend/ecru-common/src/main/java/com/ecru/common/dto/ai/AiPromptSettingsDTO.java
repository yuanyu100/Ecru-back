package com.ecru.common.dto.ai;

import lombok.Data;

@Data
public class AiPromptSettingsDTO {

    private String chatSystemPrompt;

    private String greetingReply;

    private String identityReply;

    private String conversationTitlePrompt;
}
