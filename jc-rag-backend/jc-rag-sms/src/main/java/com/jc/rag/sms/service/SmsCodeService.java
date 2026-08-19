package com.jc.rag.sms.service;

import com.jc.rag.sms.enums.SmsCodeLength;
import com.jc.rag.sms.enums.SmsCodeType;

/**
 * 短信验证码生命周期服务.
 * <p>统一封装: 发送限流 (同手机号间隔可配置) → 生成验证码 (长度/字符类型枚举化, 默认 4 位纯数字)
 * → Redis 存储 (TTL 可配置) → 渠道下发 → 校验一次性消费. 调用方 (如登录模块) 无需感知渠道与存储细节.
 *
 * @author JC
 */
public interface SmsCodeService {

    /**
     * 发送验证码 (含限流检查), 默认 4 位纯数字.
     *
     * @param phone 手机号
     * @return 距离下次可发送的剩余秒数; 0 表示本次已成功发送
     */
    long sendCode(String phone);

    /**
     * 发送指定长度的纯数字验证码 (含限流检查).
     *
     * @param phone  手机号
     * @param length 验证码长度枚举 (仅支持 4/5/6 位)
     * @return 距离下次可发送的剩余秒数; 0 表示本次已成功发送
     */
    long sendCode(String phone, SmsCodeLength length);

    /**
     * 发送指定长度与字符类型的验证码 (含限流检查).
     *
     * @param phone  手机号
     * @param length 验证码长度枚举 (仅支持 4/5/6 位)
     * @param type   字符类型枚举 (纯数字 / 数字+字母)
     * @return 距离下次可发送的剩余秒数; 0 表示本次已成功发送
     */
    long sendCode(String phone, SmsCodeLength length, SmsCodeType type);

    /**
     * 校验并消费验证码 (校验成功后立即失效, 防重放).
     * <p>开发测试后门: jc-rag.sms.test-enabled=true 时, 输入 test-code 直接放行.
     *
     * @param phone 手机号
     * @param code  用户输入的验证码
     * @return 是否校验通过
     */
    boolean verifyAndConsume(String phone, String code);

    /** @return 当前发送间隔配置 (秒), 供接口回传前端做倒计时 */
    long getSendIntervalSeconds();

    /**
     * 查询指定手机号距下次可发送的剩余秒数 (不占位, 仅供预检).
     *
     * @param phone 手机号
     * @return 剩余秒数; 0 表示当前可发送
     */
    long getRemainingInterval(String phone);
}
