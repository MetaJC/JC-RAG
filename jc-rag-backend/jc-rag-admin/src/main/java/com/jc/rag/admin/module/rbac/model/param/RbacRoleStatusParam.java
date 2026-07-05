package com.jc.rag.admin.module.rbac.model.param;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 角色状态切换参数
 * @author JC
 */
@Data
public class RbacRoleStatusParam {

    @NotNull(message = "状态不能为空")
    private Integer status;
}
