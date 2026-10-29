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


package com.jc.rag.admin.common.constants;

/**
 * RBAC 模块常量
 * @author JC
 */
public final class RbacConstants {

    private RbacConstants() {}

    /** 系统保留账号用户名 (可被 yml 覆盖) */
    public static final String DEFAULT_DEVELOPER_USERNAME = "developer";

    /** 默认初始密码 (可被 yml 覆盖) */
    public static final String DEFAULT_PASSWORD = "123456";

    /** 业务日志操作类型 */
    public static final class OpType {
        private OpType() {}
        public static final String CREATE_USER = "CREATE_USER";
        public static final String UPDATE_USER = "UPDATE_USER";
        public static final String RESET_PASSWORD = "RESET_PASSWORD";
        public static final String DELETE_USER = "DELETE_USER";
        public static final String CREATE_ROLE = "CREATE_ROLE";
        public static final String UPDATE_ROLE = "UPDATE_ROLE";
        public static final String DELETE_ROLE = "DELETE_ROLE";
        public static final String CREATE_PERMISSION = "CREATE_PERMISSION";
        public static final String UPDATE_PERMISSION = "UPDATE_PERMISSION";
        public static final String DELETE_PERMISSION = "DELETE_PERMISSION";
    }
}
