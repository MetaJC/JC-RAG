package com.jc.rag.admin.module.feature.service.impl;

import com.jc.rag.common.feature.ProEvaluationResult;
import com.jc.rag.common.feature.ProEvaluationService;
import com.jc.rag.domain.entity.chat.UserDialogueEvaluationEntity;
import com.jc.rag.domain.mapper.chat.UserDialogueEvaluationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

/**
 * 对话评估服务 - 社区版默认实现.
 * <p>激活条件: {@code jc-rag.edition = oss} 或未配置 (默认).</p>
 * <p>社区版: 默认配额 0 次/天 (受 {@code feature.dialogue_evaluation.daily_quota} 配置控制, 但社区版不允许修改该配置为 > 0).</p>
 * <p>付费版 ({@code jc-rag.edition = pro}) 由 jc-rag-pro 模块的 ProEvaluationServiceProImpl 覆盖本 Bean, 实现真实 LLM 评估.</p>
 *
 * @author JC
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "jc-rag.edition", havingValue = "oss", matchIfMissing = true)
@ConditionalOnMissingBean(name = "proEvaluationServiceProImpl")
public class ProEvaluationServiceOssImpl implements ProEvaluationService {

    private static final String CONFIG_ENABLED = "feature.dialogue_evaluation.enabled";
    private static final String CONFIG_QUOTA = "feature.dialogue_evaluation.daily_quota";

    private final JdbcTemplate jdbcTemplate;
    private final UserDialogueEvaluationMapper evaluationMapper;

    @Override
    public ProEvaluationResult evaluate(Long userId, Long sessionId) {
        log.debug("[OSS] evaluate, userId={}, sessionId={}", userId, sessionId);
        // 社区版: 即便管理员改了 quota, 社区版也只返回占位结果, 提示升级
        return ProEvaluationResult.builder()
            .score(0)
            .comment("该功能为付费版专属 (Pro Edition), 请升级到 JC RAG Pro 解锁。")
            .strengths(Collections.emptyList())
            .improvements(Collections.emptyList())
            .modelName("oss-placeholder")
            .proEnhanced(false)
            .build();
    }

    @Override
    public int getUsedToday(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();
        return evaluationMapper.countByUserIdAndWindow(userId, from, to);
    }

    @Override
    public int getDailyQuota() {
        // 社区版强制 0, 永远不允许评估
        return 0;
    }

    @Override
    public boolean isEnabled() {
        // 社区版始终返回 true (开关由 admin 控制, 但 quota=0 走不通)
        String v = jdbcTemplate.query(
            "SELECT config_value FROM sys_config WHERE config_key = ? LIMIT 1",
            rs -> rs.next() ? rs.getString(1) : null,
            CONFIG_ENABLED
        );
        return "true".equalsIgnoreCase(v) || "1".equals(v);
    }

    @Override
    public boolean isProEnhanced() {
        return false;
    }
}
