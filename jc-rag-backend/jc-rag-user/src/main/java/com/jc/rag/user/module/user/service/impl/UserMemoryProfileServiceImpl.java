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


package com.jc.rag.user.module.user.service.impl;

import com.jc.rag.domain.entity.chat.UserMemoryProfileEntity;
import com.jc.rag.domain.entity.sys.SysConfigEntity;
import com.jc.rag.domain.mapper.chat.UserMemoryProfileMapper;
import com.jc.rag.domain.mapper.sys.SysConfigMapper;
import com.jc.rag.user.module.user.service.UserMemoryProfileService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * L3 跨会话画像 Service 实现。
 * <p>调用方式:
 * <ul>
 *   <li>同步读: {@link #getProfile} / {@link #buildPromptText} (在 UserChatServiceImpl.buildPayload 拼装 systemPrompt 时调用)</li>
 *   <li>异步提取: {@link #extractProfileAsync} (在 UserChatServiceImpl 流式响应完成后触发, 不阻塞主对话)</li>
 *   <li>用户开关: {@link #isEnabled} (与 L2 enableLongMemory 独立, 走同一张 sys_config 表)</li>
 * </ul>
 *
 * @author JC
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserMemoryProfileServiceImpl implements UserMemoryProfileService {

    /** L3 画像注入 systemPrompt 时的提示前缀 (与 L1 / L2 配合, 位于最顶部) */
    public static final String PROFILE_SYSTEM_PREFIX = "\n\n[用户画像 (L3) — 跨会话聚合, AI 应主动参考]\n";

    /** 用户偏好表: 画像开关 key 后缀 */
    private static final String K_ENABLE_PROFILE = "enableMemoryProfile";

    /** 画像默认开启 */
    private static final boolean DEFAULT_ENABLE_PROFILE = true;

    /** 提取调用超时 (秒). 失败仅记日志, 不影响主对话. */
    private static final int PROFILE_EXTRACT_TIMEOUT_SEC = 60;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10)).version(HttpClient.Version.HTTP_1_1).build();
    private static final ExecutorService EXTRACT_EXECUTOR = Executors.newCachedThreadPool();

    private final UserMemoryProfileMapper profileMapper;
    private final SysConfigMapper configMapper;
    private final ObjectMapper objectMapper;

    @Value("${service.python.rag.base-url:http://localhost:8001}")
    private String ragBaseUrl;

    @Override
    public UserMemoryProfileEntity getProfile(Long userId) {
        if (userId == null) return null;
        try {
            return profileMapper.selectByUserId(userId);
        } catch (Exception e) {
            log.warn("getProfile failed: userId={}, err={}", userId, e.getMessage());
            return null;
        }
    }

    @Override
    public String buildPromptText(Long userId) {
        UserMemoryProfileEntity p = getProfile(userId);
        if (p == null || p.getProfileJson() == null || p.getProfileJson().isBlank()) {
            return null;
        }
        Map<String, Object> json;
        try {
            json = objectMapper.readValue(p.getProfileJson(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("buildPromptText: invalid profile_json for userId={}, err={}", userId, e.getMessage());
            return null;
        }
        if (json == null || json.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder(PROFILE_SYSTEM_PREFIX);
        appendLine(sb, "domain", json.get("domain"));
        appendLine(sb, "role", json.get("role"));
        appendSkills(sb, json.get("skills"));
        appendMap(sb, "preferences", json.get("preferences"));
        appendList(sb, "goals", json.get("goals"));
        String text = sb.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private static void appendLine(StringBuilder sb, String key, Object v) {
        if (v == null) return;
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return;
        sb.append("- ").append(key).append(": ").append(s).append('\n');
    }

    @SuppressWarnings("unchecked")
    private static void appendList(StringBuilder sb, String key, Object v) {
        if (!(v instanceof List) || ((List<Object>) v).isEmpty()) return;
        sb.append("- ").append(key).append(": ");
        boolean first = true;
        for (Object o : (List<Object>) v) {
            if (o == null) continue;
            String s = String.valueOf(o).trim();
            if (s.isEmpty()) continue;
            if (!first) sb.append(", ");
            sb.append(s);
            first = false;
        }
        if (!first) sb.append('\n');
    }

    @SuppressWarnings("unchecked")
    private static void appendMap(StringBuilder sb, String key, Object v) {
        if (!(v instanceof Map) || ((Map<String, Object>) v).isEmpty()) return;
        sb.append("- ").append(key).append(": ");
        boolean first = true;
        for (Map.Entry<String, Object> e : ((Map<String, Object>) v).entrySet()) {
            if (e.getValue() == null) continue;
            String s = String.valueOf(e.getValue()).trim();
            if (s.isEmpty()) continue;
            if (!first) sb.append("; ");
            sb.append(e.getKey()).append('=').append(s);
            first = false;
        }
        if (!first) sb.append('\n');
    }

    @SuppressWarnings("unchecked")
    private static void appendSkills(StringBuilder sb, Object v) {
        appendList(sb, "skills", v);
    }

    @Override
    public boolean isEnabled(Long userId) {
        if (userId == null) return DEFAULT_ENABLE_PROFILE;
        try {
            // 读取约定与 UserPreferenceServiceImpl 一致: config_group='user_pref', config_key='{userId}.enableMemoryProfile'
            String key = userId + "." + K_ENABLE_PROFILE;
            SysConfigEntity cfg = configMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SysConfigEntity>()
                    .eq("config_group", "user_pref")
                    .eq("config_key", key)
                    .last("LIMIT 1"));
            if (cfg == null || cfg.getConfigValue() == null) return DEFAULT_ENABLE_PROFILE;
            String v = cfg.getConfigValue().trim();
            return !"false".equalsIgnoreCase(v) && !"0".equals(v);
        } catch (Exception e) {
            log.debug("isEnabled fallback true: {}", e.getMessage());
            return DEFAULT_ENABLE_PROFILE;
        }
    }

    @Override
    public void extractProfileAsync(Long userId) {
        if (userId == null) return;
        if (!isEnabled(userId)) return;
        EXTRACT_EXECUTOR.execute(() -> {
            long start = System.currentTimeMillis();
            try {
                Map<String, Object> req = new LinkedHashMap<>();
                req.put("userId", String.valueOf(userId));
                req.put("maxSkills", 20);
                req.put("maxGoals", 10);
                String body = objectMapper.writeValueAsString(req);
                HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(ragBaseUrl + "/chat/profile/extract"))
                    .timeout(Duration.ofSeconds(PROFILE_EXTRACT_TIMEOUT_SEC))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
                HttpResponse<String> resp = HTTP_CLIENT.send(httpReq,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (resp.statusCode() >= 400) {
                    log.warn("extractProfileAsync: status={}, body={}", resp.statusCode(), resp.body());
                    return;
                }
                Map<String, Object> response = objectMapper.readValue(resp.body(),
                    new TypeReference<Map<String, Object>>() {});
                String status = String.valueOf(response.getOrDefault("status", ""));
                if (!"SUCCESS".equals(status)) {
                    log.info("L3 profile extract skipped: userId={}, status={}, msg={}", userId, status,
                        response.get("message"));
                    return;
                }
                Object profileObj = response.get("profile");
                if (!(profileObj instanceof Map)) {
                    log.info("L3 profile extract: no profile in response, userId={}", userId);
                    return;
                }
                String profileJson = objectMapper.writeValueAsString(profileObj);
                Integer factCount = toInt(response.get("factCount"));
                String modelName = String.valueOf(response.getOrDefault("modelName", ""));
                saveProfile(userId, profileJson, factCount, modelName);
                log.info("L3 profile extracted: userId={}, factCount={}, costMs={}",
                    userId, factCount, System.currentTimeMillis() - start);
            } catch (Exception e) {
                log.warn("extractProfileAsync failed: userId={}, err={}", userId, e.getMessage());
            }
        });
    }

    /**
     * 保存画像 (UPSERT): 存在则 version+1, 不存在则插入 version=1.
     */
    @Transactional
    public void saveProfile(Long userId, String profileJson, Integer factCount, String modelName) {
        if (userId == null || profileJson == null || profileJson.isBlank()) return;
        UserMemoryProfileEntity exist = profileMapper.selectByUserId(userId);
        LocalDateTime now = LocalDateTime.now();
        if (exist == null) {
            UserMemoryProfileEntity ent = new UserMemoryProfileEntity();
            ent.setUserId(userId);
            ent.setProfileJson(profileJson);
            ent.setVersion(1);
            ent.setFactCount(factCount == null ? 0 : factCount);
            ent.setModelName(modelName);
            ent.setLastExtractedAt(now);
            profileMapper.insert(ent);
        } else {
            exist.setProfileJson(profileJson);
            exist.setVersion(exist.getVersion() == null ? 1 : exist.getVersion() + 1);
            exist.setFactCount(factCount == null ? 0 : factCount);
            exist.setModelName(modelName);
            exist.setLastExtractedAt(now);
            profileMapper.updateById(exist);
        }
    }

    private static Integer toInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try { return Integer.valueOf(String.valueOf(o).trim()); } catch (Exception ignored) { return null; }
    }

    @Override
    @Transactional
    public int resetProfile(Long userId) {
        if (userId == null) return 0;
        UserMemoryProfileEntity exist = profileMapper.selectByUserId(userId);
        if (exist == null) return 0;
        return profileMapper.deleteById(exist.getId());
    }
}
