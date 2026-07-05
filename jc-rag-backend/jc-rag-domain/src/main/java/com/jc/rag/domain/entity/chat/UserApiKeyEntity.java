package com.jc.rag.domain.entity.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 用户自定义 API Key 实体。
 * <p>对应 user_api_keys 表, 存储用户自行配置的 OpenAI / DeepSeek 等第三方平台密钥。
 * <p>核心字段: provider 平台标识, baseUrl 自定义网关, apiKey 密文, isActive 启用状态。
 *
 * @author JC
 */
@TableName("user_api_keys")
public class UserApiKeyEntity {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 所属用户 ID
     */
    @TableField("user_id")
    private Long userId;
    /**
     * 平台标识 (openai / deepseek / qwen / 自定义)
     */
    private String provider;
    /**
     * 自定义网关地址 (支持私有化部署)
     */
    @TableField("base_url")
    private String baseUrl;
    /**
     * API Key (入库时建议 AES 加密)
     */
    @TableField("api_key")
    private String apiKey;
    /**
     * 默认模型名 (gpt-4o-mini / deepseek-chat 等)
     */
    @TableField("model_name")
    private String modelName;
    /**
     * 启用状态 (1 启用 / 0 禁用)
     */
    @TableField("is_active")
    private Integer isActive;

    public Long getId() {
        return id;
    }

    public void setId(Long v) {
        id = v;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long v) {
        userId = v;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String v) {
        provider = v;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String v) {
        baseUrl = v;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String v) {
        apiKey = v;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String v) {
        modelName = v;
    }

    public Integer getIsActive() {
        return isActive;
    }

    public void setIsActive(Integer v) {
        isActive = v;
    }
}
