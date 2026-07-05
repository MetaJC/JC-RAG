package com.jc.rag.common.exception;

import com.jc.rag.common.enums.ResultCode;

/**
 * 资源不存在异常 (404)
 * <p>按 id 查询记录不存在等场景抛出。
 * @author JC
 */
public class ResourceNotFoundException extends BizException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(ResultCode.NOT_FOUND, message);
    }

    public ResourceNotFoundException(String messageKey, Object... args) {
        super(ResultCode.NOT_FOUND, messageKey, args);
    }
}
