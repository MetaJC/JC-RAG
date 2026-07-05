package com.jc.rag.admin.module.rag.model.param;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Skill 保存参数。
 * @author JC
 */
@Data
public class RagSkillSaveParam {

    @NotBlank(message = "skillName 不能为空")
    @Size(max = 100)
    private String skillName;

    @NotBlank(message = "skillKey 不能为空")
    @Size(max = 100)
    private String skillKey;

    private String description;

    private String promptTemplate;

    private String triggerType;

    private String triggerRules;

    private String icon;

    private Integer sortOrder;

    private Integer status;
}
