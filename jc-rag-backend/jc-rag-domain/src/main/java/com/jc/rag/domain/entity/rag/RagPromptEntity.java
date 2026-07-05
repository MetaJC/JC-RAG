package com.jc.rag.domain.entity.rag;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * Prompt 模板 实体。
 * <p>对应 sys_prompts 表, 用于沉淀高频 Prompt, 支持 GLOBAL/AGENT 两种 scope 多版本管理。
 * <p>核心字段: scope 作用域, version 多版本, status 启用状态。
 *
 * @author JC
 */
@TableName("sys_prompts")
public class RagPromptEntity {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * Prompt 业务 Key (代码引用)
     */
    @TableField("prompt_key")
    private String promptKey;
    /**
     * Prompt 展示名
     */
    @TableField("prompt_name")
    private String promptName;
    /**
     * Prompt 正文 (含占位符 {{var}})
     */
    @TableField("prompt_content")
    private String promptContent;
    /**
     * 作用域 (GLOBAL 全局 / AGENT Agent 级)
     */
    private String scope;
    /**
     * 作用域 ID (scope=AGENT 时填 Agent ID)
     */
    @TableField("scope_id")
    private Long scopeId;
    /**
     * 版本号 (同 key 递增)
     */
    private Integer version;
    /**
     * 状态 (1 启用 / 0 禁用)
     */
    private Integer status;
    /**
     * 描述
     */
    private String description;
    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
    /**
     * 创建人用户 ID
     */
    @TableField("created_by")
    private Long createdBy;

    public Long getId() {
        return id;
    }

    public void setId(Long v) {
        id = v;
    }

    public String getPromptKey() {
        return promptKey;
    }

    public void setPromptKey(String v) {
        promptKey = v;
    }

    public String getPromptName() {
        return promptName;
    }

    public void setPromptName(String v) {
        promptName = v;
    }

    public String getPromptContent() {
        return promptContent;
    }

    public void setPromptContent(String v) {
        promptContent = v;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String v) {
        scope = v;
    }

    public Long getScopeId() {
        return scopeId;
    }

    public void setScopeId(Long v) {
        scopeId = v;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer v) {
        version = v;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer v) {
        status = v;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String v) {
        description = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime v) {
        createdAt = v;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime v) {
        updatedAt = v;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long v) {
        createdBy = v;
    }
}
