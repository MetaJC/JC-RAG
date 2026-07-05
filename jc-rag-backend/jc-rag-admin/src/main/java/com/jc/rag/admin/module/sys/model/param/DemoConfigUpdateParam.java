package com.jc.rag.admin.module.sys.model.param;

import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 演示模式配置更新 Param。
 * @author JC
 */
@Data
public class DemoConfigUpdateParam {
    private Integer demoModeEnabled;
    @Min(0)
    private Integer defaultQuotaPerDay;
    @Min(0)
    private Integer defaultTokensPerQ;
    @Min(0)
    private Integer defaultTokensPerDay;
    private String upgradeHint;
}
