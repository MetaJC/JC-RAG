package com.jc.rag.admin.module.sys.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审计日志通用 VO (RAG 调用 / 系统异常 共用)。
 * <p>字段名以 camelCase 输出, 兼容现有前端。
 * @author JC
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String moduleName;
    private String action;
    private String userName;
    /** 用户昵称 (RAG 调用日志, 来源 rbac_user.nickname) */
    private String userNickname;
    /** 用户 ID (RAG 调用日志) */
    private Long userId;
    /** 会话 ID (RAG 调用日志) */
    private Long sessionId;
    /** 会话名称 (RAG 调用日志, 来源 chat_session.session_title) */
    private String sessionName;
    /** 用户提问原文 (RAG 调用日志) */
    private String queryText;
    /** 改写后的查询 (RAG 调用日志) */
    private String rewrittenQuery;
    /** 召回片段数 (RAG 调用日志) */
    private Integer retrievedChunks;
    /** 模型模式: online/offline (RAG 调用日志) */
    private String modelMode;
    private String modelName;
    private String requestId;
    private String keyword;
    private Integer status;
    /** 成功标记: 1 成功 / 0 失败 (RAG 调用日志) */
    private Integer successFlag;
    private Integer durationMs;
    /** 耗时毫秒 (RAG 调用日志) */
    private Long latencyMs;
    /** 耗时秒, 保留 3 位小数 (RAG 调用日志, SQL 端 ROUND 计算) */
    private Double latencySec;
    private Integer tokensPrompt;
    private Integer tokensCompletion;
    private String errorMessage;
    private String stackTrace;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getModuleName() { return moduleName; }
    public void setModuleName(String moduleName) { this.moduleName = moduleName; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserNickname() { return userNickname; }
    public void setUserNickname(String userNickname) { this.userNickname = userNickname; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public String getSessionName() { return sessionName; }
    public void setSessionName(String sessionName) { this.sessionName = sessionName; }
    public String getQueryText() { return queryText; }
    public void setQueryText(String queryText) { this.queryText = queryText; }
    public String getRewrittenQuery() { return rewrittenQuery; }
    public void setRewrittenQuery(String rewrittenQuery) { this.rewrittenQuery = rewrittenQuery; }
    public Integer getRetrievedChunks() { return retrievedChunks; }
    public void setRetrievedChunks(Integer retrievedChunks) { this.retrievedChunks = retrievedChunks; }
    public String getModelMode() { return modelMode; }
    public void setModelMode(String modelMode) { this.modelMode = modelMode; }
    public Integer getSuccessFlag() { return successFlag; }
    public void setSuccessFlag(Integer successFlag) { this.successFlag = successFlag; }
    public Long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Long latencyMs) { this.latencyMs = latencyMs; }
    public Double getLatencySec() { return latencySec; }
    public void setLatencySec(Double latencySec) { this.latencySec = latencySec; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getDurationMs() { return durationMs; }
    public void setDurationMs(Integer durationMs) { this.durationMs = durationMs; }
    public Integer getTokensPrompt() { return tokensPrompt; }
    public void setTokensPrompt(Integer tokensPrompt) { this.tokensPrompt = tokensPrompt; }
    public Integer getTokensCompletion() { return tokensCompletion; }
    public void setTokensCompletion(Integer tokensCompletion) { this.tokensCompletion = tokensCompletion; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getStackTrace() { return stackTrace; }
    public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
