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


package com.jc.rag.sms.service.impl;

import com.jc.rag.sms.service.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 日志演示渠道实现 (provider=log, 默认).
 * <p>不调用真实短信供应商, 验证码输出到服务端日志, 供本地开发 / 演示环境开箱即用.
 * 生产环境应通过 jc-rag.sms.provider 切换为 aliyun / tencent.
 *
 * @author JC
 */
public class LogSmsServiceImpl implements SmsService {

    private static final Logger log = LoggerFactory.getLogger(LogSmsServiceImpl.class);

    @Override
    public void sendCode(String phone, String code, long ttlMinutes) {
        log.info("[SMS-LOG] 演示渠道短信 -> phone={}, code={}, 有效期={}分钟 (未真实下发)", phone, code, ttlMinutes);
    }
}
