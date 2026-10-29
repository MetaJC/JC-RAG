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

import com.jc.rag.sms.config.SmsProperties;
import com.jc.rag.sms.service.SmsSendException;
import com.jc.rag.sms.service.SmsService;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import com.tencentcloudapi.sms.v20210111.models.SendStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 腾讯云短信渠道实现 (provider=tencent).
 * <p>模板需包含变量 {code}; 手机号按腾讯云规范补 +86 前缀.
 *
 * @author JC
 */
public class TencentSmsServiceImpl implements SmsService {

    private static final Logger log = LoggerFactory.getLogger(TencentSmsServiceImpl.class);

    private final SmsClient client;
    private final SmsProperties.Tencent cfg;

    public TencentSmsServiceImpl(SmsProperties properties) {
        this.cfg = properties.getTencent();
        Credential credential = new Credential(cfg.getKeyId(), cfg.getKeySecret());
        this.client = new SmsClient(credential, cfg.getRegion());
    }

    @Override
    public void sendCode(String phone, String code, long ttlMinutes) {
        try {
            SendSmsRequest request = new SendSmsRequest();
            request.setSmsSdkAppId(cfg.getAppId());
            request.setSignName(cfg.getSign());
            request.setTemplateId(cfg.getTemplateCode());
            request.setPhoneNumberSet(new String[]{"+86" + phone});
            request.setTemplateParamSet(new String[]{code, String.valueOf(ttlMinutes)});
            SendSmsResponse response = client.SendSms(request);
            SendStatus[] statusSet = response.getSendStatusSet();
            if (statusSet == null || statusSet.length == 0) {
                throw new SmsSendException("腾讯云短信发送失败: 无回执");
            }
            if (!"Ok".equals(statusSet[0].getCode())) {
                throw new SmsSendException("腾讯云短信发送失败: code=" + statusSet[0].getCode()
                        + ", message=" + statusSet[0].getMessage());
            }
            log.info("[SMS] 腾讯云验证码已发送: phone={}", phone);
        } catch (SmsSendException e) {
            throw e;
        } catch (Exception e) {
            throw new SmsSendException("腾讯云短信发送异常: " + e.getMessage(), e);
        }
    }
}
