package com.jc.rag.admin.module.security.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jc.rag.domain.entity.sys.SecurityAuditLogEntity;
import com.jc.rag.domain.mapper.sys.SecurityAuditLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 安全审计日志 Service (admin 端查询).
 * <p>读取 user / Python 端落库的 security_audit_log, 提供分页 / 过滤 / 看板统计.
 * <p>表为 admin / user / Python 三端共享, Admin 端只读查询.
 *
 * @author JC
 */
@Service
public class SecurityAuditLogService {

    @Autowired
    private SecurityAuditLogMapper mapper;

    /**
     * 分页查询审计日志.
     *
     * @param page      页码 (从 1 开始)
     * @param size      每页大小
     * @param userId    按触发用户过滤 (可选)
     * @param auditType 按审计类型过滤 (INPUT_DENY/INPUT_ALERT/INPUT_MASK/OUTPUT_DENY/OUTPUT_MASK, 可选)
     * @param riskLevel 按风险等级过滤 (HIGH/MEDIUM/LOW, 可选)
     * @param keyword   按命中词过滤 (可选)
     * @param since     仅查此时间之后 (可选)
     */
    public IPage<SecurityAuditLogEntity> page(long page, long size,
                                               Long userId, String auditType, String riskLevel,
                                               String keyword, LocalDateTime since) {
        QueryWrapper<SecurityAuditLogEntity> qw = new QueryWrapper<>();
        if (userId != null) qw.eq("user_id", userId);
        if (auditType != null && !auditType.isBlank()) qw.eq("audit_type", auditType);
        if (riskLevel != null && !riskLevel.isBlank()) qw.eq("risk_level", riskLevel);
        if (keyword != null && !keyword.isBlank()) qw.like("matched_word", keyword.trim());
        if (since != null) qw.ge("created_at", since);
        qw.orderByDesc("id");
        return mapper.selectPage(new Page<>(page, size), qw);
    }

    /** 看板统计: 近 24h DENY 命中数 */
    public long countDenySince(LocalDateTime since) {
        return mapper.countByActionSince("DENY", since);
    }

    /** 看板统计: 近 24h MASK 命中数 */
    public long countMaskSince(LocalDateTime since) {
        return mapper.countByActionSince("MASK", since);
    }
}
