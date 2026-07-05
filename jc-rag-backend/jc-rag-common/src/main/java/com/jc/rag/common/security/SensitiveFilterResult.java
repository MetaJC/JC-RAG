package com.jc.rag.common.security;

import java.util.List;

/**
 * 敏感词过滤结果。
 * <p>封装一次过滤调用的全部信息 (是否通过 / 处置动作 / 命中列表 / 脱敏后内容)。
 * <p>调用方根据 result.action 决定下一步:
 * <ul>
 *   <li>DENY: 拒绝, 直接返回错误给用户</li>
 *   <li>ALERT: 放行, 但需写审计</li>
 *   <li>MASK: 用 maskedContent 替换原文后放行, 写审计</li>
 *   <li>PASS: 完全放行, 无需处理</li>
 * </ul>
 * <p>注: action 字符串与 sys_sensitive_word 表 action 字段一致 (DENY/ALERT/MASK/PASS), 不引用 entity 类以保持 common 模块零依赖。
 *
 * @author JC
 */
public final class SensitiveFilterResult {

    public static final String ACTION_DENY = "DENY";
    public static final String ACTION_ALERT = "ALERT";
    public static final String ACTION_MASK = "MASK";
    public static final String ACTION_PASS = "PASS";

    private final String original;
    private final String masked;
    private final String action;
    private final List<Hit> hits;

    public SensitiveFilterResult(String original, String masked, String action, List<Hit> hits) {
        this.original = original;
        this.masked = masked;
        this.action = action;
        this.hits = hits;
    }

    public String getOriginal() { return original; }
    public String getMasked() { return masked; }
    public String getAction() { return action; }
    public List<Hit> getHits() { return hits; }

    public boolean isDeny() { return ACTION_DENY.equals(action); }
    public boolean isAlert() { return ACTION_ALERT.equals(action); }
    public boolean isMask() { return ACTION_MASK.equals(action); }
    public boolean isPass() { return ACTION_PASS.equals(action); }

    /** 命中详情 */
    public static final class Hit {
        private final String word;
        private final String riskLevel;
        private final String category;
        private final String action;
        private final Long wordId;

        public Hit(String word, String riskLevel, String category, String action, Long wordId) {
            this.word = word;
            this.riskLevel = riskLevel;
            this.category = category;
            this.action = action;
            this.wordId = wordId;
        }

        public String getWord() { return word; }
        public String getRiskLevel() { return riskLevel; }
        public String getCategory() { return category; }
        public String getAction() { return action; }
        public Long getWordId() { return wordId; }
    }
}
