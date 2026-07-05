package com.jc.rag.admin.module.sys.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

/**
 * 审计日志面板概览 VO。
 * @author JC
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogDashboardVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long totalRagCalls;
    private Long totalExceptions;
    private Long totalOperations;

    public Long getTotalRagCalls() { return totalRagCalls; }
    public void setTotalRagCalls(Long totalRagCalls) { this.totalRagCalls = totalRagCalls; }
    public Long getTotalExceptions() { return totalExceptions; }
    public void setTotalExceptions(Long totalExceptions) { this.totalExceptions = totalExceptions; }
    public Long getTotalOperations() { return totalOperations; }
    public void setTotalOperations(Long totalOperations) { this.totalOperations = totalOperations; }
}
