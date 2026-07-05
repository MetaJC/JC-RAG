package com.jc.rag.admin.common.enums;

import lombok.Getter;

/**
 * 状态枚举 (通用启用/禁用)
 * @author JC
 */
@Getter
public enum StatusEnum {
    DISABLED(0, "禁用"),
    ENABLED(1, "启用");

    private final int code;
    private final String description;

    StatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static StatusEnum of(Integer code) {
        if (code == null) return DISABLED;
        return code == 1 ? ENABLED : DISABLED;
    }

    public static String textOf(Integer code) {
        return of(code).description;
    }
}
