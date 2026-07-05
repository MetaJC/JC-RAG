package com.jc.rag.user.module.user.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

/**
 * 用户配额检查响应 VO (C 端个人中心)。
 *
 * @author JC
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuotaCheckVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Boolean demoMode;
    private Boolean limited;
    private Boolean hasApiKey;
    private Integer queryCount;
    private Integer quotaPerDay;

    public Boolean getDemoMode() { return demoMode; }
    public void setDemoMode(Boolean demoMode) { this.demoMode = demoMode; }
    public Boolean getLimited() { return limited; }
    public void setLimited(Boolean limited) { this.limited = limited; }
    public Boolean getHasApiKey() { return hasApiKey; }
    public void setHasApiKey(Boolean hasApiKey) { this.hasApiKey = hasApiKey; }
    public Integer getQueryCount() { return queryCount; }
    public void setQueryCount(Integer queryCount) { this.queryCount = queryCount; }
    public Integer getQuotaPerDay() { return quotaPerDay; }
    public void setQuotaPerDay(Integer quotaPerDay) { this.quotaPerDay = quotaPerDay; }
}
