package com.ecru.outfit.service.rag;

import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 嵌入服务
 */
@Service
public class EmbeddingService {

    private final OkHttpClient okHttpClient;

    public EmbeddingService() {
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(30000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .writeTimeout(30000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * 生成文本嵌入
     * @param text 文本
     * @return 嵌入向量
     */
    public float[] generateEmbedding(String text) {
        try {
            // 构建请求体
            String requestBody = "{" +
                    "\"model\":\"text-embedding-3-small\"," +
                    "\"input\":\"" + text.replace("\"", "\\\"") + "\"" +
                    "}";

            // 构建请求
            Request request = new Request.Builder()
                    .url("https://api.siliconflow.cn/v1/embeddings")
                    .header("Authorization", "Bearer test-api-key")
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();

            // 执行请求
            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("API调用失败: " + response.code() + " " + response.message());
                }

                // 解析响应
                String responseBody = response.body().string();
                return parseEmbeddingResponse(responseBody);
            }
        } catch (Exception e) {
            System.err.println("生成文本嵌入失败: " + e.getMessage());
            return new float[1024]; // 返回默认嵌入向量
        }
    }

    /**
     * 批量生成文本嵌入
     * @param texts 文本列表
     * @return 嵌入向量列表
     */
    public List<float[]> generateBatchEmbeddings(List<String> texts) {
        try {
            // 构建请求体
            StringBuilder inputBuilder = new StringBuilder();
            for (String text : texts) {
                inputBuilder.append("\"").append(text.replace("\"", "\\\"")).append("\"," );
            }
            String input = inputBuilder.toString();
            if (input.endsWith(",")) {
                input = input.substring(0, input.length() - 1);
            }

            String requestBody = "{" +
                    "\"model\":\"text-embedding-3-small\"," +
                    "\"input\":[" + input + "]" +
                    "}";

            // 构建请求
            Request request = new Request.Builder()
                    .url("https://api.siliconflow.cn/v1/embeddings")
                    .header("Authorization", "Bearer test-api-key")
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();

            // 执行请求
            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("API调用失败: " + response.code() + " " + response.message());
                }

                // 解析响应
                String responseBody = response.body().string();
                return parseBatchEmbeddingResponse(responseBody);
            }
        } catch (Exception e) {
            System.err.println("批量生成文本嵌入失败: " + e.getMessage());
            List<float[]> defaultEmbeddings = new ArrayList<>();
            for (int i = 0; i < texts.size(); i++) {
                defaultEmbeddings.add(new float[1024]);
            }
            return defaultEmbeddings;
        }
    }

    /**
     * 解析嵌入响应
     * @param responseBody 响应体
     * @return 嵌入向量
     */
    private float[] parseEmbeddingResponse(String responseBody) {
        try {
            com.alibaba.fastjson2.JSONObject json = com.alibaba.fastjson2.JSON.parseObject(responseBody);
            com.alibaba.fastjson2.JSONArray data = json.getJSONArray("data");
            if (data.isEmpty()) {
                return new float[1024];
            }
            com.alibaba.fastjson2.JSONObject item = data.getJSONObject(0);
            com.alibaba.fastjson2.JSONArray embeddingArray = item.getJSONArray("embedding");
            float[] embedding = new float[embeddingArray.size()];
            for (int i = 0; i < embeddingArray.size(); i++) {
                embedding[i] = embeddingArray.getFloat(i);
            }
            return embedding;
        } catch (Exception e) {
            System.err.println("解析嵌入响应失败: " + e.getMessage());
            return new float[1024];
        }
    }

    /**
     * 解析批量嵌入响应
     * @param responseBody 响应体
     * @return 嵌入向量列表
     */
    private List<float[]> parseBatchEmbeddingResponse(String responseBody) {
        try {
            com.alibaba.fastjson2.JSONObject json = com.alibaba.fastjson2.JSON.parseObject(responseBody);
            com.alibaba.fastjson2.JSONArray data = json.getJSONArray("data");
            List<float[]> embeddings = new ArrayList<>();
            for (int i = 0; i < data.size(); i++) {
                com.alibaba.fastjson2.JSONObject item = data.getJSONObject(i);
                com.alibaba.fastjson2.JSONArray embeddingArray = item.getJSONArray("embedding");
                float[] embedding = new float[embeddingArray.size()];
                for (int j = 0; j < embeddingArray.size(); j++) {
                    embedding[j] = embeddingArray.getFloat(j);
                }
                embeddings.add(embedding);
            }
            return embeddings;
        } catch (Exception e) {
            System.err.println("解析批量嵌入响应失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }

}
