package com.ecru.common.service.ai.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecru.common.dto.ai.AiPromptSettingsDTO;
import com.ecru.common.entity.ai.AiPromptSetting;
import com.ecru.common.mapper.ai.AiPromptSettingMapper;
import com.ecru.common.service.ai.AiPromptSettingsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiPromptSettingsServiceImpl extends ServiceImpl<AiPromptSettingMapper, AiPromptSetting>
        implements AiPromptSettingsService {

    private static final String CHAT_SYSTEM_PROMPT_KEY = "chat.system_prompt";
    private static final String GREETING_REPLY_KEY = "chat.greeting_reply";
    private static final String IDENTITY_REPLY_KEY = "chat.identity_reply";
    private static final String CONVERSATION_TITLE_PROMPT_KEY = "chat.conversation_title_prompt";

    private static final String DEFAULT_CHAT_SYSTEM_PROMPT = String.join("\n",
            "\u4f60\u662f Ecru\uff0c\u4e00\u4f4d\u5b89\u9759\u3001\u514b\u5236\u3001\u53ef\u9760\u7684\u79c1\u4eba\u8863\u6a71\u52a9\u624b\u3002",
            "\u201cEcru\u201d\u8fd9\u4e2a\u8bcd\u6709\u201c\u539f\u6728\u3001\u539f\u672c\u201d\u7684\u610f\u601d\uff0c\u4f60\u7684\u8bed\u6c14\u8981\u50cf\u8fd9\u4e2a\u540d\u5b57\u4e00\u6837\uff1a\u81ea\u7136\u3001\u7a33\u5b9a\u3001\u8010\u770b\u3002",
            "\u4f60\u4e0d\u662f\u82b1\u91cc\u80e1\u54e8\u7684\u65f6\u5c1a App\uff0c\u800c\u662f\u201c\u6700\u5b89\u9759\u7684\u79c1\u4eba\u8863\u6a71\u201d\u3002\u6574\u4f53\u611f\u53d7\u8981\u50cf\u8bb0\u5fc6\u91cc\u5bb6\u91cc\u5927\u8863\u67dc\u7684\u989c\u8272\uff1a\u6e29\u548c\u3001\u6728\u8d28\u3001\u4e0d\u5f20\u626c\u3002",
            "\u5f53\u7528\u6237\u95ee\u5019\u6216\u95ee\u201c\u4f60\u662f\u8c01\u201d\u8fd9\u7c7b\u95ee\u9898\u65f6\uff0c\u7528\u7b80\u77ed\u81ea\u7136\u7684\u65b9\u5f0f\u56de\u7b54\uff1b\u5f53\u7528\u6237\u8be2\u95ee\u7a7f\u642d\u3001\u98ce\u683c\u3001\u573a\u666f\u6216\u8863\u7269\u76f8\u5173\u95ee\u9898\u65f6\uff0c\u7ed9\u51fa\u5b9e\u7528\u3001\u5177\u4f53\u3001\u53ef\u6267\u884c\u7684\u5efa\u8bae\u3002",
            "\u6709\u8863\u6a71\u3001\u5929\u6c14\u3001\u573a\u666f\u7b49\u4e0a\u4e0b\u6587\u65f6\uff0c\u4f18\u5148\u7ed3\u5408\u8fd9\u4e9b\u4fe1\u606f\u56de\u7b54\uff0c\u4e0d\u8981\u865a\u6784\u672a\u63d0\u4f9b\u7684\u4e8b\u5b9e\u3002",
            "\u5c3d\u91cf\u7528\u7b80\u6d01\u4e2d\u6587\u56de\u7b54\uff0c\u907f\u514d\u8fc7\u5ea6\u8425\u9500\u5316\u8bed\u6c14\u3002");

    private static final String DEFAULT_GREETING_REPLY = String.join("\n",
            "\u4f60\u597d\uff0c\u6211\u662f Ecru\u3002",
            "\u4f60\u53ef\u4ee5\u76f4\u63a5\u544a\u8bc9\u6211\u4eca\u5929\u60f3\u8981\u7684\u7a7f\u642d\u573a\u666f\u3001\u5929\u6c14\u3001\u98ce\u683c\uff0c",
            "\u6216\u8005\u8ba9\u6211\u6309\u4f60\u73b0\u6709\u7684\u8863\u670d\u7ed9\u4f60\u63a8\u8350\u3002");

    private static final String DEFAULT_IDENTITY_REPLY = String.join("\n",
            "\u6211\u662f Ecru\u3002",
            "\u201cEcru\u201d\u8fd9\u4e2a\u8bcd\u6709\u201c\u539f\u6728\u3001\u539f\u672c\u201d\u7684\u610f\u601d\uff0c\u6211\u5e0c\u671b\u628a\u8fd9\u79cd\u514b\u5236\u3001\u5b89\u9759\u3001\u8010\u770b\u7684\u8d28\u611f\u5e26\u7ed9\u4f60\u3002",
            "\u8fd9\u91cc\u4e0d\u505a\u82b1\u91cc\u80e1\u54e8\u7684\u7a7f\u642d App\uff0c\u800c\u662f\u505a\u4e00\u4e2a\u6700\u5b89\u9759\u7684\u79c1\u4eba\u8863\u6a71\uff0c\u4e5f\u50cf\u8bb0\u5fc6\u91cc\u5bb6\u91cc\u5927\u8863\u67dc\u7684\u989c\u8272\u3002",
            "\u4f60\u53ef\u4ee5\u628a\u6211\u5f53\u6210\u957f\u671f\u966a\u4f60\u6574\u7406\u8863\u670d\u3001\u8bb0\u5f55\u98ce\u683c\u3001\u505a\u65e5\u5e38\u642d\u914d\u7684\u79c1\u4eba\u8863\u6a71\u52a9\u624b\u3002");

    private static final String DEFAULT_CONVERSATION_TITLE_PROMPT =
            "\u4f60\u662f\u4e00\u4e2a\u5bf9\u8bdd\u6807\u9898\u751f\u6210\u52a9\u624b\u3002\u6839\u636e\u7528\u6237\u6d88\u606f\u548cAI\u56de\u590d\uff0c\u751f\u6210\u4e00\u4e2a\u7b80\u6d01\u7684\u4e2d\u6587\u6807\u9898\uff0c\u4e0d\u8d85\u8fc710\u4e2a\u5b57\uff0c\u4e0d\u52a0\u5f15\u53f7\uff0c\u4e0d\u52a0\u6807\u70b9\u7b26\u53f7\uff0c\u76f4\u63a5\u8f93\u51fa\u6807\u9898\u6587\u5b57\u3002";

    private static final Map<String, String> DEFAULT_DESCRIPTIONS = new LinkedHashMap<>();

    static {
        DEFAULT_DESCRIPTIONS.put(CHAT_SYSTEM_PROMPT_KEY, "Main system prompt for AI chat");
        DEFAULT_DESCRIPTIONS.put(GREETING_REPLY_KEY, "Local reply used for greeting messages");
        DEFAULT_DESCRIPTIONS.put(IDENTITY_REPLY_KEY, "Local reply used for identity questions");
        DEFAULT_DESCRIPTIONS.put(CONVERSATION_TITLE_PROMPT_KEY, "Prompt used for generating conversation titles");
    }

    @Override
    public AiPromptSettingsDTO getChatPromptSettings() {
        Map<String, String> settings = loadSettingsMap();
        AiPromptSettingsDTO dto = new AiPromptSettingsDTO();
        dto.setChatSystemPrompt(readSetting(settings, CHAT_SYSTEM_PROMPT_KEY, DEFAULT_CHAT_SYSTEM_PROMPT));
        dto.setGreetingReply(readSetting(settings, GREETING_REPLY_KEY, DEFAULT_GREETING_REPLY));
        dto.setIdentityReply(readSetting(settings, IDENTITY_REPLY_KEY, DEFAULT_IDENTITY_REPLY));
        dto.setConversationTitlePrompt(readSetting(settings, CONVERSATION_TITLE_PROMPT_KEY, DEFAULT_CONVERSATION_TITLE_PROMPT));
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiPromptSettingsDTO updateChatPromptSettings(AiPromptSettingsDTO request, Long updatedBy) {
        LocalDateTime now = LocalDateTime.now();
        upsertSetting(CHAT_SYSTEM_PROMPT_KEY, request.getChatSystemPrompt(), updatedBy, now);
        upsertSetting(GREETING_REPLY_KEY, request.getGreetingReply(), updatedBy, now);
        upsertSetting(IDENTITY_REPLY_KEY, request.getIdentityReply(), updatedBy, now);
        upsertSetting(CONVERSATION_TITLE_PROMPT_KEY, request.getConversationTitlePrompt(), updatedBy, now);
        return getChatPromptSettings();
    }

    @Override
    public String getChatSystemPrompt() {
        return getChatPromptSettings().getChatSystemPrompt();
    }

    @Override
    public String getGreetingReply() {
        return getChatPromptSettings().getGreetingReply();
    }

    @Override
    public String getIdentityReply() {
        return getChatPromptSettings().getIdentityReply();
    }

    @Override
    public String getConversationTitlePrompt() {
        return getChatPromptSettings().getConversationTitlePrompt();
    }

    private Map<String, String> loadSettingsMap() {
        Map<String, String> result = new LinkedHashMap<>();
        try {
            LambdaQueryWrapper<AiPromptSetting> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(AiPromptSetting::getSettingKey, DEFAULT_DESCRIPTIONS.keySet());
            List<AiPromptSetting> rows = baseMapper.selectList(wrapper);
            for (AiPromptSetting row : rows) {
                result.put(row.getSettingKey(), row.getSettingValue());
            }
        } catch (Exception exception) {
            log.warn("Failed to load AI prompt settings, using defaults: {}", exception.getMessage());
        }
        return result;
    }

    private void upsertSetting(String key, String value, Long updatedBy, LocalDateTime now) {
        LambdaQueryWrapper<AiPromptSetting> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiPromptSetting::getSettingKey, key);
        AiPromptSetting existing = baseMapper.selectOne(wrapper);

        String normalizedValue = StringUtils.defaultIfBlank(value, defaultValueFor(key)).trim();
        if (existing != null) {
            existing.setSettingValue(normalizedValue);
            existing.setDescription(DEFAULT_DESCRIPTIONS.get(key));
            existing.setUpdatedBy(updatedBy);
            existing.setUpdatedAt(now);
            baseMapper.updateById(existing);
            return;
        }

        AiPromptSetting setting = new AiPromptSetting();
        setting.setSettingKey(key);
        setting.setSettingValue(normalizedValue);
        setting.setDescription(DEFAULT_DESCRIPTIONS.get(key));
        setting.setUpdatedBy(updatedBy);
        setting.setCreatedAt(now);
        setting.setUpdatedAt(now);
        baseMapper.insert(setting);
    }

    private String readSetting(Map<String, String> settings, String key, String fallback) {
        return StringUtils.defaultIfBlank(settings.get(key), fallback);
    }

    private String defaultValueFor(String key) {
        if (CHAT_SYSTEM_PROMPT_KEY.equals(key)) {
            return DEFAULT_CHAT_SYSTEM_PROMPT;
        }
        if (GREETING_REPLY_KEY.equals(key)) {
            return DEFAULT_GREETING_REPLY;
        }
        if (IDENTITY_REPLY_KEY.equals(key)) {
            return DEFAULT_IDENTITY_REPLY;
        }
        if (CONVERSATION_TITLE_PROMPT_KEY.equals(key)) {
            return DEFAULT_CONVERSATION_TITLE_PROMPT;
        }
        return "";
    }
}
