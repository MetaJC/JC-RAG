package com.jc.rag.domain.enums.knowledge;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 知识库可见性范围。
 * <p>三档: PRIVATE (私有) / INTERNAL (内部) / PUBLIC (公开)。
 * <p>兼容历史值 ROLE, 解析时自动映射为 INTERNAL, 不再产生新数据。
 *
 * @author JC
 */
public enum VisibilityScopeEnum {

    PRIVATE,
    INTERNAL,
    PUBLIC,

    /** 历史值, 仅用于解析兼容, 不再作为入参或出参. */
    @Deprecated
    ROLE;

    @JsonValue
    public String getValue() {
        return name();
    }

    /**
     * 字符串转枚举, 忽略大小写, 非法返回 null.
     * <p>兼容: ROLE 自动归一为 INTERNAL.
     */
    public static VisibilityScopeEnum parse(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toUpperCase();
        if ("ROLE".equals(normalized)) return INTERNAL;
        for (VisibilityScopeEnum e : values()) {
            if (e == ROLE) continue;
            if (e.name().equals(normalized)) return e;
        }
        return null;
    }
}
