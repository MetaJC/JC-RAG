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


package com.jc.rag.common.enums;

/**
 * 数据权限范围枚举
 * <p>对应 sys_role_permission.data_scope 字段，定义角色在数据层面的可见范围。
 * <p>本枚举为数据权限（AOP 注入）预留，AOP 切面实现待数据权限需求时再补。
 * <ul>
 *   <li>ALL - 全部数据（无过滤）</li>
 *   <li>SELF - 仅本人数据（userField = currentUserId）</li>
 *   <li>DEPT - 本部门数据（deptField = currentDeptId）</li>
 *   <li>CUSTOM - 自定义（按角色绑定的 dataScope 字段）</li>
 * </ul>
 * @author JC
 */
public enum DataScope {

    ALL(1, "全部"),
    SELF(2, "本人"),
    DEPT(3, "本部门"),
    CUSTOM(4, "自定义");

    private final int code;
    private final String description;

    /**
     * @param code 数据库存储值
     * @param description 中文描述
     */
    DataScope(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /** @return 数据库存储值 */
    public int getCode() { return code; }
    /** @return 中文描述 */
    public String getDescription() { return description; }
}
