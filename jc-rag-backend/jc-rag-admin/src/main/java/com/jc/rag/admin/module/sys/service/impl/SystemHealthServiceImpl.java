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


package com.jc.rag.admin.module.sys.service.impl;

import com.jc.rag.admin.config.props.StorageProperties;
import com.jc.rag.admin.module.sys.model.vo.ServiceHealthItemVO;
import com.jc.rag.admin.module.sys.service.RuntimeConfigService;
import com.jc.rag.admin.module.sys.service.SystemHealthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SystemHealthService 实现 (从 system 包迁移到 sys 包)。
 * @author JC
 */
@Service
public class SystemHealthServiceImpl implements SystemHealthService {

    private final HealthEndpoint healthEndpoint;
    private final RuntimeConfigService runtimeConfigService;
    private final StorageProperties storageProperties;
    private final RestClient restClient;

    @Value("${elasticsearch.url}")
    private String elasticsearchUrl;

    public SystemHealthServiceImpl(
        HealthEndpoint healthEndpoint,
        RuntimeConfigService runtimeConfigService,
        StorageProperties storageProperties
    ) {
        this.healthEndpoint = healthEndpoint;
        this.runtimeConfigService = runtimeConfigService;
        this.storageProperties = storageProperties;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public List<ServiceHealthItemVO> collectHealthItems() {
        Map<String, Object> serviceConfig = runtimeConfigService.buildResponse().getService();
        Map<String, Object> ollamaConfig = asMap(serviceConfig == null ? null : serviceConfig.get("ollama"));
        String pythonRagBaseUrl = String.valueOf(serviceConfig == null ? "" : serviceConfig.getOrDefault("pythonRagBaseUrl", ""));
        String ollamaBaseUrl = String.valueOf(ollamaConfig.getOrDefault("baseUrl", ""));
        String ollamaTagsPath = String.valueOf(ollamaConfig.getOrDefault("tagsPath", ""));
        List<ServiceHealthItemVO> items = new ArrayList<>();
        items.add(localSpringBoot());
        items.add(remoteHealth("Python RAG", pythonRagBaseUrl + "/health", "文档解析、检索、模型调用服务"));
        items.add(remoteHealth("Ollama", ollamaBaseUrl + ollamaTagsPath, "本地离线模型运行时"));
        items.add(remoteHealth("Elasticsearch", elasticsearchUrl, "向量与全文检索存储"));
        items.add(remoteHealth("MinIO", storageProperties.getMinio().getEndpoint(), "文档对象存储"));
        return items;
    }

    private ServiceHealthItemVO localSpringBoot() {
        HealthComponent healthComponent = healthEndpoint.health();
        ServiceHealthItemVO item = new ServiceHealthItemVO();
        item.setName("SpringBoot");
        item.setStatus(String.valueOf(healthComponent.getStatus()));
        item.setEndpoint("/api/health/basic");
        item.setDescription("统一权限、配置与业务接口入口");
        item.setDetail(healthComponent.toString());
        return item;
    }

    private ServiceHealthItemVO remoteHealth(String name, String endpoint, String description) {
        ServiceHealthItemVO item = new ServiceHealthItemVO();
        item.setName(name);
        item.setEndpoint(endpoint);
        item.setDescription(description);
        try {
            restClient.get().uri(endpoint).retrieve().toBodilessEntity();
            item.setStatus("UP");
            item.setDetail("reachable");
        } catch (Exception ex) {
            item.setStatus("DOWN");
            item.setDetail(ex.getClass().getSimpleName());
        }
        return item;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : Map.of();
    }
}
