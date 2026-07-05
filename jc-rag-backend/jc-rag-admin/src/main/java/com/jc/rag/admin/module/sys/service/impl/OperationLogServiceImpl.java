package com.jc.rag.admin.module.sys.service.impl;

import com.jc.rag.admin.module.sys.model.param.OperationLogQueryParam;
import com.jc.rag.admin.module.sys.model.vo.OperationLogVO;
import com.jc.rag.admin.module.sys.service.OperationLogService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jc.rag.domain.entity.sys.SysOperationLog;
import com.jc.rag.domain.mapper.sys.SysOperationLogMapper;
import com.jc.rag.common.web.page.PageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 操作日志实现。
 * <p>所有写库方法均异步执行，失败仅记日志不影响主业务。
 *
 * @author JC
 */
@Slf4j
@Service
public class OperationLogServiceImpl implements OperationLogService {

    private final SysOperationLogMapper operationLogMapper;

    public OperationLogServiceImpl(SysOperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Override
    @Async
    public void saveAsync(SysOperationLog entity) {
        try {
            operationLogMapper.insert(entity);
        } catch (Exception e) {
            log.error("[OperationLog] 持久化失败: {}", e.getMessage(), e);
        }
    }

    @Override
    @Async
    public void log(String module, String action, String description, String status) {
        SysOperationLog entity = new SysOperationLog();
        entity.setModuleName(module);
        entity.setOperationType(action);
        entity.setOperationDesc(description);
        entity.setStatus(status);
        saveAsync(entity);
    }

    @Override
    public PageResponse<OperationLogVO> pageList(OperationLogQueryParam filter) {
        OperationLogQueryParam q = filter != null ? filter : new OperationLogQueryParam();
        String moduleName = q.getModuleName();
        String operationType = q.getOperationType();
        String username = q.getUsername();
        String status = q.getStatus() != null ? String.valueOf(q.getStatus()) : null;
        IPage<SysOperationLog> page = new Page<>(q.getPageNum(), q.getPageSize(), false);
        page = operationLogMapper.pageOperationLogs(page,
                blankToNull(moduleName), blankToNull(operationType), blankToNull(username),
                blankToNull(status), q.getStartTime(), q.getEndTime());
        page.setTotal(operationLogMapper.countOperationLogs(
                blankToNull(moduleName), blankToNull(operationType), blankToNull(username),
                blankToNull(status), q.getStartTime(), q.getEndTime()));
        List<OperationLogVO> voList = page.getRecords().stream().map(OperationLogVO::from).collect(Collectors.toList());
        return PageResponse.of(voList, page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
    }

    private static String blankToNull(String s) {
        return StringUtils.hasText(s) ? s : null;
    }
}
