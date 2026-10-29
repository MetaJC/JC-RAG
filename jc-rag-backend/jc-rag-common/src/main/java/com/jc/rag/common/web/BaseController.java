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


package com.jc.rag.common.web;

import com.jc.rag.common.web.page.PageResponse;

/**
 * Controller 基类,封装统一返回
 * <p>业务方法严禁返回 Map / 裸 VO / 裸 List, 一律通过 ok() / okPage() / fail() 出口。
 * <p>文件下载/重定向等特殊 HTTP 语义场景, 可直接返回 ResponseEntity<byte[]> / ResponseEntity<Resource>。
 * @author JC
 */
public abstract class BaseController {
    protected <T> R<T> ok() { return R.ok(); }
    protected <T> R<T> ok(T data) { return R.ok(data); }
    protected <T> R<T> fail(int code, String message) { return R.fail(code, message); }
    protected <T> R<T> fail(String message) { return R.fail(message); }

    /** 分页结果统一包装 */
    protected <T> R<PageResponse<T>> okPage(PageResponse<T> page) { return R.ok(page); }
}
