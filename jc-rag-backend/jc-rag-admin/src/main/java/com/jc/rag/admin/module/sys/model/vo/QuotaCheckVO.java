package com.jc.rag.admin.module.sys.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

/**
 * 管理员视角 - 用户配额检查响应 VO。
 * <p>用于 /api/sys/quota/check, 包含比 C 端更丰富的 token 用量统计。
 * @author JC
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuotaCheckVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Boolean demoMode;
    private Boolean limited;
    private Boolean hasApiKey;
    private Integer queryCount;
    private Integer tokensUsed;
    private Integer quotaPerDay;
    private Integer tokensPerQ;
    private Integer tokensPerDay;

    public Boolean getDemoMode() { return demoMode; }
    public void setDemoMode(Boolean demoMode) { this.demoMode = demoMode; }
    public Boolean getLimited() { return limited; }
    public void setLimited(Boolean limited) { this.limited = limited; }
    public Boolean getHasApiKey() { return hasApiKey; }
    public void setHasApiKey(Boolean hasApiKey) { this.hasApiKey = hasApiKey; }
    public Integer getQueryCount() { return queryCount; }
    public void setQueryCount(Integer queryCount) { this.queryCount = queryCount; }
    public Integer getTokensUsed() { return tokensUsed; }
    public void setTokensUsed(Integer tokensUsed) { this.tokensUsed = tokensUsed; }
    public Integer getQuotaPerDay() { return quotaPerDay; }
    public void setQuotaPerDay(Integer quotaPerDay) { this.quotaPerDay = quotaPerDay; }
    public Integer getTokensPerQ() { return tokensPerQ; }
    public void setTokensPerQ(Integer tokensPerQ) { this.tokensPerQ = tokensPerQ; }
    public Integer getTokensPerDay() { return tokensPerDay; }
    public void setTokensPerDay(Integer tokensPerDay) { this.tokensPerDay = tokensPerDay; }
}
