package com.jc.rag.admin.module.sys.model.param;

import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

/**
 * 字典类型创建请求参数 (admin)。
 * @author JC
 */
public class DictTypeCreateParam implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "typeCode 不能为空")
    private String typeCode;

    @NotBlank(message = "typeName 不能为空")
    private String typeName;

    private String description;

    public String getTypeCode() { return typeCode; }
    public void setTypeCode(String typeCode) { this.typeCode = typeCode; }
    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
