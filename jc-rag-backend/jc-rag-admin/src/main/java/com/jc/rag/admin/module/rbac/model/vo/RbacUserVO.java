package com.jc.rag.admin.module.rbac.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jc.rag.common.model.base.BaseVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户出参 VO（前端展示用，不可作入参）。
 * <p>字段命名与前端 el-table 保持一致，避免前端再做驼峰转换。
 *
 * @author JC
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RbacUserVO extends BaseVO {
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private Integer status;
    private String statusText;
    private String userType;
    private String userTypeText;
    /**
     * 主部门 id (冗余, 真实关联见 rbac_user_department 表)
     */
    private Long departmentId;
    /**
     * 主部门名称 (用于列表展示, 来源 rbac_department.name)
     */
    private String departmentName;
    private List<Long> roleIds;
    private List<String> roleNames;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;

    public String getUsername() { return username; }
    public void setUsername(String v) { this.username = v; }
    public String getNickname() { return nickname; }
    public void setNickname(String v) { this.nickname = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { this.phone = v; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer v) { this.status = v; }
    public String getStatusText() { return statusText; }
    public void setStatusText(String v) { this.statusText = v; }
    public String getUserType() { return userType; }
    public void setUserType(String v) { this.userType = v; }
    public String getUserTypeText() { return userTypeText; }
    public void setUserTypeText(String v) { this.userTypeText = v; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long v) { this.departmentId = v; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String v) { this.departmentName = v; }
    public List<Long> getRoleIds() { return roleIds; }
    public void setRoleIds(List<Long> v) { this.roleIds = v; }
    public List<String> getRoleNames() { return roleNames; }
    public void setRoleNames(List<String> v) { this.roleNames = v; }
    public LocalDateTime getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(LocalDateTime v) { this.lastLoginTime = v; }
    public String getLastLoginIp() { return lastLoginIp; }
    public void setLastLoginIp(String v) { this.lastLoginIp = v; }
}
