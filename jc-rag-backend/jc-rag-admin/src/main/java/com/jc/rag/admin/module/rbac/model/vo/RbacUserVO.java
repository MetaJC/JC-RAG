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
    /**
     * 真实姓名 (与昵称区分, 用户列表 / 详情弹窗展示)
     */
    private String realName;
    private String email;
    private String phone;
    private Integer status;
    private String statusText;
    private String userType;
    private String userTypeText;
    /**
     * 性别 (0=未知 1=男 2=女)
     */
    private Integer gender;
    /**
     * 性别文本 (未知/男/女, 供前端展示)
     */
    private String genderText;
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
    /**
     * 岗位 ID 列表 (多对多, 来源 rbac_user_position)
     */
    private List<Long> positionIds;
    /**
     * 岗位名列表 (用于列表展示, 来源 rbac_position.name)
     */
    private List<String> positionNames;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;

    public String getUsername() { return username; }
    public void setUsername(String v) { this.username = v; }
    public String getNickname() { return nickname; }
    public void setNickname(String v) { this.nickname = v; }
    public String getRealName() { return realName; }
    public void setRealName(String v) { this.realName = v; }
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
    public Integer getGender() { return gender; }
    public void setGender(Integer v) { this.gender = v; }
    public String getGenderText() { return genderText; }
    public void setGenderText(String v) { this.genderText = v; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long v) { this.departmentId = v; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String v) { this.departmentName = v; }
    public List<Long> getRoleIds() { return roleIds; }
    public void setRoleIds(List<Long> v) { this.roleIds = v; }
    public List<String> getRoleNames() { return roleNames; }
    public void setRoleNames(List<String> v) { this.roleNames = v; }
    public List<Long> getPositionIds() { return positionIds; }
    public void setPositionIds(List<Long> v) { this.positionIds = v; }
    public List<String> getPositionNames() { return positionNames; }
    public void setPositionNames(List<String> v) { this.positionNames = v; }
    public LocalDateTime getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(LocalDateTime v) { this.lastLoginTime = v; }
    public String getLastLoginIp() { return lastLoginIp; }
    public void setLastLoginIp(String v) { this.lastLoginIp = v; }
}
