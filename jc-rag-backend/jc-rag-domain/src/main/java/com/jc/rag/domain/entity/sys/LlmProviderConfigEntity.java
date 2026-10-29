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
 * 模型提供方配置 实体。
 * <p>对应 llm_provider_config 表, 存储各模型服务商的接入配置 (地址/密钥/默认模型/启用状态)。
 *
 * @author JC
 */
@TableName("llm_provider_config")
public class LlmProviderConfigEntity {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 提供方类型 (ONLINE / OFFLINE / OLLAMA)
     */
    @TableField("provider_type")
    private String providerType;
    /**
     * 提供方标识 (如 deepseek / qwen / openai / ollama)
     */
    @TableField("provider_name")
    private String providerName;
    /**
     * 服务地址
     */
    private String endpoint;
    /**
     * API Key (加密/明文存储)
     */
    @TableField("api_key_encrypted")
    private String apiKeyEncrypted;
    /**
     * 默认模型名
     */
    @TableField("model_name")
    private String modelName;
    /**
     * 是否启用 (1 启用 / 0 停用)
     */
    private Integer enabled;
    /**
     * 优先级序号
     */
    @TableField("priority_no")
    private Integer priorityNo;
    /**
     * 扩展配置 JSON
     */
    @TableField("extra_config_json")
    private String extraConfigJson;
    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long v) {
        id = v;
    }

    public String getProviderType() {
        return providerType;
    }

    public void setProviderType(String v) {
        providerType = v;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String v) {
        providerName = v;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String v) {
        endpoint = v;
    }

    public String getApiKeyEncrypted() {
        return apiKeyEncrypted;
    }

    public void setApiKeyEncrypted(String v) {
        apiKeyEncrypted = v;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String v) {
        modelName = v;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer v) {
        enabled = v;
    }

    public Integer getPriorityNo() {
        return priorityNo;
    }

    public void setPriorityNo(Integer v) {
        priorityNo = v;
    }

    public String getExtraConfigJson() {
        return extraConfigJson;
    }

    public void setExtraConfigJson(String v) {
        extraConfigJson = v;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime v) {
        updatedAt = v;
    }
}
