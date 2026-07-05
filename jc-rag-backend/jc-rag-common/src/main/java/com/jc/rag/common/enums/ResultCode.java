package com.jc.rag.common.enums;

/**
 * 统一业务返回码枚举
 * <p>code 与业务语义对齐，便于前端按 code 定位处理。
 * <p>DEV_GUARD(403) 专用于系统保留账号（如 developer）的保护场景。
 * @author JC
 */
public enum ResultCode {

    SUCCESS(0, "ok"),
    FAIL(500, "操作失败"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无访问权限"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "数据冲突"),
    PARAM_INVALID(400, "参数校验失败"),
    QUOTA_EXCEEDED(429, "配额已用完"),
    DEV_GUARD(403, "保留账号保护"),
    AGENT_NOT_AVAILABLE(2001, "暂无可用智能体");

    private final int code;
    private final String message;

    /**
     * @param code 业务码
     * @param message 默认提示信息
     */
    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /** @return 业务码 */
    public int getCode() { return code; }
    /** @return 默认提示信息 */
    public String getMessage() { return message; }
}
