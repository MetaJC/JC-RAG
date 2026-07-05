package com.jc.rag.admin.module.sys.service;

import com.jc.rag.admin.module.sys.model.param.OperationLogQueryParam;
import com.jc.rag.admin.module.sys.model.vo.OperationLogVO;
import com.jc.rag.domain.entity.sys.SysOperationLog;
import com.jc.rag.common.web.page.PageResponse;

/**
 * 操作日志 Service。
 * <p>供 AOP 切面及业务代码异步记录操作日志；实现位于 {@code impl/OperationLogServiceImpl}。
 *
 * @author JC
 */
public interface OperationLogService {

    /** 异步持久化单条日志（AOP 切面调用） */
    void saveAsync(SysOperationLog entity);

    /** 异步记录一条简化日志（业务代码便捷调用） */
    void log(String module, String action, String description, String status);

    /** 分页查询操作日志 (前端管理页使用) */
    PageResponse<OperationLogVO> pageList(OperationLogQueryParam filter);
}
