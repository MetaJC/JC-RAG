package com.jc.rag.admin.module.sys.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jc.rag.common.config.RuntimeConfigDefaults;
import com.jc.rag.domain.mapper.sys.SysConfigMapper;
import com.jc.rag.admin.config.AppProperties;
import com.jc.rag.admin.config.ExternalServiceProperties;
import com.jc.rag.admin.module.sys.model.vo.DirBrowseVO;
import com.jc.rag.admin.module.sys.model.vo.EmbeddingVerifyVO;
import com.jc.rag.admin.module.sys.model.vo.OllamaModelVO;
import com.jc.rag.admin.module.sys.model.vo.RuntimeConfigVO;
import com.jc.rag.admin.module.sys.model.vo.SystemResourceVO;
import com.jc.rag.admin.module.sys.model.param.RuntimeConfigUpdateParam;
import com.jc.rag.admin.module.sys.service.RuntimeConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * RuntimeConfigService 实现 (从 system 包迁移到 sys 包，包路径与命名统一)。
 *
 * <p>合并 application.yml + sys_config 动态表，提供运行时配置读写、Ollama 模型列表、
 * Python RAG 设备/资源检测、Embedding 模型验证、目录浏览等能力。
 * @author JC
 */
@Service
public class RuntimeConfigServiceImpl implements RuntimeConfigService {

    private static final Logger log = LoggerFactory.getLogger(RuntimeConfigService.class);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .version(HttpClient.Version.HTTP_1_1)
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();

    private final AppProperties appProperties;
    private final ExternalServiceProperties externalServiceProperties;
    private final SysConfigMapper sysConfigMapper;
    private final ObjectMapper objectMapper;

    public RuntimeConfigServiceImpl(
        AppProperties appProperties,
        ExternalServiceProperties externalServiceProperties,
        SysConfigMapper sysConfigMapper,
        ObjectMapper objectMapper
    ) {
        this.appProperties = appProperties;
        this.externalServiceProperties = externalServiceProperties;
        this.sysConfigMapper = sysConfigMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public RuntimeConfigVO buildResponse() {
        Map<String, String> dynamicConfig = loadDynamicConfig();
        RuntimeConfigVO response = new RuntimeConfigVO();
        response.setApp(buildAppConfig());
        response.setModel(buildModelConfig(dynamicConfig));
        response.setRag(buildRagConfig(dynamicConfig));
        response.setService(buildServiceConfig(dynamicConfig));
        return response;
    }

    @Override
    public OllamaModelVO listOllamaModels() {
        Map<String, String> dynamicConfig = loadDynamicConfig();
        String baseUrl = resolveString(dynamicConfig, "model.offline.ollama.baseUrl", RuntimeConfigDefaults.OLLAMA_BASE_URL);
        String tagsPath = resolveString(dynamicConfig, "model.offline.ollama.tagsPath", RuntimeConfigDefaults.OLLAMA_TAGS_PATH);
        String url = normalizeBaseUrl(baseUrl) + (tagsPath == null ? "" : tagsPath.trim());

        OllamaModelVO vo = new OllamaModelVO();
        vo.setBaseUrl(url);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                .GET()
                .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int statusCode = response.statusCode();
            String body = response.body();
            if (statusCode >= 400) {
                vo.setModels(List.of());
                return vo;
            }
            if (body == null || body.isBlank()) {
                vo.setModels(List.of());
                return vo;
            }
            Map<String, Object> payload = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> models = (List<Map<String, Object>>) payload.getOrDefault("models", List.of());
            vo.setModels(models);
            return vo;
        } catch (Exception ex) {
            vo.setModels(List.of());
            return vo;
        }
    }

