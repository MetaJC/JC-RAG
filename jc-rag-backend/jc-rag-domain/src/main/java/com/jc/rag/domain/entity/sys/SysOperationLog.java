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


package com.jc.rag.domain.entity.sys;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 系统操作日志。
 * <p>对应 sys_operation_log 表, 由 OperationLogAspect 在 Controller 方法前后写入, 记录请求参数/结果/耗时。
 *
 * @author JC
 */
@TableName("sys_operation_log")
public class SysOperationLog {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 操作用户 ID (匿名访问为 null)
     */
    private Long userId;
    /**
     * 操作用户名 (冗余存储, 防止用户改名/注销后无法回溯)
     */
    private String username;
    /**
     * 业务模块名
     */
    @TableField("module_name")
    private String moduleName;
    /**
     * 操作类型 (LOGIN / CREATE / UPDATE / DELETE / EXPORT ...)
     */
    @TableField("operation_type")
    private String operationType;
    /**
     * 操作描述 (如"创建知识库文档")
     */
    @TableField("operation_desc")
    private String operationDesc;
    /**
     * 请求 URI
     */
    @TableField("request_uri")
    private String requestUri;
    /**
     * 请求方法 (GET/POST/...)
     */
    @TableField("request_method")
    private String requestMethod;
    /**
     * 请求 IP
     */
    @TableField("request_ip")
    private String requestIp;
    /**
     * User-Agent
     */
    @TableField("user_agent")
    private String userAgent;
    /**
     * 入参 JSON
     */
    private String args;
    /**
     * 出参 JSON (大对象时可能被截断)
     */
    private String result;
    /**
     * 执行状态 (SUCCESS / FAIL)
     */
    private String status;
    /**
     * 失败时的错误信息
     */
    @TableField("error_msg")
    private String errorMsg;
    /**
     * 接口耗时 (毫秒)
     */
    @TableField("cost_ms")
    private Long costMs;
    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long v) {
        id = v;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long v) {
        userId = v;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String v) {
        username = v;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String v) {
        moduleName = v;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String v) {
        operationType = v;
    }

    public String getOperationDesc() {
        return operationDesc;
    }

    public void setOperationDesc(String v) {
        operationDesc = v;
    }

    public String getRequestUri() {
        return requestUri;
    }

    public void setRequestUri(String v) {
        requestUri = v;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String v) {
        requestMethod = v;
    }

    public String getRequestIp() {
        return requestIp;
    }

    public void setRequestIp(String v) {
        requestIp = v;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String v) {
        userAgent = v;
    }

    public String getArgs() {
        return args;
    }

    public void setArgs(String v) {
        args = v;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String v) {
        result = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        status = v;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String v) {
        errorMsg = v;
    }

    public Long getCostMs() {
        return costMs;
    }

    public void setCostMs(Long v) {
        costMs = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime v) {
        createdAt = v;
    }
}
