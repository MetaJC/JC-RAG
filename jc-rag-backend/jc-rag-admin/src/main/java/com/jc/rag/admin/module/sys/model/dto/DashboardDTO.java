package com.jc.rag.admin.module.sys.model.dto.model.dto;

/**
 * DashboardDTO 内部传输对象
 *
 * @author JC
 */
public class DashboardDTO {
    private long userCount;
    private long roleCount;
    private long permissionCount;
    private long activeSessionCount;

    public long getUserCount() { return userCount; }
    public void setUserCount(long userCount) { this.userCount = userCount; }
    public long getRoleCount() { return roleCount; }
    public void setRoleCount(long roleCount) { this.roleCount = roleCount; }
    public long getPermissionCount() { return permissionCount; }
    public void setPermissionCount(long permissionCount) { this.permissionCount = permissionCount; }
    public long getActiveSessionCount() { return activeSessionCount; }
    public void setActiveSessionCount(long activeSessionCount) { this.activeSessionCount = activeSessionCount; }
}