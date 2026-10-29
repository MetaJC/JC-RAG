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

import java.util.List;

/**
 * 角色出参 VO（前端展示用）。
 * <p>字段命名与 {@link com.jc.rag.common.model.entity.SysRoleEntity} / 前端 el-table 保持一致。
 *
 * @author JC
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RbacRoleVO extends BaseVO {
    private String roleCode;
    private String roleName;
    private String roleDesc;
    private Integer status;
    private String statusText;
    /** 是否允许登录管理端 (0 否 / 1 是) */
    private Integer isAdmin;
    private List<Long> permissionIds;

    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String v) { this.roleCode = v; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String v) { this.roleName = v; }
    public String getRoleDesc() { return roleDesc; }
    public void setRoleDesc(String v) { this.roleDesc = v; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer v) { this.status = v; }
    public String getStatusText() { return statusText; }
    public void setStatusText(String v) { this.statusText = v; }
    public Integer getIsAdmin() { return isAdmin; }
    public void setIsAdmin(Integer v) { this.isAdmin = v; }
    public List<Long> getPermissionIds() { return permissionIds; }
    public void setPermissionIds(List<Long> v) { this.permissionIds = v; }
}
