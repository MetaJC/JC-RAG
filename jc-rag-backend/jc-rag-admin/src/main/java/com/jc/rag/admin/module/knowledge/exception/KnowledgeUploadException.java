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


package com.jc.rag.admin.module.knowledge.exception;

import com.jc.rag.common.enums.ResultCode;
import com.jc.rag.common.exception.BizException;

/**
 * 知识库文档上传业务异常 (400)
 * <p>支持 i18n key + 占位符参数，由 GlobalExceptionHandler 通过 MessageSource 解析。
 * @author JC
 */
public class KnowledgeUploadException extends BizException {

    private static final long serialVersionUID = 1L;

    public KnowledgeUploadException(String messageKey, Object... args) {
        super(ResultCode.PARAM_INVALID, messageKey, args);
    }
}
