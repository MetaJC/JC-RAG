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

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public List<Map<String, Object>> getModels() { return models; }
    public void setModels(List<Map<String, Object>> models) { this.models = models; }
}