    @Override
    @Transactional
    public RuntimeConfigVO saveRuntimeConfig(RuntimeConfigUpdateParam request) {
        upsertConfig("model", "active.mode", request.getActiveMode(), "STRING", 0, "online/offline");
        upsertConfig("model", "offline.ollama.baseUrl", request.getOllamaBaseUrl(), "STRING", 0, "Ollama base url");
        upsertConfig("model", "offline.ollama.chatPath", request.getOllamaChatPath(), "STRING", 0, "Ollama chat path");
        upsertConfig("model", "offline.ollama.tagsPath", request.getOllamaTagsPath(), "STRING", 0, "Ollama model list path");
        upsertConfig("model", "offline.ollama.defaultModel", request.getOfflineModel(), "STRING", 0, "Preferred local model");
        upsertConfig("model", "online.baseUrl", request.getOnlineBaseUrl(), "STRING", 0, "Online provider base url");
        upsertConfig("model", "online.modelName", request.getOnlineModelName(), "STRING", 0, "Online model name");
        upsertConfig("rag", "embedding.model.path", request.getEmbeddingModelPath(), "STRING", 0, "Embedding model path");
        upsertConfig("rag", "embedding.device", request.getEmbeddingDevice(), "STRING", 0, "Embedding device (auto/cpu/cuda)");
        upsertConfig("rag", "embedding.dims", String.valueOf(request.getEmbeddingDims() == null ? 0 : request.getEmbeddingDims()), "NUMBER", 0, "Embedding vector dims (0=auto)");
        upsertConfig("rag", "embedding.batchSize", String.valueOf(request.getEmbeddingBatchSize() == null ? 32 : request.getEmbeddingBatchSize()), "NUMBER", 0, "Embedding batch size");
        upsertConfig("rag", "embedding.normalizeEmbeddings", String.valueOf(request.getEmbeddingNormalizeEmbeddings() == null ? true : request.getEmbeddingNormalizeEmbeddings()), "BOOLEAN", 0, "Embedding normalize embeddings");
        upsertConfig("model", "default.temperature", String.valueOf(request.getTemperature()), "NUMBER", 0, "Default temperature");
        upsertConfig("model", "default.topP", String.valueOf(request.getTopP()), "NUMBER", 0, "Default top_p");
        upsertConfig("model", "default.maxTokens", String.valueOf(request.getMaxTokens()), "NUMBER", 0, "Default max tokens");
        upsertConfig("rag", "retrieval.topK", String.valueOf(request.getTopK()), "NUMBER", 0, "Top K results");
        upsertConfig("rag", "retrieval.topN", String.valueOf(request.getTopN()), "NUMBER", 0, "Candidate recall size");
        upsertConfig("rag", "retrieval.similarityThreshold", String.valueOf(request.getSimilarityThreshold()), "NUMBER", 0, "Similarity threshold");
        upsertConfig("rag", "chunk.size", String.valueOf(request.getChunkSize()), "NUMBER", 0, "Chunk size");
        upsertConfig("rag", "chunk.overlap", String.valueOf(request.getChunkOverlap()), "NUMBER", 0, "Chunk overlap");
        if (request.getOnlineApiKey() != null && !request.getOnlineApiKey().isBlank()) {
            upsertConfig("model", "online.apiKey", request.getOnlineApiKey(), "STRING", 1, "Online model api key");
        }
        return buildResponse();
    }

