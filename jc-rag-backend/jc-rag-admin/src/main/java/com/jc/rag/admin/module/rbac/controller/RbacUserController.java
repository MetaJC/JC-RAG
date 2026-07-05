package com.jc.rag.admin.module.rbac.controller;

import com.jc.rag.admin.common.annotation.OperationLog;
import com.jc.rag.admin.module.rbac.model.param.RbacUserAssignRoleParam;
import com.jc.rag.admin.module.rbac.model.param.RbacUserQueryParam;
import com.jc.rag.admin.module.rbac.model.param.RbacUserResetPasswordParam;
import com.jc.rag.admin.module.rbac.model.param.RbacUserSaveParam;
import com.jc.rag.admin.module.rbac.model.param.RbacUserStatusParam;
import com.jc.rag.admin.module.rbac.model.vo.RbacUserVO;
import com.jc.rag.admin.module.rbac.service.RbacUserService;
import com.jc.rag.common.web.R;
import com.jc.rag.common.web.page.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理 Controller
 * @author JC
 */
@Slf4j
@RestController
@RequestMapping("/api/rbac/user")
@RequiredArgsConstructor
public class RbacUserController {

    private final RbacUserService rbacUserService;

    @GetMapping
    public R<PageResponse<RbacUserVO>> list(RbacUserQueryParam query) {
        return R.ok(rbacUserService.pageListUsers(query));
    }

    @PostMapping
    @OperationLog(module = "RBAC", action = "CREATE_USER", description = "创建用户")
    public R<Long> create(@RequestBody @Valid RbacUserSaveParam param) {
        return R.ok(rbacUserService.createUser(param));
    }

    @PutMapping("/{id}")
    @OperationLog(module = "RBAC", action = "UPDATE_USER", description = "更新用户")
    public R<Boolean> update(@PathVariable Long id, @RequestBody @Valid RbacUserSaveParam param) {
        return R.ok(rbacUserService.updateUser(id, param));
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "RBAC", action = "DELETE_USER", description = "删除用户")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(rbacUserService.deleteUser(id));
    }

    @PostMapping("/{id}/reset-password")
    @OperationLog(module = "RBAC", action = "RESET_PASSWORD", description = "重置密码")
    public R<Boolean> resetPassword(@PathVariable Long id, @RequestBody @Valid RbacUserResetPasswordParam param) {
        return R.ok(rbacUserService.resetPassword(id, param.getPassword()));
    }

    @PutMapping("/{id}/status")
    @OperationLog(module = "RBAC", action = "UPDATE_USER_STATUS", description = "切换用户状态")
    public R<Boolean> updateStatus(@PathVariable Long id, @RequestBody @Valid RbacUserStatusParam param) {
        return R.ok(rbacUserService.updateStatus(id, param.getStatus()));
    }

    /** 覆盖式为用户分配角色（仅更新角色，不触发用户基本信息校验） */
    @PutMapping("/{id}/roles")
    @OperationLog(module = "RBAC", action = "ASSIGN_USER_ROLES", description = "为用户分配角色")
    public R<Boolean> assignRoles(@PathVariable Long id, @RequestBody @Valid RbacUserAssignRoleParam param) {
        return R.ok(rbacUserService.replaceUserRoles(id, param.getRoleIds()));
    }
}
