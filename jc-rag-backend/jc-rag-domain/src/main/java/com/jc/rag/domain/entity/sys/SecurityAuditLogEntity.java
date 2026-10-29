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


package com.jc.rag.domain.entity.sys;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 安全审计日志实体。
 * <p>对应 security_audit_log 表, 记录敏感词命中/拒绝/脱敏全留痕。
 * <p>关键设计: original_content_hash 存 SHA-256 指纹 (不存原文, 保护用户隐私, 符合 GDPR/PIPL)。
 *
 * @author JC
 */
@TableName("security_audit_log")
public class SecurityAuditLogEntity {

    /** 审计类型: 输入拒绝 */
    public static final String TYPE_INPUT_DENY = "INPUT_DENY";
    /** 审计类型: 输入告警 */
    public static final String TYPE_INPUT_ALERT = "INPUT_ALERT";
    /** 审计类型: 输入脱敏 */
    public static final String TYPE_INPUT_MASK = "INPUT_MASK";
    /** 审计类型: 输出拒绝 */
    public static final String TYPE_OUTPUT_DENY = "OUTPUT_DENY";
    /** 审计类型: 输出脱敏 */
    public static final String TYPE_OUTPUT_MASK = "OUTPUT_MASK";

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 触发用户 */
    @TableField("user_id")
    private Long userId;
    /** 关联会话 */
    @TableField("session_id")
    private Long sessionId;
    /** 审计类型 */
    @TableField("audit_type")
    private String auditType;
    /** 风险等级 */
    @TableField("risk_level")
    private String riskLevel;
    /** 命中敏感词 ID (关联 sys_sensitive_word.id) */
    @TableField("matched_word_id")
    private Long matchedWordId;
    /** 命中关键词 (冗余, 便于审计展示) */
    @TableField("matched_word")
    private String matchedWord;
    /** 原文 SHA-256 指纹 (不存原文) */
    @TableField("original_content_hash")
    private String originalContentHash;
    /** 脱敏后内容 (供审计查阅) */
    @TableField("masked_content")
    private String maskedContent;
    /** 处置动作 */
    private String action;
    /** 客户端 IP */
    @TableField("client_ip")
    private String clientIp;
    /** UA */
    @TableField("user_agent")
    private String userAgent;
    @TableField("created_at")
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long v) { id = v; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { userId = v; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long v) { sessionId = v; }
    public String getAuditType() { return auditType; }
    public void setAuditType(String v) { auditType = v; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String v) { riskLevel = v; }
    public Long getMatchedWordId() { return matchedWordId; }
    public void setMatchedWordId(Long v) { matchedWordId = v; }
    public String getMatchedWord() { return matchedWord; }
    public void setMatchedWord(String v) { matchedWord = v; }
    public String getOriginalContentHash() { return originalContentHash; }
    public void setOriginalContentHash(String v) { originalContentHash = v; }
    public String getMaskedContent() { return maskedContent; }
    public void setMaskedContent(String v) { maskedContent = v; }
    public String getAction() { return action; }
    public void setAction(String v) { action = v; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String v) { clientIp = v; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String v) { userAgent = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { createdAt = v; }
}
