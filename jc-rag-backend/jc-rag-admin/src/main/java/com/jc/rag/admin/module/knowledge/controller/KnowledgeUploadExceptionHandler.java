package com.jc.rag.admin.module.knowledge.controller;

import com.jc.rag.admin.config.KnowledgeUploadProperties;
import com.jc.rag.admin.module.knowledge.exception.KnowledgeTaskConflictException;
import com.jc.rag.admin.module.knowledge.exception.KnowledgeUploadException;
import com.jc.rag.admin.module.knowledge.model.vo.KnowledgeUploadErrorVO;
import com.jc.rag.common.web.R;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

/**
 * 知识库上传异常切面。
 * <p>仅处理 admin/knowledge 模块下文件上传特有的异常, 与全局 GlobalExceptionHandler 互补。
 * <p>异常一律用 R.fail() 表达业务失败, 避免与 R.ok() 成功语义混淆。
 *
 * @author JC
 */
@RestControllerAdvice
public class KnowledgeUploadExceptionHandler {

    private final KnowledgeUploadProperties knowledgeUploadProperties;

    public KnowledgeUploadExceptionHandler(KnowledgeUploadProperties knowledgeUploadProperties) {
        this.knowledgeUploadProperties = knowledgeUploadProperties;
    }

    @ExceptionHandler(KnowledgeUploadException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<KnowledgeUploadErrorVO> handleKnowledgeUploadException(KnowledgeUploadException ex) {
        return R.fail(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
    }

    @ExceptionHandler(KnowledgeTaskConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public R<KnowledgeUploadErrorVO> handleKnowledgeTaskConflictException(KnowledgeTaskConflictException ex) {
        return R.fail(HttpStatus.CONFLICT.value(), ex.getMessage());
    }

    @ExceptionHandler({ MaxUploadSizeExceededException.class, MultipartException.class })
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public R<KnowledgeUploadErrorVO> handleMultipartException(Exception ex) {
        return R.fail(HttpStatus.PAYLOAD_TOO_LARGE.value(),
            "单文件上限为 " + formatBytes(knowledgeUploadProperties.getMaxFileSize().toBytes())
                + "，单次上传总量上限为 " + formatBytes(knowledgeUploadProperties.getMaxRequestSize().toBytes()));
    }

    private String formatBytes(long bytes) {
        if (bytes >= 1024L * 1024L * 1024L) {
            return String.format("%.0f GB", bytes / 1024d / 1024d / 1024d);
        }
        if (bytes >= 1024L * 1024L) {
            return String.format("%.0f MB", bytes / 1024d / 1024d);
        }
        if (bytes >= 1024L) {
            return String.format("%.0f KB", bytes / 1024d);
        }
        return bytes + " B";
    }
}
