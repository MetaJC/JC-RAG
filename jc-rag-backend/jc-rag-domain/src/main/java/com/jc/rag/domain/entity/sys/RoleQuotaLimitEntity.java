package com.jc.rag.domain.entity.sys;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 角色配额限制 实体。
 * <p>对应 role_quota_limits 表, 按 roleId 限定该角色下的用户配额 (每天次数/单次 Token/每日 Token/是否允许配 API Key)。
 *
 * @author JC
 */
@TableName("role_quota_limits")
public class RoleQuotaLimitEntity {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 角色 ID
     */
    @TableField("role_id")
    private Long roleId;
    /**
     * 每日调用次数上限
     */
    @TableField("quota_per_day")
    private Integer quotaPerDay;
    /**
     * 单次请求 Token 上限
     */
    @TableField("tokens_per_q")
    private Integer tokensPerQ;
    /**
     * 每日 Token 总额上限
     */
    @TableField("tokens_per_day")
    private Integer tokensPerDay;
    /**
     * 是否允许配置自有 API Key (1 允许 / 0 不允许)
     */
    @TableField("can_configure_api_key")
    private Integer canConfigureApiKey;

    public Long getId() {
        return id;
    }

    public void setId(Long v) {
        id = v;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long v) {
        roleId = v;
    }

    public Integer getQuotaPerDay() {
        return quotaPerDay;
    }

    public void setQuotaPerDay(Integer v) {
        quotaPerDay = v;
    }

    public Integer getTokensPerQ() {
        return tokensPerQ;
    }

    public void setTokensPerQ(Integer v) {
        tokensPerQ = v;
    }

    public Integer getTokensPerDay() {
        return tokensPerDay;
    }

    public void setTokensPerDay(Integer v) {
        tokensPerDay = v;
    }

    public Integer getCanConfigureApiKey() {
        return canConfigureApiKey;
    }

    public void setCanConfigureApiKey(Integer v) {
        canConfigureApiKey = v;
    }
}
