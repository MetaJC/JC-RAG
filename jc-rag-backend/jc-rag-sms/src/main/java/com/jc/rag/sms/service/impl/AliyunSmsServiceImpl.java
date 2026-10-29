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

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.jc.rag.sms.config.SmsProperties;
import com.jc.rag.sms.service.SmsSendException;
import com.jc.rag.sms.service.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 阿里云短信渠道实现 (provider=aliyun).
 * <p>模板需包含变量 ${code}; 客户端在构造时一次性初始化, 线程安全可复用.
 *
 * @author JC
 */
public class AliyunSmsServiceImpl implements SmsService {

    private static final Logger log = LoggerFactory.getLogger(AliyunSmsServiceImpl.class);

    private final Client client;
    private final SmsProperties.Aliyun cfg;

    public AliyunSmsServiceImpl(SmsProperties properties) {
        this.cfg = properties.getAliyun();
        try {
            Config config = new Config()
                    .setAccessKeyId(cfg.getKeyId())
                    .setAccessKeySecret(cfg.getKeySecret());
            config.endpoint = cfg.getEndpoint();
            this.client = new Client(config);
        } catch (Exception e) {
            throw new SmsSendException("阿里云短信客户端初始化失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendCode(String phone, String code, long ttlMinutes) {
        try {
            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(phone)
                    .setSignName(cfg.getSign())
                    .setTemplateCode(cfg.getTemplateCode())
                    .setTemplateParam("{\"code\":\"" + code + "\",\"minutes\":\"" + ttlMinutes + "\"}");
            SendSmsResponse response = client.sendSms(request);
            String respCode = response.getBody() == null ? null : response.getBody().getCode();
            if (!"OK".equals(respCode)) {
                String msg = response.getBody() == null ? "无响应体" : response.getBody().getMessage();
                throw new SmsSendException("阿里云短信发送失败: code=" + respCode + ", message=" + msg);
            }
            log.info("[SMS] 阿里云验证码已发送: phone={}", phone);
        } catch (SmsSendException e) {
            throw e;
        } catch (Exception e) {
            throw new SmsSendException("阿里云短信发送异常: " + e.getMessage(), e);
        }
    }
}
