package com.jc.rag.admin.module.sys.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jc.rag.admin.module.sys.model.param.ModelProviderSaveParam;
import com.jc.rag.admin.module.sys.model.param.ModelProviderTestParam;
import com.jc.rag.admin.module.sys.service.ModelProviderConfigService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ModelProviderConfigServiceImpl implements ModelProviderConfigService {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public ModelProviderConfigServiceImpl(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Map<String, Object>> list() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            SELECT id, provider_type AS providerType, provider_name AS providerName, endpoint,
                   model_name AS modelName, enabled, priority_no AS priorityNo,
                   api_key_encrypted AS apiKeyEncrypted,
                   updated_at AS updatedAt
            FROM llm_provider_config ORDER BY provider_type, priority_no, id
            """);
        rows.forEach(this::applyApiKeyMask);
        return rows;
    }

    @Override
    @Transactional
    public Map<String, Object> save(ModelProviderSaveParam param) {
        Long id = param.getId();
        int priority = param.getPriorityNo() == null ? 100 : param.getPriorityNo();
        if (id == null) {
            jdbcTemplate.update("""
                INSERT INTO llm_provider_config (provider_type, provider_name, endpoint, api_key_encrypted, model_name, enabled, priority_no)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, param.getProviderType(), param.getProviderName(), param.getEndpoint(), blankToNull(param.getApiKey()),
                param.getModelName(), param.getEnabled() ? 1 : 0, priority);
            id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        } else if (param.getApiKey() == null || param.getApiKey().isBlank()) {
            jdbcTemplate.update("""
                UPDATE llm_provider_config SET provider_type=?, provider_name=?, endpoint=?, model_name=?, enabled=?, priority_no=? WHERE id=?
                """, param.getProviderType(), param.getProviderName(), param.getEndpoint(), param.getModelName(),
                param.getEnabled() ? 1 : 0, priority, id);
        } else {
            jdbcTemplate.update("""
                UPDATE llm_provider_config SET provider_type=?, provider_name=?, endpoint=?, api_key_encrypted=?, model_name=?, enabled=?, priority_no=? WHERE id=?
                """, param.getProviderType(), param.getProviderName(), param.getEndpoint(), param.getApiKey(), param.getModelName(),
                param.getEnabled() ? 1 : 0, priority, id);
        }
        return applyApiKeyMask(jdbcTemplate.queryForMap("""
            SELECT id, provider_type AS providerType, provider_name AS providerName, endpoint, model_name AS modelName,
                   enabled, priority_no AS priorityNo,
                   api_key_encrypted AS apiKeyEncrypted
            FROM llm_provider_config WHERE id=?
            """, id));
    }

    @Override public void delete(Long id) { jdbcTemplate.update("DELETE FROM llm_provider_config WHERE id=?", id); }

    @Override
    public List<String> refreshModels(Long id) {
        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT provider_type, endpoint, api_key_encrypted FROM llm_provider_config WHERE id=?", id);
        String endpoint = normalize(String.valueOf(row.get("endpoint")));
        String providerType = String.valueOf(row.get("provider_type"));
        String url = "OLLAMA".equalsIgnoreCase(providerType) ? endpoint + "/api/tags" : endpoint + "/models";
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15)).GET();
            Object apiKey = row.get("api_key_encrypted");
            if (apiKey != null && !String.valueOf(apiKey).isBlank()) builder.header("Authorization", "Bearer " + apiKey);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) return List.of();
            Map<String, Object> payload = objectMapper.readValue(response.body(), new TypeReference<>() {});
            List<String> models = new ArrayList<>();
            Object source = "OLLAMA".equalsIgnoreCase(providerType) ? payload.get("models") : payload.get("data");
            if (source instanceof List<?> items) for (Object item : items) {
                if (item instanceof Map<?, ?> map) {
                    Object name = map.get("name") == null ? map.get("id") : map.get("name");
                    if (name != null) models.add(String.valueOf(name));
                }
            }
            return models;
        } catch (Exception ignored) { return List.of(); }
    }

    @Override
    public Map<String, Object> testConnection(ModelProviderTestParam param) {
        String provider = param.getProviderName().toLowerCase();
        String endpoint = normalize(param.getEndpoint());
        String apiKey = param.getApiKey();
        if ((apiKey == null || apiKey.isBlank()) && param.getId() != null) {
            List<String> stored = jdbcTemplate.query("SELECT api_key_encrypted FROM llm_provider_config WHERE id=?", (rs, rowNum) -> rs.getString(1), param.getId());
            if (!stored.isEmpty()) apiKey = stored.get(0);
        }
        String url;
        if ("OLLAMA".equalsIgnoreCase(param.getProviderType())) url = endpoint + "/api/tags";
        else if ("anthropic".equals(provider)) url = endpoint + "/models";
        else if ("gemini".equals(provider)) url = endpoint + "/models?key=" + java.net.URLEncoder.encode(apiKey == null ? "" : apiKey, java.nio.charset.StandardCharsets.UTF_8);
        else url = endpoint + "/models";
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15)).GET();
            if ("anthropic".equals(provider)) {
                builder.header("x-api-key", apiKey == null ? "" : apiKey).header("anthropic-version", "2023-06-01");
            } else if (!"gemini".equals(provider) && apiKey != null && !apiKey.isBlank()) {
                builder.header("Authorization", "Bearer " + apiKey);
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
            return Map.of("success", success, "message", success ? "连接验证成功" : "服务返回 HTTP " + response.statusCode());
        } catch (Exception ex) {
            return Map.of("success", false, "message", "连接失败: " + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
        }
    }

    @Override
    @Transactional
    public void activate(Long id) {
        Map<String, Object> row = jdbcTemplate.queryForMap("""
            SELECT provider_type, endpoint, api_key_encrypted, model_name, enabled
            FROM llm_provider_config WHERE id=?
            """, id);
        if (!Boolean.parseBoolean(String.valueOf(row.get("enabled")))) {
            throw new IllegalArgumentException("模型服务尚未启用");
        }
        String type = String.valueOf(row.get("provider_type"));
        String endpoint = String.valueOf(row.get("endpoint"));
        String model = String.valueOf(row.get("model_name"));
        if ("OLLAMA".equalsIgnoreCase(type)) {
            upsertRuntime("model", "active.mode", "offline", 0, "Active model provider mode");
            upsertRuntime("model", "offline.ollama.baseUrl", endpoint, 0, "Active Ollama endpoint");
            upsertRuntime("model", "offline.ollama.defaultModel", model, 0, "Active Ollama model");
        } else {
            upsertRuntime("model", "active.mode", "online", 0, "Active model provider mode");
            upsertRuntime("model", "online.baseUrl", endpoint, 0, "Active online provider endpoint");
            upsertRuntime("model", "online.modelName", model, 0, "Active online provider model");
            Object key = row.get("api_key_encrypted");
            if (key != null && !String.valueOf(key).isBlank()) {
                upsertRuntime("model", "online.apiKey", String.valueOf(key), 1, "Active online provider api key");
            }
        }
    }

    private String normalize(String endpoint) { return endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint; }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value; }

    /**
     * 将查询结果中的明文密钥替换为脱敏字段后返回。
     *
     * <p>输出 apiKeyConfigured (是否已配置) 与 apiKeyMasked (脱敏展示值, 保留首尾各 4 位),
     * 明文密钥不会下发给前端。
     * @param row 含 apiKeyEncrypted 字段的查询结果行
     * @return 处理后的同一行
     * @author JC
     */
    private Map<String, Object> applyApiKeyMask(Map<String, Object> row) {
        Object raw = row.get("apiKeyEncrypted");
        String key = raw == null ? null : String.valueOf(raw);
        boolean configured = key != null && !key.isBlank();
        row.put("apiKeyConfigured", configured);
        row.put("apiKeyMasked", configured ? maskApiKey(key) : null);
        row.remove("apiKeyEncrypted");
        return row;
    }

    /**
     * API Key 脱敏: 保留开头与结尾各 4 位, 中间以 * 号代替 (仅供展示, 不得回填保存)。
     * @author JC
     */
    private String maskApiKey(String apiKey) {
        String key = apiKey.trim();
        if (key.length() <= 8) {
            return key.charAt(0) + "****" + key.charAt(key.length() - 1);
        }
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }
    private void upsertRuntime(String group, String key, String value, int encrypted, String remark) {
        jdbcTemplate.update("""
            INSERT INTO sys_config (config_group, config_key, config_value, value_type, encrypted, remark)
            VALUES (?, ?, ?, 'STRING', ?, ?)
            ON DUPLICATE KEY UPDATE config_value=VALUES(config_value), encrypted=VALUES(encrypted), remark=VALUES(remark)
            """, group, key, value, encrypted, remark);
    }
}
