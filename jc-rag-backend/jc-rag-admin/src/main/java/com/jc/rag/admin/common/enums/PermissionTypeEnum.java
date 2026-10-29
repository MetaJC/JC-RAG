/*
 * Copyright 2024-2026 OpenJC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 本文件来源于 JC RAG Community Edition
 * 开源地址: https://gitee.com/Open-JC/jc-rag | https://github.com/MetaJC/JC-RAG
 * 二次开发请保留此出处声明。
 */


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
