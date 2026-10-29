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


package com.jc.rag.common.constants;

/**
 * 系统级常量
 * <p>与业务无关的全局常量统一在本类；业务专属常量（如 RBAC 操作类型）保留在各模块的常量类。
 * <p>预留用户 ID 池（1~10）用于系统保留账号，目前仅 1=developer 启用；
 * <p>未来新增预留账号（如 ops 运维）时，在 application.yml 配置 reserved-user-ids 即可，无需改代码。
 * @author JC
 */
public final class SysConstants {

    private SysConstants() {}

    /** 列表查询默认 page */
    public static final int DEFAULT_PAGE = 1;
    /** 列表查询默认 size */
    public static final int DEFAULT_SIZE = 10;
    /** 列表查询最大 size（防单次拉取过大） */
    public static final int MAX_SIZE = 200;

    /** 系统预留用户 ID 池（1~10），目前仅 1=developer 启用 */
    public static final java.util.List<Long> RESERVED_USER_IDS =
            java.util.List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
    /** developer 用户 ID（占位 1 号槽） */
    public static final long DEVELOPER_USER_ID = 1L;
    /** developer 用户名（种子数据） */
    public static final String DEVELOPER_USERNAME = "developer";

    /** 角色/权限 authority 标识 */
    public static final String AUTHORITY_DEVELOPER = "DEVELOPER";
    public static final String AUTHORITY_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String AUTHORITY_ADMIN = "ADMIN";
    public static final String AUTHORITY_USER = "USER";

    /** 跳过鉴权 authority 集合（developer 专用） */
    public static final java.util.List<String> SKIP_AUTH_AUTHORITIES =
            java.util.List.of("DEVELOPER", "ROLE_DEVELOPER");
}
