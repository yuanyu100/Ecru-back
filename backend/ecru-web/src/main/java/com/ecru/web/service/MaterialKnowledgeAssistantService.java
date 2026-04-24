package com.ecru.web.service;

import com.ecru.common.exception.BusinessException;
import com.ecru.common.service.ai.AiImageAnalyzerService;
import com.ecru.common.service.ai.AiTextGeneratorService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class MaterialKnowledgeAssistantService {

    private static final int MATCH_LIMIT = 5;

    private final KnowledgeBaseService knowledgeBaseService;
    private final AiTextGeneratorService aiTextGeneratorService;

    public MaterialKnowledgeAssistantService(KnowledgeBaseService knowledgeBaseService,
                                             AiTextGeneratorService aiTextGeneratorService) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.aiTextGeneratorService = aiTextGeneratorService;
    }

    public Map<String, Object> askMaterialQuestion(String material, String question, Long userId) {
        String safeMaterial = StringUtils.trimToEmpty(material);
        String safeQuestion = StringUtils.trimToEmpty(question);
        if (StringUtils.isBlank(safeMaterial)) {
            throw new BusinessException(400, "材质不能为空");
        }
        if (StringUtils.isBlank(safeQuestion)) {
            throw new BusinessException(400, "问题不能为空");
        }

        List<String> keywords = collectKeywords(safeMaterial, safeQuestion, null);
        List<Map<String, Object>> matchedFabrics = knowledgeBaseService.matchFabrics(keywords, MATCH_LIMIT);
        List<Map<String, Object>> matchedCareLabels = knowledgeBaseService.matchCareLabels(keywords, MATCH_LIMIT);

        String answer = generateAiAnswer(safeMaterial, safeQuestion, matchedFabrics, matchedCareLabels, null, userId);
        String answerSource = "ai";
        if (StringUtils.isBlank(answer)) {
            answer = buildFallbackAnswer(safeMaterial, safeQuestion, matchedFabrics, matchedCareLabels, null);
            answerSource = "knowledge-base";
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("material", safeMaterial);
        result.put("matchedFabrics", matchedFabrics);
        result.put("matchedCareLabels", matchedCareLabels);
        result.put("answer", answer);
        result.put("answerSource", answerSource);
        return result;
    }

    public Map<String, Object> buildMaterialAnalysisResponse(
            AiImageAnalyzerService.MaterialLabelAnalysisResult analysis,
            String materialHint,
            String question,
            Long userId) {
        AiImageAnalyzerService.MaterialLabelAnalysisResult safeAnalysis =
                analysis != null ? analysis : new AiImageAnalyzerService.MaterialLabelAnalysisResult();

        List<String> keywords = collectKeywords(materialHint, question, safeAnalysis);
        List<Map<String, Object>> matchedFabrics = knowledgeBaseService.matchFabrics(keywords, MATCH_LIMIT);
        List<Map<String, Object>> matchedCareLabels = knowledgeBaseService.matchCareLabels(keywords, MATCH_LIMIT);

        String inferredMaterial = resolvePrimaryMaterial(safeAnalysis, materialHint, matchedFabrics);
        String safeQuestion = StringUtils.trimToEmpty(question);
        String answer = null;
        String answerSource = null;
        if (StringUtils.isNotBlank(safeQuestion)) {
            answer = generateAiAnswer(inferredMaterial, safeQuestion, matchedFabrics, matchedCareLabels, safeAnalysis, userId);
            answerSource = "ai";
            if (StringUtils.isBlank(answer)) {
                answer = buildFallbackAnswer(inferredMaterial, safeQuestion, matchedFabrics, matchedCareLabels, safeAnalysis);
                answerSource = "knowledge-base";
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("analysis", toAnalysisMap(safeAnalysis));
        result.put("matchedFabrics", matchedFabrics);
        result.put("matchedCareLabels", matchedCareLabels);
        result.put("material", inferredMaterial);
        if (StringUtils.isNotBlank(safeQuestion)) {
            result.put("answer", answer);
            result.put("answerSource", answerSource);
        }
        return result;
    }

    private String generateAiAnswer(String material,
                                    String question,
                                    List<Map<String, Object>> matchedFabrics,
                                    List<Map<String, Object>> matchedCareLabels,
                                    AiImageAnalyzerService.MaterialLabelAnalysisResult analysis,
                                    Long userId) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("material", material);
        context.put("matchedFabrics", matchedFabrics);
        context.put("matchedCareLabels", matchedCareLabels);
        if (analysis != null) {
            context.put("imageAnalysis", toAnalysisMap(analysis));
        }

        String systemPrompt = "你是服装材质、面料和洗护知识助手。请基于给定的知识库匹配结果和图片识别结果回答，" +
                "不要编造知识库里没有的明确事实；若不确定要明确说明。回答使用简洁中文，优先说明材质特点、优缺点、适用场景和洗护建议。";
        String userPrompt = "用户识别到的主要材质是：" + StringUtils.defaultIfBlank(material, "未明确") + "\n"
                + "用户问题：" + question + "\n"
                + "请结合参考信息回答。如果问题涉及优缺点、是否值得购买、适不适合某场景，也请一并说明。";

        String answer = aiTextGeneratorService.generateCustomResponse(systemPrompt, userPrompt, context, userId);
        return StringUtils.trimToNull(answer);
    }

    private String buildFallbackAnswer(String material,
                                       String question,
                                       List<Map<String, Object>> matchedFabrics,
                                       List<Map<String, Object>> matchedCareLabels,
                                       AiImageAnalyzerService.MaterialLabelAnalysisResult analysis) {
        String subject = StringUtils.defaultIfBlank(material, "这件衣物");
        List<String> parts = new ArrayList<>();

        if (analysis != null && StringUtils.isNotBlank(analysis.getSummary())) {
            parts.add("图片识别结果显示，" + analysis.getSummary());
        }

        Map<String, Object> topFabric = matchedFabrics.isEmpty() ? null : matchedFabrics.get(0);
        if (topFabric != null) {
            String fabricName = resolveFabricDisplayName(topFabric);
            parts.add(subject + "较接近知识库中的「" + fabricName + "」。");

            String summary = asString(topFabric.get("summary"));
            if (StringUtils.isNotBlank(summary)) {
                parts.add(summary);
            }

            Map<String, Object> characteristics = readMap(topFabric.get("characteristics"));
            List<String> pros = new ArrayList<>();
            List<String> cautions = new ArrayList<>();
            appendCharacteristic(pros, cautions, characteristics, "warmth", "保暖性强", "保暖性一般");
            appendCharacteristic(pros, cautions, characteristics, "breathability", "透气性好", "透气性一般");
            appendCharacteristic(pros, cautions, characteristics, "comfort", "穿着舒适度高", "舒适度一般");
            appendCharacteristic(pros, cautions, characteristics, "durability", "耐穿耐磨", "耐用度一般");

            if (needsAdvantageAnswer(question) && !pros.isEmpty()) {
                parts.add("优点上，" + String.join("、", pros) + "。");
            }
            if (needsAdvantageAnswer(question) && !cautions.isEmpty()) {
                parts.add("需要注意的是，" + String.join("、", cautions) + "。");
            }

            if (needsSceneAnswer(question)) {
                List<String> seasons = readStringList(topFabric.get("suitableSeasons"));
                List<String> occasions = readStringList(topFabric.get("suitableOccasions"));
                if (!seasons.isEmpty() || !occasions.isEmpty()) {
                    parts.add("更适合" + joinSceneText(seasons, occasions) + "。");
                }
            }

            if (needsCareAnswer(question) || matchedCareLabels.isEmpty()) {
                String careGuide = asString(topFabric.get("careGuide"));
                if (StringUtils.isNotBlank(careGuide)) {
                    parts.add("护理上建议：" + careGuide);
                }
            }
        }

        if (!matchedCareLabels.isEmpty()) {
            List<String> careParts = new ArrayList<>();
            for (int i = 0; i < Math.min(2, matchedCareLabels.size()); i++) {
                Map<String, Object> label = matchedCareLabels.get(i);
                String name = asString(label.get("symbolName"));
                String instruction = asString(label.get("instruction"));
                String doText = asString(label.get("doText"));
                String dontText = asString(label.get("dontText"));
                StringBuilder builder = new StringBuilder();
                if (StringUtils.isNotBlank(name)) {
                    builder.append("「").append(name).append("」");
                }
                if (StringUtils.isNotBlank(instruction)) {
                    builder.append(instruction);
                }
                if (StringUtils.isNotBlank(doText)) {
                    builder.append("，建议").append(doText);
                }
                if (StringUtils.isNotBlank(dontText)) {
                    builder.append("，避免").append(dontText);
                }
                if (builder.length() > 0) {
                    careParts.add(builder.toString());
                }
            }
            if (!careParts.isEmpty()) {
                parts.add("洗护上重点关注：" + String.join("；", careParts) + "。");
            }
        }

        if (parts.isEmpty()) {
            return "暂时还没有匹配到足够的材质知识，建议补拍更清晰的成分标或直接提供具体面料名称后再问。";
        }
        return String.join("", parts);
    }

    private Map<String, Object> toAnalysisMap(AiImageAnalyzerService.MaterialLabelAnalysisResult analysis) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("detectedText", analysis.getDetectedText());
        result.put("materials", analysis.getMaterials() == null ? List.of() : analysis.getMaterials());
        result.put("careLabels", analysis.getCareLabels() == null ? List.of() : analysis.getCareLabels());
        result.put("productType", analysis.getProductType());
        result.put("confidence", analysis.getConfidence());
        result.put("summary", analysis.getSummary());
        return result;
    }

    private List<String> collectKeywords(String materialHint,
                                         String question,
                                         AiImageAnalyzerService.MaterialLabelAnalysisResult analysis) {
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        addKeywordText(keywords, materialHint);
        addKeywordText(keywords, question);

        if (analysis != null) {
            addKeywordText(keywords, analysis.getDetectedText());
            addKeywordText(keywords, analysis.getProductType());
            addKeywordText(keywords, analysis.getSummary());

            if (analysis.getMaterials() != null) {
                for (AiImageAnalyzerService.MaterialComponent material : analysis.getMaterials()) {
                    addKeywordText(keywords, material.getName());
                    addKeywordText(keywords, material.getRawText());
                    addKeywordText(keywords, material.getRatio());
                }
            }
            if (analysis.getCareLabels() != null) {
                for (AiImageAnalyzerService.CareLabelSignal careLabel : analysis.getCareLabels()) {
                    addKeywordText(keywords, careLabel.getSymbolCode());
                    addKeywordText(keywords, careLabel.getSymbolName());
                    addKeywordText(keywords, careLabel.getInstruction());
                    addKeywordText(keywords, careLabel.getRawText());
                }
            }
        }

        return new ArrayList<>(keywords);
    }

    private void addKeywordText(LinkedHashSet<String> keywords, String value) {
        String trimmed = StringUtils.trimToEmpty(value);
        if (StringUtils.isBlank(trimmed)) {
            return;
        }
        keywords.add(trimmed);
        for (String token : trimmed.split("[\\s,，。；;/]+")) {
            String normalized = token.trim();
            if (StringUtils.isNotBlank(normalized)) {
                keywords.add(normalized);
            }
        }
    }

    private String resolvePrimaryMaterial(AiImageAnalyzerService.MaterialLabelAnalysisResult analysis,
                                          String materialHint,
                                          List<Map<String, Object>> matchedFabrics) {
        if (analysis != null && analysis.getMaterials() != null) {
            for (AiImageAnalyzerService.MaterialComponent material : analysis.getMaterials()) {
                if (StringUtils.isNotBlank(material.getName())) {
                    return material.getName();
                }
            }
        }
        if (StringUtils.isNotBlank(materialHint)) {
            return materialHint.trim();
        }
        if (!matchedFabrics.isEmpty()) {
            return asString(matchedFabrics.get(0).get("name"));
        }
        return "";
    }

    private boolean needsAdvantageAnswer(String question) {
        String q = StringUtils.defaultString(question);
        return q.contains("好") || q.contains("优点") || q.contains("缺点") || q.contains("值得")
                || q.contains("怎么样") || q.contains("适合");
    }

    private boolean needsCareAnswer(String question) {
        String q = StringUtils.defaultString(question);
        return q.contains("洗") || q.contains("护理") || q.contains("保养") || q.contains("晾") || q.contains("熨");
    }

    private boolean needsSceneAnswer(String question) {
        String q = StringUtils.defaultString(question);
        return q.contains("适合") || q.contains("季节") || q.contains("场景") || q.contains("穿");
    }

    private void appendCharacteristic(List<String> pros,
                                      List<String> cautions,
                                      Map<String, Object> characteristics,
                                      String key,
                                      String positiveText,
                                      String cautionText) {
        Integer score = readInt(characteristics.get(key));
        if (score == null) {
            return;
        }
        if (score >= 80) {
            pros.add(positiveText);
        } else if (score <= 60) {
            cautions.add(cautionText);
        }
    }

    private String resolveFabricDisplayName(Map<String, Object> fabric) {
        String name = asString(fabric.get("name"));
        List<String> alias = readStringList(fabric.get("alias"));
        for (String item : alias) {
            if (containsChinese(item)) {
                return item + (StringUtils.isNotBlank(name) ? "（" + name + "）" : "");
            }
        }
        return StringUtils.defaultIfBlank(name, "未知面料");
    }

    private boolean containsChinese(String value) {
        for (char ch : StringUtils.defaultString(value).toCharArray()) {
            if (Character.UnicodeScript.of(ch) == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        return false;
    }

    private String joinSceneText(List<String> seasons, List<String> occasions) {
        List<String> parts = new ArrayList<>();
        if (!seasons.isEmpty()) {
            parts.add(String.join("、", seasons) + "季");
        }
        if (!occasions.isEmpty()) {
            parts.add(String.join("、", occasions) + "场景");
        }
        return String.join("，", parts);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, mapValue) -> result.put(String.valueOf(key), mapValue));
            return result;
        }
        return Map.of();
    }

    private List<String> readStringList(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .toList();
        }
        return List.of();
    }

    private Integer readInt(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
