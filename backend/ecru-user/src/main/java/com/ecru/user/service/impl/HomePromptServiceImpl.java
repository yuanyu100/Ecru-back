package com.ecru.user.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.ecru.common.service.ai.AiTextGeneratorService;
import com.ecru.user.dto.HomePromptItemDTO;
import com.ecru.user.dto.HomePromptPdfPreviewDTO;
import com.ecru.user.dto.HomePromptSettingsDTO;
import com.ecru.user.service.HomePromptService;
import com.ecru.user.service.UserSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomePromptServiceImpl implements HomePromptService {

    private static final String SETTINGS_KEY_ITEMS = "homePromptItems";
    private static final String SETTINGS_KEY_VISIBLE = "homeFlowDefaultVisible";
    private static final String SETTINGS_KEY_STAY_MS = "homePromptStayMs";
    private static final String SETTINGS_KEY_FADE_MS = "homePromptFadeMs";
    private static final String SETTINGS_KEY_LEGACY_PROMPT = "homePrompt";
    private static final String SETTINGS_KEY_LEGACY_PROMPTS = "homePrompts";

    private static final String SOURCE_TYPE_MANUAL = "manual";
    private static final String SOURCE_TYPE_PDF = "pdf";
    private static final String SOURCE_LABEL_MANUAL = "手动添加";
    private static final String SOURCE_LABEL_PDF = "PDF 导入";
    private static final String DEFAULT_PROMPT = "今天想穿成什么样";

    private static final int MAX_PROMPT_LENGTH = 20;
    private static final int MAX_PROMPT_COUNT = 60;
    private static final int PDF_PREVIEW_LIMIT = 12;
    private static final int DEFAULT_STAY_MS = 2200;
    private static final int DEFAULT_FADE_MS = 820;

    private static final Pattern LEADING_INDEX_PATTERN =
            Pattern.compile("^[0-9一二三四五六七八九十]+[.、)）\\-\\s]+");
    private static final Pattern LEADING_BULLET_PATTERN =
            Pattern.compile("^[\\-•·●◆■□▪◦]+");
    private static final Pattern INVALID_QUOTE_PATTERN =
            Pattern.compile("[“”\"'‘’]");

    private final UserSettingsService userSettingsService;
    private final AiTextGeneratorService aiTextGeneratorService;

    @Override
    public HomePromptSettingsDTO getHomePromptSettings(Long userId) {
        return buildSettingsDto(userSettingsService.getUserSettings(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HomePromptSettingsDTO updateHomePromptSettings(Long userId, HomePromptSettingsDTO request) {
        HomePromptSettingsDTO normalized = normalizeRequest(request);
        List<String> enabledTexts = extractEnabledTexts(normalized.getItems());
        String primaryText = enabledTexts.isEmpty() ? DEFAULT_PROMPT : enabledTexts.get(0);

        Map<String, String> settings = new LinkedHashMap<>();
        settings.put(SETTINGS_KEY_ITEMS, JSON.toJSONString(stripTransientFields(normalized.getItems())));
        settings.put(SETTINGS_KEY_VISIBLE, String.valueOf(Boolean.TRUE.equals(normalized.getHomeFlowDefaultVisible())));
        settings.put(SETTINGS_KEY_STAY_MS, String.valueOf(normalized.getHomePromptStayMs()));
        settings.put(SETTINGS_KEY_FADE_MS, String.valueOf(normalized.getHomePromptFadeMs()));
        settings.put(SETTINGS_KEY_LEGACY_PROMPT, primaryText);
        settings.put(SETTINGS_KEY_LEGACY_PROMPTS, JSON.toJSONString(enabledTexts.isEmpty() ? List.of(DEFAULT_PROMPT) : enabledTexts));

        userSettingsService.updateUserSettings(userId, settings);
        return getHomePromptSettings(userId);
    }

    @Override
    public HomePromptPdfPreviewDTO previewHomePromptsFromPdf(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请先上传 PDF 文件");
        }

        String filename = StringUtils.defaultIfBlank(file.getOriginalFilename(), "导入文件.pdf");
        if (!filename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new IllegalArgumentException("目前只支持导入 PDF 文件");
        }

        String extractedText = extractPdfText(file);
        if (StringUtils.isBlank(extractedText)) {
            throw new IllegalArgumentException("PDF 没有识别到可用文本");
        }

        List<String> promptTexts = parsePromptsWithAi(userId, extractedText);
        if (promptTexts.isEmpty()) {
            promptTexts = fallbackPrompts(extractedText);
        }
        if (promptTexts.isEmpty()) {
            throw new IllegalArgumentException("没有识别到可导入的提示语");
        }

        HomePromptPdfPreviewDTO response = new HomePromptPdfPreviewDTO();
        response.setSourceLabel(filename);
        response.setItems(buildPromptItems(promptTexts, SOURCE_TYPE_PDF, filename, true));
        return response;
    }

    private HomePromptSettingsDTO buildSettingsDto(Map<String, String> settings) {
        List<HomePromptItemDTO> items = parseItems(settings);
        if (items.isEmpty()) {
            items = buildPromptItems(List.of(DEFAULT_PROMPT), SOURCE_TYPE_MANUAL, SOURCE_LABEL_MANUAL, true);
        }
        ensureAtLeastOneEnabled(items);

        HomePromptSettingsDTO dto = new HomePromptSettingsDTO();
        dto.setItems(items);
        dto.setSelectedPromptId(resolvePrimaryEnabledId(items));
        dto.setHomeFlowDefaultVisible(parseVisible(settings.get(SETTINGS_KEY_VISIBLE)));
        dto.setHomePromptStayMs(parseMs(settings.get(SETTINGS_KEY_STAY_MS), DEFAULT_STAY_MS, 1200, 8000));
        dto.setHomePromptFadeMs(parseMs(settings.get(SETTINGS_KEY_FADE_MS), DEFAULT_FADE_MS, 400, 2200));
        return dto;
    }

    private HomePromptSettingsDTO normalizeRequest(HomePromptSettingsDTO request) {
        List<HomePromptItemDTO> normalizedItems = sanitizeItems(request != null ? request.getItems() : null);
        if (normalizedItems.isEmpty()) {
            normalizedItems = buildPromptItems(List.of(DEFAULT_PROMPT), SOURCE_TYPE_MANUAL, SOURCE_LABEL_MANUAL, true);
        }
        ensureAtLeastOneEnabled(normalizedItems);

        HomePromptSettingsDTO dto = new HomePromptSettingsDTO();
        dto.setItems(normalizedItems);
        dto.setSelectedPromptId(resolvePrimaryEnabledId(normalizedItems));
        dto.setHomeFlowDefaultVisible(request == null || request.getHomeFlowDefaultVisible() == null
                ? Boolean.TRUE
                : request.getHomeFlowDefaultVisible());
        dto.setHomePromptStayMs(parseMs(request == null ? null : String.valueOf(request.getHomePromptStayMs()),
                DEFAULT_STAY_MS, 1200, 8000));
        dto.setHomePromptFadeMs(parseMs(request == null ? null : String.valueOf(request.getHomePromptFadeMs()),
                DEFAULT_FADE_MS, 400, 2200));
        return dto;
    }

    private List<HomePromptItemDTO> parseItems(Map<String, String> settings) {
        String rawItems = settings.get(SETTINGS_KEY_ITEMS);
        if (StringUtils.isNotBlank(rawItems)) {
            try {
                List<HomePromptItemDTO> parsed = JSON.parseArray(rawItems, HomePromptItemDTO.class);
                List<HomePromptItemDTO> sanitized = sanitizeItems(parsed);
                if (!sanitized.isEmpty()) {
                    return sanitized;
                }
            } catch (Exception exception) {
                log.warn("Failed to parse home prompt items, fallback to legacy settings: {}", exception.getMessage());
            }
        }

        LinkedHashSet<String> legacyTexts = new LinkedHashSet<>();
        String rawPrompts = settings.get(SETTINGS_KEY_LEGACY_PROMPTS);
        if (StringUtils.isNotBlank(rawPrompts)) {
            try {
                JSONArray array = JSON.parseArray(rawPrompts);
                for (Object item : array) {
                    String text = sanitizePromptText(String.valueOf(item));
                    if (text != null) {
                        legacyTexts.add(text);
                    }
                }
            } catch (Exception exception) {
                log.warn("Failed to parse legacy home prompts: {}", exception.getMessage());
            }
        }

        if (legacyTexts.isEmpty()) {
            String rawPrompt = settings.get(SETTINGS_KEY_LEGACY_PROMPT);
            if (StringUtils.isNotBlank(rawPrompt)) {
                for (String part : rawPrompt.split("\\r?\\n")) {
                    String text = sanitizePromptText(part);
                    if (text != null) {
                        legacyTexts.add(text);
                    }
                }
            }
        }

        if (legacyTexts.isEmpty()) {
            return new ArrayList<>();
        }
        return buildPromptItems(new ArrayList<>(legacyTexts), SOURCE_TYPE_MANUAL, SOURCE_LABEL_MANUAL, true);
    }

    private List<HomePromptItemDTO> sanitizeItems(List<HomePromptItemDTO> items) {
        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }

        List<HomePromptItemDTO> result = new ArrayList<>();
        Set<String> seenText = new LinkedHashSet<>();

        for (HomePromptItemDTO item : items) {
            String text = sanitizePromptText(item != null ? item.getText() : null);
            if (text == null || !seenText.add(text) || result.size() >= MAX_PROMPT_COUNT) {
                continue;
            }

            HomePromptItemDTO next = new HomePromptItemDTO();
            next.setId(StringUtils.defaultIfBlank(item != null ? item.getId() : null, newPromptId()));
            next.setText(text);
            String sourceType = normalizeSourceType(item != null ? item.getSourceType() : null);
            next.setSourceType(sourceType);
            next.setSourceLabel(resolveSourceLabel(sourceType, item != null ? item.getSourceLabel() : null));
            next.setEnabled(resolveEnabledFlag(item));
            next.setSelected(next.getEnabled());
            result.add(next);
        }

        return result;
    }

    private Boolean resolveEnabledFlag(HomePromptItemDTO item) {
        if (item == null) {
            return Boolean.FALSE;
        }
        if (item.getEnabled() != null) {
            return item.getEnabled();
        }
        return Boolean.TRUE.equals(item.getSelected());
    }

    private List<HomePromptItemDTO> stripTransientFields(List<HomePromptItemDTO> items) {
        List<HomePromptItemDTO> result = new ArrayList<>();
        for (HomePromptItemDTO item : items) {
            HomePromptItemDTO next = new HomePromptItemDTO();
            next.setId(item.getId());
            next.setText(item.getText());
            next.setSourceType(item.getSourceType());
            next.setSourceLabel(item.getSourceLabel());
            next.setEnabled(Boolean.TRUE.equals(item.getEnabled()));
            next.setSelected(null);
            result.add(next);
        }
        return result;
    }

    private List<HomePromptItemDTO> buildPromptItems(List<String> texts, String sourceType, String sourceLabel, boolean enabled) {
        List<HomePromptItemDTO> result = new ArrayList<>();
        Set<String> seenText = new LinkedHashSet<>();

        for (String text : texts) {
            String normalizedText = sanitizePromptText(text);
            if (normalizedText == null || !seenText.add(normalizedText) || result.size() >= MAX_PROMPT_COUNT) {
                continue;
            }

            HomePromptItemDTO item = new HomePromptItemDTO();
            item.setId(newPromptId());
            item.setText(normalizedText);
            item.setSourceType(normalizeSourceType(sourceType));
            item.setSourceLabel(resolveSourceLabel(sourceType, sourceLabel));
            item.setEnabled(enabled);
            item.setSelected(enabled);
            result.add(item);
        }

        return result;
    }

    private void ensureAtLeastOneEnabled(List<HomePromptItemDTO> items) {
        boolean hasEnabled = items.stream().anyMatch(item -> Boolean.TRUE.equals(item.getEnabled()));
        if (!hasEnabled && !items.isEmpty()) {
            items.get(0).setEnabled(Boolean.TRUE);
            items.get(0).setSelected(Boolean.TRUE);
        }
        for (HomePromptItemDTO item : items) {
            item.setSelected(Boolean.TRUE.equals(item.getEnabled()));
        }
    }

    private String resolvePrimaryEnabledId(List<HomePromptItemDTO> items) {
        for (HomePromptItemDTO item : items) {
            if (Boolean.TRUE.equals(item.getEnabled())) {
                return item.getId();
            }
        }
        return items.isEmpty() ? null : items.get(0).getId();
    }

    private List<String> extractEnabledTexts(List<HomePromptItemDTO> items) {
        return items.stream()
                .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                .map(HomePromptItemDTO::getText)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }

    private Boolean parseVisible(String raw) {
        if (raw == null) {
            return Boolean.TRUE;
        }
        return !"false".equalsIgnoreCase(raw.trim());
    }

    private int parseMs(String raw, int fallback, int min, int max) {
        try {
            int value = Integer.parseInt(StringUtils.defaultIfBlank(raw, String.valueOf(fallback)).trim());
            return Math.min(Math.max(value, min), max);
        } catch (Exception exception) {
            return fallback;
        }
    }

    private String extractPdfText(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream();
             PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException exception) {
            throw new IllegalArgumentException("PDF 读取失败，请换一个文件再试", exception);
        }
    }

    private List<String> parsePromptsWithAi(Long userId, String text) {
        String systemPrompt = String.join("\n",
                "你是一个中文文案整理助手。",
                "请从用户提供的 PDF 文本中提炼适合放在穿搭应用首页轮播的短提示语。",
                "要求：",
                "1. 每条都必须是简短中文句子。",
                "2. 每条不超过 20 个字或字符。",
                "3. 风格安静、克制、自然，不要营销口号。",
                "4. 只返回 JSON 数组，例如 [\"今天想穿成什么样\",\"把衣柜重新理一理\"]。",
                "5. 最多返回 12 条，不要输出任何额外解释。");

        String prompt = "PDF 文本如下：\n" + abbreviateForAi(text);
        String response = aiTextGeneratorService.generateCustomResponse(systemPrompt, prompt, null, userId);
        return extractPromptArray(response);
    }

    private List<String> extractPromptArray(String response) {
        if (StringUtils.isBlank(response)) {
            return new ArrayList<>();
        }

        try {
            int start = response.indexOf('[');
            int end = response.lastIndexOf(']');
            if (start >= 0 && end > start) {
                JSONArray array = JSON.parseArray(response.substring(start, end + 1));
                return sanitizePromptTexts(array.toJavaList(String.class), PDF_PREVIEW_LIMIT);
            }
        } catch (Exception exception) {
            log.warn("Failed to parse AI prompt response: {}", exception.getMessage());
        }

        return new ArrayList<>();
    }

    private List<String> fallbackPrompts(String text) {
        List<String> candidates = new ArrayList<>();
        String normalized = StringUtils.defaultString(text)
                .replace("\r", "\n")
                .replaceAll("[\\t\\u3000]+", " ")
                .replaceAll("\n{2,}", "\n");

        for (String line : normalized.split("\n")) {
            if (StringUtils.isBlank(line)) {
                continue;
            }

            for (String sentence : line.split("[。！？!?；;]")) {
                if (StringUtils.isBlank(sentence)) {
                    continue;
                }
                collectSentenceParts(candidates, sentence);
            }
        }

        return sanitizePromptTexts(candidates, PDF_PREVIEW_LIMIT);
    }

    private void collectSentenceParts(List<String> candidates, String text) {
        String trimmed = StringUtils.trimToEmpty(text);
        if (trimmed.isEmpty()) {
            return;
        }

        String cleaned = sanitizePromptText(trimmed);
        if (cleaned != null) {
            candidates.add(cleaned);
            return;
        }

        for (String segment : trimmed.split("[，、,:：/]")) {
            String shortText = sanitizePromptText(segment);
            if (shortText != null) {
                candidates.add(shortText);
            }
        }
    }

    private List<String> sanitizePromptTexts(List<String> texts, int limit) {
        List<String> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String text : texts) {
            String normalized = sanitizePromptText(text);
            if (normalized == null || !seen.add(normalized) || result.size() >= limit) {
                continue;
            }
            result.add(normalized);
        }
        return result;
    }

    private String sanitizePromptText(String text) {
        if (text == null) {
            return null;
        }

        String normalized = text.trim()
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ");
        normalized = LEADING_INDEX_PATTERN.matcher(normalized).replaceFirst("");
        normalized = LEADING_BULLET_PATTERN.matcher(normalized).replaceFirst("");
        normalized = INVALID_QUOTE_PATTERN.matcher(normalized).replaceAll("");
        normalized = normalized
                .replaceAll("[()（）【】\\[\\]]", "")
                .replaceAll("^[,，.。:：;；]+", "")
                .replaceAll("[,，.。:：;；]+$", "")
                .trim();

        if (normalized.isEmpty()) {
            return null;
        }

        int length = normalized.codePointCount(0, normalized.length());
        if (length > MAX_PROMPT_LENGTH) {
            return null;
        }
        return normalized;
    }

    private String normalizeSourceType(String sourceType) {
        return SOURCE_TYPE_PDF.equalsIgnoreCase(sourceType) ? SOURCE_TYPE_PDF : SOURCE_TYPE_MANUAL;
    }

    private String resolveSourceLabel(String sourceType, String sourceLabel) {
        if (StringUtils.isNotBlank(sourceLabel)) {
            return sourceLabel.trim();
        }
        return SOURCE_TYPE_PDF.equalsIgnoreCase(sourceType) ? SOURCE_LABEL_PDF : SOURCE_LABEL_MANUAL;
    }

    private String abbreviateForAi(String text) {
        String normalized = StringUtils.defaultString(text).trim();
        if (normalized.length() <= 5000) {
            return normalized;
        }
        return normalized.substring(0, 5000);
    }

    private String newPromptId() {
        return "hp_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
