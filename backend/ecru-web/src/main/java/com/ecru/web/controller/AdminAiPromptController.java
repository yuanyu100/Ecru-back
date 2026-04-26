package com.ecru.web.controller;

import com.ecru.common.dto.ai.AiPromptSettingsDTO;
import com.ecru.common.exception.BusinessException;
import com.ecru.common.result.ErrorCode;
import com.ecru.common.result.Result;
import com.ecru.common.service.ai.AiPromptSettingsService;
import com.ecru.common.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/ai-prompts")
@RequiredArgsConstructor
@Tag(name = "AI Prompt Settings", description = "Admin APIs for AI prompt settings")
public class AdminAiPromptController {

    private final AiPromptSettingsService aiPromptSettingsService;

    @GetMapping("/chat")
    @Operation(summary = "Get chat prompt settings")
    public Result<AiPromptSettingsDTO> getChatPromptSettings() {
        requireAdmin();
        return Result.success(aiPromptSettingsService.getChatPromptSettings());
    }

    @PutMapping("/chat")
    @Operation(summary = "Update chat prompt settings")
    public Result<AiPromptSettingsDTO> updateChatPromptSettings(@RequestBody AiPromptSettingsDTO request) {
        requireAdmin();
        return Result.success("AI prompt settings updated", aiPromptSettingsService.updateChatPromptSettings(request, UserContext.getCurrentUserId()));
    }

    private void requireAdmin() {
        if (!UserContext.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (!UserContext.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
