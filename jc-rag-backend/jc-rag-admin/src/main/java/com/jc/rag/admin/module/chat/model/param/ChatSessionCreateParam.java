package com.jc.rag.admin.module.chat.model.param;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ChatSessionCreateParam {

    @NotBlank(message = "会话标题不能为空")
    private String sessionTitle;

    @NotNull
    private Long kbId;

    public String getSessionTitle() {
        return sessionTitle;
    }

    public void setSessionTitle(String sessionTitle) {
        this.sessionTitle = sessionTitle;
    }

    public Long getKbId() {
        return kbId;
    }

    public void setKbId(Long kbId) {
        this.kbId = kbId;
    }
}
