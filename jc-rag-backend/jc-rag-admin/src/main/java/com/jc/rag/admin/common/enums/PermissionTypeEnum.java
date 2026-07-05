package com.jc.rag.admin.common.enums;

import lombok.Getter;

/**
 * 权限类型枚举
 * @author JC
 */
@Getter
public enum PermissionTypeEnum {
    /** 目录（容器型，无 page） */
    CATALOG("CATALOG", "目录"),
    /** 菜单（叶子型，有 page） */
    MENU("MENU", "菜单"),
    /** 按钮 */
    BUTTON("BUTTON", "按钮"),
    /** 接口 */
    API("API", "接口");

    private final String code;
    private final String description;

    PermissionTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static PermissionTypeEnum of(String code) {
        if (code == null) return MENU;
        for (PermissionTypeEnum e : values()) {
            if (e.code.equalsIgnoreCase(code)) return e;
        }
        return MENU;
    }

    /** 是否属于菜单族 (目录 + 菜单)，用于菜单树/侧边栏过滤 */
    public boolean isMenuLike() {
        return this == CATALOG || this == MENU;
    }
}
