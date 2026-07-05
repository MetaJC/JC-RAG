package com.jc.rag.admin.module.chat.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 问答中心-消息 VO。
 * <p>对应 chat_message 表; references 字段保留 Map 形态, 因为来源于 Python 端原始 JSON 透传, 不做 DTO 化。
 *
 * @author JC
 */
@Data
public class AdminChatMessageVO {
    private Long id;
    private String roleType;
    private String messageContent;
    private Integer messageIndex;
    private Integer tokenCount;
    private Long latencyMs;
    private LocalDateTime createdAt;
    private List<Map<String, Object>> references;
}
