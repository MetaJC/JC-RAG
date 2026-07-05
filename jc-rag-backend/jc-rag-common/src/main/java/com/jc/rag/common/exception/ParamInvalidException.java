package com.jc.rag.common.exception;

import com.jc.rag.common.enums.ResultCode;

/**
 * 参数校验异常 (400)
 * <p>业务层校验失败时抛出（区别于 @Valid 在 Controller 层抛出的 MethodArgumentNotValidException）。
 * @author JC
 */
public class ParamInvalidException extends BizException {

    private static final long serialVersionUID = 1L;

    public ParamInvalidException(String message) {
        super(ResultCode.PARAM_INVALID, message);
    }

    public ParamInvalidException(String messageKey, Object... args) {
        super(ResultCode.PARAM_INVALID, messageKey, args);
    }
}
