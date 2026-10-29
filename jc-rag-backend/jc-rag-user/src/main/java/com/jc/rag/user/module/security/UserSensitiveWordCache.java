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


package com.jc.rag.user.module.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jc.rag.common.security.AhoCorasickAutomaton;
import com.jc.rag.common.security.AhoCorasickAutomaton.WordEntry;
import com.jc.rag.common.security.SensitiveFilterEngine;
import com.jc.rag.common.security.SensitiveFilterResult;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * C 端敏感词本地缓存 (user 模块独立组件)。
 * <p>user 模块不依赖 admin 模块, 通过 HTTP 拉取 admin 端的 /api/internal/security/words/export, 本地构建 AC 引擎。
 * <p>启动加载 + 每 5 分钟定时刷新, 失败时保留旧词库 (降级放行 + 记日志)。
 *
 * @author JC
 */
@Component
public class UserSensitiveWordCache {

    private static final Logger log = LoggerFactory.getLogger(UserSensitiveWordCache.class);

    @Value("${jc-rag.user.sensitive.admin-base-url:http://localhost:8080}")
    private String adminBaseUrl;
    @Value("${jc-rag.user.sensitive.internal-token:}")
    private String internalToken;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final AtomicReference<SensitiveFilterEngine> engineRef = new AtomicReference<>(emptyEngine());

    @PostConstruct
    public void init() {
        try {
            reload();
        } catch (Exception e) {
            log.warn("[UserSensitiveWord] init failed, will use empty engine: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 5 * 60 * 1000L, initialDelay = 5 * 60 * 1000L)
    public void scheduledReload() {
        try {
            reload();
        } catch (Exception e) {
            log.warn("[UserSensitiveWord] scheduled reload failed: {}", e.getMessage());
        }
    }

    private synchronized void reload() throws Exception {
        String url = adminBaseUrl + "/api/internal/security/words/export";
        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .GET();
        if (internalToken != null && !internalToken.isBlank()) {
            reqBuilder.header("X-Internal-Token", internalToken);
        }
        HttpResponse<String> resp = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IllegalStateException("admin returned status " + resp.statusCode());
        }
        List<Map<String, Object>> list = objectMapper.readValue(
            resp.body(), new TypeReference<List<Map<String, Object>>>() {});
        List<WordEntry> entries = list.stream()
            .map(m -> new WordEntry(String.valueOf(m.get("word")), m))
            .toList();
        AhoCorasickAutomaton ac = entries.isEmpty()
            ? AhoCorasickAutomaton.build(Collections.emptyList())
            : AhoCorasickAutomaton.build(entries);
        engineRef.set(new SensitiveFilterEngine(ac));
        log.info("[UserSensitiveWord] reloaded {} words from admin", entries.size());
    }

    private SensitiveFilterEngine emptyEngine() {
        return new SensitiveFilterEngine(AhoCorasickAutomaton.build(Collections.emptyList()));
    }

    /** 获取当前过滤引擎 (无锁, 返回不可变快照) */
    public SensitiveFilterEngine currentEngine() {
        return engineRef.get();
    }

    /** 便捷过滤接口 */
    public SensitiveFilterResult filter(String text) {
        return currentEngine().filter(text);
    }
}
