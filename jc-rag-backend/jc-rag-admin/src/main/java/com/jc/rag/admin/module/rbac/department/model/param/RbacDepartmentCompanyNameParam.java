package com.jc.rag.admin.module.rbac.department.model.param;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 修改公司名称 Param。
 * <p>对应顶级部门 (parent_id=0) 的 name 字段, 仅 super_admin / developer 可调用.
 *
 * @author JC
 */
public class RbacDepartmentCompanyNameParam {

    @NotBlank(message = "公司名称不能为空")
    @Size(max = 50, message = "公司名称长度不能超过 50")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
