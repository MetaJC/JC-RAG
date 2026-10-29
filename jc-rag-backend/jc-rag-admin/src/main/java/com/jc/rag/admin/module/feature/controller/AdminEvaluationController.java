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


package com.jc.rag.admin.module.feature.controller;

import com.jc.rag.admin.common.annotation.OperationLog;
import com.jc.rag.common.web.R;
import com.jc.rag.domain.entity.chat.UserDialogueEvaluationEntity;
import com.jc.rag.domain.mapper.chat.UserDialogueEvaluationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 对话评估 - 管理端 Controller.
 * <p>提供: 配置查看/修改 / 评估历史查询 / Pro 功能状态查询.</p>
 *
 * @author JC
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/evaluation")
@RequiredArgsConstructor
public class AdminEvaluationController {

    private static final String CONFIG_ENABLED = "feature.dialogue_evaluation.enabled";
    private static final String CONFIG_QUOTA = "feature.dialogue_evaluation.daily_quota";

    private final JdbcTemplate jdbcTemplate;
    private final UserDialogueEvaluationMapper evaluationMapper;

    /**
     * 读取配置 + 当前激活的 Pro Edition 状态.
     */
    @GetMapping("/config")
    public R<Map<String, Object>> getConfig() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", readConfig(CONFIG_ENABLED, "true"));
        out.put("dailyQuota", readConfig(CONFIG_QUOTA, "0"));
        out.put("proEditionActive", isProEditionActive());
        out.put("tip", isProEditionActive()
            ? "当前为 Pro 付费版, 配置可自由修改"
            : "当前为社区版, 修改 enabled 不会生效 (评估功能强制 0 次/天, 仅 Pro 版可用)");
        return R.ok(out);
    }

    /**
     * 更新配置 (社区版允许改 enabled, 但 daily_quota 社区版被锁为 0).
     */
    @PostMapping("/config")
    @OperationLog(module = "FEATURE", action = "UPDATE_EVAL_CONFIG", description = "更新 Pro 功能配置")
    public R<Map<String, Object>> updateConfig(@RequestBody Map<String, Object> body) {
        boolean pro = isProEditionActive();
        if (body.containsKey("enabled")) {
            String v = String.valueOf(body.get("enabled"));
            upsertConfig(CONFIG_ENABLED, v);
        }
        if (body.containsKey("dailyQuota")) {
            String v = String.valueOf(body.get("dailyQuota"));
            if (!pro) {
                // 社区版: 强制锁 0
                upsertConfig(CONFIG_QUOTA, "0");
                return R.ok(Map.of("status", "LOCKED", "message", "社区版 daily_quota 强制为 0, 请使用 Pro 版"));
            }
            upsertConfig(CONFIG_QUOTA, v);
        }
        return R.ok(Map.of("status", "SUCCESS", "message", "已更新"));
    }

    /**
     * 全部评估记录 (管理端查看).
     */
    @GetMapping("/list")
    public R<Map<String, Object>> listAll(@org.springframework.web.bind.annotation.RequestParam(defaultValue = "1") int page,
                                          @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int size) {
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        int offset = (page <= 0 ? 1 : page - 1) * safeSize;
        List<UserDialogueEvaluationEntity> records = evaluationMapper.selectAllPaged(offset, safeSize);
        if (records == null) records = List.of();
        List<Map<String, Object>> items = records.stream().map(this::toMap).collect(Collectors.toList());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("page", page);
        out.put("size", safeSize);
        out.put("items", items);
        out.put("total", items.size()); // 简化, 真实分页时另加 count
        return R.ok(out);
    }

    private Map<String, Object> toMap(UserDialogueEvaluationEntity e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("userId", e.getUserId());
        m.put("sessionId", e.getSessionId());
        m.put("score", e.getScore());
        m.put("comment", e.getComment());
        m.put("modelName", e.getModelName());
        m.put("proEnhanced", e.getProEnhanced() != null && e.getProEnhanced() == 1);
        m.put("createdAt", e.getCreatedAt() == null ? null : e.getCreatedAt().toString());
        return m;
    }

    private String readConfig(String key, String def) {
        try {
            String v = jdbcTemplate.query(
                "SELECT config_value FROM sys_config WHERE config_key = ? LIMIT 1",
                rs -> rs.next() ? rs.getString(1) : null,
                key
            );
            return v == null ? def : v;
        } catch (Exception ex) {
            return def;
        }
    }

    private void upsertConfig(String key, String value) {
        jdbcTemplate.update(
            "INSERT INTO sys_config (config_group, config_key, config_value, value_type, encrypted, remark) VALUES (?,?,?,?,?,?) " +
                "ON DUPLICATE KEY UPDATE config_value=VALUES(config_value)",
            "feature", key, value, "STRING", 0, "Pro 功能配置"
        );
    }

    /**
     * 检测当前是否为 Pro 付费版.
     * <p>判断依据: {@code -Djc-rag.edition=pro} 系统参数 / 环境变量 {@code JC_RAG_EDITION=pro} /
     * Spring profile {@code pro}.</p>
     * <p>同样逻辑被 jc-rag-pro 模块的 {@code @ConditionalOnProperty} 使用, 这里保持一致.</p>
     */
    private boolean isProEditionActive() {
        try {
            String edition = System.getProperty("jc-rag.edition");
            if (edition == null) edition = System.getenv("JC_RAG_EDITION");
            if ("pro".equalsIgnoreCase(edition)) return true;
            org.springframework.web.context.WebApplicationContext ctx =
                org.springframework.web.context.ContextLoader.getCurrentWebApplicationContext();
            if (ctx != null && ctx.getEnvironment() != null) {
                for (String p : ctx.getEnvironment().getActiveProfiles()) {
                    if ("pro".equalsIgnoreCase(p)) return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}
