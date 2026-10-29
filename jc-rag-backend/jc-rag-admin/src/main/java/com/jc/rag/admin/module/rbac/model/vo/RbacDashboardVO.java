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

import lombok.Data;

import java.util.List;

/**
 * RBAC Dashboard 聚合视图。
 * <p>前端 RbacManageView / RbacUserView / RbacRoleView 三个页面的 onMounted 都会调
 * {@code GET /api/rbac/dashboard} 拿本用户下的全部角色 + 权限, 用于"分配角色"下拉等场景.
 * <p>为减少前端分页拉全表的成本, 一次返回精简字段 (id / code / name / type), 不含冗余.
 *
 * @author JC
 */
@Data
public class RbacDashboardVO {

    /** 全量角色列表 (简化字段) */
    private List<RoleItem> roles;

    /** 全量权限列表 (简化字段) */
    private List<PermissionItem> permissions;

    /** 角色精简项 */
    @Data
    public static class RoleItem {
        private Long id;
        private String roleCode;
        private String roleName;
        private Integer status;
    }

    /** 权限精简项 */
    @Data
    public static class PermissionItem {
        private Long id;
        private String permCode;
        private String permName;
        private String permType;
        private Long parentId;
    }
}
