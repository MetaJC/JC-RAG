package com.jc.rag.domain.entity.rag;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * Skill 技能 实体。
 * <p>对应 sys_skills 表, Agent 可挂载的技能 (提示模板 + 触发规则), 由 Orchestrator 在对话流中按规则激活。
 * <p>核心字段: triggerType KEYWORD / INTENT / ALWAYS, triggerRules JSON 规则, sortOrder 优先级。
 *
 * @author JC
 */
@TableName("sys_skills")
public class RagSkillEntity {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * Skill 名称
     */
    @TableField("skill_name")
    private String skillName;
    /**
     * Skill 业务 Key (代码引用)
     */
    @TableField("skill_key")
    private String skillKey;
    /**
     * 描述
     */
    private String description;
    /**
     * 提示模板
     */
    @TableField("prompt_template")
    private String promptTemplate;
    /**
     * 触发类型 (KEYWORD 关键词 / INTENT 意图 / ALWAYS 总是)
     */
    @TableField("trigger_type")
    private String triggerType;
    /**
     * 触发规则 JSON
     */
    @TableField("trigger_rules")
    private String triggerRules;
    /**
     * 图标 URL
     */
    private String icon;
    /**
     * 排序号 (升序)
     */
    @TableField("sort_order")
    private Integer sortOrder;
    /**
     * 状态 (1 启用 / 0 禁用)
     */
    private Integer status;
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

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String v) {
        skillName = v;
    }

    public String getSkillKey() {
        return skillKey;
    }

    public void setSkillKey(String v) {
        skillKey = v;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String v) {
        description = v;
    }

    public String getPromptTemplate() {
        return promptTemplate;
    }

    public void setPromptTemplate(String v) {
        promptTemplate = v;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String v) {
        triggerType = v;
    }

    public String getTriggerRules() {
        return triggerRules;
    }

    public void setTriggerRules(String v) {
        triggerRules = v;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String v) {
        icon = v;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer v) {
        sortOrder = v;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer v) {
        status = v;
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
