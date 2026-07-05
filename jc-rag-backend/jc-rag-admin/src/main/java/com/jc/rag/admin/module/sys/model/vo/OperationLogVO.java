package com.jc.rag.admin.module.sys.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jc.rag.domain.entity.sys.SysOperationLog;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志 VO
 * <p>字段命名与实体对齐, 避免 Controller 双重转换; 时间字段统一 yyyy-MM-dd HH:mm:ss (GMT+8)。
 * @author JC
 */
@Data
public class OperationLogVO {

    private Long id;
    private Long userId;
    private String username;
    private String moduleName;
    private String operationType;
    private String operationDesc;
    private String requestUri;
    private String requestMethod;
    private String requestIp;
    private String userAgent;
    private String args;
    private String result;
    private String status;
    private String errorMsg;
    private Long costMs;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    /** Entity -> VO */
    public static OperationLogVO from(SysOperationLog e) {
        if (e == null) return null;
        OperationLogVO v = new OperationLogVO();
        v.setId(e.getId());
        v.setUserId(e.getUserId());
        v.setUsername(e.getUsername());
        v.setModuleName(e.getModuleName());
        v.setOperationType(e.getOperationType());
        v.setOperationDesc(e.getOperationDesc());
        v.setRequestUri(e.getRequestUri());
        v.setRequestMethod(e.getRequestMethod());
        v.setRequestIp(e.getRequestIp());
        v.setUserAgent(e.getUserAgent());
        v.setArgs(e.getArgs());
        v.setResult(e.getResult());
        v.setStatus(e.getStatus());
        v.setErrorMsg(e.getErrorMsg());
        v.setCostMs(e.getCostMs());
        v.setCreatedAt(e.getCreatedAt());
        return v;
    }
}
