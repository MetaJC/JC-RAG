package com.jc.rag.admin.module.rbac.model.dto.model.dto;

/**
 * RbacUserDTO 内部传输对象
 *
 * @author JC
 */
public class RbacUserDTO {
    private Long id;
    private String username;
    private String displayName;
    private String email;
    private String phone;
    private Integer status;
    private Integer userType;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getUserType() { return userType; }
    public void setUserType(Integer userType) { this.userType = userType; }
}