package com.jc.rag.admin.module.rbac.model.dto.model.dto;

/**
 * RbacPermissionDTO 内部传输对象
 *
 * @author JC
 */
public class RbacPermissionDTO {
    private Long id;
    private String code;
    private String name;
    private String type;
    private Long parentId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
}