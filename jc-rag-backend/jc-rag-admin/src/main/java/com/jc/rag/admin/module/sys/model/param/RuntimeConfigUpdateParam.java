/*
 * Copyright 2024-2026 OpenJC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 本文件来源于 JC RAG Community Edition
 * 开源地址: https://gitee.com/Open-JC/jc-rag | https://github.com/MetaJC/JC-RAG
 * 二次开发请保留此出处声明。
 */


package com.jc.rag.admin.module.sys.model.param;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 运行时配置更新参数 (从 /api/sys/runtime-config 提交)。
 * @author JC
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RuntimeConfigUpdateParam {

    @NotBlank(message = "运行模式不能为空")
    private String activeMode;

    @NotBlank(message = "离线模型不能为空")
    private String offlineModel;

    @NotBlank(message = "Ollama BaseUrl 不能为空")
    private String ollamaBaseUrl;

    @NotBlank(message = "Ollama Chat 路径不能为空")
    private String ollamaChatPath;

    @NotBlank(message = "Ollama Tags 路径不能为空")
    private String ollamaTagsPath;

    private String onlineBaseUrl;
    private String onlineModelName;
    private String onlineApiKey;

    @NotBlank(message = "Embedding 模型路径不能为空")
    private String embeddingModelPath;

    @NotBlank(message = "Embedding 设备不能为空")
    private String embeddingDevice;

    @Min(0) @Max(4096)
    private Integer embeddingDims;

    @Min(1) @Max(256)
    private Integer embeddingBatchSize;

    private Boolean embeddingNormalizeEmbeddings;

    /** Embedding 来源: local=本地模型, service=在线模型服务; 空值按 local 处理 (兼容旧前端) */
    private String embeddingSource;

    /** embeddingSource=service 时选中的服务商 (llm_provider_config.provider_name) */
    private String embeddingServiceProvider;

    /** embeddingSource=service 时选中的 Embedding 模型名 */
    private String embeddingServiceModel;

    /** embeddingSource=service 时的向量维度 (0=自动) */
    private Integer embeddingServiceDims;

    @NotNull @Min(0) @Max(2)
    private Double temperature;

    @NotNull @Min(0) @Max(1)
    private Double topP;

    @NotNull @Min(128) @Max(8192)
    private Integer maxTokens;

    @NotNull @Min(1) @Max(20)
    private Integer topK;

    @NotNull @Min(1) @Max(100)
    private Integer topN;

    @NotNull @Min(0) @Max(1)
    private Double similarityThreshold;

    @NotNull @Min(100) @Max(2000)
    private Integer chunkSize;

    @NotNull @Min(0) @Max(500)
    private Integer chunkOverlap;

    public String getActiveMode() { return activeMode; }
    public void setActiveMode(String activeMode) { this.activeMode = activeMode; }
    public String getOfflineModel() { return offlineModel; }
    public void setOfflineModel(String offlineModel) { this.offlineModel = offlineModel; }
    public String getOllamaBaseUrl() { return ollamaBaseUrl; }
    public void setOllamaBaseUrl(String ollamaBaseUrl) { this.ollamaBaseUrl = ollamaBaseUrl; }
    public String getOllamaChatPath() { return ollamaChatPath; }
    public void setOllamaChatPath(String ollamaChatPath) { this.ollamaChatPath = ollamaChatPath; }
    public String getOllamaTagsPath() { return ollamaTagsPath; }
    public void setOllamaTagsPath(String ollamaTagsPath) { this.ollamaTagsPath = ollamaTagsPath; }
    public String getOnlineBaseUrl() { return onlineBaseUrl; }
    public void setOnlineBaseUrl(String onlineBaseUrl) { this.onlineBaseUrl = onlineBaseUrl; }
    public String getOnlineModelName() { return onlineModelName; }
    public void setOnlineModelName(String onlineModelName) { this.onlineModelName = onlineModelName; }
    public String getOnlineApiKey() { return onlineApiKey; }
    public void setOnlineApiKey(String onlineApiKey) { this.onlineApiKey = onlineApiKey; }
    public String getEmbeddingModelPath() { return embeddingModelPath; }
    public void setEmbeddingModelPath(String embeddingModelPath) { this.embeddingModelPath = embeddingModelPath; }
    public String getEmbeddingDevice() { return embeddingDevice; }
    public void setEmbeddingDevice(String embeddingDevice) { this.embeddingDevice = embeddingDevice; }
    public Integer getEmbeddingDims() { return embeddingDims; }
    public void setEmbeddingDims(Integer embeddingDims) { this.embeddingDims = embeddingDims; }
    public Integer getEmbeddingBatchSize() { return embeddingBatchSize; }
    public void setEmbeddingBatchSize(Integer embeddingBatchSize) { this.embeddingBatchSize = embeddingBatchSize; }
    public Boolean getEmbeddingNormalizeEmbeddings() { return embeddingNormalizeEmbeddings; }
    public void setEmbeddingNormalizeEmbeddings(Boolean embeddingNormalizeEmbeddings) { this.embeddingNormalizeEmbeddings = embeddingNormalizeEmbeddings; }
    public String getEmbeddingSource() { return embeddingSource; }
    public void setEmbeddingSource(String embeddingSource) { this.embeddingSource = embeddingSource; }
    public String getEmbeddingServiceProvider() { return embeddingServiceProvider; }
    public void setEmbeddingServiceProvider(String embeddingServiceProvider) { this.embeddingServiceProvider = embeddingServiceProvider; }
    public String getEmbeddingServiceModel() { return embeddingServiceModel; }
    public void setEmbeddingServiceModel(String embeddingServiceModel) { this.embeddingServiceModel = embeddingServiceModel; }
    public Integer getEmbeddingServiceDims() { return embeddingServiceDims; }
    public void setEmbeddingServiceDims(Integer embeddingServiceDims) { this.embeddingServiceDims = embeddingServiceDims; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Double getTopP() { return topP; }
    public void setTopP(Double topP) { this.topP = topP; }
    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
    public Integer getTopK() { return topK; }
    public void setTopK(Integer topK) { this.topK = topK; }
    public Integer getTopN() { return topN; }
    public void setTopN(Integer topN) { this.topN = topN; }
    public Double getSimilarityThreshold() { return similarityThreshold; }
    public void setSimilarityThreshold(Double similarityThreshold) { this.similarityThreshold = similarityThreshold; }
    public Integer getChunkSize() { return chunkSize; }
    public void setChunkSize(Integer chunkSize) { this.chunkSize = chunkSize; }
    public Integer getChunkOverlap() { return chunkOverlap; }
    public void setChunkOverlap(Integer chunkOverlap) { this.chunkOverlap = chunkOverlap; }
}
