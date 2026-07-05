package com.jc.rag.admin.module.sys.model.vo;

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
    private String modelName;
    private String requestId;
    private String keyword;
    private Integer status;
    private Integer durationMs;
    private Integer tokensPrompt;
    private Integer tokensCompletion;
    private String errorMessage;
    private String stackTrace;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getModuleName() { return moduleName; }
    public void setModuleName(String moduleName) { this.moduleName = moduleName; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
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
