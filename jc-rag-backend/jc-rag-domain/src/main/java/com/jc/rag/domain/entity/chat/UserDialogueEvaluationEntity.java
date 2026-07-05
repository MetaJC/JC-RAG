package com.jc.rag.domain.entity.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 用户对话评估记录实体。
 * <p>对应 user_dialogue_evaluation 表, 每次用户评估一段对话后插入一条记录。</p>
 * <p>核心字段: score 综合评分, strengths/improvements JSON 数组字符串, proEnhanced 是否付费版评估。</p>
 *
 * @author JC
 */
@TableName("user_dialogue_evaluation")
public class UserDialogueEvaluationEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("session_id")
    private Long sessionId;

    @TableField("score")
    private Integer score;

    @TableField("comment")
    private String comment;

    @TableField("strengths")
    private String strengths;

    @TableField("improvements")
    private String improvements;

    @TableField("model_name")
    private String modelName;

    @TableField("pro_enhanced")
    private Integer proEnhanced;

    @TableField("created_at")
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long v) { id = v; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { userId = v; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long v) { sessionId = v; }
    public Integer getScore() { return score; }
    public void setScore(Integer v) { score = v; }
    public String getComment() { return comment; }
    public void setComment(String v) { comment = v; }
    public String getStrengths() { return strengths; }
    public void setStrengths(String v) { strengths = v; }
    public String getImprovements() { return improvements; }
    public void setImprovements(String v) { improvements = v; }
    public String getModelName() { return modelName; }
    public void setModelName(String v) { modelName = v; }
    public Integer getProEnhanced() { return proEnhanced; }
    public void setProEnhanced(Integer v) { proEnhanced = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { createdAt = v; }
}
