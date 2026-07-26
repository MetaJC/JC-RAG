package com.jc.rag.admin.module.sys.model.param;

import jakarta.validation.constraints.NotBlank;

/** Connection probe input. The API key is used only for this request and is never persisted here. */
public class ModelProviderTestParam {
    private Long id;
    @NotBlank private String providerName;
    @NotBlank private String providerType;
    @NotBlank private String endpoint;
    private String apiKey;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    public String getProviderType() { return providerType; }
    public void setProviderType(String providerType) { this.providerType = providerType; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
}
