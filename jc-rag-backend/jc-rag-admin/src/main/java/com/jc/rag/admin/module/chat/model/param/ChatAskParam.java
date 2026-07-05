package com.jc.rag.admin.module.chat.model.param;

import jakarta.validation.constraints.NotBlank;

public class ChatAskParam {

    @NotBlank(message = "问题内容不能为空")
    private String question;

    private Long agentId;

    private Boolean deepThinking;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public Boolean getDeepThinking() {
        return deepThinking;
    }

    public void setDeepThinking(Boolean deepThinking) {
        this.deepThinking = deepThinking;
    }
}