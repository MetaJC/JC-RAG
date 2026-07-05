package com.jc.rag.admin.module.rbac.model.param;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户状态切换参数
 * @author JC
 */
@Data
public class RbacUserStatusParam {

    @NotNull(message = "状态不能为空")
    private Integer status;
}
