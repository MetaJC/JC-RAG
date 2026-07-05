package com.jc.rag.common.feature;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户端读取的"对话评估"功能状态.
 * <p>前端根据 enabled/remainingQuota 决定是否显示"评估对话"按钮.</p>
 *
 * @author JC
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationStatusVO {

    /** 功能是否启用. */
    private boolean enabled;

    /** 每日配额 (0 = 不可用). */
    private int dailyQuota;

    /** 今日已用次数. */
    private int usedToday;

    /** 剩余次数. */
    private int remaining;

    /** 是否为付费增强版 (控制 UI 提示, e.g. 升级到 Pro 解锁无限). */
    private boolean proEnhanced;

    /** 提示文案 (社区版可填: "升级到 Pro 解锁"). */
    private String tip;
}
