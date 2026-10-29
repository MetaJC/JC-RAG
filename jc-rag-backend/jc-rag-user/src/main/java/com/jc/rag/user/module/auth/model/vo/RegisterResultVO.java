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


package com.jc.rag.user.module.auth.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

/**
 * 注册结果 VO。
 *
 * @author JC
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegisterResultVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Boolean success;
    private String code;
    private String message;

    public static RegisterResultVO ok() {
        RegisterResultVO vo = new RegisterResultVO();
        vo.setSuccess(Boolean.TRUE);
        vo.setMessage("注册成功");
        return vo;
    }

    public static RegisterResultVO fail(String code, String message) {
        RegisterResultVO vo = new RegisterResultVO();
        vo.setSuccess(Boolean.FALSE);
        vo.setCode(code);
        vo.setMessage(message);
        return vo;
    }

    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
