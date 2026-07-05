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
