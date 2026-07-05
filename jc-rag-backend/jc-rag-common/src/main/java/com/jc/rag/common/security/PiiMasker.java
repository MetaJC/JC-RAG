package com.jc.rag.common.security;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PII (个人敏感信息) 脱敏工具类。
 * <p>使用正则识别身份证/手机号/邮箱/银行卡, 命中后部分替换为 * 保护隐私。
 * <p>设计: 不抛异常, 不阻断流程, 仅做字符替换, 保证 LLM 仍能理解上下文。
 *
 * <p>使用示例:
 * <pre>{@code
 * String masked = PiiMasker.mask("我的身份证是 110101199001011234, 电话 13800138000");
 * // -> "我的身份证是 110101********1234, 电话 138****8000"
 * }</pre>
 *
 * @author JC
 */
public final class PiiMasker {

    private PiiMasker() {}

    /** 身份证: 17 位数字 + 1 位数字或 X/x */
    private static final Pattern ID_CARD = Pattern.compile("\\b\\d{17}[\\dXx]\\b");
    /** 手机号: 11 位 1[3-9] 开头 */
    private static final Pattern MOBILE = Pattern.compile("\\b1[3-9]\\d{9}\\b");
    /** 邮箱 */
    private static final Pattern EMAIL = Pattern.compile("\\b[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}\\b");
    /** 银行卡: 16~19 位连续数字 */
    private static final Pattern BANK_CARD = Pattern.compile("\\b\\d{16,19}\\b");

    /**
     * 对文本中的 PII 进行脱敏。
     *
     * @param text 原始文本 (可为 null)
     * @return 脱敏后文本; 入参 null 时返回 null
     */
    public static String mask(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String result = text;
        result = maskIdCard(result);
        result = maskMobile(result);
        result = maskEmail(result);
        result = maskBankCard(result);
        return result;
    }

    /** 身份证: 保留前 6 位 + 后 4 位, 中间 8 位用 * 替换 */
    private static String maskIdCard(String s) {
        Matcher m = ID_CARD.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String id = m.group();
            String masked = id.substring(0, 6) + "********" + id.substring(14);
            m.appendReplacement(sb, Matcher.quoteReplacement(masked));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** 手机号: 保留前 3 位 + 后 4 位, 中间 4 位用 * 替换 */
    private static String maskMobile(String s) {
        Matcher m = MOBILE.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String mobile = m.group();
            String masked = mobile.substring(0, 3) + "****" + mobile.substring(7);
            m.appendReplacement(sb, Matcher.quoteReplacement(masked));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** 邮箱: 保留首字符 + @ + 域名, 中间用 *** 替换 */
    private static String maskEmail(String s) {
        Matcher m = EMAIL.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String email = m.group();
            int atIdx = email.indexOf('@');
            if (atIdx <= 1) {
                // 邮箱名太短 (≤1 字符), 整体保留不脱敏
                continue;
            }
            String masked = email.charAt(0) + "***" + email.substring(atIdx);
            m.appendReplacement(sb, Matcher.quoteReplacement(masked));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** 银行卡: 保留前 4 位 + 后 4 位, 中间用 * 替换 */
    private static String maskBankCard(String s) {
        Matcher m = BANK_CARD.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String card = m.group();
            // 避免与身份证 / 手机号重复脱敏
            if (ID_CARD.matcher(card).matches() || MOBILE.matcher(card).matches()) {
                continue;
            }
            String masked = card.substring(0, 4) + repeat('*', card.length() - 8) + card.substring(card.length() - 4);
            m.appendReplacement(sb, Matcher.quoteReplacement(masked));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String repeat(char c, int n) {
        char[] arr = new char[n];
        java.util.Arrays.fill(arr, c);
        return new String(arr);
    }
}
