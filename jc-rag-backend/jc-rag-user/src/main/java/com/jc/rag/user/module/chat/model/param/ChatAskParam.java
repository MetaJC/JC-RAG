package com.jc.rag.user.module.chat.model.param;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * C 端问答请求 Param (普通问答 + 流式问答共用)。
 * @author JC
 */
@Data
public class ChatAskParam {

    @NotBlank(message = "question 不能为空")
    private String question;

    /** 是否开启深度思考. */
    private Boolean deepThinking;

    /** 指定智能体 ID (可选). */
    private Long agentId;

    /**
     * 自定义智能体 ID (可选, 互斥于 agentId).
     * <p>传入时, Java 端会查 user_custom_agent 表, 取其 content 字段作为 user 层 prompt 片段注入,
     * 不会触及 system 层. 与系统 agentId 互斥: 优先使用 customAgentId.
     */
    private Long customAgentId;

    /** 覆盖会话的 KB (可选). */
    private Long kbId;

    /** 附件列表 (可选). */
    private List<Map<String, Object>> attachments;
}
