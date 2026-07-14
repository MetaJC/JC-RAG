package com.jc.rag.admin.module.rbac.department.model.param;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 部门分配用户请求参数。
 * <p>语义为覆盖式: userIds 即该部门树范围内最终绑定的用户 ID 列表;
 * 未传入的解除绑定; 空集合 = 清空该部门树范围内所有用户.
 *
 * @author JC
 */
public class RbacDepartmentAssignUsersParam {

    @NotNull(message = "用户列表不能为空")
    private List<Long> userIds;

    public List<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<Long> userIds) {
        this.userIds = userIds;
    }
}
