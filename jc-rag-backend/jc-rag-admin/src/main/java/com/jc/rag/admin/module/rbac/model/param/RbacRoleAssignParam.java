package com.jc.rag.admin.module.rbac.model.param;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 角色分配用户请求参数。
 * <p>语义为覆盖式：传入的 userIds 即为最终绑定关系，未传入的解除绑定。
 *
 * @author JC
 */
@Data
public class RbacRoleAssignParam {

    /** 用户 ID 列表。空集合表示清空该角色下全部用户。 */
    @NotNull(message = "用户列表不能为空")
    private List<Long> userIds;
}
