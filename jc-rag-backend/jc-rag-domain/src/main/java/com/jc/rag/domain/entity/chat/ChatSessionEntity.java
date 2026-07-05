package com.jc.rag.domain.entity.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 聊天会话实体。
 * <p>对应 chat_session 表, 记录用户发起的每一次会话。
 * <p>核心字段: userId 会话归属人, agentId 使用的 Agent 配置, status 1 进行中 / 0 已结束。
 *
 * @author JC
 */
@TableName("chat_session")
public class ChatSessionEntity {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 会话归属用户 ID
     */
    @TableField("user_id")
    private Long userId;
    /**
     * 会话标题 (取首条用户问题前 N 字)
     */
    @TableField("session_title")
    private String sessionTitle;
    /**
     * 会话模式 (RAG / DIRECT / AGENT 等)
     */
    @TableField("session_mode")
    private String sessionMode;
    /**
     * 模型类型 (openai / deepseek / ollama / 自定义)
     */
    @TableField("model_mode")
    private String modelMode;
    /**
     * 具体模型名 (gpt-4o-mini / deepseek-chat 等)
     */
    @TableField("model_name")
    private String modelName;
    /**
     * 关联知识库 ID (RAG 模式必填)
     */
    @TableField("kb_id")
    private Long kbId;
    /**
     * 是否置顶 (1 置顶 / 0 正常)
     */
    @TableField("is_pinned")
    private Integer isPinned;
    /**
     * 状态 (1 进行中 / 0 已结束)
     */
    private Integer status;
    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
    /**
     * 最近活跃时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long v) {
        id = v;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long v) {
        userId = v;
    }

    public String getSessionTitle() {
        return sessionTitle;
    }

    public void setSessionTitle(String v) {
        sessionTitle = v;
    }

    public String getSessionMode() {
        return sessionMode;
    }

    public void setSessionMode(String v) {
        sessionMode = v;
    }

    public String getModelMode() {
        return modelMode;
    }

    public void setModelMode(String v) {
        modelMode = v;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String v) {
        modelName = v;
    }

    public Long getKbId() {
        return kbId;
    }

    public void setKbId(Long v) {
        kbId = v;
    }

    public Integer getIsPinned() {
        return isPinned;
    }

    public void setIsPinned(Integer v) {
        isPinned = v;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer v) {
        status = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime v) {
        createdAt = v;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime v) {
        updatedAt = v;
    }
}
