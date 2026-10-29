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
 * 缓存 key 常量
 * <p>统一管理跨模块使用的 Redis / Caffeine 缓存 key 前缀，避免散落硬编码。
 * <p>命名规则：模块:业务:维度（如 sys:perm:route:v1）
 * @author JC
 */
public final class CacheConstants {

    private CacheConstants() {}

    /** 权限路由表缓存（pattern -> method -> permCode） */
    public static final String PERM_ROUTE_MAP = "sys:perm:route:v1";
    /** 用户权限码缓存（userId -> Set<permCode>） */
    public static final String USER_PERM_CODES = "sys:user:perm-codes:v1";
    /** 用户角色缓存（userId -> Set<roleCode>） */
    public static final String USER_ROLES = "sys:user:roles:v1";

    /** 缓存统一默认过期时间（秒） */
    public static final long DEFAULT_TTL_SECONDS = 300L;
}
