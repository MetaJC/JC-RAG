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


package com.jc.rag.sms.service;

/**
 * 短信发送渠道抽象.
 * <p>实现按 jc-rag.sms.provider 配置切换: log (默认) | aliyun | tencent.
 * 实现类只负责"把验证码送达手机号", 限流/存储/校验由 {@link SmsCodeService} 统一管理.
 *
 * @author JC
 */
public interface SmsService {

    /**
     * 发送验证码短信.
     *
     * @param phone  手机号 (国内 11 位)
     * @param code   验证码明文
     * @param ttlMinutes 有效期分钟数 (用于模板文案)
     * @throws SmsSendException 渠道发送失败
     */
    void sendCode(String phone, String code, long ttlMinutes);
}
