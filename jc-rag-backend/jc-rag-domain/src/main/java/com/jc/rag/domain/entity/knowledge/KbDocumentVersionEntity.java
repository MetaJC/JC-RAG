package com.jc.rag.domain.entity.knowledge;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 知识库文档历史版本 实体。
 * <p>对应 kb_document_version 表, 同一 document 多次上传会保留历史 MinIO 对象与 file_hash, 便于回滚。
 *
 * @author JC
 */
@TableName("kb_document_version")
public class KbDocumentVersionEntity {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属文档 ID
     */
    @TableField("document_id")
    private Long documentId;

    /**
     * 版本号
     */
    @TableField("version_no")
    private Integer versionNo;

    /**
     * 该版本在 MinIO 中的对象名
     */
    @TableField("minio_object_name")
    private String minioObjectName;

    /**
     * 该版本文件 SHA-256
     */
    @TableField("file_hash")
    private String fileHash;

    /**
     * 上传人用户 ID
     */
    @TableField("created_by")
    private Long createdBy;

    /**
     * 上传时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 备注 (本次上传说明)
     */
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public String getMinioObjectName() {
        return minioObjectName;
    }

    public void setMinioObjectName(String minioObjectName) {
        this.minioObjectName = minioObjectName;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
