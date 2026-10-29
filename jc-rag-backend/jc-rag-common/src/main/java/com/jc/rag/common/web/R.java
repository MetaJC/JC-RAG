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

import com.jc.rag.common.enums.ResultCode;

import java.io.Serializable;

/**
 * 统一响应包装
 * <p>业务码取自 ResultCode 枚举；success=0，fail 由具体业务决定。
 * @author JC
 */
public class R<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    private int code;
    private String message;
    private T data;

    public R() {}

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public static <T> R<T> ok() { return ok(null); }

    /**
     * @param data 业务数据
     * @return code=0, message="ok" 的成功响应
     */
    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(0);
        r.setMessage("ok");
        r.setData(data);
        return r;
    }

    /**
     * @param code 业务码
     * @param message 错误信息
     * @return 失败响应
     */
    public static <T> R<T> fail(int code, String message) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }

    /**
     * 默认 code = 500
     * @param message 错误信息
     * @return 失败响应
     */
    public static <T> R<T> fail(String message) { return fail(500, message); }

    /**
     * code + message 都取自 ResultCode
     * @param rc 业务码枚举
     * @return 失败响应
     */
    public static <T> R<T> fail(ResultCode rc) {
        return fail(rc.getCode(), rc.getMessage());
    }

    /**
     * code 取自 ResultCode，message 自定义
     * @param rc 业务码枚举
     * @param message 自定义错误信息
     * @return 失败响应
     */
    public static <T> R<T> fail(ResultCode rc, String message) {
        return fail(rc.getCode(), message);
    }
}
