package com.jc.rag.domain.entity.rag;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Agent 实体。
 * <p>对应 sys_agents 表, 描述一个可被会话引用的智能体配置 (system prompt / 模型参数)。
 * <p>核心字段: agentKey 全局唯一标识, temperature/maxTokens 推理参数, version + publishedAt 灰度发布。
 *
 * @author JC
 */
@TableName("sys_agents")
public class RagAgentEntity {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * Agent 名称
     */
    @TableField("agent_name")
    private String agentName;
    /**
     * Agent 业务 Key (全局唯一, 用于代码引用)
     */
    @TableField("agent_key")
    private String agentKey;
    /**
     * 是否系统智能体 (1=系统 Admin 管控; 0=用户自建, 仅 admin 端展示)
     */
    @TableField("is_system")
    private Integer isSystem;
    /**
     * 描述
     */
    private String description;
    /**
     * System Prompt 内容 (仅系统智能体 is_system=1 使用, 作为 LLM 的 system message)
     */
    @TableField("system_prompt")
    private String systemPrompt;
    /**
     * User Prompt 内容 (仅内建用户智能体 is_system=0 使用, 作为 LLM 的 user message 增强片段)
     */
    @TableField("content")
    private String content;
    /**
     * 温度参数 (0~2, 控制创造性)
     */
    private BigDecimal temperature;
    /**
     * 单次最大 Token 上限
     */
    @TableField("max_tokens")
    private Integer maxTokens;
    /**
     * 状态 (1 启用 / 0 禁用)
     */
    private Integer status;
    /**
     * 配置版本号 (灰度发布用)
     */
    private Integer version;
    /**
     * 发布时间
     */
    @TableField("published_at")
    private LocalDateTime publishedAt;
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

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String v) {
        agentName = v;
    }

    public String getAgentKey() {
        return agentKey;
    }

    public void setAgentKey(String v) {
        agentKey = v;
    }

    public Integer getIsSystem() { return isSystem; }
    public void setIsSystem(Integer v) { isSystem = v; }

    public String getDescription() {
        return description;
    }

    public void setDescription(String v) {
        description = v;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String v) {
        systemPrompt = v;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String v) {
        content = v;
    }

    public BigDecimal getTemperature() {
        return temperature;
    }

    public void setTemperature(BigDecimal v) {
        temperature = v;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer v) {
        maxTokens = v;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer v) {
        status = v;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer v) {
        version = v;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime v) {
        publishedAt = v;
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
