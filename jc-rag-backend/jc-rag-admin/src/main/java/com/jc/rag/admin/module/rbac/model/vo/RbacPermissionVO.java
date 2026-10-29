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


package com.jc.rag.admin.module.rbac.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jc.rag.common.model.base.BaseVO;

/**
 * 权限/菜单出参 VO（前端展示用）。
 * <p>字段命名与 {@link com.jc.rag.common.model.entity.SysPermissionEntity} / 前端 el-table 保持一致。
 *
 * @author JC
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RbacPermissionVO extends BaseVO {
    private String permCode;
    private String permName;
    private String permType;
    private String permTypeText;
    private Long parentId;
    private String path;
    private String method;
    private Integer status;
    private String statusText;
    private Integer sortOrder;
    private String icon;

    public String getPermCode() { return permCode; }
    public void setPermCode(String v) { this.permCode = v; }
    public String getPermName() { return permName; }
    public void setPermName(String v) { this.permName = v; }
    public String getPermType() { return permType; }
    public void setPermType(String v) { this.permType = v; }
    public String getPermTypeText() { return permTypeText; }
    public void setPermTypeText(String v) { this.permTypeText = v; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long v) { this.parentId = v; }
    public String getPath() { return path; }
    public void setPath(String v) { this.path = v; }
    public String getMethod() { return method; }
    public void setMethod(String v) { this.method = v; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer v) { this.status = v; }
    public String getStatusText() { return statusText; }
    public void setStatusText(String v) { this.statusText = v; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer v) { this.sortOrder = v; }
    public String getIcon() { return icon; }
    public void setIcon(String v) { this.icon = v; }
}
