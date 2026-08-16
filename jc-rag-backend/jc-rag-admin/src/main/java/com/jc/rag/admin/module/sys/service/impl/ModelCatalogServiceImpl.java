package com.jc.rag.admin.module.sys.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jc.rag.admin.module.sys.model.vo.ModelCatalogVO;
import com.jc.rag.admin.module.sys.service.ModelCatalogService;
import com.jc.rag.common.config.RuntimeConfigDefaults;
import com.jc.rag.domain.mapper.sys.SysConfigMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 模型目录服务实现。
 *
 * <p>读取优先级: TTL 内远程缓存 &gt; 远程拉取 &gt; 过期缓存 &gt; 内置目录 (L1)。
 * 远程地址与 TTL 通过 sys_config 动态配置 (model.catalog.remoteUrl / model.catalog.ttlHours),
 * 远程缓存持久化在 sys_config (model.catalog.cache), 零魔法值、离线自动回退。
 * @author JC
 */
@Service
public class ModelCatalogServiceImpl implements ModelCatalogService {

    private static final Logger log = LoggerFactory.getLogger(ModelCatalogServiceImpl.class);
    private static final String BUILTIN_RESOURCE = "model-catalog.json";
    private static final String SOURCE_BUILTIN = "builtin";
    private static final String SOURCE_REMOTE = "remote";
    private static final String CONFIG_GROUP = "model";
    private static final String KEY_REMOTE_URL = "model.catalog.remoteUrl";
    private static final String KEY_TTL_HOURS = "model.catalog.ttlHours";
    private static final String KEY_CACHE = "model.catalog.cache";

    private final SysConfigMapper sysConfigMapper;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    /** 内置目录仅解析一次, volatile 保证多线程可见性 */
    private volatile ModelCatalogVO builtinCatalog;

    public ModelCatalogServiceImpl(SysConfigMapper sysConfigMapper, ObjectMapper objectMapper) {
        this.sysConfigMapper = sysConfigMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public ModelCatalogVO getCatalog() {
        CacheEntry cache = loadCache();
        if (cache != null && !isExpired(cache.fetchedAt, resolveTtlHours())) {
            return withSource(cache.catalog, SOURCE_REMOTE);
        }
        String remoteUrl = resolveConfig(KEY_REMOTE_URL, RuntimeConfigDefaults.MODEL_CATALOG_REMOTE_URL);
        if (remoteUrl != null && !remoteUrl.isBlank()) {
            try {
                ModelCatalogVO remote = fetchRemote(remoteUrl);
                saveCache(remote);
                return withSource(remote, SOURCE_REMOTE);
            } catch (Exception ex) {
                log.warn("模型目录远程拉取失败, 回退缓存/内置目录: {}", ex.getMessage());
            }
        }
        if (cache != null) {
            return withSource(cache.catalog, SOURCE_REMOTE);
        }
        return withSource(loadBuiltin(), SOURCE_BUILTIN);
    }

    @Override
    public ModelCatalogVO refreshCatalog() {
        String remoteUrl = resolveConfig(KEY_REMOTE_URL, RuntimeConfigDefaults.MODEL_CATALOG_REMOTE_URL);
        if (remoteUrl == null || remoteUrl.isBlank()) {
            throw new IllegalArgumentException("未配置模型目录远程地址 (model.catalog.remoteUrl), 当前仅使用内置目录");
        }
        ModelCatalogVO remote;
        try {
            remote = fetchRemote(remoteUrl);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("模型目录远程更新失败: "
                + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
        }
        saveCache(remote);
        return withSource(remote, SOURCE_REMOTE);
    }

    /* ============= 私有构造方法 ============= */

    private ModelCatalogVO fetchRemote(String remoteUrl) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(remoteUrl))
            .timeout(Duration.ofSeconds(15))
            .GET()
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 400) {
            throw new IllegalArgumentException("远程目录返回 HTTP " + response.statusCode());
        }
        ModelCatalogVO catalog = objectMapper.readValue(response.body(), ModelCatalogVO.class);
        if (catalog.getProviders() == null || catalog.getProviders().isEmpty()) {
            throw new IllegalArgumentException("远程目录内容为空或格式不合法");
        }
        return catalog;
    }

    private ModelCatalogVO loadBuiltin() {
        ModelCatalogVO cached = builtinCatalog;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (builtinCatalog == null) {
                try (InputStream in = new ClassPathResource(BUILTIN_RESOURCE).getInputStream()) {
                    builtinCatalog = objectMapper.readValue(in, ModelCatalogVO.class);
                    if (builtinCatalog.getUpdatedAt() == null) {
                        builtinCatalog.setUpdatedAt(builtinCatalog.getVersion());
                    }
                } catch (Exception ex) {
                    log.error("内置模型目录加载失败: {}", ex.getMessage(), ex);
                    ModelCatalogVO empty = new ModelCatalogVO();
                    empty.setProviders(java.util.List.of());
                    return empty;
                }
            }
            return builtinCatalog;
        }
    }

    private CacheEntry loadCache() {
        try {
            String raw = sysConfigMapper.selectStringValue(KEY_CACHE);
            if (raw == null || raw.isBlank()) {
                return null;
            }
            JsonNode root = objectMapper.readTree(raw);
            long fetchedAt = root.path("fetchedAt").asLong(0L);
            JsonNode catalogNode = root.get("catalog");
            if (fetchedAt <= 0 || catalogNode == null) {
                return null;
            }
            ModelCatalogVO catalog = objectMapper.treeToValue(catalogNode, ModelCatalogVO.class);
            if (catalog.getProviders() == null || catalog.getProviders().isEmpty()) {
                return null;
            }
            return new CacheEntry(fetchedAt, catalog);
        } catch (Exception ex) {
            log.warn("模型目录缓存读取失败: {}", ex.getMessage());
            return null;
        }
    }

    private void saveCache(ModelCatalogVO catalog) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("fetchedAt", System.currentTimeMillis());
            root.set("catalog", objectMapper.valueToTree(catalog));
            sysConfigMapper.upsertConfig(CONFIG_GROUP, KEY_CACHE,
                objectMapper.writeValueAsString(root), "JSON", 0, "模型目录远程缓存 (L2)");
        } catch (Exception ex) {
            log.warn("模型目录缓存写入失败: {}", ex.getMessage());
        }
    }

    private String resolveConfig(String key, String defaultValue) {
        try {
            String value = sysConfigMapper.selectStringValue(key);
            return value == null || value.isBlank() ? defaultValue : value;
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private int resolveTtlHours() {
        try {
            return Integer.parseInt(resolveConfig(KEY_TTL_HOURS,
                String.valueOf(RuntimeConfigDefaults.MODEL_CATALOG_TTL_HOURS)));
        } catch (NumberFormatException ex) {
            return RuntimeConfigDefaults.MODEL_CATALOG_TTL_HOURS;
        }
    }

    private boolean isExpired(long fetchedAt, int ttlHours) {
        return System.currentTimeMillis() - fetchedAt > ttlHours * 3600_000L;
    }

    private ModelCatalogVO withSource(ModelCatalogVO catalog, String source) {
        catalog.setSource(source);
        return catalog;
    }

    /** 远程目录缓存条目 (拉取时间 + 目录内容) */
    private record CacheEntry(long fetchedAt, ModelCatalogVO catalog) {
    }
}
