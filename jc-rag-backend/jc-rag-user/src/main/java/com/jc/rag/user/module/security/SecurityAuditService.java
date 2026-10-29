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

import com.jc.rag.common.security.SensitiveFilterResult;
import com.jc.rag.common.security.SensitiveFilterResult.Hit;
import com.jc.rag.domain.entity.sys.SecurityAuditLogEntity;
import com.jc.rag.domain.mapper.sys.SecurityAuditLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * C 端安全审计日志 Service (user 模块本地落库)。
 * <p>异步落库, 不阻塞主流程; 敏感原文只存 SHA-256 指纹 (不存原文, 保护隐私)。
 *
 * @author JC
 */
@Service
public class SecurityAuditService {

    private static final Logger log = LoggerFactory.getLogger(SecurityAuditService.class);

    @Autowired
    private SecurityAuditLogMapper mapper;

    /**
     * 异步记录一次输入命中 (askStream 入口调用)。
     */
    @Async
    public void logInput(Long userId, Long sessionId, String originalText, SensitiveFilterResult r,
                          String clientIp, String userAgent) {
        if (r == null || r.isPass()) return;
        save(userId, sessionId, originalText, r, clientIp, userAgent, "INPUT");
    }

    /**
     * 异步记录一次输出命中 (Python 输出后处理回传, 或本地预过滤)。
     */
    @Async
    public void logOutput(Long userId, Long sessionId, String originalText, SensitiveFilterResult r,
                           String clientIp, String userAgent) {
        if (r == null || r.isPass()) return;
        save(userId, sessionId, originalText, r, clientIp, userAgent, "OUTPUT");
    }

    private void save(Long userId, Long sessionId, String originalText, SensitiveFilterResult r,
                      String clientIp, String userAgent, String direction) {
        try {
            Hit top = r.getHits().isEmpty() ? null : r.getHits().get(0);
            SecurityAuditLogEntity e = new SecurityAuditLogEntity();
            e.setUserId(userId);
            e.setSessionId(sessionId);
            e.setAuditType(mapAuditType(direction, r.getAction()));
            e.setRiskLevel(top == null ? null : top.getRiskLevel());
            e.setMatchedWordId(top == null ? null : top.getWordId());
            e.setMatchedWord(top == null ? null : top.getWord());
            e.setOriginalContentHash(sha256(originalText));
            e.setMaskedContent(truncate(r.getMasked(), 2000));
            e.setAction(r.getAction());
            e.setClientIp(clientIp);
            e.setUserAgent(truncate(userAgent, 250));
            e.setCreatedAt(LocalDateTime.now());
            mapper.insert(e);
        } catch (Exception ex) {
            log.error("[SecurityAudit] save failed", ex);
        }
    }

    private String mapAuditType(String direction, String action) {
        if ("DENY".equals(action)) return "INPUT".equals(direction) ? "INPUT_DENY" : "OUTPUT_DENY";
        if ("MASK".equals(action)) return "INPUT".equals(direction) ? "INPUT_MASK" : "OUTPUT_MASK";
        return "INPUT".equals(direction) ? "INPUT_ALERT" : "INPUT_ALERT";
    }

    private String sha256(String s) {
        if (s == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return null;
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}
