package com.jc.rag.admin.module.rag.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Skill 视图。
 * @author JC
 */
@Data
public class RagSkillVO {
    private Long id;
    private String skillName;
    private String skillKey;
    private String description;
    private String promptTemplate;
    private String triggerType;
    private String triggerRules;
    private String icon;
    private Integer sortOrder;
    private Integer status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
    private Long createdBy;
}
