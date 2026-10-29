/*
 * Copyright 2024-2026 OpenJC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 本文件来源于 JC RAG Community Edition
 * 开源地址: https://gitee.com/Open-JC/jc-rag | https://github.com/MetaJC/JC-RAG
 * 二次开发请保留此出处声明。
 */


package com.jc.rag.admin.module.sys.service.impl;

import com.jc.rag.admin.module.sys.model.param.AuditLogQueryParam;
import com.jc.rag.admin.module.sys.model.vo.LogDashboardVO;
import com.jc.rag.admin.module.sys.model.vo.LogVO;
import com.jc.rag.admin.module.sys.service.AuditLogService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jc.rag.common.web.page.PageResponse;
import com.jc.rag.domain.mapper.rag.RagCallLogMapper;
import com.jc.rag.domain.mapper.sys.SysExceptionLogMapper;
import com.jc.rag.domain.mapper.sys.SysOperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 审计日志 Service 实现 (MyBatis-Plus + Mapper XML, 不再使用 JdbcTemplate)。
 * <p>RAG 调用日志 / 异常日志 / 操作日志计数全部走对应 Mapper, 避免 SQL 字符串拼装。
 * @author JC
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final RagCallLogMapper ragCallLogMapper;
    private final SysExceptionLogMapper sysExceptionLogMapper;
    private final SysOperationLogMapper sysOperationLogMapper;

    @Override
    public PageResponse<LogVO> pageRagCallLogs(AuditLogQueryParam filter) {
        AuditLogQueryParam q = filter != null ? filter : new AuditLogQueryParam();
        final String keyword = StringUtils.hasText(q.getKeyword()) ? q.getKeyword() : null;
        final String modelName = StringUtils.hasText(q.getModelName()) ? q.getModelName() : null;
        final String userName = StringUtils.hasText(q.getUserName()) ? q.getUserName() : null;
        long total = safeCount("rag-call", () -> ragCallLogMapper.countRagCallLogs(keyword, modelName, userName));
        IPage<Map<String, Object>> page = new Page<>(q.getPageNum(), q.getPageSize(), false);
        page = ragCallLogMapper.pageRagCallLogs(page, keyword, modelName, userName);
        return PageResponse.of(page.getRecords().stream().map(AuditLogServiceImpl::toLogVO).toList(),
                total, (int) page.getCurrent(), (int) page.getSize());
    }

    @Override
    public PageResponse<LogVO> pageExceptionLogs(AuditLogQueryParam filter) {
        AuditLogQueryParam q = filter != null ? filter : new AuditLogQueryParam();
        final String keyword = StringUtils.hasText(q.getKeyword()) ? q.getKeyword() : null;
        final String moduleName = StringUtils.hasText(q.getModuleName()) ? q.getModuleName() : null;
        long total = safeCount("exception", () -> sysExceptionLogMapper.countExceptionLogs(keyword, moduleName));
        IPage<Map<String, Object>> page = new Page<>(q.getPageNum(), q.getPageSize(), false);
        page = sysExceptionLogMapper.pageExceptionLogs(page, keyword, moduleName);
        List<LogVO> items = page.getRecords().stream().map(r -> {
            LogVO vo = toLogVO(r);
            Object st = r.get("stackTrace");
            if (st != null && st.toString().length() > 200) {
                vo.setStackTrace(st.toString().substring(0, 200) + "...");
            }
            return vo;
        }).toList();
        return PageResponse.of(items, total, (int) page.getCurrent(), (int) page.getSize());
    }

    @Override
    public LogDashboardVO dashboard() {
        LogDashboardVO vo = new LogDashboardVO();
        vo.setTotalRagCalls(safeCount("rag_call_log", () -> ragCallLogMapper.selectCount(null)));
        vo.setTotalExceptions(safeCount("sys_exception_log", () -> sysExceptionLogMapper.selectCount(null)));
        vo.setTotalOperations(safeCount("sys_operation_log", () -> sysOperationLogMapper.selectCount(null)));
        return vo;
    }

    private static LogVO toLogVO(Map<String, Object> r) {
        LogVO vo = new LogVO();
        vo.setId(toLong(r.get("id")));
        vo.setModuleName((String) r.get("moduleName"));
        vo.setAction((String) r.get("action"));
        vo.setUserName((String) r.get("userName"));
        // RAG 调用日志专属字段 (异常日志无此字段, NON_NULL 不输出)
        vo.setUserNickname((String) r.get("userNickname"));
        vo.setUserId(toLong(r.get("userId")));
        vo.setSessionId(toLong(r.get("sessionId")));
        vo.setSessionName((String) r.get("sessionName"));
        vo.setQueryText((String) r.get("queryText"));
        vo.setRewrittenQuery((String) r.get("rewrittenQuery"));
        vo.setRetrievedChunks(toInt(r.get("retrievedChunks")));
        vo.setModelMode((String) r.get("modelMode"));
        vo.setSuccessFlag(toInt(r.get("successFlag")));
        vo.setLatencyMs(toLong(r.get("latencyMs")));
        vo.setLatencySec(toDouble(r.get("latencySec")));
        vo.setModelName((String) r.get("modelName"));
        vo.setRequestId((String) r.get("requestId"));
        vo.setKeyword((String) r.get("keyword"));
        vo.setStatus(toInt(r.get("status")));
        vo.setDurationMs(toInt(r.get("durationMs")));
        vo.setTokensPrompt(toInt(r.get("tokensPrompt")));
        vo.setTokensCompletion(toInt(r.get("tokensCompletion")));
        vo.setErrorMessage((String) r.get("errorMessage"));
        vo.setStackTrace((String) r.get("stackTrace"));
        vo.setCreatedAt(toLocalDateTime(r.get("createdAt")));
        return vo;
    }

    private static Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        return Long.valueOf(v.toString());
    }

    private static Integer toInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return null; }
    }

    private static Double toDouble(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(v.toString()); } catch (Exception e) { return null; }
    }

    private static LocalDateTime toLocalDateTime(Object v) {
        if (v == null) return null;
        if (v instanceof LocalDateTime t) return t;
        return LocalDateTime.parse(v.toString().replace(' ', 'T'));
    }

    private long safeCount(String tag, java.util.function.Supplier<Long> supplier) {
        try {
            Long v = supplier.get();
            return v == null ? 0L : v;
        } catch (Exception e) {
            log.warn("[AuditLog] {} count failed: {}", tag, e.getMessage());
            return 0L;
        }
    }
}
