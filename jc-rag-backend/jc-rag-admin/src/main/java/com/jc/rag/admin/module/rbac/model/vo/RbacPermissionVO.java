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
