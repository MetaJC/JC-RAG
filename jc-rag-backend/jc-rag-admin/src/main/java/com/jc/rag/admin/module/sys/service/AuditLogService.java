package com.jc.rag.admin.module.sys.service;

import com.jc.rag.admin.module.sys.model.param.AuditLogQueryParam;
import com.jc.rag.admin.module.sys.model.vo.LogDashboardVO;
import com.jc.rag.admin.module.sys.model.vo.LogVO;
import com.jc.rag.common.web.page.PageResponse;

/**
 * 审计日志 Service (RAG 调用日志 + 系统异常日志)。
 * @author JC
 */
public interface AuditLogService {

    /** RAG 调用日志分页查询 */
    PageResponse<LogVO> pageRagCallLogs(AuditLogQueryParam filter);

    /** 系统异常日志分页查询 */
    PageResponse<LogVO> pageExceptionLogs(AuditLogQueryParam filter);

    /** 审计面板概览 */
    LogDashboardVO dashboard();
}
