package com.jc.rag.admin.module.knowledge.model.param;

import jakarta.validation.constraints.NotBlank;

public class KnowledgeDocumentUpdateParam {

    @NotBlank(message = "文档名称不能为空")
    private String fileName;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}

