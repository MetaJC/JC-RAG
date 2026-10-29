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


package com.jc.rag.sms.enums;

/**
 * 短信验证码字符类型枚举.
 * <p>字母数字码统一大写生成; 字符集剔除易混淆字符 (0/O/1/I/L), 降低用户输错概率.
 *
 * @author JC
 */
public enum SmsCodeType {

    /** 纯数字 (0-9) */
    NUMERIC("0123456789"),

    /** 数字 + 大写字母 (剔除 0/O/1/I/L 易混淆字符) */
    ALPHANUMERIC("23456789ABCDEFGHJKMNPQRSTUVWXYZ");

    /** 默认类型: 纯数字 */
    public static final SmsCodeType DEFAULT = NUMERIC;

    private final String charset;

    SmsCodeType(String charset) {
        this.charset = charset;
    }

    public String getCharset() {
        return charset;
    }
}
