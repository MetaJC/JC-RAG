package com.jc.rag.admin.module.rag.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Prompt 视图。
 * @author JC
 */
@Data
public class RagPromptVO {
    private Long id;
    private String promptKey;
    private String promptName;
    private String promptContent;
    private String scope;
    private Long scopeId;
    private Integer version;
    private Integer status;
    private String description;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
    private Long createdBy;
}
