package com.ecru.outfit.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.ecru.common.exception.BusinessException;
import com.ecru.outfit.dto.request.AdminConversationQueryRequest;
import com.ecru.outfit.dto.response.ChatMessageVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminAiChatService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final JdbcTemplate jdbcTemplate;

    public AdminAiChatService(@Qualifier("mysqlDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public Map<String, Object> getConversationOverview() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("conversationTotal", queryCount("SELECT COUNT(*) FROM ai_conversations"));
        result.put("conversationActive", queryCount("SELECT COUNT(*) FROM ai_conversations WHERE is_active = 1"));
        result.put("messageTotal", queryCount("SELECT COUNT(*) FROM ai_chat_messages"));
        result.put("userConversationUsers", queryCount("SELECT COUNT(DISTINCT user_id) FROM ai_conversations"));
        return result;
    }

    public Map<String, Object> listConversations(AdminConversationQueryRequest request) {
        int page = safePage(request.getPage());
        int size = safeSize(request.getSize());

        List<Object> whereArgs = new ArrayList<>();
        String where = buildWhereClause(request, whereArgs);

        long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_conversations c LEFT JOIN users u ON u.id = c.user_id" + where,
                Long.class,
                whereArgs.toArray());

        List<Object> queryArgs = new ArrayList<>(whereArgs);
        queryArgs.add(size);
        queryArgs.add((page - 1L) * size);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT c.id, c.user_id, c.session_id, c.title, c.context, c.is_active, c.message_count, c.metadata, c.created_at, c.updated_at, " +
                        "u.username, u.nickname, u.email, " +
                        "(SELECT m.content FROM ai_chat_messages m WHERE m.conversation_id = c.id ORDER BY m.created_at DESC LIMIT 1) AS last_message_preview " +
                        "FROM ai_conversations c " +
                        "LEFT JOIN users u ON u.id = c.user_id " +
                        where +
                        " ORDER BY c.updated_at DESC, c.id DESC LIMIT ? OFFSET ?",
                queryArgs.toArray());

        List<Map<String, Object>> list = rows.stream().map(this::toConversationItem).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("page", page);
        result.put("size", size);
        result.put("total", total);
        result.put("list", list);
        return result;
    }

    public List<ChatMessageVO> getConversationMessages(String sessionId) {
        Map<String, Object> conversation = getConversationRow(sessionId);
        Long conversationId = asLong(conversation.get("id"));
        if (conversationId == null) {
            throw new BusinessException(404, "会话不存在");
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, role, content, message_type, recommendations, context_snapshot, metadata, created_at " +
                        "FROM ai_chat_messages WHERE conversation_id = ? ORDER BY created_at ASC, id ASC",
                conversationId);

        return rows.stream().map(this::toMessageVO).toList();
    }

    public void deleteConversation(String sessionId) {
        Map<String, Object> conversation = getConversationRow(sessionId);
        Long conversationId = asLong(conversation.get("id"));
        if (conversationId == null) {
            throw new BusinessException(404, "会话不存在");
        }

        jdbcTemplate.update("DELETE FROM ai_chat_messages WHERE conversation_id = ?", conversationId);
        jdbcTemplate.update("DELETE FROM ai_conversations WHERE id = ?", conversationId);
    }

    private Map<String, Object> getConversationRow(String sessionId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, user_id, session_id, title, context, is_active, message_count, metadata, created_at, updated_at " +
                        "FROM ai_conversations WHERE session_id = ?",
                sessionId);
        if (rows.isEmpty()) {
            throw new BusinessException(404, "会话不存在");
        }
        return rows.get(0);
    }

    private String buildWhereClause(AdminConversationQueryRequest request, List<Object> args) {
        List<String> conditions = new ArrayList<>();

        String keyword = StringUtils.trimToEmpty(request.getKeyword());
        if (StringUtils.isNotBlank(keyword)) {
            conditions.add("(c.title LIKE ? OR c.session_id LIKE ? OR EXISTS (" +
                    "SELECT 1 FROM ai_chat_messages m WHERE m.conversation_id = c.id AND m.content LIKE ?" +
                    "))");
            args.add("%" + keyword + "%");
            args.add("%" + keyword + "%");
            args.add("%" + keyword + "%");
        }

        String ownerKeyword = StringUtils.trimToEmpty(request.getOwnerKeyword());
        if (StringUtils.isNotBlank(ownerKeyword)) {
            conditions.add("(u.username LIKE ? OR u.nickname LIKE ? OR u.email LIKE ?)");
            args.add("%" + ownerKeyword + "%");
            args.add("%" + ownerKeyword + "%");
            args.add("%" + ownerKeyword + "%");
        }

        String context = StringUtils.trimToEmpty(request.getContext());
        if (StringUtils.isNotBlank(context)) {
            conditions.add("c.context = ?");
            args.add(context);
        }

        if (request.getActive() != null && (request.getActive() == 0 || request.getActive() == 1)) {
            conditions.add("c.is_active = ?");
            args.add(request.getActive());
        }

        return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
    }

    private Map<String, Object> toConversationItem(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", asLong(row.get("id")));
        result.put("userId", asLong(row.get("user_id")));
        result.put("sessionId", asString(row.get("session_id")));
        result.put("title", asString(row.get("title")));
        result.put("context", asString(row.get("context")));
        result.put("isActive", asInt(row.get("is_active")) == 1);
        result.put("messageCount", asInt(row.get("message_count")));
        result.put("metadata", parseMap(row.get("metadata")));
        result.put("createdAt", row.get("created_at"));
        result.put("updatedAt", row.get("updated_at"));
        result.put("lastMessagePreview", asString(row.get("last_message_preview")));
        result.put("username", asString(row.get("username")));
        result.put("nickname", asString(row.get("nickname")));
        result.put("email", asString(row.get("email")));
        return result;
    }

    private ChatMessageVO toMessageVO(Map<String, Object> row) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(asLong(row.get("id")));
        vo.setRole(asString(row.get("role")));
        vo.setContent(asString(row.get("content")));
        vo.setMessageType(asString(row.get("message_type")));
        vo.setRecommendations(parseListMap(row.get("recommendations")));
        vo.setContextSnapshot(parseMap(row.get("context_snapshot")));
        vo.setMetadata(parseMap(row.get("metadata")));
        vo.setCreatedAt(row.get("created_at") == null ? null : (java.time.LocalDateTime) row.get("created_at"));
        return vo;
    }

    private long queryCount(String sql) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count == null ? 0L : count;
    }

    private int safePage(Integer page) {
        return page == null || page < 1 ? DEFAULT_PAGE : page;
    }

    private int safeSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private Map<String, Object> parseMap(Object value) {
        String text = asString(value);
        if (StringUtils.isBlank(text)) {
            return Map.of();
        }
        try {
            return JSON.parseObject(text, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            return Map.of("raw", text);
        }
    }

    private List<Map<String, Object>> parseListMap(Object value) {
        String text = asString(value);
        if (StringUtils.isBlank(text)) {
            return List.of();
        }
        try {
            return JSON.parseObject(text, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception ex) {
            return List.of(Map.of("raw", text));
        }
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private int asInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }
}
