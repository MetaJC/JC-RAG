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
