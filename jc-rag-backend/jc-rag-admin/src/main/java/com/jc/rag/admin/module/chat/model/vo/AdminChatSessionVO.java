package com.jc.rag.admin.module.chat.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问答中心-会话 VO。
 * <p>对应 chat_session 表, 用于会话列表 / 详情 / 创建回显。
 * <p>字段命名遵循前端 camelCase 约定, 序列化时与现有接口保持兼容。
 *
 * @author JC
 */
@Data
public class AdminChatSessionVO {
    private Long id;
    private Long userId;
    private String sessionTitle;
    private String sessionMode;
    private String modelMode;
    private String modelName;
    private Long kbId;
    private String kbName;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