    @Override
    public Map<String, Object> buildProviderOverrideForPython() {
        Map<String, String> dynamicConfig = loadDynamicConfig();
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("temperature", resolveDouble(dynamicConfig, "model.default.temperature", RuntimeConfigDefaults.MODEL_TEMPERATURE));
        defaults.put("topP", resolveDouble(dynamicConfig, "model.default.topP", RuntimeConfigDefaults.MODEL_TOP_P));
        defaults.put("maxTokens", resolveInt(dynamicConfig, "model.default.maxTokens", RuntimeConfigDefaults.MODEL_MAX_TOKENS));

        Map<String, Object> offlineOllama = new LinkedHashMap<>();
        offlineOllama.put("baseUrl", resolveString(dynamicConfig, "model.offline.ollama.baseUrl", RuntimeConfigDefaults.OLLAMA_BASE_URL));
        offlineOllama.put("chatPath", resolveString(dynamicConfig, "model.offline.ollama.chatPath", RuntimeConfigDefaults.OLLAMA_CHAT_PATH));
        offlineOllama.put("tagsPath", resolveString(dynamicConfig, "model.offline.ollama.tagsPath", RuntimeConfigDefaults.OLLAMA_TAGS_PATH));
        offlineOllama.put("modelName", resolveString(dynamicConfig, "model.offline.ollama.defaultModel", RuntimeConfigDefaults.OLLAMA_DEFAULT_MODEL));

        Map<String, Object> offline = new LinkedHashMap<>();
        offline.put("provider", "ollama");
        offline.put("ollama", offlineOllama);

        Map<String, Object> online = new LinkedHashMap<>();
        online.put("provider", "openai-compatible");
        online.put("baseUrl", resolveString(dynamicConfig, "model.online.baseUrl", RuntimeConfigDefaults.ONLINE_BASE_URL));
        online.put("chatPath", resolveString(dynamicConfig, "model.online.chatPath", RuntimeConfigDefaults.ONLINE_CHAT_PATH));
        online.put("modelName", resolveString(dynamicConfig, "model.online.modelName", RuntimeConfigDefaults.ONLINE_MODEL_NAME));
        String apiKey = resolveString(dynamicConfig, "model.online.apiKey", "");
        online.put("apiKey", apiKey == null ? "" : apiKey);

        Map<String, Object> override = new LinkedHashMap<>();
        override.put("activeMode", resolveString(dynamicConfig, "model.active.mode", RuntimeConfigDefaults.MODEL_ACTIVE_MODE));
        override.put("defaults", defaults);
        override.put("offline", offline);
        override.put("online", online);
        return override;
    }

    @Override
    public Map<String, Object> buildRagOverrideForPython() {
        Map<String, String> dynamicConfig = loadDynamicConfig();
        Map<String, Object> embedding = new LinkedHashMap<>();
        embedding.put("modelPath", resolveString(dynamicConfig, "rag.embedding.model.path", RuntimeConfigDefaults.EMBEDDING_MODEL_PATH));
        embedding.put("device", resolveString(dynamicConfig, "rag.embedding.device", RuntimeConfigDefaults.EMBEDDING_DEVICE));
        embedding.put("dims", resolveInt(dynamicConfig, "rag.embedding.dims", RuntimeConfigDefaults.EMBEDDING_DIMS));
        embedding.put("batchSize", resolveInt(dynamicConfig, "rag.embedding.batchSize", RuntimeConfigDefaults.EMBEDDING_BATCH_SIZE));
        embedding.put("normalizeEmbeddings", resolveBoolean(dynamicConfig, "rag.embedding.normalizeEmbeddings", RuntimeConfigDefaults.EMBEDDING_NORMALIZE));

        Map<String, Object> retrieval = new LinkedHashMap<>();
        retrieval.put("topK", resolveInt(dynamicConfig, "rag.retrieval.topK", RuntimeConfigDefaults.RETRIEVAL_TOP_K));
        retrieval.put("topN", resolveInt(dynamicConfig, "rag.retrieval.topN", RuntimeConfigDefaults.RETRIEVAL_TOP_N));
        retrieval.put("similarityThreshold", resolveDouble(dynamicConfig, "rag.retrieval.similarityThreshold", RuntimeConfigDefaults.RETRIEVAL_SIMILARITY_THRESHOLD));

        Map<String, Object> chunk = new LinkedHashMap<>();
        chunk.put("size", resolveInt(dynamicConfig, "rag.chunk.size", RuntimeConfigDefaults.CHUNK_SIZE));
        chunk.put("overlap", resolveInt(dynamicConfig, "rag.chunk.overlap", RuntimeConfigDefaults.CHUNK_OVERLAP));

        Map<String, Object> override = new LinkedHashMap<>();
        override.put("embedding", embedding);
        override.put("retrieval", retrieval);
        override.put("chunk", chunk);
        return override;
    }

    @Override
    public int resolveRagTopK() {
        return resolveInt(loadDynamicConfig(), "rag.retrieval.topK", RuntimeConfigDefaults.RETRIEVAL_TOP_K);
    }

    @Override
    public int resolveRagTopN() {
        return resolveInt(loadDynamicConfig(), "rag.retrieval.topN", RuntimeConfigDefaults.RETRIEVAL_TOP_N);
    }

