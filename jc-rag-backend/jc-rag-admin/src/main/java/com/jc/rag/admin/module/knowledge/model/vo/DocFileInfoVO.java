package com.jc.rag.admin.module.knowledge.model.vo;

import lombok.Data;

import java.io.InputStream;
import java.time.LocalDateTime;

/**
 * 文档下载文件信息 VO。
 * <p>Controller 直接读取 stream 输出, 避免二次 I/O。
 *
 * @author JC
 */
@Data
public class DocFileInfoVO {
    private String fileName;
    private String contentType;
    private Long fileSize;
    private LocalDateTime uploadedAt;
    /** 实时输入流, 由 Controller 读取后关闭, 不可跨接口返回. */
    private transient InputStream stream;
}
