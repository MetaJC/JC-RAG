package com.jc.rag.admin.module.knowledge.exception;

import com.jc.rag.common.enums.ResultCode;
import com.jc.rag.common.exception.BizException;

/**
 * 知识库文档上传业务异常 (400)
 * <p>支持 i18n key + 占位符参数，由 GlobalExceptionHandler 通过 MessageSource 解析。
 * @author JC
 */
public class KnowledgeUploadException extends BizException {

    private static final long serialVersionUID = 1L;

    public KnowledgeUploadException(String messageKey, Object... args) {
        super(ResultCode.PARAM_INVALID, messageKey, args);
    }
}
