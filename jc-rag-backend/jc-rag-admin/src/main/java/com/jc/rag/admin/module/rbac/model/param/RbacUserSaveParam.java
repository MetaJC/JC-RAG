package com.jc.rag.admin.module.rbac.model.param;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class RbacUserSaveParam {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    /**
     * 真实姓名 (与昵称区分, 用户列表 / 详情弹窗展示).
     * <p>前端必填, 后端仅校验长度; 历史数据允许空字符串.
     */
    @Size(max = 50, message = "真实姓名长度不能超过 50")
    private String realName;

    private String phone;

    private String email;

    @NotBlank(message = "用户类型不能为空")
    private String userType;

    @NotNull
    private Integer status;

    private String password;

    /**
     * 主部门 id (可空, 表示未分配主部门)
     */
    private Long departmentId;

    /**
     * 性别 (0=未知 1=男 2=女, 可空; 不传视为 0)
     */
    private Integer gender;

    /**
     * 角色 ID 列表。
     * 允许为空（新建用户不强制必须分配角色，避免误操作被校验拦截）。
     */
    private List<Long> roleIds;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public List<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<Long> roleIds) {
        this.roleIds = roleIds;
    }
}
