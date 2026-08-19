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
