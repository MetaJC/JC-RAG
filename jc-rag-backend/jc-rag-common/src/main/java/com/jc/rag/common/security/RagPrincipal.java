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


package com.jc.rag.common.security;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * 统一登录身份接口 (双版本共享).
 * <p>admin 端使用 DbUserDetails, user 端使用 CoreUserDetails,
 * 跨模块组件 (如商业版切面) 无法绑定具体实现类, 统一通过本接口
 * 获取用户 ID 与用户类型.
 *
 * @author JC
 */
public interface RagPrincipal extends UserDetails {

    /**
     * 获取用户 ID.
     *
     * @return 用户 ID
     */
    Long getUserId();

    /**
     * 获取用户类型 (DEVELOPER / ADMIN / NORMAL).
     *
     * @return 用户类型
     */
    String getUserType();
}
