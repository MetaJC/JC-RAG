package com.jc.rag.admin.module.rbac.model.param;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 用户分配角色请求参数。
 * <p>仅传 roleIds，不触发 RbacUserSaveParam 的非空校验。
 * <p>语义为覆盖式：传入的 roleIds 即为最终绑定关系。
 *
 * @author JC
 */
@Data
public class RbacUserAssignRoleParam {

    /** 角色 ID 列表。空集合表示清空该用户全部角色。 */
    @NotNull(message = "角色列表不能为空")
    private List<Long> roleIds;
}
