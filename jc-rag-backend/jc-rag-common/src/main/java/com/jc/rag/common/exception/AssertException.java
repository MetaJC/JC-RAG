package com.jc.rag.common.exception;

import com.jc.rag.common.enums.ResultCode;

/**
 * 断言式业务异常。
 * <p>使用示例（推荐）：
 * <pre>
 *   throw new AssertException("10100", "kb.not.found", kbId);
 *   throw new AssertException(ResultCode.PARAM_INVALID, "param.not.blank", "kbName");
 * </pre>
 * <p>code 给程序用，messageKey 在 i18n 资源文件中定义，args 为 {0}{1} 占位符参数。
 * <p>由 {@link com.jc.rag.admin.common.exception.GlobalExceptionHandler} 通过 MessageSource 解析后返回。
 *
 * @author JC
 */
public class AssertException extends BizException {

    private static final long serialVersionUID = 1L;

    /**
     * @param code 业务码
     * @param messageKey i18n 资源 key
     * @param args 占位符参数
     */
    public AssertException(int code, String messageKey, Object... args) {
        super(code, messageKey, args);
    }

    /**
     * @param rc 业务码枚举
     * @param messageKey i18n 资源 key
     * @param args 占位符参数
     */
    public AssertException(ResultCode rc, String messageKey, Object... args) {
        super(rc, messageKey, args);
    }
}
