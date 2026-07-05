package com.jc.rag.admin.module.knowledge.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库文档 VO。
 * <p>对应 kb_document 表, 用于列表 / 详情 / 上传回显 / 重建索引等场景。
 *
 * @author JC
 */
@Data
public class KbDocumentVO {
    private Long id;
    private String fileName;
    private String fileExt;
    private Long fileSize;
    private String contentType;
    private Integer versionNo;
    private String docType;
    private String securityLevel;
    private String parseStatus;
    private String parseMessage;
    private Integer pageCount;
    private Integer chunkCount;
    private LocalDateTime uploadedAt;
    private LocalDateTime updatedAt;
}
