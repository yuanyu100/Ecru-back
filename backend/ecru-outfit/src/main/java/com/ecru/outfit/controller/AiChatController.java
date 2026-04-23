package com.ecru.outfit.controller;

import com.ecru.common.result.Result;
import com.ecru.common.util.UserContext;
import com.ecru.outfit.dto.request.ChatRequestDTO;
import com.ecru.outfit.dto.response.ChatMessageVO;
import com.ecru.outfit.dto.response.ChatResponseDTO;
import com.ecru.outfit.dto.response.ConversationVO;
import com.ecru.outfit.service.AiChatService;
import com.github.pagehelper.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI对话控制器
 */
@Slf4j
@RestController
@RequestMapping("/ai-chat")
@Tag(name = "AI对话", description = "AI智能对话相关接口")
public class AiChatController {

    @Autowired
    private AiChatService aiChatService;

    /**
     * 发送消息并获取AI回复
     */
    @PostMapping("/chat")
    @Operation(summary = "AI对话", description = "发送消息给AI助手并获取回复，支持上下文对话")
    public Result<ChatResponseDTO> chat(@Valid @RequestBody ChatRequestDTO request) {
        try {
            Long userId = UserContext.getCurrentUserId();
            if (userId == null) {
                return Result.error(401, "用户未登录");
            }

            ChatResponseDTO response = aiChatService.chat(userId, request);
            return Result.success(response);
        } catch (Exception e) {
            log.error("AI对话失败: {}", e.getMessage(), e);
            return Result.error(500, "AI对话失败: " + e.getMessage());
        }
    }

    // ==================== 会话管理接口 ====================

