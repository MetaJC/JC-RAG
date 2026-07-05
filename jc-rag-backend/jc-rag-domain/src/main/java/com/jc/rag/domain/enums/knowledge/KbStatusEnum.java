package com.jc.rag.domain.enums.knowledge;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 知识库启用状态。
 *
 * @author JC
 */
public enum KbStatusEnum {

    DISABLED(0, "禁用"),
    ENABLED(1, "启用");

    @EnumValue
    @JsonValue
    private final int code;
    private final String label;

    KbStatusEnum(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() { return code; }
    public String getLabel() { return label; }

    public static KbStatusEnum of(Integer code) {
        if (code == null) return null;
        for (KbStatusEnum e : values()) {
            if (e.code == code) return e;
        }
        return null;
    }
}
