package com.jc.rag.admin.module.sys.model.param;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ModelProviderSaveParam {
    private Long id;
    @NotBlank private String providerType;
    @NotBlank private String providerName;
    @NotBlank private String endpoint;
    @NotBlank private String modelName;
    @NotNull private Boolean enabled;
    private Integer priorityNo;
    private String apiKey;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProviderType() { return providerType; }
    public void setProviderType(String providerType) { this.providerType = providerType; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Integer getPriorityNo() { return priorityNo; }
    public void setPriorityNo(Integer priorityNo) { this.priorityNo = priorityNo; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
}
