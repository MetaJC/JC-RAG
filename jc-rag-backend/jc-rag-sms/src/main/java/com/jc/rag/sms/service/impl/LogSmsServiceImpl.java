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
