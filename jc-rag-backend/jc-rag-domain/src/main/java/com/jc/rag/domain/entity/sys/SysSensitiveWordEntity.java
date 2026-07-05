package com.jc.rag.domain.entity.sys;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 系统敏感词实体。
 * <p>对应 sys_sensitive_word 表, 落库管理敏感词, 启动全量加载到内存 (Aho-Corasick 匹配)。
 * <p>处置动作独立于风险等级: 同一 risk_level 可配置不同 action (如 HIGH 可临时降级为 ALERT 用于灰度)。
 *
 * @author JC
 */
@TableName("sys_sensitive_word")
public class SysSensitiveWordEntity {

    /** 风险等级: 高 */
    public static final String LEVEL_HIGH = "HIGH";
    /** 风险等级: 中 */
    public static final String LEVEL_MEDIUM = "MEDIUM";
    /** 风险等级: 低 */
    public static final String LEVEL_LOW = "LOW";

    /** 处置动作: 拒绝 */
    public static final String ACTION_DENY = "DENY";
    /** 处置动作: 告警 (放行 + 记审计) */
    public static final String ACTION_ALERT = "ALERT";
    /** 处置动作: 脱敏 (替换为 *** 后放行) */
    public static final String ACTION_MASK = "MASK";
    /** 处置动作: 仅审计 */
    public static final String ACTION_PASS = "PASS";

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关键词 */
    private String word;
    /** 风险等级 (HIGH/MEDIUM/LOW) */
    @TableField("risk_level")
    private String riskLevel;
    /** 分类 (PROMPT_INJECTION/CREDENTIAL/PII/ABUSE) */
    private String category;
    /** 处置动作 (DENY/ALERT/MASK/PASS) */
    private String action;
    /** 1=启用 0=禁用 */
    private Integer enabled;
    /** 说明 */
    private String description;
    /** 创建人 */
    @TableField("created_by")
    private Long createdBy;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long v) { id = v; }
    public String getWord() { return word; }
    public void setWord(String v) { word = v; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String v) { riskLevel = v; }
    public String getCategory() { return category; }
    public void setCategory(String v) { category = v; }
    public String getAction() { return action; }
    public void setAction(String v) { action = v; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer v) { enabled = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { description = v; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long v) { createdBy = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { createdAt = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { updatedAt = v; }
}