    /**
     * 获取用户的会话列表
     */
    @GetMapping("/conversations")
    @Operation(summary = "获取会话列表", description = "分页获取用户的所有AI对话会话")
    public Result<Page<ConversationVO>> getConversations(
            @Parameter(description = "页码", example = "1") @RequestParam(value = "page", defaultValue = "1") Integer page,
            @Parameter(description = "每页大小", example = "10") @RequestParam(value = "size", defaultValue = "10") Integer size) {
        try {
            Long userId = UserContext.getCurrentUserId();
            if (userId == null) {
                return Result.error(401, "用户未登录");
            }

            Page<ConversationVO> conversations = aiChatService.getUserConversations(userId, page, size);
            return Result.success(conversations);
        } catch (Exception e) {
            log.error("获取会话列表失败: {}", e.getMessage(), e);
            return Result.error(500, "获取会话列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取会话详情
     */
    @GetMapping("/conversations/{sessionId}")
    @Operation(summary = "获取会话详情", description = "获取指定会话的详细信息")
    public Result<ConversationVO> getConversationDetail(
            @Parameter(description = "会话ID", required = true) @PathVariable String sessionId) {
        try {
            Long userId = UserContext.getCurrentUserId();
            if (userId == null) {
                return Result.error(401, "用户未登录");
            }

            ConversationVO conversation = aiChatService.getConversationDetail(userId, sessionId);
            if (conversation == null) {
                return Result.error(404, "会话不存在");
            }
            return Result.success(conversation);
        } catch (Exception e) {
            log.error("获取会话详情失败: {}", e.getMessage(), e);
            return Result.error(500, "获取会话详情失败: " + e.getMessage());
        }
    }

    /**
     * 更新会话标题
     */
    @PutMapping("/conversations/{sessionId}/title")
    @Operation(summary = "更新会话标题", description = "修改指定会话的标题")
    public Result<String> updateConversationTitle(
            @Parameter(description = "会话ID", required = true) @PathVariable String sessionId,
            @Parameter(description = "新标题", required = true) @RequestParam String title) {
        try {
            Long userId = UserContext.getCurrentUserId();
            if (userId == null) {
                return Result.error(401, "用户未登录");
            }

            boolean success = aiChatService.updateConversationTitle(userId, sessionId, title);
            if (success) {
                return Result.success("更新成功", null);
            } else {
                return Result.error(404, "会话不存在");
            }
        } catch (Exception e) {
            log.error("更新会话标题失败: {}", e.getMessage(), e);
            return Result.error(500, "更新会话标题失败: " + e.getMessage());
        }
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/conversations/{sessionId}")
    @Operation(summary = "删除会话", description = "删除指定的对话会话及其所有消息")
    public Result<String> deleteConversation(
            @Parameter(description = "会话ID", required = true) @PathVariable String sessionId) {
        try {
            Long userId = UserContext.getCurrentUserId();
            if (userId == null) {
                return Result.error(401, "用户未登录");
            }

            boolean success = aiChatService.deleteConversation(userId, sessionId);
            if (success) {
                return Result.success("删除成功", null);
            } else {
                return Result.error(404, "会话不存在");
            }
        } catch (Exception e) {
            log.error("删除会话失败: {}", e.getMessage(), e);
            return Result.error(500, "删除会话失败: " + e.getMessage());
        }
    }

    /**
     * 设置会话活跃状态
     */
    @PutMapping("/conversations/{sessionId}/active")
    @Operation(summary = "设置会话状态", description = "设置会话的活跃/非活跃状态")
    public Result<String> setConversationActive(
            @Parameter(description = "会话ID", required = true) @PathVariable String sessionId,
            @Parameter(description = "是否活跃", required = true) @RequestParam Boolean isActive) {
        try {
            Long userId = UserContext.getCurrentUserId();
            if (userId == null) {
                return Result.error(401, "用户未登录");
            }

            boolean success = aiChatService.setConversationActive(userId, sessionId, isActive);
            if (success) {
                return Result.success("设置成功", null);
            } else {
                return Result.error(404, "会话不存在");
            }
        } catch (Exception e) {
            log.error("设置会话状态失败: {}", e.getMessage(), e);
            return Result.error(500, "设置会话状态失败: " + e.getMessage());
        }
    }

    // ==================== 消息管理接口 ====================

    /**
     * 获取会话的消息列表(分页)
     */
    @GetMapping("/conversations/{sessionId}/messages")
    @Operation(summary = "获取消息列表", description = "分页获取指定会话的消息列表")
    public Result<Page<ChatMessageVO>> getConversationMessages(
            @Parameter(description = "会话ID", required = true) @PathVariable String sessionId,
            @Parameter(description = "页码", example = "1") @RequestParam(value = "page", defaultValue = "1") Integer page,
            @Parameter(description = "每页大小", example = "20") @RequestParam(value = "size", defaultValue = "20") Integer size) {
        try {
            Long userId = UserContext.getCurrentUserId();
            if (userId == null) {
                return Result.error(401, "用户未登录");
            }

            Page<ChatMessageVO> messages = aiChatService.getConversationMessages(userId, sessionId, page, size);
            return Result.success(messages);
        } catch (Exception e) {
            log.error("获取消息列表失败: {}", e.getMessage(), e);
            return Result.error(500, "获取消息列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取会话的所有消息(不分页)
     */
    @GetMapping("/conversations/{sessionId}/messages/all")
    @Operation(summary = "获取所有消息", description = "获取指定会话的所有消息(不分页)")
    public Result<List<ChatMessageVO>> getAllConversationMessages(
            @Parameter(description = "会话ID", required = true) @PathVariable String sessionId) {
        try {
            Long userId = UserContext.getCurrentUserId();
            if (userId == null) {
                return Result.error(401, "用户未登录");
            }

            List<ChatMessageVO> messages = aiChatService.getAllConversationMessages(userId, sessionId);
            return Result.success(messages);
        } catch (Exception e) {
            log.error("获取所有消息失败: {}", e.getMessage(), e);
            return Result.error(500, "获取所有消息失败: " + e.getMessage());
        }
    }

}
