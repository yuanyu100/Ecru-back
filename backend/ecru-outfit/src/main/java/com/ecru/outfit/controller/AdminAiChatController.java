package com.ecru.outfit.controller;

import com.ecru.common.exception.BusinessException;
import com.ecru.common.result.ErrorCode;
import com.ecru.common.result.Result;
import com.ecru.common.util.UserContext;
import com.ecru.outfit.dto.request.AdminConversationQueryRequest;
import com.ecru.outfit.dto.response.ChatMessageVO;
import com.ecru.outfit.service.AdminAiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/ai-chat")
@RequiredArgsConstructor
@Tag(name = "管理员 AI 会话管理", description = "管理员查看和清理全局 AI 会话")
public class AdminAiChatController {

    private final AdminAiChatService adminAiChatService;

    @GetMapping("/overview")
    @Operation(summary = "获取 AI 会话概览")
    public Result<Map<String, Object>> getOverview() {
        requireAdmin();
        return Result.success(adminAiChatService.getConversationOverview());
    }

    @GetMapping("/conversations")
    @Operation(summary = "获取全局 AI 会话列表")
    public Result<Map<String, Object>> getConversations(AdminConversationQueryRequest request) {
        requireAdmin();
        return Result.success(adminAiChatService.listConversations(request));
    }

    @GetMapping("/conversations/{sessionId}/messages")
    @Operation(summary = "获取指定会话全部消息")
    public Result<List<ChatMessageVO>> getConversationMessages(@PathVariable String sessionId) {
        requireAdmin();
        return Result.success(adminAiChatService.getConversationMessages(sessionId));
    }

    @DeleteMapping("/conversations/{sessionId}")
    @Operation(summary = "管理员删除会话")
    public Result<Void> deleteConversation(@PathVariable String sessionId) {
        requireAdmin();
        adminAiChatService.deleteConversation(sessionId);
        return Result.success("会话删除成功", null);
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
