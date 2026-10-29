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


package com.jc.rag.admin.module.sys.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Dashboard 聚合数据 VO (admin)。
 * @author JC
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userCount;
    private Long roleCount;
    private Long permissionCount;
    private Long menuCount;
    private Long todayLogCount;
    private LocalDateTime serverTime;
    private String currentUsername;
    private List<RoleSummary> roles;
    private List<PermissionSummary> permissions;

    public Long getUserCount() { return userCount; }
    public void setUserCount(Long userCount) { this.userCount = userCount; }
    public Long getRoleCount() { return roleCount; }
    public void setRoleCount(Long roleCount) { this.roleCount = roleCount; }
    public Long getPermissionCount() { return permissionCount; }
    public void setPermissionCount(Long permissionCount) { this.permissionCount = permissionCount; }
    public Long getMenuCount() { return menuCount; }
    public void setMenuCount(Long menuCount) { this.menuCount = menuCount; }
    public Long getTodayLogCount() { return todayLogCount; }
    public void setTodayLogCount(Long todayLogCount) { this.todayLogCount = todayLogCount; }
    public LocalDateTime getServerTime() { return serverTime; }
    public void setServerTime(LocalDateTime serverTime) { this.serverTime = serverTime; }
    public String getCurrentUsername() { return currentUsername; }
    public void setCurrentUsername(String currentUsername) { this.currentUsername = currentUsername; }
    public List<RoleSummary> getRoles() { return roles; }
    public void setRoles(List<RoleSummary> roles) { this.roles = roles; }
    public List<PermissionSummary> getPermissions() { return permissions; }
    public void setPermissions(List<PermissionSummary> permissions) { this.permissions = permissions; }

    /** 角色摘要。 */
    public static class RoleSummary implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
        private String name;
        private String code;
        private String description;
        private Integer status;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
    }

    /** 权限摘要。 */
    public static class PermissionSummary implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
        private String code;
        private String name;
        private String permType;
        private Integer status;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPermType() { return permType; }
        public void setPermType(String permType) { this.permType = permType; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
    }
}
