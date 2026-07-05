package com.jc.rag.common.exception;

import com.jc.rag.common.enums.ResultCode;

/**
 * 系统保留账号保护异常 (DEV_GUARD = 403)
 * <p>对 developer 等保留账号做修改/删除/角色变更时抛出。
 * <p>继承 BizException，统一由 GlobalExceptionHandler 处理。
 * @author JC
 */
public class DeveloperGuardException extends BizException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message 保护说明（如 "developer 账号为系统保留账号, 不允许删除"）
     */
    public DeveloperGuardException(String message) {
        super(ResultCode.DEV_GUARD, message);
    }
}
