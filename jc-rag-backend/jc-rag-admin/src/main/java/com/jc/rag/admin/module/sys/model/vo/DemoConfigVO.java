package com.jc.rag.admin.module.sys.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

/**
 * 演示模式配置 VO (admin)。
 * @author JC
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DemoConfigVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Integer demoModeEnabled;
    private Integer defaultQuotaPerDay;
    private Integer defaultTokensPerQ;
    private Integer defaultTokensPerDay;
    private String upgradeHint;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getDemoModeEnabled() { return demoModeEnabled; }
    public void setDemoModeEnabled(Integer demoModeEnabled) { this.demoModeEnabled = demoModeEnabled; }
    public Integer getDefaultQuotaPerDay() { return defaultQuotaPerDay; }
    public void setDefaultQuotaPerDay(Integer defaultQuotaPerDay) { this.defaultQuotaPerDay = defaultQuotaPerDay; }
    public Integer getDefaultTokensPerQ() { return defaultTokensPerQ; }
    public void setDefaultTokensPerQ(Integer defaultTokensPerQ) { this.defaultTokensPerQ = defaultTokensPerQ; }
    public Integer getDefaultTokensPerDay() { return defaultTokensPerDay; }
    public void setDefaultTokensPerDay(Integer defaultTokensPerDay) { this.defaultTokensPerDay = defaultTokensPerDay; }
    public String getUpgradeHint() { return upgradeHint; }
    public void setUpgradeHint(String upgradeHint) { this.upgradeHint = upgradeHint; }
}
