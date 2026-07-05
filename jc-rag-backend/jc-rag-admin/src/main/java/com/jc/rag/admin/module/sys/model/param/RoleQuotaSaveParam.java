package com.jc.rag.admin.module.sys.model.param;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 角色配额保存 Param。
 * @author JC
 */
@Data
public class RoleQuotaSaveParam {
    @NotNull
    private Long roleId;
    @Min(0)
    private Integer quotaPerDay;
    @Min(0)
    private Integer tokensPerQ;
    @Min(0)
    private Integer tokensPerDay;
    private Integer canConfigureApiKey;
}
