package com.jc.rag.admin.module.rag.model.param;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Prompt 保存参数。
 * @author JC
 */
@Data
public class RagPromptSaveParam {

    @NotBlank(message = "promptKey 不能为空")
    @Size(max = 100)
    private String promptKey;

    @NotBlank(message = "promptName 不能为空")
    @Size(max = 100)
    private String promptName;

    @NotBlank(message = "promptContent 不能为空")
    private String promptContent;

    private String scope;

    private Long scopeId;

    private Integer status;

    private String description;
}
