package com.ecru.outfit.service.rag;

import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 嵌入服务
 */
@Service
public class EmbeddingService {

    @Value("${ai.siliconflow.api-key}")
    private String apiKey;

    @Value("${ai.siliconflow.base-url}")
    private String baseUrl;

    @Value("${ai.siliconflow.embedding.model}")
    private String modelName;

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
            System.err.println("开始生成文本嵌入: " + text);
            // 调用硅基流动API生成真实嵌入
            System.err.println("使用API密钥: 硅基流动密钥");
            String endpoint = "/embeddings"; // 嵌入端点
            String fullUrl = baseUrl + endpoint;
            System.err.println("API URL: " + fullUrl);

            // 构建请求体
            com.alibaba.fastjson2.JSONObject requestBody = new com.alibaba.fastjson2.JSONObject();
            requestBody.put("model", modelName);
            requestBody.put("input", text);
            requestBody.put("encoding_format", "float");
            System.err.println("请求体: " + requestBody.toJSONString());

            // 创建请求
            RequestBody body = RequestBody.create(
                    requestBody.toJSONString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(fullUrl)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + this.apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            // 执行请求
            System.err.println("开始执行API请求");
            try (Response response = okHttpClient.newCall(request).execute()) {
                System.err.println("API响应状态码: " + response.code());
                if (!response.isSuccessful()) {
                    System.err.println("API调用失败: " + response.code() + " " + response.message());
                    throw new IOException("API调用失败: " + response.code() + " " + response.message());
                }
                String responseBody = response.body().string();
                System.err.println("API响应体长度: " + responseBody.length());
                System.err.println("API响应体前100字符: " + responseBody.substring(0, Math.min(100, responseBody.length())));
                float[] embedding = parseEmbeddingResponse(responseBody);
                System.err.println("解析嵌入成功，长度: " + embedding.length);
                System.err.println("嵌入前10个元素: " + java.util.Arrays.toString(java.util.Arrays.copyOfRange(embedding, 0, Math.min(10, embedding.length))));
                return embedding;
            }
        } catch (Exception e) {
            System.err.println("生成文本嵌入失败: " + e.getMessage());
            e.printStackTrace();
            // 失败时返回模拟向量作为兜底
            System.err.println("使用模拟向量作为兜底");
            float[] embedding = new float[1536];
            for (int i = 0; i < embedding.length; i++) {
                embedding[i] = (float) (text.length() % (i + 1)) / (i + 1);
            }
            System.err.println("模拟嵌入前10个元素: " + java.util.Arrays.toString(java.util.Arrays.copyOfRange(embedding, 0, Math.min(10, embedding.length))));
            return embedding;
        }
    }

    /**
     * 批量生成文本嵌入
     * @param texts 文本列表
     * @return 嵌入向量列表
     */
    public List<float[]> generateBatchEmbeddings(List<String> texts) {
        try {
            System.err.println("开始批量生成文本嵌入: " + texts.size() + " 条文本");
            // 调用硅基流动API批量生成真实嵌入
            System.err.println("使用API密钥: 硅基流动密钥");
            String endpoint = "/embeddings"; // 嵌入端点
            String fullUrl = baseUrl + endpoint;
            System.err.println("API URL: " + fullUrl);

            // 构建请求体
            com.alibaba.fastjson2.JSONObject requestBody = new com.alibaba.fastjson2.JSONObject();
            requestBody.put("model", modelName);
            requestBody.put("input", texts);
            requestBody.put("encoding_format", "float");

            // 创建请求
            RequestBody body = RequestBody.create(
                    requestBody.toJSONString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(fullUrl)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + this.apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            // 执行请求
            System.err.println("开始执行API请求");
            try (Response response = okHttpClient.newCall(request).execute()) {
                System.err.println("API响应状态码: " + response.code());
                if (!response.isSuccessful()) {
                    System.err.println("API调用失败: " + response.code() + " " + response.message());
                    throw new IOException("API调用失败: " + response.code() + " " + response.message());
                }
                String responseBody = response.body().string();
                System.err.println("API响应体长度: " + responseBody.length());
                System.err.println("API响应体前100字符: " + responseBody.substring(0, Math.min(100, responseBody.length())));
                List<float[]> embeddings = parseBatchEmbeddingResponse(responseBody);
                System.err.println("解析批量嵌入成功，数量: " + embeddings.size());
                if (!embeddings.isEmpty()) {
                    System.err.println("第一个嵌入长度: " + embeddings.get(0).length);
                    System.err.println("第一个嵌入前10个元素: " + java.util.Arrays.toString(java.util.Arrays.copyOfRange(embeddings.get(0), 0, Math.min(10, embeddings.get(0).length))));
                }
                return embeddings;
            }
        } catch (Exception e) {
            System.err.println("批量生成文本嵌入失败: " + e.getMessage());
            // 失败时返回模拟向量作为兜底
            List<float[]> embeddings = new ArrayList<>();
            for (String text : texts) {
                float[] embedding = new float[1536];
                for (int i = 0; i < embedding.length; i++) {
                    int hash = text.hashCode() + i;
                    embedding[i] = (float) (Math.abs(hash) % 100) / 100.0f;
                }
                embeddings.add(embedding);
            }
            return embeddings;
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
                return new float[1536];
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
            return new float[1536];
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