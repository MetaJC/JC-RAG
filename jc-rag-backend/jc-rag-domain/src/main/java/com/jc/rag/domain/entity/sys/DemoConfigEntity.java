package com.jc.rag.domain.entity.sys;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 演示模式配置 实体。
 * <p>对应 demo_config 表, 全表只有 1 行, 控制 demo 模式开关与默认配额。
 *
 * @author JC
 */
@TableName("demo_config")
public class DemoConfigEntity {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 演示模式开关 (1 启用 / 0 关闭)
     */
    @TableField("demo_mode_enabled")
    private Integer demoModeEnabled;
    /**
     * 用户默认每日调用次数
     */
    @TableField("default_quota_per_day")
    private Integer defaultQuotaPerDay;
    /**
     * 单次请求默认 Token 上限
     */
    @TableField("default_tokens_per_q")
    private Integer defaultTokensPerQ;
    /**
     * 每日默认 Token 总额
     */
    @TableField("default_tokens_per_day")
    private Integer defaultTokensPerDay;
    /**
     * 升级提示文案 (命中配额时展示)
     */
    @TableField("upgrade_hint")
    private String upgradeHint;
    /**
     * 最近修改人用户 ID
     */
    @TableField("updated_by")
    private Long updatedBy;

    public Long getId() {
        return id;
    }

    public void setId(Long v) {
        id = v;
    }

    public Integer getDemoModeEnabled() {
        return demoModeEnabled;
    }

    public void setDemoModeEnabled(Integer v) {
        demoModeEnabled = v;
    }

    public Integer getDefaultQuotaPerDay() {
        return defaultQuotaPerDay;
    }

    public void setDefaultQuotaPerDay(Integer v) {
        defaultQuotaPerDay = v;
    }

    public Integer getDefaultTokensPerQ() {
        return defaultTokensPerQ;
    }

    public void setDefaultTokensPerQ(Integer v) {
        defaultTokensPerQ = v;
    }

    public Integer getDefaultTokensPerDay() {
        return defaultTokensPerDay;
    }

    public void setDefaultTokensPerDay(Integer v) {
        defaultTokensPerDay = v;
    }

    public String getUpgradeHint() {
        return upgradeHint;
    }

    public void setUpgradeHint(String v) {
        upgradeHint = v;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long v) {
        updatedBy = v;
    }
}
