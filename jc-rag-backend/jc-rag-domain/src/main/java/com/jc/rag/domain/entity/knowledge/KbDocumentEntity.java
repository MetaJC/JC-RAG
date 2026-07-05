package com.jc.rag.domain.entity.knowledge;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 知识库文档元数据 实体。
 * <p>对应 kb_document 表, 记录上传到 MinIO 的原始文件及其解析/切块状态。
 * <p>核心字段: parseStatus PENDING/PROCESSING/SUCCESS/FAILED, chunkCount 切块数量, versionNo 版本号。
 *
 * @author JC
 */
@TableName("kb_document")
public class KbDocumentEntity {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属知识库 ID
     */
    @TableField("kb_id")
    private Long kbId;

    /**
     * 原始文件名
     */
    @TableField("file_name")
    private String fileName;

    /**
     * 文件扩展名 (pdf / docx / md 等)
     */
    @TableField("file_ext")
    private String fileExt;

    /**
     * 文件字节大小
     */
    @TableField("file_size")
    private Long fileSize;

    /**
     * MIME 类型
     */
    @TableField("content_type")
    private String contentType;

    /**
     * MinIO 桶名
     */
    @TableField("minio_bucket")
    private String minioBucket;

    /**
     * MinIO 对象名 (含路径)
     */
    @TableField("minio_object_name")
    private String minioObjectName;

    /**
     * 文件 SHA-256 (秒传/去重)
     */
    @TableField("file_hash")
    private String fileHash;

    /**
     * 当前版本号 (每次重新上传 +1)
     */
    @TableField("version_no")
    private Integer versionNo;

    /**
     * 原始路径 (管理员审计用)
     */
    @TableField("source_path")
    private String sourcePath;

    /**
     * 文档类型 (合同/手册/法规/技术文档 等)
     */
    @TableField("doc_type")
    private String docType;

    /**
     * 保密级别 (PUBLIC / INTERNAL / CONFIDENTIAL / SECRET)
     */
    @TableField("security_level")
    private String securityLevel;

    /**
     * 解析状态 (PENDING / PROCESSING / SUCCESS / FAILED)
     */
    @TableField("parse_status")
    private String parseStatus;

    /**
     * 解析失败时的错误信息
     */
    @TableField("parse_message")
    private String parseMessage;

    /**
     * 解析得到的页数
     */
    @TableField("page_count")
    private Integer pageCount;

    /**
     * 切块后总块数
     */
    @TableField("chunk_count")
    private Integer chunkCount;

    /**
     * 上传者用户 ID
     */
    @TableField("uploaded_by")
    private Long uploadedBy;

    /**
     * 上传时间
     */
    @TableField("uploaded_at")
    private LocalDateTime uploadedAt;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除标记 (0 正常 / 1 已删)
     */
    private Integer deleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getKbId() {
        return kbId;
    }

    public void setKbId(Long kbId) {
        this.kbId = kbId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileExt() {
        return fileExt;
    }

    public void setFileExt(String fileExt) {
        this.fileExt = fileExt;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getMinioBucket() {
        return minioBucket;
    }

    public void setMinioBucket(String minioBucket) {
        this.minioBucket = minioBucket;
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

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public String getSecurityLevel() {
        return securityLevel;
    }

    public void setSecurityLevel(String securityLevel) {
        this.securityLevel = securityLevel;
    }

    public String getParseStatus() {
        return parseStatus;
    }

    public void setParseStatus(String parseStatus) {
        this.parseStatus = parseStatus;
    }

    public String getParseMessage() {
        return parseMessage;
    }

    public void setParseMessage(String parseMessage) {
        this.parseMessage = parseMessage;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }

    public Integer getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(Integer chunkCount) {
        this.chunkCount = chunkCount;
    }

    public Long getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(Long uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
