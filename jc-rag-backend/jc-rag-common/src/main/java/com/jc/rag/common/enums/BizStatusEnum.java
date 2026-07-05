package com.jc.rag.common.enums;

/**
 * 通用业务状态枚举 (操作日志 / 异常日志 / 通用成功失败标记)。
 * <p>将魔法值 "0"/"1" 统一为枚举, 与 ResultCode 区分 (ResultCode 面向 API 返回码, 本枚举面向业务状态字段)。
 * <p>注: jc-rag-common 模块未引入 lombok, 保持手写 getter。
 * @author JC
 */
public enum BizStatusEnum {

    SUCCESS("1", "成功"),
    FAIL("0", "失败");

    private final String code;
    private final String desc;

    BizStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }

    public static BizStatusEnum of(String code) {
        if (code == null) return null;
        for (BizStatusEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }
}
