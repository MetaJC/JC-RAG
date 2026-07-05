package com.jc.rag.admin.module.knowledge.model.param;

import jakarta.validation.constraints.NotBlank;

public class KnowledgeBaseUpdateParam {

    @NotBlank(message = "知识库名称不能为空")
    private String kbName;

    private String kbDesc;

    private String visibilityScope;

    /** 是否允许客户端用户自行选择 (0 否 / 1 是) - 仅 PUBLIC 生效 */
    private Integer clientSelectable;

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

    public String getVisibilityScope() {
        return visibilityScope;
    }

    public void setVisibilityScope(String visibilityScope) {
        this.visibilityScope = visibilityScope;
    }

    public Integer getClientSelectable() {
        return clientSelectable;
    }

    public void setClientSelectable(Integer clientSelectable) {
        this.clientSelectable = clientSelectable;
    }
}

