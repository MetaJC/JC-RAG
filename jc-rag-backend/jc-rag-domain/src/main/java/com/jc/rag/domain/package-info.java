/**
 * 共享领域层。
 * <p>
 * 为 jc-rag-admin（管理端）和 jc-rag-user（用户端）提供：
 * <ul>
 *   <li>实体类（entity.{rbac|sys|chat|rag}）</li>
 *   <li>Mapper 接口（mapper.{rbac|sys|chat|rag}）</li>
 *   <li>MyBatis XML 映射文件（resources/mapper/{rbac|sys|chat|rag}）</li>
 *   <li>通用常量（constants）、枚举（enums）</li>
 * </ul>
 * <p>本模块不包含业务 Service、Controller，保证领域层纯净。
 *
 * @author JC
 */
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


package com.jc.rag.domain;
