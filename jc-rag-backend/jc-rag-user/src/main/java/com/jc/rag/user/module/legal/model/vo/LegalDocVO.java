package com.jc.rag.user.module.legal.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 协议/政策文档 VO (用户端公共查询)。
 * @author JC
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LegalDocVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String docType;
    private String title;
    private String content;
    private Integer version;
    private LocalDateTime publishedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDocType() { return docType; }
    public void setDocType(String docType) { this.docType = docType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
}
