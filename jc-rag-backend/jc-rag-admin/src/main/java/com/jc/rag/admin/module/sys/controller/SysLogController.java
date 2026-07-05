package com.jc.rag.admin.module.sys.controller;

import com.jc.rag.admin.module.sys.model.param.AuditLogQueryParam;
import com.jc.rag.admin.module.sys.model.param.OperationLogQueryParam;
import com.jc.rag.admin.module.sys.model.vo.LogDashboardVO;
import com.jc.rag.admin.module.sys.model.vo.LogVO;
import com.jc.rag.admin.module.sys.model.vo.OperationLogVO;
import com.jc.rag.admin.module.sys.service.AuditLogService;
import com.jc.rag.admin.module.sys.service.OperationLogService;
import com.jc.rag.common.web.R;
import com.jc.rag.common.web.page.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统日志统一 Controller (操作日志 + RAG 调用日志 + 系统异常日志)。
 * <p>路径: /api/sys/log, 与 rbac/rag 范式一致; 老路径 /api/audit/* 与 /api/sys/operation-log 通过兼容 Controller 保留。
 * @author JC
 */
@Slf4j
@RestController
@RequestMapping("/api/sys/log")
@RequiredArgsConstructor
public class SysLogController {

    private final OperationLogService operationLogService;
    private final AuditLogService auditLogService;

    @GetMapping("/operation")
    public R<PageResponse<OperationLogVO>> pageOperation(OperationLogQueryParam filter) {
        return R.ok(operationLogService.pageList(filter));
    }

    /** RAG 调用日志分页 */
    @GetMapping("/rag-call")
    public R<PageResponse<LogVO>> pageRagCall(AuditLogQueryParam filter) {
        return R.ok(auditLogService.pageRagCallLogs(filter));
    }

    /** 系统异常日志分页 */
    @GetMapping("/exception")
    public R<PageResponse<LogVO>> pageException(AuditLogQueryParam filter) {
        return R.ok(auditLogService.pageExceptionLogs(filter));
    }

    /** 审计面板概览 */
    @GetMapping("/dashboard")
    public R<LogDashboardVO> dashboard() {
        return R.ok(auditLogService.dashboard());
    }
}
