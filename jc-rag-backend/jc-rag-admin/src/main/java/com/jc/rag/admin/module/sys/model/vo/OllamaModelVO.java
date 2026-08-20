package com.jc.rag.admin.module.sys.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Ollama 模型列表响应 VO (admin)。
 * @author JC
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OllamaModelVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String baseUrl;
    private List<Map<String, Object>> models;
    /** SUCCESS / FAILED: 查询是否成功 (失败时前端可提示, 不再静默空列表) */
    private String status;
    /** 失败原因 (status=FAILED 时携带) */
    private String message;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public List<Map<String, Object>> getModels() { return models; }
    public void setModels(List<Map<String, Object>> models) { this.models = models; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
