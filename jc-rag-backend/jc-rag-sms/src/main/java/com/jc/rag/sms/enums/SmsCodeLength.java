package com.jc.rag.sms.enums;

/**
 * 短信验证码长度枚举.
 * <p>调用方只能通过枚举指定长度, 禁止裸数字传参, 避免非法长度.
 *
 * @author JC
 */
public enum SmsCodeLength {

    /** 4 位验证码 */
    FOUR(4),

    /** 5 位验证码 */
    FIVE(5),

    /** 6 位验证码 */
    SIX(6);

    /** 默认长度: 4 位 */
    public static final SmsCodeLength DEFAULT = FOUR;

    private final int digits;

    SmsCodeLength(int digits) {
        this.digits = digits;
    }

    public int getDigits() {
        return digits;
    }
}
