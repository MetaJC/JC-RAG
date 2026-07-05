package com.jc.rag.admin.module.rag.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Agent 视图。
 * @author JC
 */
@Data
public class RagAgentVO {
    private Long id;
    private String agentName;
    private String agentKey;
    /** 1=系统智能体 (Admin 管控, C 端不可见) 0=内建用户智能体 (Admin 创建, C 端可见) */
    private Integer isSystem;
    private String description;
    /** System Prompt (系统智能体 isSystem=1 使用) */
    private String systemPrompt;
    /** User Prompt (内建用户智能体 isSystem=0 使用, 对应 user_custom_agent.content 语义) */
    private String content;
    private BigDecimal temperature;
    private Integer maxTokens;
    private Integer status;
    private Integer version;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime publishedAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
    private Long createdBy;
    private java.util.List<Long> skillIds;
}
