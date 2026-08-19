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
