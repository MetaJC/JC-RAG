package com.jc.rag.admin.module.security.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jc.rag.admin.module.security.service.SecurityAuditLogService;
import com.jc.rag.common.web.R;
import com.jc.rag.domain.entity.sys.SecurityAuditLogEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 安全审计日志查询 Controller (Admin 端).
 * <p>审计日志由 user 模块 + Python 共同落库, Admin 端只读.
 * <p>权限 code: security:audit:list (与敏感词管理权限同级).
 *
 * @author JC
 */
@RestController
@RequestMapping("/api/admin/security/audit")
public class SecurityAuditLogController {

    private final SecurityAuditLogService service;

    @Autowired
    public SecurityAuditLogController(SecurityAuditLogService service) {
        this.service = service;
    }

    /**
     * 分页查询审计日志.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('security:audit:list')")
    public R<IPage<SecurityAuditLogEntity>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String auditType,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        return R.ok(service.page(page, size, userId, auditType, riskLevel, keyword, since));
    }

    /**
     * 首页看板: 近 24h DENY / MASK 命中数.
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('security:audit:list')")
    public R<Map<String, Object>> summary() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("deny24h", service.countDenySince(since));
        data.put("mask24h", service.countMaskSince(since));
        data.put("since", since);
        return R.ok(data);
    }
}
