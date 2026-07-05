package com.jc.rag.common.exception;

import com.jc.rag.common.enums.ResultCode;

/**
 * 数据冲突异常 (409)
 * <p>唯一索引冲突、状态机非法转移、并发覆盖等场景抛出。
 * @author JC
 */
public class DataConflictException extends BizException {

    private static final long serialVersionUID = 1L;

    public DataConflictException(String message) {
        super(ResultCode.CONFLICT, message);
    }

    public DataConflictException(String messageKey, Object... args) {
        super(ResultCode.CONFLICT, messageKey, args);
    }
}
