package com.jc.rag.domain.entity.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;

/**
 * 聊天消息引用实体。
 * <p>对应 chat_message_reference 表, 记录一次回答引用的文档片段, 支撑前端展示"引用来源"。
 * <p>核心字段: messageId 关联 chat_message.id, documentId/chunkId 定位原文位置, score 相似度。
 *
 * @author JC
 */
@TableName("chat_message_reference")
public class ChatMessageReferenceEntity {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 所属消息 ID
     */
    @TableField("message_id")
    private Long messageId;
    /**
     * 文档 ID
     */
    @TableField("document_id")
    private Long documentId;
    /**
     * 切片 ID (Python 端 chunk 标识)
     */
    @TableField("chunk_id")
    private String chunkId;
    /**
     * 页码
     */
    @TableField("page_no")
    private Integer pageNo;
    /**
     * 章节标题
     */
    @TableField("section_title")
    private String sectionTitle;
    /**
     * 相似度分数
     */
    private BigDecimal score;
    /**
     * 引用片段原文
     */
    @TableField("quote_text")
    private String quoteText;

    public Long getId() { return id; }
    public void setId(Long v) { id = v; }
    public Long getMessageId() { return messageId; }
    public void setMessageId(Long v) { messageId = v; }
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long v) { documentId = v; }
    public String getChunkId() { return chunkId; }
    public void setChunkId(String v) { chunkId = v; }
    public Integer getPageNo() { return pageNo; }
    public void setPageNo(Integer v) { pageNo = v; }
    public String getSectionTitle() { return sectionTitle; }
    public void setSectionTitle(String v) { sectionTitle = v; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal v) { score = v; }
    public String getQuoteText() { return quoteText; }
    public void setQuoteText(String v) { quoteText = v; }
}
