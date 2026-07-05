package com.jc.rag.user.module.user.model.param;

import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

/**
 * 保存用户 API Key 请求参数 (C 端个人中心)。
 * @author JC
 */
public class UserApiKeySaveParam implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "provider 不能为空")
    private String provider;

    private String baseUrl;

    @NotBlank(message = "apiKey 不能为空")
    private String apiKey;

    private String modelName;

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
}
