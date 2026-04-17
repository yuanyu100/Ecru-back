package com.ecru.outfit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecru.outfit.entity.AiChatMessage;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI对话消息Mapper
 */
@Mapper
public interface AiChatMessageMapper extends BaseMapper<AiChatMessage> {

    /**
     * 根据会话ID查询消息列表
     * @param conversationId 会话ID
     * @return 消息列表
     */
    List<AiChatMessage> selectByConversationId(@Param("conversationId") Long conversationId);

    /**
     * 分页查询会话消息
     * @param conversationId 会话ID
     * @return 消息分页列表
     */
    Page<AiChatMessage> selectByConversationIdPage(@Param("conversationId") Long conversationId);

    /**
     * 根据会话ID和角色查询消息
     * @param conversationId 会话ID
     * @param role 角色
     * @return 消息列表
     */
    List<AiChatMessage> selectByConversationIdAndRole(@Param("conversationId") Long conversationId, @Param("role") String role);

    /**
     * 查询最近的N条消息
     * @param conversationId 会话ID
     * @param limit 限制数量
     * @return 消息列表
     */
    List<AiChatMessage> selectRecentMessages(@Param("conversationId") Long conversationId, @Param("limit") Integer limit);

    /**
     * 统计会话消息数量
     * @param conversationId 会话ID
     * @return 消息数量
     */
    Integer countByConversationId(@Param("conversationId") Long conversationId);

    /**
     * 根据用户ID查询所有消息
     * @param userId 用户ID
     * @return 消息列表
     */
    Page<AiChatMessage> selectByUserId(@Param("userId") Long userId);

    /**
     * 删除会话的所有消息
     * @param conversationId 会话ID
     * @return 影响行数
     */
    int deleteByConversationId(@Param("conversationId") Long conversationId);

}
