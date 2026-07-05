package com.jc.rag.domain.entity.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;

/**
 * 用户当日配额用量实体。
 * <p>对应 user_quota_usage 表, 按 userId + usageDate 唯一, 记录当天请求次数与 Token 消耗。
 * <p>核心字段: queryCount 当日请求数, tokensUsed 当日 Token 消耗。
 *
 * @author JC
 */
@TableName("user_quota_usage")
public class UserQuotaUsageEntity {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 用户 ID
     */
    @TableField("user_id")
    private Long userId;
    /**
     * 用量统计日期 (按天分桶)
     */
    @TableField("usage_date")
    private LocalDate usageDate;
    /**
     * 当日请求次数
     */
    @TableField("query_count")
    private Integer queryCount;
    /**
     * 当日 Token 消耗总数 (prompt + completion)
     */
    @TableField("tokens_used")
    private Integer tokensUsed;

    public Long getId() {
        return id;
    }

    public void setId(Long v) {
        id = v;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long v) {
        userId = v;
    }

    public LocalDate getUsageDate() {
        return usageDate;
    }

    public void setUsageDate(LocalDate v) {
        usageDate = v;
    }

    public Integer getQueryCount() {
        return queryCount;
    }

    public void setQueryCount(Integer v) {
        queryCount = v;
    }

    public Integer getTokensUsed() {
        return tokensUsed;
    }

    public void setTokensUsed(Integer v) {
        tokensUsed = v;
    }
}
