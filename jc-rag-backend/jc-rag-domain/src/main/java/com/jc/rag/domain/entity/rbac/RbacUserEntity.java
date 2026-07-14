package com.jc.rag.domain.entity.rbac;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 用户实体 (admin/user 共用, 统一认证源)。
 * <p>对应 rbac_user 表, passwordHash 存 BCrypt 密文, userType ADMIN / USER / GUEST, deleted 逻辑删除。
 *
 * @author JC
 */
@TableName("rbac_user")
public class RbacUserEntity {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 登录用户名 (唯一)
     */
    private String username;
    /**
     * 密码 BCrypt 密文
     */
    private String passwordHash;
    /**
     * 昵称
     */
    private String nickname;
    /**
     * 手机号
     */
    private String phone;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 账号状态 (1 启用 / 0 禁用)
     */
    private Integer status;
    /**
     * 用户类型 (ADMIN 管理端 / USER 用户端 / GUEST 访客)
     */
    private String userType;
    /**
     * 主部门 id (冗余, 真实关联见 rbac_user_department 表)
     */
    private Long departmentId;
    /**
     * 最近登录时间
     */
    private LocalDateTime lastLoginTime;
    /**
     * 最近登录 IP
     */
    private String lastLoginIp;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    /**
     * 逻辑删除标记 (0 正常 / 1 已删)
     */
    private Integer deleted;

    public Long getId() {
        return id;
    }

    public void setId(Long v) {
        id = v;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String v) {
        username = v;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String v) {
        passwordHash = v;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String v) {
        nickname = v;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String v) {
        phone = v;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String v) {
        email = v;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer v) {
        status = v;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String v) {
        userType = v;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long v) {
        departmentId = v;
    }

    public LocalDateTime getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(LocalDateTime v) {
        lastLoginTime = v;
    }

    public String getLastLoginIp() {
        return lastLoginIp;
    }

    public void setLastLoginIp(String v) {
        lastLoginIp = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime v) {
        createdAt = v;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime v) {
        updatedAt = v;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer v) {
        deleted = v;
    }
}
