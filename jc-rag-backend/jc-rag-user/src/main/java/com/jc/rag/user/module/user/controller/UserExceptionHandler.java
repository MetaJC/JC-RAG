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


package com.jc.rag.user.module.user.controller;

import com.jc.rag.common.enums.ResultCode;
import com.jc.rag.common.exception.BizException;
import com.jc.rag.common.web.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * C 端全局异常处理。
 * <p>业务异常通过 MessageSource 解析 i18n 消息; 配额类业务异常用 429 状态码。
 *
 * @author JC
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.jc.rag.user")
@RequiredArgsConstructor
public class UserExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(BizException.class)
    public R<Void> handleBizException(BizException ex) {
        String msg = resolve(ex);
        if (isQuota(msg)) {
            return R.fail(ResultCode.QUOTA_EXCEEDED.getCode(), msg);
        }
        return R.fail(ex.getCode(), msg);
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleRuntime(RuntimeException ex) {
        if (ex instanceof BizException) {
            return handleBizException((BizException) ex);
        }
        log.error("user 端未捕获异常: ", ex);
        return R.fail(400, ex.getMessage());
    }

    private String resolve(BizException ex) {
        try {
            return ex.resolveMessage(messageSource, LocaleContextHolder.getLocale());
        } catch (NoSuchMessageException e) {
            return ex.getMessage();
        }
    }

    private boolean isQuota(String msg) {
        return msg != null && (msg.contains("已达限制") || msg.contains("已用完"));
    }
}
