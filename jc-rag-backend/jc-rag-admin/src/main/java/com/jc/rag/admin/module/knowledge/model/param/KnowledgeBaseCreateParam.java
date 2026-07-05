package com.jc.rag.admin.module.knowledge.model.param;

import jakarta.validation.constraints.NotBlank;

/**
 * 知识库创建请求。
 *
 * @author JC
 */
public class KnowledgeBaseCreateParam {

    private String kbCode;

    @NotBlank(message = "知识库名称不能为空")
    private String kbName;

    private String kbDesc;

    public String getKbCode() {
        return kbCode;
    }

    public void setKbCode(String kbCode) {
        this.kbCode = kbCode;
    }

    public String getKbName() {
        return kbName;
    }

    public void setKbName(String kbName) {
        this.kbName = kbName;
    }

    public String getKbDesc() {
        return kbDesc;
    }

    public void setKbDesc(String kbDesc) {
        this.kbDesc = kbDesc;
    }
}
