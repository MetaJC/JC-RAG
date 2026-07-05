package com.jc.rag.domain.entity.sys;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 系统异常日志。
 * <p>对应 sys_exception_log 表, 由 GlobalExceptionHandler 在全局异常时自动写入, 供运维排障。
 *
 * @author JC
 */
@TableName("sys_exception_log")
public class SysExceptionLog {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 链路追踪 ID
     */
    @TableField("trace_id")
    private String traceId;
    /**
     * 发生异常的模块名
     */
    @TableField("module_name")
    private String moduleName;
    /**
     * 异常类全名
     */
    @TableField("exception_type")
    private String exceptionType;
    /**
     * 异常 message
     */
    @TableField("exception_message")
    private String exceptionMessage;
    /**
     * 异常堆栈完整文本
     */
    @TableField("stack_trace")
    private String stackTrace;
    /**
     * 操作人用户 ID (匿名访问为 null)
     */
    @TableField("operator_id")
    private Long operatorId;
    /**
     * 发生时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long v) {
        id = v;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String v) {
        traceId = v;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String v) {
        moduleName = v;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(String v) {
        exceptionType = v;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public void setExceptionMessage(String v) {
        exceptionMessage = v;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String v) {
        stackTrace = v;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long v) {
        operatorId = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime v) {
        createdAt = v;
    }
}
