package com.jc.rag.admin.common.enums;

import lombok.Getter;

/**
 * 用户类型枚举
 * @author JC
 */
@Getter
public enum UserTypeEnum {
    /** 普通用户：即用户端用户，不可登录后台系统 */
    NORMAL("NORMAL", "普通用户", 0),
    /** 管理用户：即后台用户，可登录后台系统，但被分为不同角色*/
    ADMIN("ADMIN", "管理用户", 1),
    /** 开发者 (系统保留) */
    DEVELOPER("DEVELOPER", "开发者", 2);

    private final String code;
    private final String description;
    private final int level;

    UserTypeEnum(String code, String description, int level) {
        this.code = code;
        this.description = description;
        this.level = level;
    }

    public static UserTypeEnum of(String code) {
        if (code == null) return NORMAL;
        for (UserTypeEnum e : values()) {
            if (e.code.equalsIgnoreCase(code)) return e;
        }
        return NORMAL;
    }

    public boolean isDeveloper() {
        return this == DEVELOPER;
    }
}
