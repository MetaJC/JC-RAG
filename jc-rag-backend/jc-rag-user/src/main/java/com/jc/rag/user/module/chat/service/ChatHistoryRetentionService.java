package com.jc.rag.user.module.chat.service;

import com.jc.rag.domain.mapper.chat.ChatMessageMapper;
import com.jc.rag.domain.mapper.chat.ChatSessionMapper;
import com.jc.rag.domain.mapper.chat.ChatSessionSummaryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * L0 聊天历史保留 (retention) 后台任务。
 * <p>按 {@code jc-rag.memory.l0.retention-days} 配置, 物理删除早于该天数 (默认 90) 的:
 * <ul>
 *   <li>chat_message (L0 原文)</li>
 *   <li>chat_session (L0 会话)</li>
 *   <li>chat_session_summary (L1 摘要)</li>
 * </ul>
 * <p>执行时机: 每天凌晨 3:00 (与 L1 摘要压缩避开), 顺序: 摘要 → 消息 → 会话.
 * <p>关闭策略: 设置 retention-days = 0 或负数即关闭 (不删).
 * <p>与 forgetMe 的关系: forgetMe 是"用户主动一键清空", retention 是"平台级时间窗自动清理", 两者互补不冲突.
 *
 * @author JC
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatHistoryRetentionService {

    private final ChatMessageMapper chatMessageMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatSessionSummaryMapper chatSessionSummaryMapper;

    /** 保留天数; <= 0 表示关闭 retention. */
    @Value("${jc-rag.memory.l0.retention-days:90}")
    private int retentionDays;

    /**
     * 定时任务: 每天凌晨 3:00 执行一次.
     * <p>cron 解释: 秒 分 时 日 月 周; 0 0 3 * * ? = 每天 03:00:00.
     * <p>失败仅记日志, 不抛异常, 避免污染调度器.
     */
    @Scheduled(cron = "${jc-rag.memory.l0.retention-cron:0 0 3 * * ?}")
    public void scheduledRetention() {
        if (retentionDays <= 0) {
            log.info("[L0 retention] disabled (retention-days={}), skip", retentionDays);
            return;
        }
        try {
            RetentionStat stat = cleanup(retentionDays);
            log.info("[L0 retention] done: days={}, messages={}, sessions={}, summaries={}, total={}",
                retentionDays, stat.messages, stat.sessions, stat.summaries, stat.total());
        } catch (Exception e) {
            log.error("[L0 retention] failed: days={}, err={}", retentionDays, e.getMessage(), e);
        }
    }

    /**
     * 手动触发: 物理删除 N 天前的 L0/L1 数据.
     * <p>单事务包裹, 失败整体回滚; 顺序: 摘要 → 消息 → 会话 (依赖关系).
     * <p>用于: 手动清理 / 测试 / 一次性历史数据瘦身.
     * @param days 保留天数 (days <= 0 直接返回 0)
     * @return 清理统计
     */
    @Transactional(rollbackFor = Exception.class)
    public RetentionStat cleanup(int days) {
        if (days <= 0) {
            return new RetentionStat(0, 0, 0);
        }
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        int summaries = chatSessionSummaryMapper.deleteByCreatedAtBefore(cutoff);
        int messages = chatMessageMapper.deleteByCreatedAtBefore(cutoff);
        int sessions = chatSessionMapper.deleteByCreatedAtBefore(cutoff);
        return new RetentionStat(messages, sessions, summaries);
    }

    /** 清理结果统计 (用于日志/返回值). */
    public record RetentionStat(int messages, int sessions, int summaries) {
        public int total() {
            return messages + sessions + summaries;
        }
    }
}
