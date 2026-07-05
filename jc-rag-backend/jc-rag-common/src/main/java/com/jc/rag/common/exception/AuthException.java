package com.jc.rag.common.exception;

import com.jc.rag.common.enums.ResultCode;

/**
 * 鉴权/权限异常 (403)
 * <p>PermissionInterceptor 命中拒绝规则、@PreAuthorize 校验失败等场景抛出。
 * @author JC
 */
public class AuthException extends BizException {

    private static final long serialVersionUID = 1L;

    public AuthException(String message) {
        super(ResultCode.FORBIDDEN, message);
    }

    public AuthException(ResultCode rc, String message) {
        super(rc, message);
    }

    public AuthException(ResultCode rc, String messageKey, Object... args) {
        super(rc, messageKey, args);
    }
}
