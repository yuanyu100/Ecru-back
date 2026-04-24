package com.ecru.common.service.ai;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 图片分析服务。
 */
@Service("aiImageAnalyzerService")
public class AiImageAnalyzerService {

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private OkHttpClient okHttpClient;

    @PostConstruct
    public void init() {
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(300000, TimeUnit.MILLISECONDS)
                .readTimeout(300000, TimeUnit.MILLISECONDS)
                .writeTimeout(300000, TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * 分析穿搭图片。
     */
    public ImageAnalysisResult analyzeOutfit(InputStream imageStream) {
        try {
            byte[] imageBytes = inputStreamToByteArray(imageStream);
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String imageHash = Base64.getEncoder().encodeToString(Objects.requireNonNull(imageBytes));
            String cacheKey = "image_analysis:" + imageHash;

            boolean enableCache = true;
            if (enableCache && redisTemplate != null) {
                try {
                    String cachedData = redisTemplate.opsForValue().get(cacheKey);
                    if (cachedData != null) {
                        return ImageAnalysisResult.fromJson(cachedData);
                    }
                } catch (Exception e) {
                    System.err.println("Redis缓存读取失败: " + e.getMessage());
                }
            }

            ImageAnalysisResult result = callQwenVlApi(base64Image, getOutfitAnalysisPrompt());

            if (result != null && enableCache && redisTemplate != null) {
                try {
                    redisTemplate.opsForValue().set(cacheKey, result.toJson(), 86400, TimeUnit.SECONDS);
                } catch (Exception e) {
                    System.err.println("Redis缓存写入失败: " + e.getMessage());
                }
            }

            return result;
        } catch (Exception e) {
            System.err.println("分析穿搭图片失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 分析衣物图片。
     */
    public ClothingAnalysisResult analyzeClothing(InputStream imageStream) throws Exception {
        byte[] imageBytes = inputStreamToByteArray(imageStream);
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String imageHash = Base64.getEncoder().encodeToString(Objects.requireNonNull(imageBytes));
        String cacheKey = "clothing_analysis:" + imageHash;

        boolean enableCache = true;
        if (enableCache && redisTemplate != null) {
            try {
                String cachedData = redisTemplate.opsForValue().get(cacheKey);
                if (cachedData != null) {
                    return ClothingAnalysisResult.fromJson(cachedData);
                }
            } catch (Exception e) {
                System.err.println("Redis缓存读取失败: " + e.getMessage());
            }
        }

        ClothingAnalysisResult result = callQwenVlApiForClothing(base64Image, getClothingAnalysisPrompt());

        if (result != null && enableCache && redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(cacheKey, result.toJson(), 86400, TimeUnit.SECONDS);
            } catch (Exception e) {
                System.err.println("Redis缓存写入失败: " + e.getMessage());
            }
        }

        if (result == null) {
            throw new Exception("AI分析结果为空");
        }

        return result;
    }

    /**
     * 分析材质/成分/洗护标签图片。
     */
    public MaterialLabelAnalysisResult analyzeMaterialLabel(InputStream imageStream) throws Exception {
        byte[] imageBytes = inputStreamToByteArray(imageStream);
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String imageHash = Base64.getEncoder().encodeToString(Objects.requireNonNull(imageBytes));
        String cacheKey = "material_label_analysis:" + imageHash;

        boolean enableCache = true;
        if (enableCache && redisTemplate != null) {
            try {
                String cachedData = redisTemplate.opsForValue().get(cacheKey);
                if (cachedData != null) {
                    return MaterialLabelAnalysisResult.fromJson(cachedData);
                }
            } catch (Exception e) {
                System.err.println("Redis缓存读取失败: " + e.getMessage());
            }
        }

        MaterialLabelAnalysisResult result = null;
        Exception materialException = null;
        try {
            result = callQwenVlApiForMaterialLabel(base64Image, getMaterialLabelAnalysisPrompt());
        } catch (Exception e) {
            materialException = e;
        }

        if (result == null) {
            try {
                ClothingAnalysisResult clothingAnalysisResult =
                        analyzeClothing(new ByteArrayInputStream(imageBytes));
                result = buildFallbackMaterialLabelResult(clothingAnalysisResult);
            } catch (Exception e) {
                if (materialException != null) {
                    throw materialException;
                }
                throw e;
            }
        }

        if (result != null && enableCache && redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(cacheKey, result.toJson(), 86400, TimeUnit.SECONDS);
            } catch (Exception e) {
                System.err.println("Redis缓存写入失败: " + e.getMessage());
            }
        }

        if (result == null) {
            throw new Exception("AI材质标签分析结果为空");
        }

        return result;
    }

    private ClothingAnalysisResult callQwenVlApiForClothing(String base64Image, String prompt) throws IOException {
        JSONObject response = callQwenVlApiRaw(base64Image, prompt);
        return parseClothingAnalysisResponse(response);
    }

    private MaterialLabelAnalysisResult callQwenVlApiForMaterialLabel(String base64Image, String prompt) throws IOException {
        JSONObject response = callQwenVlApiRaw(base64Image, prompt);
        return parseMaterialLabelAnalysisResponse(response);
    }

    private ImageAnalysisResult callQwenVlApi(String base64Image, String prompt) throws IOException {
        JSONObject response = callQwenVlApiRaw(base64Image, prompt);
        return parseImageAnalysisResponse(response);
    }

    private JSONObject callQwenVlApiRaw(String base64Image, String prompt) throws IOException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "Qwen/Qwen3-VL-8B-Instruct");
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 2048);

        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();
        message.put("role", "user");

        JSONArray content = new JSONArray();

        JSONObject textContent = new JSONObject();
        textContent.put("type", "text");
        textContent.put("text", prompt);
        content.add(textContent);

        JSONObject imageContent = new JSONObject();
        imageContent.put("type", "image_url");
        JSONObject imageUrl = new JSONObject();
        imageUrl.put("url", "data:image/jpeg;base64," + base64Image);
        imageContent.put("image_url", imageUrl);
        content.add(imageContent);

        message.put("content", content);
        messages.add(message);
        requestBody.put("messages", messages);

        Request request = new Request.Builder()
                .url("https://api.siliconflow.cn/v1/chat/completions")
                .header("Authorization", "Bearer sk-rfukaeuhisaxyfjyfnigayueguxsfwikfswwjubmggxhwvvb")
                .header("Content-Type", "application/json")
                .post(RequestBody.create(requestBody.toJSONString(), MediaType.parse("application/json")))
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = null;
                if (response.body() != null) {
                    errorBody = response.body().string();
                }
                throw new IOException("API调用失败: " + response.code() + " " + response.message() + ". 错误详情: " + errorBody);
            }
            String responseBody = response.body().string();
            return JSON.parseObject(responseBody);
        }
    }

    private ImageAnalysisResult parseImageAnalysisResponse(JSONObject response) {
        try {
            JSONObject analysisJson = extractJsonContent(response);
            if (analysisJson == null) {
                return null;
            }

            ImageAnalysisResult result = new ImageAnalysisResult();
            result.setDetectedItems(new ArrayList<>());
            result.setStyle(analysisJson.getString("style"));
            result.setColorAnalysis(analysisJson.getString("colorAnalysis"));
            result.setOccasion(analysisJson.getString("occasion"));
            result.setSeason(analysisJson.getString("season"));
            result.setSuggestions(analysisJson.getString("suggestions"));
            return result;
        } catch (Exception e) {
            System.err.println("解析穿搭图片响应失败: " + e.getMessage());
            return null;
        }
    }

    private ClothingAnalysisResult parseClothingAnalysisResponse(JSONObject response) {
        try {
            JSONObject analysisJson = extractJsonContent(response);
            if (analysisJson == null) {
                return null;
            }

            ClothingAnalysisResult result = new ClothingAnalysisResult();
            result.setCategory(analysisJson.getString("category"));
            result.setColor(new HashMap<>());
            if (analysisJson.containsKey("color")) {
                result.setColor(analysisJson.getJSONObject("color").toJavaObject(Map.class));
            }
            result.setStyle(new ArrayList<>());
            if (analysisJson.containsKey("style")) {
                result.setStyle(analysisJson.getJSONArray("style").toJavaList(String.class));
            }
            result.setMaterial(analysisJson.getString("material"));
            result.setPattern(analysisJson.getString("pattern"));
            result.setOccasion(new ArrayList<>());
            if (analysisJson.containsKey("occasion")) {
                result.setOccasion(analysisJson.getJSONArray("occasion").toJavaList(String.class));
            }
            result.setSeason(new ArrayList<>());
            if (analysisJson.containsKey("season")) {
                result.setSeason(analysisJson.getJSONArray("season").toJavaList(String.class));
            }
            return result;
        } catch (Exception e) {
            System.err.println("解析衣物图片响应失败: " + e.getMessage());
            return null;
        }
    }

    private MaterialLabelAnalysisResult parseMaterialLabelAnalysisResponse(JSONObject response) {
        try {
            JSONObject analysisJson = extractJsonContent(response);
            if (analysisJson == null) {
                return null;
            }

            MaterialLabelAnalysisResult result = new MaterialLabelAnalysisResult();
            result.setDetectedText(analysisJson.getString("detectedText"));
            result.setProductType(analysisJson.getString("productType"));
            result.setConfidence(analysisJson.getDouble("confidence"));
            result.setSummary(analysisJson.getString("summary"));
            result.setMaterials(parseMaterialComponents(analysisJson.getJSONArray("materials")));
            result.setCareLabels(parseCareLabelSignals(analysisJson.getJSONArray("careLabels")));
            return result;
        } catch (Exception e) {
            System.err.println("解析材质标签响应失败: " + e.getMessage());
            return null;
        }
    }

    private JSONObject extractJsonContent(JSONObject response) {
        JSONArray choices = response.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            return null;
        }

        JSONObject choice = choices.getJSONObject(0);
        if (choice == null || choice.getJSONObject("message") == null) {
            return null;
        }

        String content = choice.getJSONObject("message").getString("content");
        if (content == null || content.isBlank()) {
            return null;
        }

        String jsonContent = content.trim();
        if (jsonContent.startsWith("```")) {
            int firstBreak = jsonContent.indexOf('\n');
            if (firstBreak >= 0) {
                jsonContent = jsonContent.substring(firstBreak + 1);
            }
            if (jsonContent.endsWith("```")) {
                jsonContent = jsonContent.substring(0, jsonContent.length() - 3);
            }
            jsonContent = jsonContent.trim();
        }

        int start = jsonContent.indexOf('{');
        int end = jsonContent.lastIndexOf('}');
        if (start >= 0 && end > start) {
            jsonContent = jsonContent.substring(start, end + 1);
        }
        return JSON.parseObject(jsonContent);
    }

    private List<MaterialComponent> parseMaterialComponents(JSONArray materialsArray) {
        List<MaterialComponent> materials = new ArrayList<>();
        if (materialsArray == null) {
            return materials;
        }

        for (Object item : materialsArray) {
            if (item instanceof JSONObject materialObject) {
                MaterialComponent material = new MaterialComponent();
                material.setName(materialObject.getString("name"));
                material.setRatio(materialObject.getString("ratio"));
                material.setRawText(materialObject.getString("rawText"));
                material.setConfidence(materialObject.getDouble("confidence"));
                materials.add(material);
            } else if (item != null) {
                MaterialComponent material = new MaterialComponent();
                material.setName(String.valueOf(item));
                materials.add(material);
            }
        }
        return materials;
    }

    private List<CareLabelSignal> parseCareLabelSignals(JSONArray careLabelsArray) {
        List<CareLabelSignal> careLabels = new ArrayList<>();
        if (careLabelsArray == null) {
            return careLabels;
        }

        for (Object item : careLabelsArray) {
            if (item instanceof JSONObject careLabelObject) {
                CareLabelSignal careLabel = new CareLabelSignal();
                careLabel.setSymbolCode(careLabelObject.getString("symbolCode"));
                careLabel.setSymbolName(careLabelObject.getString("symbolName"));
                careLabel.setInstruction(careLabelObject.getString("instruction"));
                careLabel.setRawText(careLabelObject.getString("rawText"));
                careLabel.setConfidence(careLabelObject.getDouble("confidence"));
                careLabels.add(careLabel);
            } else if (item != null) {
                CareLabelSignal careLabel = new CareLabelSignal();
                careLabel.setInstruction(String.valueOf(item));
                careLabels.add(careLabel);
            }
        }
        return careLabels;
    }

    private MaterialLabelAnalysisResult buildFallbackMaterialLabelResult(ClothingAnalysisResult clothingAnalysisResult) {
        MaterialLabelAnalysisResult result = new MaterialLabelAnalysisResult();
        result.setMaterials(new ArrayList<>());
        result.setCareLabels(new ArrayList<>());

        if (clothingAnalysisResult == null) {
            result.setConfidence(0.3D);
            result.setSummary("未能精确识别材质标签，可以重新上传更清晰的成分标或洗护标图片。");
            return result;
        }

        result.setProductType(clothingAnalysisResult.getCategory());
        result.setConfidence(0.45D);
        if (clothingAnalysisResult.getMaterial() != null && !clothingAnalysisResult.getMaterial().isBlank()) {
            MaterialComponent material = new MaterialComponent();
            material.setName(clothingAnalysisResult.getMaterial());
            material.setConfidence(0.45D);
            result.getMaterials().add(material);
            result.setSummary("未识别到完整的成分标或洗护标，但基于图片判断材质可能为 " + clothingAnalysisResult.getMaterial() + "。");
        } else {
            result.setSummary("未识别到完整的成分标或洗护标，可以尽量拍摄局部文字、成分表和洗护图标。");
        }
        return result;
    }

    private String getOutfitAnalysisPrompt() {
        return "请分析这张穿搭照片，严格输出 JSON，不要输出额外说明。字段如下：\n" +
                "{\n" +
                "  \"detectedItems\": [\n" +
                "    {\"item\": \"上衣\", \"color\": \"白色\", \"category\": \"上装\", \"confidence\": 0.95}\n" +
                "  ],\n" +
                "  \"style\": \"韩系休闲风\",\n" +
                "  \"colorAnalysis\": \"灰黑配色，低调简约\",\n" +
                "  \"occasion\": \"日常休闲\",\n" +
                "  \"season\": \"秋冬\",\n" +
                "  \"suggestions\": \"版型较宽松，建议强调腰线提升比例\"\n" +
                "}";
    }

    private String getClothingAnalysisPrompt() {
        return "请分析这张衣物图片，严格输出 JSON，不要输出额外说明。字段如下：\n" +
                "{\n" +
                "  \"category\": \"上装\",\n" +
                "  \"color\": {\n" +
                "    \"primary\": \"白色\",\n" +
                "    \"secondary\": \"浅蓝色\"\n" +
                "  },\n" +
                "  \"style\": [\"简约\", \"通勤\"],\n" +
                "  \"material\": \"棉\",\n" +
                "  \"pattern\": \"条纹\",\n" +
                "  \"occasion\": [\"日常\", \"通勤\"],\n" +
                "  \"season\": [\"春\", \"秋\"]\n" +
                "}";
    }

    private String getMaterialLabelAnalysisPrompt() {
        return "你是服装成分标与洗护标识别助手。请识别图片中的面料成分、材质名称、洗护图标、洗护文字和可能的品类。" +
                "严格输出 JSON，不要输出额外说明。如果看不清，请尽量给出低置信度推断并说明。字段如下：\n" +
                "{\n" +
                "  \"detectedText\": \"成分和洗护标签上识别到的主要文字，保留原文\",\n" +
                "  \"materials\": [\n" +
                "    {\"name\": \"cotton\", \"ratio\": \"95%\", \"rawText\": \"95% COTTON\", \"confidence\": 0.96},\n" +
                "    {\"name\": \"spandex\", \"ratio\": \"5%\", \"rawText\": \"5% SPANDEX\", \"confidence\": 0.90}\n" +
                "  ],\n" +
                "  \"careLabels\": [\n" +
                "    {\"symbolCode\": \"hand-wash\", \"symbolName\": \"Hand wash only\", \"instruction\": \"冷水手洗\", \"rawText\": \"HAND WASH\", \"confidence\": 0.92}\n" +
                "  ],\n" +
                "  \"productType\": \"shirt\",\n" +
                "  \"confidence\": 0.88,\n" +
                "  \"summary\": \"这是一件以棉为主、含少量弹性纤维的上衣，建议冷水轻柔清洗。\"\n" +
                "}";
    }

    private byte[] inputStreamToByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, length);
        }
        return outputStream.toByteArray();
    }

    public static class ImageAnalysisResult {
        private List<Map<String, Object>> detectedItems;
        private String style;
        private String colorAnalysis;
        private String occasion;
        private String season;
        private String suggestions;

        public List<Map<String, Object>> getDetectedItems() {
            return detectedItems;
        }

        public void setDetectedItems(List<Map<String, Object>> detectedItems) {
            this.detectedItems = detectedItems;
        }

        public String getStyle() {
            return style;
        }

        public void setStyle(String style) {
            this.style = style;
        }

        public String getColorAnalysis() {
            return colorAnalysis;
        }

        public void setColorAnalysis(String colorAnalysis) {
            this.colorAnalysis = colorAnalysis;
        }

        public String getOccasion() {
            return occasion;
        }

        public void setOccasion(String occasion) {
            this.occasion = occasion;
        }

        public String getSeason() {
            return season;
        }

        public void setSeason(String season) {
            this.season = season;
        }

        public String getSuggestions() {
            return suggestions;
        }

        public void setSuggestions(String suggestions) {
            this.suggestions = suggestions;
        }

        public String toJson() {
            return JSON.toJSONString(this);
        }

        public static ImageAnalysisResult fromJson(String json) {
            return JSON.parseObject(json, ImageAnalysisResult.class);
        }
    }

    public static class ClothingAnalysisResult {
        private String category;
        private Map<String, String> color;
        private List<String> style;
        private String material;
        private String pattern;
        private List<String> occasion;
        private List<String> season;

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public Map<String, String> getColor() {
            return color;
        }

        public void setColor(Map<String, String> color) {
            this.color = color;
        }

        public List<String> getStyle() {
            return style;
        }

        public void setStyle(List<String> style) {
            this.style = style;
        }

        public String getMaterial() {
            return material;
        }

        public void setMaterial(String material) {
            this.material = material;
        }

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }

        public List<String> getOccasion() {
            return occasion;
        }

        public void setOccasion(List<String> occasion) {
            this.occasion = occasion;
        }

        public List<String> getSeason() {
            return season;
        }

        public void setSeason(List<String> season) {
            this.season = season;
        }

        public String toJson() {
            return JSON.toJSONString(this);
        }

        public static ClothingAnalysisResult fromJson(String json) {
            return JSON.parseObject(json, ClothingAnalysisResult.class);
        }
    }

    public static class MaterialLabelAnalysisResult {
        private String detectedText;
        private List<MaterialComponent> materials;
        private List<CareLabelSignal> careLabels;
        private String productType;
        private Double confidence;
        private String summary;

        public String getDetectedText() {
            return detectedText;
        }

        public void setDetectedText(String detectedText) {
            this.detectedText = detectedText;
        }

        public List<MaterialComponent> getMaterials() {
            return materials;
        }

        public void setMaterials(List<MaterialComponent> materials) {
            this.materials = materials;
        }

        public List<CareLabelSignal> getCareLabels() {
            return careLabels;
        }

        public void setCareLabels(List<CareLabelSignal> careLabels) {
            this.careLabels = careLabels;
        }

        public String getProductType() {
            return productType;
        }

        public void setProductType(String productType) {
            this.productType = productType;
        }

        public Double getConfidence() {
            return confidence;
        }

        public void setConfidence(Double confidence) {
            this.confidence = confidence;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public String toJson() {
            return JSON.toJSONString(this);
        }

        public static MaterialLabelAnalysisResult fromJson(String json) {
            return JSON.parseObject(json, MaterialLabelAnalysisResult.class);
        }
    }

    public static class MaterialComponent {
        private String name;
        private String ratio;
        private String rawText;
        private Double confidence;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getRatio() {
            return ratio;
        }

        public void setRatio(String ratio) {
            this.ratio = ratio;
        }

        public String getRawText() {
            return rawText;
        }

        public void setRawText(String rawText) {
            this.rawText = rawText;
        }

        public Double getConfidence() {
            return confidence;
        }

        public void setConfidence(Double confidence) {
            this.confidence = confidence;
        }
    }

    public static class CareLabelSignal {
        private String symbolCode;
        private String symbolName;
        private String instruction;
        private String rawText;
        private Double confidence;

        public String getSymbolCode() {
            return symbolCode;
        }

        public void setSymbolCode(String symbolCode) {
            this.symbolCode = symbolCode;
        }

        public String getSymbolName() {
            return symbolName;
        }

        public void setSymbolName(String symbolName) {
            this.symbolName = symbolName;
        }

        public String getInstruction() {
            return instruction;
        }

        public void setInstruction(String instruction) {
            this.instruction = instruction;
        }

        public String getRawText() {
            return rawText;
        }

        public void setRawText(String rawText) {
            this.rawText = rawText;
        }

        public Double getConfidence() {
            return confidence;
        }

        public void setConfidence(Double confidence) {
            this.confidence = confidence;
        }
    }
}
