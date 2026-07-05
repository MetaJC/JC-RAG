package com.jc.rag.admin.module.audit.controller;

import com.jc.rag.admin.module.sys.model.param.AuditLogQueryParam;
import com.jc.rag.admin.module.sys.model.vo.LogDashboardVO;
import com.jc.rag.admin.module.sys.model.vo.LogVO;
import com.jc.rag.admin.module.sys.service.AuditLogService;
import com.jc.rag.common.web.R;
import com.jc.rag.common.web.page.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 审计日志查询 Controller (兼容层)。
 * <p>逻辑已迁移到 sys/AuditLogService, 新统一入口为 /api/sys/log/*; 本 Controller 保留旧路径 /api/audit/* 以保证前端平滑过渡。
 * @deprecated 请迁移至 {@code SysLogController} (路径: /api/sys/log/*); 后续版本将移除。
 * @author JC
 */
@Deprecated
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditLogQueryController {

    private final AuditLogService auditLogService;

    @GetMapping("/rag-call-logs")
    public R<PageResponse<LogVO>> ragCallLogs(AuditLogQueryParam filter) {
        return R.ok(auditLogService.pageRagCallLogs(filter));
    }

    @GetMapping("/exception-logs")
    public R<PageResponse<LogVO>> exceptionLogs(AuditLogQueryParam filter) {
        return R.ok(auditLogService.pageExceptionLogs(filter));
    }

    @GetMapping("/dashboard")
    public R<LogDashboardVO> dashboard() {
        return R.ok(auditLogService.dashboard());
    }
}
