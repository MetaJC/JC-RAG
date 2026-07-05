package com.jc.rag.common.feature;

/**
 * 对话评估服务 (商业付费功能扩展点).
 * <p>社区版与付费版各自提供实现, 通过 {@code @ConditionalOnProperty(name = "jc-rag.edition", havingValue = ...)} 激活.</p>
 * <p>调用方注入此接口即可, 无需关心当前运行的是开源版还是商业版.</p>
 *
 * @author JC
 */
public interface ProEvaluationService {

    /**
     * 评估一段对话质量.
     *
     * @param userId  用户 ID
     * @param sessionId 会话 ID
     * @return 评估结果; 社区版若配额为 0 或功能未启用, 返回占位结果
     */
    ProEvaluationResult evaluate(Long userId, Long sessionId);

    /**
     * 查询用户今日已用评估次数.
     *
     * @param userId 用户 ID
     * @return 已用次数
     */
    int getUsedToday(Long userId);

    /**
     * 查询用户每日评估配额.
     *
     * @return 每日配额 (0 = 不可用)
     */
    int getDailyQuota();

    /**
     * 功能是否启用.
     *
     * @return true = 启用, false = 关闭
     */
    boolean isEnabled();

    /**
     * 是否为付费增强版.
     */
    default boolean isProEnhanced() {
        return false;
    }
}
