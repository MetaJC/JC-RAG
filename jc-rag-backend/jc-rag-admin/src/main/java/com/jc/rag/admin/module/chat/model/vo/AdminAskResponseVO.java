package com.jc.rag.admin.module.chat.model.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 问答中心-非流式提问响应 VO。
 * <p>userMessageId / assistantMessageId 用于前端回显消息 id, answer 为模型最终回答, references 为引用溯源列表。
 *
 * @author JC
 */
@Data
public class AdminAskResponseVO {
    private Long userMessageId;
    private Long assistantMessageId;
    private String answer;
    private List<Map<String, Object>> references;
}