    @Override
    public double resolveRagSimilarityThreshold() {
        return resolveDouble(loadDynamicConfig(), "rag.retrieval.similarityThreshold", RuntimeConfigDefaults.RETRIEVAL_SIMILARITY_THRESHOLD);
    }

    @Override
    public List<Map<String, Object>> detectDevices() {
        List<Map<String, Object>> devices = new java.util.ArrayList<>();
        String pythonRagBaseUrl = resolveString(loadDynamicConfig(), "service.python.rag.baseUrl",
            externalServiceProperties.getPython().getRag().getBaseUrl());
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(normalizeBaseUrl(pythonRagBaseUrl) + "/system/devices"))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
            HttpResponse<String> resp = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() == 200 && resp.body() != null) {
                List<Map<String, Object>> pyDevices = objectMapper.readValue(
                    resp.body(), new TypeReference<List<Map<String, Object>>>() {});
                if (pyDevices != null) devices.addAll(pyDevices);
            }
        } catch (Exception ex) {
            log.warn("detectDevices via Python failed: {}", ex.getMessage());
        }
        if (devices.isEmpty()) {
            Map<String, Object> auto = new LinkedHashMap<>();
            auto.put("id", "auto");
            auto.put("name", "Auto（自动选择最优设备）");
            auto.put("type", "other");
            auto.put("performanceRank", 999);
            auto.put("available", true);
            auto.put("detail", "Python RAG 服务未连接，仅 Auto 可用");
            devices.add(auto);
        }
        return devices;
    }

    @Override
    public SystemResourceVO getSystemResources() {
        String pythonRagBaseUrl = resolveString(loadDynamicConfig(), "service.python.rag.baseUrl",
            externalServiceProperties.getPython().getRag().getBaseUrl());
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(normalizeBaseUrl(pythonRagBaseUrl) + "/system/resources"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
            HttpResponse<String> resp = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() == 200 && resp.body() != null) {
                Map<String, Object> raw = objectMapper.readValue(resp.body(), new TypeReference<Map<String, Object>>() {});
                SystemResourceVO vo = new SystemResourceVO();
                @SuppressWarnings("unchecked")
                Map<String, Object> cpu = (Map<String, Object>) raw.get("cpu");
                vo.setCpu(cpu);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> gpus = (List<Map<String, Object>>) raw.get("gpu");
                vo.setGpus(gpus);
                @SuppressWarnings("unchecked")
                Map<String, Object> memory = (Map<String, Object>) raw.get("ram");
                vo.setMemory(memory);
                @SuppressWarnings("unchecked")
                Map<String, Object> disk = (Map<String, Object>) raw.get("disk");
                vo.setDisk(disk);
                return vo;
            }
        } catch (Exception ex) {
            log.warn("getSystemResources via Python failed: {}", ex.getMessage());
        }
        SystemResourceVO vo = new SystemResourceVO();
        vo.setCpu(Map.of("percent", 0));
        vo.setMemory(Map.of("percent", 0, "usedGb", 0, "totalGb", 0));
        return vo;
    }

    @Override
    public EmbeddingVerifyVO verifyEmbeddingModel(String modelPath) {
        String pythonRagBaseUrl = resolveString(loadDynamicConfig(), "service.python.rag.baseUrl",
            externalServiceProperties.getPython().getRag().getBaseUrl());
        EmbeddingVerifyVO vo = new EmbeddingVerifyVO();
        vo.setModelPath(modelPath);
        try {
            Map<String, Object> payload = Map.of("modelPath", modelPath != null ? modelPath : "");
            String body = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(normalizeBaseUrl(pythonRagBaseUrl) + "/system/verify-model"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> resp = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() == 200 && resp.body() != null) {
                Map<String, Object> raw = objectMapper.readValue(resp.body(), new TypeReference<Map<String, Object>>() {});
                vo.setValid(Boolean.TRUE.equals(raw.get("valid")) || "SUCCESS".equals(raw.get("status")));
                vo.setMessage((String) raw.get("message"));
                Object sizeObj = raw.get("sizeBytes");
                if (sizeObj instanceof Number n) {
                    vo.setSizeBytes(n.longValue());
                } else if (sizeObj != null) {
                    try { vo.setSizeBytes(Long.parseLong(sizeObj.toString())); } catch (Exception ignore) {}
                }
                return vo;
            }
        } catch (Exception ex) {
            log.warn("verifyEmbeddingModel failed: {}", ex.getMessage());
        }
        vo.setValid(false);
        vo.setMessage("Python RAG 服务不可用或模型加载失败");
        return vo;
    }

    @Override
    public DirBrowseVO browseDirectory(String path) {
        DirBrowseVO result = new DirBrowseVO();
        String dirPath = (path == null || path.isBlank()) ? "D:\\" : path.trim();
        java.io.File dir = new java.io.File(dirPath);
        result.setCurrentPath(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            result.setParentPath(dir.getParent());
            result.setEntries(List.of());
            return result;
        }
        java.io.File[] children = dir.listFiles();
        java.util.List<DirBrowseVO.DirEntry> entries = new java.util.ArrayList<>();
        if (children != null) {
            java.util.Arrays.sort(children, java.util.Comparator.comparing(java.io.File::getName, String.CASE_INSENSITIVE_ORDER));
            for (java.io.File child : children) {
                DirBrowseVO.DirEntry entry = new DirBrowseVO.DirEntry();
                entry.setName(child.getName());
                entry.setPath(child.getAbsolutePath());
                entry.setDirectory(child.isDirectory());
                if (child.isFile()) {
                    entry.setSizeBytes(child.length());
                }
                entries.add(entry);
            }
        }
        result.setCurrentPath(dir.getAbsolutePath());
        result.setParentPath(dir.getParent());
        result.setEntries(entries);
        return result;
    }

    @Override
    public List<Map<String, Object>> listLocalModels() {
        String baseUrl = resolveString(loadDynamicConfig(), "service.python.rag.baseUrl",
            externalServiceProperties.getPython().getRag().getBaseUrl());
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(normalizeBaseUrl(baseUrl) + "/system/local-models"))
                .timeout(Duration.ofSeconds(5)).GET().build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), new TypeReference<List<Map<String, Object>>>() {});
            }
        } catch (Exception ex) {
            log.warn("listLocalModels failed: {}", ex.getMessage());
        }
        return List.of();
    }

    @Override
    public Map<String, Object> downloadLocalModel(String modelId) {
        String baseUrl = resolveString(loadDynamicConfig(), "service.python.rag.baseUrl",
            externalServiceProperties.getPython().getRag().getBaseUrl());
        try {
            String body = objectMapper.writeValueAsString(Map.of("modelId", modelId));
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(normalizeBaseUrl(baseUrl) + "/system/local-models/download"))
                .timeout(Duration.ofSeconds(5)).header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception ex) {
            log.warn("downloadLocalModel failed: {}", ex.getMessage());
        }
        return Map.of("success", false, "message", "Python RAG 服务不可用");
    }

    /* ============= 私有构造方法 ============= */
    private Map<String, Object> buildAppConfig() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", appProperties.getName());
        map.put("version", appProperties.getVersion());
        map.put("deploymentMode", appProperties.getDeploymentMode());
        return map;
    }

    private Map<String, Object> buildModelConfig(Map<String, String> dynamicConfig) {
        Map<String, Object> map = new LinkedHashMap<>();
        Map<String, Object> defaultConfig = new LinkedHashMap<>();
        defaultConfig.put("temperature", resolveDouble(dynamicConfig, "model.default.temperature", RuntimeConfigDefaults.MODEL_TEMPERATURE));
        defaultConfig.put("topP", resolveDouble(dynamicConfig, "model.default.topP", RuntimeConfigDefaults.MODEL_TOP_P));
        defaultConfig.put("maxTokens", resolveInt(dynamicConfig, "model.default.maxTokens", RuntimeConfigDefaults.MODEL_MAX_TOKENS));

        Map<String, Object> offlineOllama = new LinkedHashMap<>();
        offlineOllama.put("baseUrl", resolveString(dynamicConfig, "model.offline.ollama.baseUrl", RuntimeConfigDefaults.OLLAMA_BASE_URL));
        offlineOllama.put("chatPath", resolveString(dynamicConfig, "model.offline.ollama.chatPath", RuntimeConfigDefaults.OLLAMA_CHAT_PATH));
        offlineOllama.put("tagsPath", resolveString(dynamicConfig, "model.offline.ollama.tagsPath", RuntimeConfigDefaults.OLLAMA_TAGS_PATH));
        offlineOllama.put("defaultModel", resolveString(dynamicConfig, "model.offline.ollama.defaultModel", RuntimeConfigDefaults.OLLAMA_DEFAULT_MODEL));

        Map<String, Object> offlineConfig = new LinkedHashMap<>();
        offlineConfig.put("provider", "ollama");
        offlineConfig.put("ollama", offlineOllama);

        Map<String, Object> onlineConfig = new LinkedHashMap<>();
        onlineConfig.put("baseUrl", resolveString(dynamicConfig, "model.online.baseUrl", RuntimeConfigDefaults.ONLINE_BASE_URL));
        onlineConfig.put("chatPath", resolveString(dynamicConfig, "model.online.chatPath", RuntimeConfigDefaults.ONLINE_CHAT_PATH));
        onlineConfig.put("modelName", resolveString(dynamicConfig, "model.online.modelName", RuntimeConfigDefaults.ONLINE_MODEL_NAME));
        String onlineApiKey = resolveString(dynamicConfig, "model.online.apiKey", "");
        onlineConfig.put("apiKeyConfigured", onlineApiKey != null && !onlineApiKey.isBlank());

        map.put("activeMode", resolveString(dynamicConfig, "model.active.mode", RuntimeConfigDefaults.MODEL_ACTIVE_MODE));
        map.put("defaults", defaultConfig);
        map.put("offline", offlineConfig);
        map.put("online", onlineConfig);
        return map;
    }

    private Map<String, Object> buildRagConfig(Map<String, String> dynamicConfig) {
        Map<String, Object> map = new LinkedHashMap<>();
        Map<String, Object> embeddingConfig = new LinkedHashMap<>();
        embeddingConfig.put("modelPath", resolveString(dynamicConfig, "rag.embedding.model.path", RuntimeConfigDefaults.EMBEDDING_MODEL_PATH));
        embeddingConfig.put("device", resolveString(dynamicConfig, "rag.embedding.device", RuntimeConfigDefaults.EMBEDDING_DEVICE));
        embeddingConfig.put("dims", resolveInt(dynamicConfig, "rag.embedding.dims", RuntimeConfigDefaults.EMBEDDING_DIMS));
        embeddingConfig.put("batchSize", resolveInt(dynamicConfig, "rag.embedding.batchSize", RuntimeConfigDefaults.EMBEDDING_BATCH_SIZE));
        embeddingConfig.put("normalizeEmbeddings", resolveBoolean(dynamicConfig, "rag.embedding.normalizeEmbeddings", RuntimeConfigDefaults.EMBEDDING_NORMALIZE));

        Map<String, Object> retrievalConfig = new LinkedHashMap<>();
        retrievalConfig.put("topK", resolveInt(dynamicConfig, "rag.retrieval.topK", RuntimeConfigDefaults.RETRIEVAL_TOP_K));
        retrievalConfig.put("topN", resolveInt(dynamicConfig, "rag.retrieval.topN", RuntimeConfigDefaults.RETRIEVAL_TOP_N));
        retrievalConfig.put("similarityThreshold", resolveDouble(dynamicConfig, "rag.retrieval.similarityThreshold", RuntimeConfigDefaults.RETRIEVAL_SIMILARITY_THRESHOLD));

        Map<String, Object> chunkConfig = new LinkedHashMap<>();
        chunkConfig.put("size", resolveInt(dynamicConfig, "rag.chunk.size", RuntimeConfigDefaults.CHUNK_SIZE));
        chunkConfig.put("overlap", resolveInt(dynamicConfig, "rag.chunk.overlap", RuntimeConfigDefaults.CHUNK_OVERLAP));

        Map<String, Object> historyConfig = new LinkedHashMap<>();
        historyConfig.put("rounds", resolveInt(dynamicConfig, "rag.history.rounds", RuntimeConfigDefaults.HISTORY_ROUNDS));

        map.put("embedding", embeddingConfig);
        map.put("retrieval", retrievalConfig);
        map.put("chunk", chunkConfig);
        map.put("history", historyConfig);
        return map;
    }

    private Map<String, Object> buildServiceConfig(Map<String, String> dynamicConfig) {
        Map<String, Object> map = new LinkedHashMap<>();
        Map<String, Object> ollamaConfig = new LinkedHashMap<>();
        ollamaConfig.put("baseUrl", resolveString(dynamicConfig, "model.offline.ollama.baseUrl", RuntimeConfigDefaults.OLLAMA_BASE_URL));
        ollamaConfig.put("chatPath", resolveString(dynamicConfig, "model.offline.ollama.chatPath", RuntimeConfigDefaults.OLLAMA_CHAT_PATH));
        ollamaConfig.put("tagsPath", resolveString(dynamicConfig, "model.offline.ollama.tagsPath", RuntimeConfigDefaults.OLLAMA_TAGS_PATH));
        map.put("pythonRagBaseUrl", resolveString(dynamicConfig, "service.python.rag.baseUrl", externalServiceProperties.getPython().getRag().getBaseUrl()));
        map.put("ollama", ollamaConfig);
        return map;
    }

    private Map<String, String> loadDynamicConfig() {
        try {
            List<Map<String, Object>> rows = sysConfigMapper.listAllKeyValue();
            Map<String, String> map = new LinkedHashMap<>();
            for (Map<String, Object> row : rows) {
                map.put(String.valueOf(row.get("configKey")), String.valueOf(row.get("configValue")));
            }
            return map;
        } catch (Exception ex) {
            log.warn("Load dynamic config failed, fallback to application.yml: {}", ex.getMessage());
            return Map.of();
        }
    }

    private void upsertConfig(String group, String key, String value, String valueType, int encrypted, String remark) {
        String fullKey = key;
        if (group != null && !group.isBlank()) {
            String prefix = group.trim() + ".";
            if (!key.startsWith(prefix)) fullKey = prefix + key;
        }
        sysConfigMapper.upsertConfig(group, fullKey,
                Objects.requireNonNullElse(value, ""), valueType, encrypted, remark);
    }

    private String resolveString(Map<String, String> dynamicConfig, String key, String defaultValue) {
        String direct = dynamicConfig.get(key);
        if (direct != null) return direct;
        String fallbackKey = fallbackKey(key);
        if (fallbackKey != null) {
            String fallback = dynamicConfig.get(fallbackKey);
            if (fallback != null) return fallback;
        }
        return defaultValue;
    }

    private int resolveInt(Map<String, String> dynamicConfig, String key, int defaultValue) {
        try {
            return Integer.parseInt(resolveString(dynamicConfig, key, String.valueOf(defaultValue)));
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private double resolveDouble(Map<String, String> dynamicConfig, String key, double defaultValue) {
        try {
            return Double.parseDouble(resolveString(dynamicConfig, key, String.valueOf(defaultValue)));
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private boolean resolveBoolean(Map<String, String> dynamicConfig, String key, boolean defaultValue) {
        String value = resolveString(dynamicConfig, key, String.valueOf(defaultValue));
        if (value == null) return defaultValue;
        String trimmed = value.trim().toLowerCase();
        if (trimmed.isEmpty()) return defaultValue;
        return "true".equals(trimmed) || "1".equals(trimmed) || "yes".equals(trimmed) || "y".equals(trimmed);
    }

    private String fallbackKey(String key) {
        if (key == null) return null;
        int idx = key.indexOf('.');
        if (idx <= 0 || idx >= key.length() - 1) return null;
        return key.substring(idx + 1);
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null) return "";
        String trimmed = baseUrl.trim();
        if (trimmed.endsWith("/")) return trimmed.substring(0, trimmed.length() - 1);
        return trimmed;
    }
}
