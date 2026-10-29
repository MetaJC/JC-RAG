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
