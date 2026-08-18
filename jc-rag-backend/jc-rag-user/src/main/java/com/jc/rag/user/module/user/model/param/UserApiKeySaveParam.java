package com.jc.rag.user.module.user.model.param;

import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

/**
 * 保存用户 API Key 请求参数 (C 端个人中心)。
 * <p>id 非空为编辑该条记录; 空为新增 (新增默认禁用, 需手动启用)。
 * <p>apiKey 校验规则由服务层按场景区分: 新增必填; 编辑留空表示保持原 Key 不变
 * (列表接口不回传密钥明文, 前端无法回填)。
 * @author JC
 */
public class UserApiKeySaveParam implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 记录 ID (非空 = 编辑, 空 = 新增) */
    private Long id;

    @NotBlank(message = "provider 不能为空")
    private String provider;

    private String baseUrl;

    /** API Key (新增必填; 编辑留空 = 保持原值) */
    private String apiKey;

    private String modelName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
}
