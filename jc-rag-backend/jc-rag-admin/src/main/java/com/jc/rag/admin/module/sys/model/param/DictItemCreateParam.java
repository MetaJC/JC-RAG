package com.jc.rag.admin.module.sys.model.param;

import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

/**
 * 字典项创建请求参数 (admin)。
 * @author JC
 */
public class DictItemCreateParam implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "typeCode 不能为空")
    private String typeCode;

    @NotBlank(message = "itemCode 不能为空")
    private String itemCode;

    @NotBlank(message = "itemName 不能为空")
    private String itemName;

    private Integer sort;

    public String getTypeCode() { return typeCode; }
    public void setTypeCode(String typeCode) { this.typeCode = typeCode; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
}
