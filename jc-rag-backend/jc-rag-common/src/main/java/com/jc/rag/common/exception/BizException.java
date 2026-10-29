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
import org.springframework.context.MessageSource;

import java.util.Locale;

/**
 * 业务异常基类。
 * <p>支持两种使用方式：
 * <ol>
 *   <li>直接传入已解析的 message（兼容旧用法）</li>
 *   <li>传入 i18n key + 参数占位符，由 {@link GlobalExceptionHandler} 通过 {@link MessageSource} 解析</li>
 * </ol>
 *
 * @author JC
 */
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;
    private final String messageKey;
    private final Object[] args;

    public BizException(String message) {
        super(message);
        this.code = ResultCode.FAIL.getCode();
        this.messageKey = null;
        this.args = null;
    }

    public BizException(ResultCode rc) {
        super(rc.getMessage());
        this.code = rc.getCode();
        this.messageKey = null;
        this.args = null;
    }

    public BizException(ResultCode rc, String message) {
        super(message);
        this.code = rc.getCode();
        this.messageKey = null;
        this.args = null;
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
        this.messageKey = null;
        this.args = null;
    }

    /**
     * 自定义 code + i18n key + 占位符参数
     */
    public BizException(int code, String messageKey, Object... args) {
        super(messageKey);
        this.code = code;
        this.messageKey = messageKey;
        this.args = args;
    }

    /**
     * 业务码枚举 + i18n key + 占位符参数
     */
    public BizException(ResultCode rc, String messageKey, Object... args) {
        super(messageKey);
        this.code = rc.getCode();
        this.messageKey = messageKey;
        this.args = args;
    }

    /** @return 业务码 */
    public int getCode() { return code; }
    /** @return i18n 资源 key (可为 null) */
    public String getMessageKey() { return messageKey; }
    /** @return 占位符参数 (可为 null) */
    public Object[] getArgs() { return args; }

    /**
     * 若持有 messageKey, 由 MessageSource 解析; 否则返回默认 message
     */
    public String resolveMessage(MessageSource ms, Locale locale) {
        if (messageKey == null) {
            return getMessage();
        }
        try {
            return ms.getMessage(messageKey, args, locale);
        } catch (Exception ex) {
            return messageKey;
        }
    }
}
