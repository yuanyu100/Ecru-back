package com.ecru.outfit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecru.outfit.entity.AiConversation;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI对话会话Mapper
 */
@Mapper
public interface AiConversationMapper extends BaseMapper<AiConversation> {

    /**
     * 根据用户ID查询会话列表
     * @param userId 用户ID
     * @return 会话列表
     */
    Page<AiConversation> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID和活跃状态查询会话列表
     * @param userId 用户ID
     * @param isActive 是否活跃
     * @return 会话列表
     */
    List<AiConversation> selectByUserIdAndActive(@Param("userId") Long userId, @Param("isActive") Boolean isActive);

    /**
     * 根据会话ID查询会话
     * @param sessionId 会话ID
     * @return 会话
     */
    AiConversation selectBySessionId(@Param("sessionId") String sessionId);

    /**
     * 根据用户ID和场景查询会话列表
     * @param userId 用户ID
     * @param context 场景
     * @return 会话列表
     */
    List<AiConversation> selectByUserIdAndContext(@Param("userId") Long userId, @Param("context") String context);

    /**
     * 统计用户会话数量
     * @param userId 用户ID
     * @return 会话数量
     */
    Integer countByUserId(@Param("userId") Long userId);

    /**
     * 更新会话消息数量
     * @param conversationId 会话ID
     * @param messageCount 消息数量
     * @return 影响行数
     */
    int updateMessageCount(@Param("conversationId") Long conversationId, @Param("messageCount") Integer messageCount);

}
