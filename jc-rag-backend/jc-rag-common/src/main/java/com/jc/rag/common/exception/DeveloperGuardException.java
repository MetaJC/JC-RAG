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


package com.jc.rag.common.exception;

import com.jc.rag.common.enums.ResultCode;

/**
 * 系统保留账号保护异常 (DEV_GUARD = 403)
 * <p>对 developer 等保留账号做修改/删除/角色变更时抛出。
 * <p>继承 BizException，统一由 GlobalExceptionHandler 处理。
 * @author JC
 */
public class DeveloperGuardException extends BizException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message 保护说明（如 "developer 账号为系统保留账号, 不允许删除"）
     */
    public DeveloperGuardException(String message) {
        super(ResultCode.DEV_GUARD, message);
    }
}
