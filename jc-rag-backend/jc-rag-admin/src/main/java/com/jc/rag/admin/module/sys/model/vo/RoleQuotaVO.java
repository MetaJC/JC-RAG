package com.jc.rag.admin.module.sys.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

/**
 * 角色配额 VO (admin)。
 * @author JC
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleQuotaVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long roleId;
    private String roleCode;
    private String roleName;
    private Integer quotaPerDay;
    private Integer tokensPerQ;
    private Integer tokensPerDay;
    private Integer canConfigureApiKey;

    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public Integer getQuotaPerDay() { return quotaPerDay; }
    public void setQuotaPerDay(Integer quotaPerDay) { this.quotaPerDay = quotaPerDay; }
    public Integer getTokensPerQ() { return tokensPerQ; }
    public void setTokensPerQ(Integer tokensPerQ) { this.tokensPerQ = tokensPerQ; }
    public Integer getTokensPerDay() { return tokensPerDay; }
    public void setTokensPerDay(Integer tokensPerDay) { this.tokensPerDay = tokensPerDay; }
    public Integer getCanConfigureApiKey() { return canConfigureApiKey; }
    public void setCanConfigureApiKey(Integer canConfigureApiKey) { this.canConfigureApiKey = canConfigureApiKey; }
}
